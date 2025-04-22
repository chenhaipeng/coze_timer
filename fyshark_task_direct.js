const http = require('http');

// 创建并直接执行任务
async function createAndExecuteTask() {
  console.log('创建并立即执行任务...');
  
  try {
    // 1. 创建任务 
    const taskId = await createTask();
    console.log(`创建任务成功，ID: ${taskId}`);
    
    // 2. 获取任务详情
    const taskInfo = await getTaskDetails(taskId);
    console.log('任务详情:', JSON.stringify(taskInfo, null, 2));
    
    // 3. 直接执行任务
    console.log('正在执行任务...');
    await executeTaskDirectly(taskInfo.httpEndpoint, taskId);
    
    console.log('任务执行完毕!');
  } catch (error) {
    console.error('处理过程中发生错误:', error);
  }
}

// 创建任务
async function createTask() {
  return new Promise((resolve, reject) => {
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
      // 使用当前时间作为开始时间，但实际上我们会手动执行而不依赖定时器
      startTime: new Date().toISOString()
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

// 直接执行HTTP请求
async function executeTaskDirectly(endpoint, taskId) {
  console.log(`向目标发送HTTP请求: ${endpoint}`);
  
  return new Promise((resolve, reject) => {
    const url = new URL(endpoint);
    const options = {
      hostname: url.hostname,
      port: url.port || 80,
      path: url.pathname + url.search,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'accept': 'application/json'
      }
    };
    
    const requestBody = JSON.stringify({
      page: 1,
      pageSize: 10,
      status: "string"
    });
    
    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        console.log(`响应状态码: ${res.statusCode}`);
        console.log('响应内容:', responseData);
        
        // 调用任务完成API
        completeTask(taskId, res.statusCode, responseData)
          .then(resolve)
          .catch(reject);
      });
    });
    
    req.on('error', (e) => {
      console.error(`请求执行错误: ${e.message}`);
      reject(e);
    });
    
    req.write(requestBody);
    req.end();
  });
}

// 标记任务为已完成
async function completeTask(taskId, statusCode, responseBody) {
  console.log(`标记任务为已完成: ${taskId}`);
  
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'localhost',
      port: 8080,
      path: `/api/v1/tasks/${taskId}/cancel`,
      method: 'PUT'
    };
    
    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        console.log('任务状态更新结果:', responseData);
        resolve();
      });
    });
    
    req.on('error', (e) => {
      reject(e);
    });
    
    req.end();
  });
}

// 执行主函数
createAndExecuteTask(); 