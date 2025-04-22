# Coze Timer 分布式定时任务调度系统

一个基于Spring Boot的分布式HTTP定时任务调度系统，支持多种任务类型和调度策略。

## 核心功能

- **多种任务类型**：支持间隔执行(interval)、一次性执行(once)和Cron表达式(cron)三种调度方式
- **分布式架构**：基于Redis和ZooKeeper实现分布式调度和锁，确保任务不重复执行
- **高性能HTTP执行**：集成OkHttp异步客户端，高效执行HTTP回调任务
- **流量控制**：内置令牌桶算法实现请求限流，保护下游系统
- **任务监控**：记录任务执行日志，支持追踪任务执行状态和历史
- **灵活的停止条件**：支持按最大执行次数或响应条件自动停止任务

## 技术架构

- **开发框架**：Spring Boot 2.7
- **数据存储**：MySQL + MyBatis
- **缓存队列**：Redis
- **分布式协调**：ZooKeeper + Curator
- **HTTP客户端**：OkHttp
- **限流组件**：Bucket4j
- **容器化部署**：Docker + Docker Compose

## 快速开始

### 前置条件

- Docker & Docker Compose
- Java 11+ (仅开发环境需要)

### 部署步骤

1. 克隆代码仓库
   ```bash
   git clone https://github.com/your-username/coze-timer.git
   cd coze-timer
   ```

2. 使用Docker Compose启动服务
   ```bash
   docker-compose up -d
   ```

3. 验证服务状态
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### API接口使用示例

1. 创建一个每20秒执行一次的定时任务
   ```bash
   curl -X POST http://localhost:8080/api/v1/tasks \
     -H "Content-Type: application/json" \
     -d '{
       "user_id": 1001,
       "type": "interval",
       "http_endpoint": "https://api.example.com/callback",
       "method": "POST",
       "headers": {
         "Authorization": "Bearer your-token"
       },
       "body": {
         "data": "example-data"
       },
       "interval_seconds": 20,
       "stop_condition": {
         "max_cnt": 100
       }
     }'
   ```

2. 查询任务状态
   ```bash
   curl http://localhost:8080/api/v1/tasks/{task_id}
   ```

3. 取消任务
   ```bash
   curl -X PUT http://localhost:8080/api/v1/tasks/{task_id}/cancel
   ```

## 开发环境搭建

1. 设置开发环境
   ```bash
   # 启动依赖服务
   docker-compose up mysql redis zookeeper -d
   
   # 编译项目
   ./mvnw clean package -DskipTests
   
   # 启动应用
   java -jar target/timer-0.0.1-SNAPSHOT.jar
   ```

## 系统扩展

- **集群部署**：直接启动多个应用实例，系统会自动协调
- **负载均衡**：可配置Nginx等负载均衡器实现API请求分发
- **监控告警**：集成Prometheus和Grafana实现系统监控

# Coze Timer 任务时间修复工具

这是一个用于修复 Coze Timer 数据库中任务时间异常的工具。主要针对年份错误的任务记录（如年份显示为2025年）进行修复，确保时间格式一致性。

## 问题说明

在 Coze Timer 系统中，部分任务记录可能存在时间异常的问题，特别是以下字段可能出现错误的年份（如2025年）：

- `start_time` (开始时间)
- `next_run_time` (下次运行时间)
- `created_at` (创建时间)
- `updated_at` (更新时间)

这些问题可能是由于系统时间设置不正确导致的。本工具通过将异常年份调整为正确年份（2023年）来修复这些记录。

## 功能介绍

`fix_task_time.js` 脚本的主要功能包括：

1. 连接到 Coze Timer 数据库
2. 查询年份大于2024的任务记录
3. 显示需要修复的记录详情
4. 在用户确认后，将这些记录的年份调整为2023年
5. 验证修复结果并显示

## 使用前提

运行此脚本前，请确保您的系统已安装：

- Node.js（版本12或更高）
- `mysql2` 包

安装依赖包：

```bash
npm install mysql2
```

## 配置说明

在运行脚本前，请先在 `fix_task_time.js` 文件中修改数据库连接配置：

```javascript
const dbConfig = {
  host: 'localhost',  // 数据库主机地址
  port: 13306,        // 数据库端口
  user: 'root',       // 数据库用户名
  password: 'password', // 数据库密码
  database: 'coze_timer' // 数据库名称
};
```

请根据您的实际环境修改这些配置值。

## 使用方法

1. 在命令行中运行以下命令：

```bash
node fix_task_time.js
```

2. 脚本执行流程：
   - 连接数据库
   - 查询并显示需要修复的任务记录
   - 等待用户确认（按Enter继续，Ctrl+C取消）
   - 执行修复操作
   - 显示修复结果

## 注意事项

- **重要**：在执行修复前，请先备份您的数据库，以防意外发生。
- 确保数据库连接设置正确，否则脚本将无法连接到数据库。
- 修复过程不可逆，请谨慎操作。
- 如在使用过程中遇到任何问题，请联系系统管理员寻求帮助。 