const http = require('http');

// 启动一个HTTP服务器来接收回调
function startCallbackServer() {
  return new Promise((resolve) => {
    const server = http.createServer((req, res) => {
      console.log(`收到回调请求：${req.method} ${req.url}`);
      
      let body = '';
      req.on('data', (chunk) => {
        body += chunk;
      });
      
      req.on('end', () => {
        console.log('请求体：', body);
        
        // 返回成功响应
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: 'success', message: '回调处理成功' }));
      });
    });
    
    server.listen(18080, () => {
      console.log('回调服务器已启动，监听端口：18080');
      resolve(server);
    });
  });
}

// 创建并等待定时任务执行
async function createAndWaitTask() {
  console.log('创建定时任务...');
  
  try {
    // 1. 创建定时任务，设置为5秒后执行
    const taskId = await createTimedTask();
    console.log(`创建任务成功，ID: ${taskId}`);
    
    // 2. 监控任务执行状态
    console.log('开始监控任务执行...');
    await monitorTaskExecution(taskId);
    
    console.log('任务监控完毕!');
  } catch (error) {
    console.error('处理过程中发生错误:', error);
  }
}

// 创建定时任务，设置为指定时间后执行
async function createTimedTask() {
  return new Promise((resolve, reject) => {
    // 计算5秒后的时间
    const executionTime = new Date(new Date().getTime() + 5000);
    console.log('计划执行时间:', executionTime.toISOString());
    
    // 创建任务请求体
    const taskRequest = {
      type: 'once',
      httpEndpoint: 'http://host.docker.internal:18080/callback',
      method: 'GET',
      userId: 1,
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

// 主函数
async function main() {
  // 启动回调服务器
  const server = await startCallbackServer();
  
  try {
    // 创建并监控任务
    await createAndWaitTask();
  } finally {
    // 任务执行完成后等待5秒，以确保回调处理完成
    console.log('等待回调处理完成...');
    await new Promise(resolve => setTimeout(resolve, 5000));
    
    // 关闭服务器
    server.close(() => {
      console.log('回调服务器已关闭');
    });
  }
}

// 执行主函数
main(); 