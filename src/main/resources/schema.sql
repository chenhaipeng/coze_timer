-- 创建ShedLock表
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建任务表
CREATE TABLE IF NOT EXISTS tasks (
  task_id VARCHAR(36) PRIMARY KEY,
  user_id INT NOT NULL COMMENT '用户ID',
  type ENUM('interval', 'once', 'cron') NOT NULL,
  http_endpoint VARCHAR(255) NOT NULL,
  method VARCHAR(10) NOT NULL,
  headers TEXT,
  request_body TEXT,
  interval_seconds INT,
  cron_expression VARCHAR(100),
  start_time DATETIME,
  status ENUM('pending', 'running', 'completed', 'failed', 'stopped') NOT NULL DEFAULT 'pending',
  next_run_time DATETIME,
  stop_condition TEXT COMMENT 'JSON格式中止条件',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_status (status),
  INDEX idx_next_run_time (next_run_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建任务日志表
CREATE TABLE IF NOT EXISTS task_logs (
  log_id VARCHAR(36) PRIMARY KEY,
  task_id VARCHAR(36) NOT NULL,
  user_id INT NOT NULL,
  http_status INT,
  response_body TEXT,
  execution_time INT COMMENT '执行耗时(毫秒)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_task_id (task_id),
  INDEX idx_user_id (user_id),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
  user_id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL COMMENT '加密存储',
  email VARCHAR(100) UNIQUE,
  api_key VARCHAR(64) UNIQUE COMMENT 'API访问密钥',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_api_key (api_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建实例表
CREATE TABLE IF NOT EXISTS instance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  instance_name VARCHAR(50) NOT NULL UNIQUE,
  ip_address VARCHAR(50) NOT NULL,
  port INT DEFAULT 8080,
  status ENUM('active', 'inactive', 'maintenance') NOT NULL DEFAULT 'active',
  last_heartbeat DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_status (status),
  INDEX idx_ip_port (ip_address, port)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建任务分配表
CREATE TABLE IF NOT EXISTS task_assignment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(36) NOT NULL,
  instance_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_task_id (task_id),
  INDEX idx_instance_id (instance_id),
  FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
  FOREIGN KEY (instance_id) REFERENCES instance(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4; 