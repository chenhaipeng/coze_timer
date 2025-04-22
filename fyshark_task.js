const http = require('http');
const https = require('https');

// 使用宿主机的实际IP地址
const HOST_IP = '192.168.1.9';

// 创建外部API请求任务
async function createFysharkTask() {
  try {
    // 创建任务数据
    const taskData = JSON.stringify({
      type: 'once',
      httpEndpoint: 'http://test-ts-api.fyshark.com/api/rnhubtask/list',
      method: 'POST',
      userId: 1,
      headers: {
        'accept': 'application/json',
        'Content-Type': 'application/json'
      },
      requestBody: {
        page: 1,
        pageSize: 10,
        status: "string"
      },
      startTime: new Date(Date.now() + 20000).toISOString() // 20秒后执行
    });
    
    console.log('创建任务数据:', taskData);
    
    // 创建任务
    const taskId = await createTask(taskData);
    console.log(`创建fyshark API任务成功，任务ID: ${taskId}`);
    
    // 5秒后查询任务状态
    setTimeout(async () => {
      const taskInfo = await getTaskDetails(taskId);
      console.log('任务详情:', JSON.stringify(taskInfo, null, 2));
      
      // 如果任务仍处于pending状态，手动执行
      if (taskInfo.taskStatus === 'pending') {
        console.log('等待任务执行时间到达...');
        
        // 等待任务执行时间接近
        const waitTime = new Date(taskInfo.nextRunTime) - new Date() - 1000;
        if (waitTime > 0) {
          console.log(`将在${Math.floor(waitTime/1000)}秒后执行任务...`);
          setTimeout(async () => {
            // 再次检查任务状态
            const updatedTask = await getTaskDetails(taskId);
            if (updatedTask.taskStatus === 'pending') {
              console.log('时间到，开始执行任务...');
              await executeTask(updatedTask);
            } else {
              console.log(`任务状态已变为${updatedTask.taskStatus}，不需要手动执行`);
            }
          }, waitTime);
        } else {
          console.log('任务执行时间已到，立即执行...');
          await executeTask(taskInfo);
        }
      }
    }, 5000);
    
  } catch (error) {
    console.error('创建任务过程中发生错误:', error);
  }
}

// 直接向后端系统添加一个Fyshark API任务
async function addDirectFysharkTask() {
  try {
    // 创建任务请求
    console.log('直接使用curl命令添加任务...');
    await runCurlCommand();
    
  } catch (error) {
    console.error('创建任务过程中发生错误:', error);
  }
}

// 使用curl命令添加任务
async function runCurlCommand() {
  return new Promise((resolve, reject) => {
    const { spawn } = require('child_process');
    
    const curlCommand = spawn('curl', [
      '-X', 'POST',
      'http://localhost:8080/api/v1/tasks',
      '-H', 'Content-Type: application/json',
      '-d', JSON.stringify({
        type: 'once',
        httpEndpoint: 'http://test-ts-api.fyshark.com/api/rnhubtask/list',
        method: 'POST',
        userId: 1,
        headers: {
          'accept': 'application/json',
          'Content-Type': 'application/json'
        },
        requestBody: {
          page: 1,
          pageSize: 10,
          status: "string"
        },
        startTime: new Date(Date.now() + 30000).toISOString() // 30秒后执行
      })
    ]);
    
    let output = '';
    let errorOutput = '';
    
    curlCommand.stdout.on('data', (data) => {
      output += data.toString();
    });
    
    curlCommand.stderr.on('data', (data) => {
      errorOutput += data.toString();
    });
    
    curlCommand.on('close', (code) => {
      if (code === 0) {
        console.log('curl命令执行成功，输出:', output);
        try {
          const response = JSON.parse(output);
          if (response.taskId) {
            console.log('成功创建任务，ID:', response.taskId);
            // 等待10秒后执行任务
            setTimeout(() => {
              executeExistingTask(response.taskId);
            }, 10000);
          }
        } catch (e) {
          console.error('解析响应失败:', e);
        }
        resolve();
      } else {
        console.error('curl命令执行失败，错误码:', code);
        console.error('错误信息:', errorOutput);
        reject(new Error(`curl命令执行失败，错误码: ${code}`));
      }
    });
  });
}

// 创建任务
async function createTask(taskData) {
  const options = {
    hostname: 'localhost',
    port: 8080,
    path: '/api/v1/tasks',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(taskData)
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
          console.log('创建任务响应:', responseData);
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
    
    req.write(taskData);
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
  const isHttps = url.protocol === 'https:';
  
  // 准备请求头
  let headers = {};
  try {
    if (task.headers) {
      if (typeof task.headers === 'string') {
        headers = JSON.parse(task.headers);
      } else {
        headers = task.headers;
      }
    }
  } catch (e) {
    console.error('解析请求头失败:', e);
  }
  
  // 准备请求体
  let body = null;
  try {
    if (task.requestBody) {
      if (typeof task.requestBody === 'string') {
        body = task.requestBody;
      } else {
        body = JSON.stringify(task.requestBody);
      }
    }
  } catch (e) {
    console.error('解析请求体失败:', e);
  }
  
  // 准备请求选项
  const options = {
    hostname: url.hostname,
    port: url.port || (isHttps ? 443 : 80),
    path: url.pathname + url.search,
    method: task.method,
    headers: {
      ...headers
    }
  };
  
  if (body) {
    options.headers['Content-Length'] = Buffer.byteLength(body);
  }
  
  console.log('请求选项:', JSON.stringify(options, null, 2));
  
  const client = isHttps ? https : http;
  
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
    
    // 发送请求体
    if (body) {
      req.write(body);
    }
    
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
    console.log('已有任务详情:', JSON.stringify(taskDetails, null, 2));
    
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

// 立即执行 - 使用curl方式添加任务
addDirectFysharkTask();
console.log('Fyshark API测试任务已启动...'); 