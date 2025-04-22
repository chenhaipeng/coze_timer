const http = require('http');

// 创建HTTP服务器
const server = http.createServer((req, res) => {
  console.log(`收到请求: ${req.method} ${req.url}`);
  
  // 记录请求头
  console.log('请求头:', JSON.stringify(req.headers, null, 2));
  
  // 处理请求体
  let body = '';
  req.on('data', chunk => {
    body += chunk.toString();
  });
  
  req.on('end', () => {
    if (body) {
      console.log('请求体:', body);
    }
    
    // 返回成功响应
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'success', message: 'Task callback received' }));
  });
});

// 监听18080端口, 使用0.0.0.0表示监听所有网络接口
const PORT = 18080;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`模拟HTTP服务器已启动，监听端口 ${PORT} (所有网络接口)`);
}); 