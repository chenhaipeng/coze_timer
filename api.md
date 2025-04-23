# Coze Timer API 文档

## 1. 创建任务

### 请求示例

```http
POST /api/v1/tasks
Content-Type: application/json

{
    "type": "once",
    "httpEndpoint": "https://www.baidu.com",
    "method": "GET",
    "userId": 1,
    "headers": {
        "User-Agent": "Coze-Timer/1.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
    },
    "startTime": "2024-04-22T15:30:00Z"
}
```

### 响应示例

```json
{
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "success",
    "message": "Task created successfully"
}
```

## 2. 查询任务状态

### 请求示例

```http
GET /api/v1/tasks/550e8400-e29b-41d4-a716-446655440000
```

### 响应示例

```json
{
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "taskStatus": "running",
    "type": "once",
    "httpEndpoint": "https://www.baidu.com",
    "nextRunTime": "2024-04-22T15:30:00Z",
    "createdAt": "2024-04-22T15:29:00Z",
    "lastExecution": {
        "httpStatus": 200,
        "responseBody": "<!DOCTYPE html><html>...</html>",
        "executionTime": 150
    }
}
```

## 3. 取消任务

### 请求示例

```http
PUT /api/v1/tasks/550e8400-e29b-41d4-a716-446655440000/cancel
```

### 响应示例

```json
{
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "success",
    "message": "Task cancelled successfully"
}
```

## 4. 测试用例

### 4.1 一次性任务测试

```bash
# 1. 创建任务
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "type": "once",
    "httpEndpoint": "https://www.baidu.com",
    "method": "GET",
    "userId": 1,
    "headers": {
        "User-Agent": "Coze-Timer/1.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
    },
    "startTime": "2024-04-22T15:30:00Z"
  }'

# 2. 查询任务状态
curl http://localhost:8080/api/v1/tasks/550e8400-e29b-41d4-a716-446655440000

# 3. 取消任务
curl -X PUT http://localhost:8080/api/v1/tasks/550e8400-e29b-41d4-a716-446655440000/cancel
```

### 4.2 间隔执行任务测试

```bash
# 1. 创建间隔执行任务
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "type": "interval",
    "httpEndpoint": "https://www.baidu.com",
    "method": "GET",
    "userId": 1,
    "intervalSeconds": 300,
    "headers": {
        "User-Agent": "Coze-Timer/1.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
    },
    "startTime": "2024-04-22T15:30:00Z"
  }'

# 2. 查询任务状态
curl http://localhost:8080/api/v1/tasks/550e8400-e29b-41d4-a716-446655440000

# 3. 取消任务
curl -X PUT http://localhost:8080/api/v1/tasks/550e8400-e29b-41d4-a716-446655440000/cancel
```

### 4.3 Cron 任务测试

```bash
# 1. 创建 Cron 任务
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "type": "cron",
    "httpEndpoint": "https://www.baidu.com",
    "method": "GET",
    "userId": 1,
    "cronExpression": "0 0 * * * ?",
    "headers": {
        "User-Agent": "Coze-Timer/1.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
    },
    "startTime": "2024-04-22T15:30:00Z"
  }'

# 2. 查询任务状态
curl http://localhost:8080/api/v1/tasks/550e8400-e29b-41d4-a716-446655440000

# 3. 取消任务
curl -X PUT http://localhost:8080/api/v1/tasks/550e8400-e29b-41d4-a716-446655440000/cancel
```

## 5. 注意事项

1. 所有时间字段使用 ISO 8601 格式
2. 任务状态包括：pending、running、completed、failed、stopped
3. HTTP 请求支持 GET、POST 方法
4. 请求头和请求体支持 JSON 格式
5. 任务执行结果会记录 HTTP 状态码、响应内容和执行时间
6. 支持任务重试机制，默认重试 3 次
7. 支持限流控制，默认每秒最多执行 10 个请求
