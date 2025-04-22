const http = require('http');

// 手动执行任务函数
async function manuallyExecuteTask(taskId) {
  console.log(`准备手动执行任务: ${taskId}`);
  
  // 先获取任务详情
  const taskInfo = await getTaskDetails(taskId);
  console.log('任务详情:', JSON.stringify(taskInfo, null, 2));
  
  if (taskInfo.taskStatus !== 'pending') {
    console.log(`任务状态为 ${taskInfo.taskStatus}，不需要执行`);
    return;
  }
  
  console.log('正在手动执行任务...');
  await executeTaskDirectly(taskInfo.httpEndpoint, taskInfo.taskId);
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
      console.error(`获取任务详情错误: ${e.message}`);
      reject(e);
    });
    
    req.end();
  });
}

// 直接执行HTTP请求
async function executeTaskDirectly(endpoint, taskId) {
  console.log(`直接向目标发送HTTP请求: ${endpoint}`);
  
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
  
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        console.log(`响应状态码: ${res.statusCode}`);
        console.log('响应内容:', responseData);
        
        // 更新任务状态为已完成
        updateTaskStatus(taskId, res.statusCode, responseData)
          .then(() => resolve())
          .catch(reject);
      });
    });
    
    req.on('error', (e) => {
      console.error(`请求执行错误: ${e.message}`);
      
      // 将任务状态更新为失败
      updateTaskStatus(taskId, 500, e.message)
        .then(() => reject(e))
        .catch(reject);
    });
    
    // 发送请求体
    req.write(requestBody);
    req.end();
  });
}

// 更新任务状态和记录执行结果
async function updateTaskStatus(taskId, statusCode, responseBody) {
  console.log(`更新任务状态: ${taskId}`);
  
  // 这里应该调用TaskLog API记录执行结果
  // 实际项目中需要添加这部分代码
  
  // 然后调用取消任务API将状态更新为completed
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
        try {
          console.log('任务状态更新结果:', responseData);
          resolve();
        } catch (e) {
          console.error('更新任务状态失败:', e);
          reject(e);
        }
      });
    });
    
    req.on('error', (e) => {
      console.error(`更新任务状态错误: ${e.message}`);
      reject(e);
    });
    
    req.end();
  });
}

// 主函数
async function main() {
  if (process.argv.length < 3) {
    console.log('用法: node fyshark_task_manual.js <任务ID>');
    return;
  }
  
  const taskId = process.argv[2];
  console.log(`准备处理任务ID: ${taskId}`);
  
  try {
    await manuallyExecuteTask(taskId);
    console.log('任务处理完成');
  } catch (error) {
    console.error('处理任务时发生错误:', error);
  }
}

// 执行主函数
main(); 