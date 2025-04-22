/**
 * 定时任务测试脚本
 * 测试定时任务系统在时间修复后的正常工作
 */

const http = require('http');

// 创建HTTP服务器接收回调
function startCallbackServer() {
  return new Promise((resolve) => {
    const server = http.createServer((req, res) => {
      console.log(`收到回调请求：${req.method} ${req.url}`);
      
      let body = '';
      req.on('data', (chunk) => {
        body += chunk;
      });
      
      req.on('end', () => {
        if (body) {
          console.log('请求体:', body);
        }
        
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: 'success', message: '回调处理成功' }));
      });
    });
    
    server.listen(18080, () => {
      console.log('回调服务器已启动，监听端口: 18080');
      resolve(server);
    });
  });
}

// 创建定时任务
async function createTimedTask() {
  return new Promise((resolve, reject) => {
    console.log('正在创建定时任务...');
    
    // 计算当前时间5秒后的时间
    const now = new Date();
    const executionTime = new Date(now.getTime() + 5000);
    console.log('计划执行时间:', executionTime.toISOString());
    
    // 创建任务请求体
    const taskRequest = {
      type: 'once',
      httpEndpoint: 'http://host.docker.internal:18080/callback',
      method: 'GET',
      userId: 1,
      startTime: executionTime.toISOString()
    };
    
    // 发送创建任务请求
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
    
    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        try {
          const response = JSON.parse(responseData);
          if (response.taskId) {
            console.log('任务创建成功, ID:', response.taskId);
            resolve(response.taskId);
          } else {
            console.error('创建任务失败:', responseData);
            reject(new Error('创建任务失败: ' + responseData));
          }
        } catch (e) {
          console.error('解析响应错误:', e);
          reject(e);
        }
      });
    });
    
    req.on('error', (e) => {
      console.error('请求错误:', e.message);
      reject(e);
    });
    
    req.write(requestData);
    req.end();
  });
}

// 监控任务执行状态
async function monitorTaskExecution(taskId) {
  console.log(`开始监控任务 ${taskId} 的执行状态...`);
  
  const maxChecks = 20;
  const checkInterval = 1000; // 1秒
  let checkCount = 0;
  
  return new Promise((resolve) => {
    const checkStatus = () => {
      checkCount++;
      console.log(`[${checkCount}/${maxChecks}] 检查任务状态...`);
      
      getTaskStatus(taskId).then(taskInfo => {
        if (!taskInfo) {
          console.log('获取任务信息失败');
          if (checkCount < maxChecks) {
            setTimeout(checkStatus, checkInterval);
          } else {
            console.log('达到最大检查次数，停止监控');
            resolve();
          }
          return;
        }
        
        console.log(`任务状态: ${taskInfo.taskStatus || '未知'}`);
        
        // 如果任务状态不是待处理，说明已经执行或失败
        if (taskInfo.taskStatus !== 'pending') {
          console.log('任务状态已更新!');
          console.log('详细信息:', JSON.stringify(taskInfo, null, 2));
          resolve();
          return;
        }
        
        // 继续检查
        if (checkCount < maxChecks) {
          setTimeout(checkStatus, checkInterval);
        } else {
          console.log('达到最大检查次数，停止监控');
          resolve();
        }
      }).catch(err => {
        console.error('检查状态出错:', err);
        if (checkCount < maxChecks) {
          setTimeout(checkStatus, checkInterval);
        } else {
          resolve();
        }
      });
    };
    
    // 开始第一次检查
    checkStatus();
  });
}

// 获取任务状态
async function getTaskStatus(taskId) {
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
          if (res.statusCode !== 200) {
            console.error(`请求失败，状态码: ${res.statusCode}`);
            resolve(null);
            return;
          }
          
          const parsedData = JSON.parse(responseData);
          resolve(parsedData);
        } catch (e) {
          console.error('解析任务状态出错:', e);
          reject(e);
        }
      });
    });
    
    req.on('error', (e) => {
      console.error('获取任务状态请求错误:', e.message);
      reject(e);
    });
    
    req.end();
  });
}

// 主函数
async function main() {
  // 启动回调服务器
  console.log('启动回调服务器...');
  const server = await startCallbackServer();
  
  try {
    // 查看系统当前时间
    const currentTime = new Date();
    console.log('当前系统时间:', currentTime.toString());
    
    // 创建定时任务
    const taskId = await createTimedTask();
    
    // 监控任务执行
    await monitorTaskExecution(taskId);
    
    console.log('测试完成');
  } catch (error) {
    console.error('测试过程中发生错误:', error);
  } finally {
    // 关闭回调服务器
    console.log('关闭回调服务器...');
    server.close(() => {
      console.log('回调服务器已关闭');
    });
  }
}

// 执行主函数
main().catch(console.error); 