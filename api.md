
一、核心模块设计
1. 任务管理层  
  - 任务提交接口：提供RESTful API接收任务参数（HTTP地址、Token、请求体、间隔时间），生成全局唯一UUID作为task_id  
  - 任务存储：MySQL存储任务元数据（类型/间隔时间/中止条件），Redis缓存待调度任务队列，支持任务动态修改与撤回  
  - 任务状态机：定义pending/running/completed/failed/stopped五种状态，通过数据库事务+乐观锁保证状态一致性

2. 调度器层  
  - 时间轮算法：采用多级时间轮（分钟/秒/毫秒级）管理任务触发时序，支持秒级精度调度  
  - 分布式调度：基于Zookeeper实现节点协调，通过Redis分布式锁防止重复调度  
  - 策略引擎：支持Crontab表达式、固定间隔、一次性任务，自动计算next_run_time

3. 执行器层  
  - HTTP客户端池：集成OkHttp/Requests实现异步请求，配置连接池（默认200连接）与超时策略（5s连接/15s传输）  
  - 回调处理：通过独立线程池执行HTTP请求，失败时触发指数退避重试（默认3次）  
  - 流量控制：基于令牌桶算法限制下游请求速率，防止服务过载

二、存储层设计
-- 核心表结构
CREATE TABLE tasks (
  task_id VARCHAR(36) PRIMARY KEY,
  user_id INT NOT NULL COMMENT '用户ID',
  type ENUM('interval', 'once', 'cron'),
  http_endpoint VARCHAR(255),
  auth_token VARCHAR(512),  -- AES加密存储
  request_body TEXT,
  interval_seconds INT,
  status ENUM('pending', 'running', 'completed', 'failed', 'stopped'),
  next_run_time DATETIME,
  stop_condition TEXT  -- JSON格式中止条件
);

CREATE TABLE task_logs (
user_id INT PRIMARY KEY,
  log_id VARCHAR(36),
  task_id VARCHAR(36),
  http_status INT,
  response_body TEXT,
  created_at DATETIME
);

定时任务管理接口定义（基于RESTful规范）


---

1. 添加定时任务接口
URL  
POST /api/v1/tasks

请求头  
Authorization: Bearer {access_token}
Content-Type: application/json

请求体示例  
{
"user_id": 1001,  // 新增必填字段
  "type": "interval",          // 任务类型：interval/once/cron
  "http_endpoint": "https://api.example.com/execute",
  "method": "POST",           // HTTP方法：GET/POST
  "headers": {                // 请求头（如认证Token）
    "Authorization": "Bearer {service_token}"
  },
  "body": {                   // 请求体数据（JSON格式）
    "data": "example"
  },
  "interval_seconds": 20,      // 间隔任务专用
  "cron_expression": "0 0 12 * * ?", // 计划任务专用
  "start_time": "2025-04-22T10:00:00Z", // 首次执行时间
  "stop_condition": {         // 中止条件（如响应码或执行次数）
    "max_cnt": 300,  //最大执行数
    "finsh": true  //响应里面的某个参数值。
  }
}

响应示例  
{
  "status": "success",
  "task_id": "550e8400-e29b-41d4-a716-446655440000",
  "next_run_time": "2025-04-22T10:00:20Z"
}
参考实现：网页1、网页3、网页5中的任务提交逻辑，支持动态参数传递与任务类型区分。


---

2. 查看任务接口
URL  
GET /api/v1/tasks/{task_id}

请求头  
Authorization: Bearer {access_token}

响应示例  
{
  "task_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "running",        // pending/running/completed/failed/stopped
  "http_endpoint": "https://api.example.com/execute",
  "last_execution": {         // 最近一次执行记录
    "timestamp": "2025-04-22T10:00:00Z",
    "http_status": 200,
    "response_body": "OK"
  },
  "next_run_time": "2025-04-22T10:00:20Z",
  "created_at": "2025-04-22T09:59:00Z"
}

---

3. 取消任务接口
URL  
PUT /api/v1/tasks/{task_id}/cancel

请求头  
Authorization: Bearer {access_token}

响应示例  
{
  "status": "success",
  "message": "Task stopped successfully"
}
逻辑说明：  
- 若任务正在执行，强制中断当前线程（参考网页9、网页10的ScheduledFuture.cancel(true)）。
- 更新任务状态为stopped，保留任务元数据。


---

4. 删除任务接口
URL  
DELETE /api/v1/tasks/{task_id}

请求头  
Authorization: Bearer {access_token}

响应示例  
{
  "status": "success",
  "message": "Task deleted permanently"
}

按用户查询任务列表
GET /api/v1/users/{user_id}/tasks?status=running
Response:
{
  "tasks": [
    {
      "task_id": "550e8400-e29b...",
      "status": "running",
      "http_endpoint": "https://..."
    }
  ],
  "total": 15
}
1. 获取用户运行任务数
GET /api/v1/users/{user_id}/tasks/running-count
Response:
{
  "user_id": 1001,
  "running_tasks": 5,
  "last_updated": "2025-04-22T14:30:45Z"
}
