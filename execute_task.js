const http = require('http');
const https = require('https');

// 使用宿主机的实际IP地址
const HOST_IP = '192.168.1.9';

// 定期检查和执行任务的函数
async function checkAndExecuteTasks() {
  try {
    // 1. 获取当前时间
    const now = new Date();
    console.log(`检查待执行任务 [${now.toISOString()}]`);
    
    // 2. 创建一个任务以测试执行
    const taskId = await createTestTask();
    console.log(`创建测试任务: ${taskId}`);
    
    // 3. 等待几秒，然后手动执行任务
    await new Promise(resolve => setTimeout(resolve, 5000));
    
    // 4. 获取任务详情
    const taskDetails = await getTaskDetails(taskId);
    console.log('任务详情:', taskDetails);
    
    // 5. 如果任务还是pending状态，手动执行它
    if (taskDetails.taskStatus === 'pending') {
      console.log('任务仍处于pending状态，手动执行...');
      await executeTask(taskDetails);
    }
    
  } catch (error) {
    console.error('执行过程中发生错误:', error);
  }
}

// 创建测试任务
async function createTestTask() {
  const data = JSON.stringify({
    type: 'once',
    httpEndpoint: `http://${HOST_IP}:18080/api/callback`,
    method: 'GET',
    userId: 1,
    startTime: new Date(Date.now() + 10000).toISOString() // 10秒后执行
  });
  
  const options = {
    hostname: 'localhost',
    port: 8080,
    path: '/api/v1/tasks',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': data.length
    }
  };
  
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        try {
          const parsedData = JSON.parse(responseData);
          resolve(parsedData.taskId);
        } catch (e) {
          reject(e);
        }
      });
    });
    
    req.on('error', (e) => {
      reject(e);
    });
    
    req.write(data);
    req.end();
  });
}

// 获取任务详情
async function getTaskDetails(taskId) {
  const options = {
    hostname: 'localhost',
    port: 8080,
    path: `/api/v1/tasks/${taskId}`,
    method: 'GET'
  };
  
  return new Promise((resolve, reject) => {
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

// 手动执行任务
async function executeTask(task) {
  console.log(`正在执行任务: ${task.taskId} (${task.httpEndpoint})`);
  
  // 解析URL
  const url = new URL(task.httpEndpoint);
  
  const options = {
    hostname: url.hostname,
    port: url.port,
    path: url.pathname + url.search,
    method: task.method || 'GET'
  };
  
  const client = url.protocol === 'https:' ? https : http;
  
  return new Promise((resolve, reject) => {
    const req = client.request(options, (res) => {
      console.log(`执行结果: 状态码 ${res.statusCode}`);
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        console.log('响应内容:', responseData);
        
        // 将任务状态更新为已完成
        updateTaskStatus(task.taskId, 'completed')
          .then(() => resolve(responseData))
          .catch(reject);
      });
    });
    
    req.on('error', (e) => {
      console.error(`执行错误: ${e.message}`);
      reject(e);
    });
    
    req.end();
  });
}

// 更新任务状态
async function updateTaskStatus(taskId, status) {
  const options = {
    hostname: 'localhost',
    port: 8080,
    path: `/api/v1/tasks/${taskId}/cancel`,
    method: 'PUT'
  };
  
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        try {
          console.log(`任务 ${taskId} 状态已更新为 ${status}`);
          resolve(responseData);
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

// 手动执行一个已有任务
async function executeExistingTask(taskId) {
  try {
    // 获取任务详情
    const taskDetails = await getTaskDetails(taskId);
    console.log('已有任务详情:', taskDetails);
    
    // 执行任务
    if (taskDetails.taskStatus === 'pending') {
      console.log('任务仍处于pending状态，手动执行...');
      await executeTask(taskDetails);
    } else {
      console.log(`任务状态为 ${taskDetails.taskStatus}，不需要执行`);
    }
  } catch (error) {
    console.error('执行已有任务时发生错误:', error);
  }
}

// 检查之前创建的任务
// 如果有之前创建的任务ID可以提供，可以取消注释以下代码来执行
// executeExistingTask('你的任务ID');

// 立即执行一次
checkAndExecuteTasks();

// 每分钟检查一次
setInterval(checkAndExecuteTasks, 60000);

console.log('任务执行辅助脚本已启动...'); 