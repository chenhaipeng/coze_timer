const http = require('http');
const mysql = require('mysql2/promise');

// 数据库连接配置
const dbConfig = {
  host: 'localhost',
  port: 13306,
  user: 'root',
  password: 'password',
  database: 'coze_timer'
};

// 创建并等待定时任务执行（在正确的时区设置下）
async function createAndWaitTask() {
  console.log('创建定时任务...');
  
  try {
    // 1. 获取当前容器内时间
    const containerTime = await getContainerTime();
    console.log('容器当前时间:', containerTime);
    
    // 2. 创建定时任务，设置为10秒后执行
    const taskId = await createTimedTask(containerTime);
    console.log(`创建任务成功，ID: ${taskId}`);
    
    // 3. 检查数据库中的任务记录
    await checkDatabaseRecord(taskId);
    
    // 4. 监控任务执行状态
    console.log('开始监控任务执行...');
    await monitorTaskExecution(taskId);
    
    console.log('任务监控完毕!');
  } catch (error) {
    console.error('处理过程中发生错误:', error);
  }
}

// 获取容器内当前时间
async function getContainerTime() {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'localhost',
      port: 8080,
      path: '/api/v1/system/time',
      method: 'GET'
    };
    
    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        try {
          // 如果API不存在，使用本地时间
          if (res.statusCode !== 200) {
            console.log('无法获取容器时间，使用本地时间');
            resolve(new Date().toISOString());
            return;
          }
          
          const parsedData = JSON.parse(responseData);
          if (parsedData.currentTime) {
            resolve(parsedData.currentTime);
          } else {
            console.log('返回格式不符合预期，使用本地时间');
            resolve(new Date().toISOString());
          }
        } catch (e) {
          console.error('解析时间数据失败:', e);
          resolve(new Date().toISOString());
        }
      });
    });
    
    req.on('error', (e) => {
      console.error(`获取容器时间错误: ${e.message}`);
      resolve(new Date().toISOString());
    });
    
    req.end();
  });
}

// 创建定时任务，设置为指定时间后执行
async function createTimedTask(currentTimeIso) {
  return new Promise((resolve, reject) => {
    // 解析当前时间
    const currentTime = new Date(currentTimeIso);
    
    // 计算10秒后的时间
    const executionTime = new Date(currentTime.getTime() + 10000);
    console.log('计划执行时间:', executionTime.toISOString());
    
    // 创建任务请求体
    const taskRequest = {
      type: 'once',
      httpEndpoint: 'http://test-ts-api.fyshark.com/api/rnhubtask/list',
      method: 'POST',
      userId: 1,
      headers: {
        'accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: {
        page: 1,
        pageSize: 10,
        status: "string"
      },
      startTime: executionTime.toISOString()
    };
    
    // 构建HTTP请求
    const requestData = JSON.stringify(taskRequest);
    const options = {
      hostname: 'localhost',
      port: 8080,
      path: '/api/v1/tasks',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(requestData)
      }
    };
    
    // 发送请求
    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        try {
          const response = JSON.parse(responseData);
          if (response.taskId) {
            resolve(response.taskId);
          } else {
            reject(new Error('创建任务失败: ' + responseData));
          }
        } catch (e) {
          reject(e);
        }
      });
    });
    
    req.on('error', (e) => {
      reject(e);
    });
    
    req.write(requestData);
    req.end();
  });
}

// 查询数据库中的任务记录
async function checkDatabaseRecord(taskId) {
  try {
    console.log('连接到MySQL数据库...');
    const connection = await mysql.createConnection(dbConfig);
    
    console.log(`查询任务ID为 ${taskId} 的记录...`);
    const [rows] = await connection.execute('SELECT * FROM tasks WHERE task_id = ?', [taskId]);
    
    if (rows.length > 0) {
      const task = rows[0];
      console.log('数据库中的任务记录:');
      console.log('----------------');
      console.log('task_id:', task.task_id);
      console.log('type:', task.type);
      console.log('http_endpoint:', task.http_endpoint);
      console.log('status:', task.status);
      console.log('start_time:', task.start_time ? task.start_time.toString() : null);
      console.log('next_run_time:', task.next_run_time ? task.next_run_time.toString() : null);
      console.log('created_at:', task.created_at ? task.created_at.toString() : null);
      console.log('updated_at:', task.updated_at ? task.updated_at.toString() : null);
      console.log('----------------');
      
      // 检查时区是否正确（中国标准时间）
      if (task.created_at) {
        const createdAtHour = task.created_at.getHours();
        console.log(`创建时间小时数: ${createdAtHour} (如果是中国时区，应该是8-23之间的值)`);
      }
    } else {
      console.log(`没有找到任务ID为 ${taskId} 的记录`);
    }
    
    await connection.end();
  } catch (error) {
    console.error('查询数据库时出错:', error);
  }
}

// 监控任务执行
async function monitorTaskExecution(taskId) {
  console.log(`开始监控任务执行: ${taskId}`);
  
  // 检查间隔(毫秒)
  const checkInterval = 1000;
  // 最大检查次数
  const maxChecks = 20;
  let checkCount = 0;
  
  return new Promise((resolve, reject) => {
    const checkStatus = () => {
      checkCount++;
      console.log(`检查任务状态 (${checkCount}/${maxChecks})...`);
      
      getTaskDetails(taskId).then(taskInfo => {
        if (!taskInfo) {
          console.log('获取任务信息失败，将在1秒后重试...');
          if (checkCount < maxChecks) {
            setTimeout(checkStatus, checkInterval);
          } else {
            console.log('达到最大检查次数，停止监控');
            resolve();
          }
          return;
        }
        
        console.log(`当前任务状态: ${taskInfo.taskStatus || '未知'}`);
        
        // 如果任务已经执行完成或失败
        if (taskInfo.taskStatus !== 'pending') {
          console.log('任务状态变更!');
          console.log('最终任务状态:', JSON.stringify(taskInfo, null, 2));
          if (taskInfo.lastExecution) {
            console.log('执行结果:', JSON.stringify(taskInfo.lastExecution, null, 2));
          }
          resolve();
          return;
        }
        
        // 如果还未执行完毕，继续检查
        if (checkCount < maxChecks) {
          setTimeout(checkStatus, checkInterval);
        } else {
          console.log('达到最大检查次数，停止监控');
          resolve();
        }
      }).catch(err => {
        console.error('检查状态时出错:', err);
        if (checkCount < maxChecks) {
          setTimeout(checkStatus, checkInterval);
        } else {
          reject(err);
        }
      });
    };
    
    // 立即开始第一次检查
    checkStatus();
  });
}

// 获取任务详情
async function getTaskDetails(taskId) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'localhost',
      port: 8080,
      path: `/api/v1/tasks/${taskId}`,
      method: 'GET'
    };
    
    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        try {
          const parsedData = JSON.parse(responseData);
          resolve(parsedData);
        } catch (e) {
          console.error('解析任务详情失败:', e);
          reject(e);
        }
      });
    });
    
    req.on('error', (e) => {
      reject(e);
    });
    
    req.end();
  });
}

// 执行主函数
createAndWaitTask(); 