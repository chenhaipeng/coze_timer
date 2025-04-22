/**
 * 时区测试脚本
 * 测试不同时区格式的任务创建
 */

const http = require('http');

// 创建HTTP回调服务器
function startCallbackServer() {
  return new Promise((resolve) => {
    const server = http.createServer((req, res) => {
      console.log(`收到回调: ${req.method} ${req.url}`);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'success' }));
    });
    
    server.listen(18080, () => {
      console.log('回调服务器已启动，监听端口: 18080');
      resolve(server);
    });
  });
}

// 创建定时任务 - 使用ISO时区格式
async function createTaskWithISOTime() {
  return new Promise((resolve, reject) => {
    console.log('创建ISO格式时间任务...');
    
    // 当前时间5秒后执行
    const now = new Date();
    const executionTime = new Date(now.getTime() + 5000);
    
    console.log('当前时间:', now.toString());
    console.log('计划执行时间 (ISO):', executionTime.toISOString());
    
    // 创建任务
    const taskRequest = {
      type: 'once',
      httpEndpoint: 'http://host.docker.internal:18080/callback',
      method: 'GET',
      userId: 1,
      startTime: executionTime.toISOString() // ISO格式带Z表示UTC时间
    };
    
    sendTaskRequest(taskRequest, resolve, reject);
  });
}

// 创建定时任务 - 使用格式化的CST时区时间字符串
async function createTaskWithLocalTime() {
  return new Promise((resolve, reject) => {
    console.log('创建本地时区格式时间任务...');
    
    // 当前时间10秒后执行
    const now = new Date();
    const executionTime = new Date(now.getTime() + 10000);
    
    // 格式化为: YYYY-MM-DD HH:MM:SS
    const formattedTime = executionTime.toISOString()
      .replace('T', ' ')
      .replace(/\.\d+Z$/, '');
    
    console.log('当前时间:', now.toString());
    console.log('计划执行时间 (格式化):', formattedTime);
    
    // 创建任务
    const taskRequest = {
      type: 'once',
      httpEndpoint: 'http://host.docker.internal:18080/callback2',
      method: 'GET',
      userId: 1,
      startTime: formattedTime // 不带时区信息的时间字符串
    };
    
    sendTaskRequest(taskRequest, resolve, reject);
  });
}

// 发送任务请求
function sendTaskRequest(taskRequest, resolve, reject) {
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
}

// 查询任务状态
async function checkTaskInDB(taskId) {
  console.log(`\n查询任务 ${taskId} 在数据库中的记录...`);
  
  return new Promise((resolve) => {
    const command = `docker exec coze_timer_mysql mysql -uroot -ppassword -e "SELECT task_id, start_time, next_run_time, created_at FROM coze_timer.tasks WHERE task_id = '${taskId}';"`;
    
    const { exec } = require('child_process');
    exec(command, (error, stdout, stderr) => {
      if (error) {
        console.error(`执行错误: ${error}`);
        resolve();
        return;
      }
      
      console.log(stdout);
      resolve();
    });
  });
}

// 主函数
async function main() {
  // 启动回调服务器
  const server = await startCallbackServer();
  
  try {
    // 创建使用不同时间格式的任务
    console.log('\n===== 测试不同时区格式的任务创建 =====');
    
    // 1. ISO格式时间
    const isoTaskId = await createTaskWithISOTime();
    await checkTaskInDB(isoTaskId);
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    // 2. 本地格式化时间
    const localTaskId = await createTaskWithLocalTime();
    await checkTaskInDB(localTaskId);
    
    // 等待足够时间让任务执行
    console.log('\n等待15秒让任务有机会执行...');
    await new Promise(resolve => setTimeout(resolve, 15000));
    
    // 检查两个任务的最终状态
    await checkTaskInDB(isoTaskId);
    await checkTaskInDB(localTaskId);
    
    console.log('\n测试完成！请查看回调服务器是否收到请求。');
    
  } catch (error) {
    console.error('测试过程中发生错误:', error);
  } finally {
    // 关闭回调服务器
    server.close(() => {
      console.log('回调服务器已关闭');
    });
  }
}

// 执行主函数
main().catch(console.error); 