const http = require('http');
const https = require('https');

// 使用宿主机的实际IP地址
const HOST_IP = '192.168.1.9';

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
    
    // 获取当前日期时间
    const now = new Date();
    // 计算5秒后的时间
    const executionTime = new Date(now.getTime() + 5000);
    console.log('当前系统时间:', now.toISOString());
    console.log('计划执行时间:', executionTime.toISOString());
    
    // 由于系统时间设置为2025年，所以我们不需要进一步修改日期
    // 任务将在5秒后执行
    
    // 创建符合TaskRequest格式的请求体
    const taskRequest = {
      type: 'once',
      httpEndpoint: 'http://test-ts-api.fyshark.com/api/rnhubtask/list',
      method: 'POST',
      userId: 1,
      headers: {
        'accept': 'application/json',
        'Content-Type': 'application/json'
      },
      // 注意：这里使用body而不是requestBody
      body: {
        page: 1,
        pageSize: 10,
        status: "string"
      },
      startTime: executionTime.toISOString() // 5秒后执行
    };
    
    console.log('请求数据:', JSON.stringify(taskRequest, null, 2));
    
    const curlCommand = spawn('curl', [
      '-X', 'POST',
      'http://localhost:8080/api/v1/tasks',
      '-H', 'Content-Type: application/json',
      '-d', JSON.stringify(taskRequest)
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
            // 立即检查任务状态
            console.log('开始监控任务执行...');
            monitorTaskExecution(response.taskId);
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

// 监控任务执行
async function monitorTaskExecution(taskId) {
  console.log(`开始监控任务执行: ${taskId}`);
  
  // 检查间隔(毫秒)
  const checkInterval = 1000;
  // 最大检查次数
  const maxChecks = 60;
  let checkCount = 0;
  
  const checkStatus = () => {
    checkCount++;
    console.log(`检查任务状态 (${checkCount}/${maxChecks})...`);
    
    getTaskDetails(taskId, (taskInfo) => {
      if (!taskInfo) {
        console.log('获取任务信息失败，将在1秒后重试...');
        if (checkCount < maxChecks) {
          setTimeout(checkStatus, checkInterval);
        } else {
          console.log('达到最大检查次数，停止监控');
        }
        return;
      }
      
      console.log(`当前任务状态: ${taskInfo.taskStatus || '未知'}`);
      
      // 如果任务已经执行完成或失败
      if (taskInfo.taskStatus === 'completed' || taskInfo.taskStatus === 'failed' || taskInfo.taskStatus === 'stopped') {
        console.log('任务执行完成!');
        console.log('最终任务状态:', JSON.stringify(taskInfo, null, 2));
        if (taskInfo.lastExecution) {
          console.log('执行结果:', JSON.stringify(taskInfo.lastExecution, null, 2));
        }
        return;
      }
      
      // 如果还未执行完毕，继续检查
      if (checkCount < maxChecks) {
        setTimeout(checkStatus, checkInterval);
      } else {
        console.log('达到最大检查次数，停止监控');
      }
    });
  };
  
  // 立即开始第一次检查
  checkStatus();
}

// 获取任务详情
async function getTaskDetails(taskId, callback) {
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
        callback(parsedData);
      } catch (e) {
        console.error('解析任务详情失败:', e);
        callback(null);
      }
    });
  });
  
  req.on('error', (e) => {
    console.error(`获取任务详情错误: ${e.message}`);
    callback(null);
  });
  
  req.end();
}

// 立即执行 - 使用curl方式添加任务
addDirectFysharkTask();
console.log('Fyshark API测试任务已启动...'); 