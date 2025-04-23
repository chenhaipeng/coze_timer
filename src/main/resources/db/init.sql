-- 创建数据库
CREATE DATABASE IF NOT EXISTS coze_timer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE coze_timer;

-- 创建任务表
CREATE TABLE IF NOT EXISTS tasks (
    task_id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    http_endpoint VARCHAR(500) NOT NULL,
    method VARCHAR(10) NOT NULL,
    headers TEXT,
    request_body TEXT,
    interval_seconds INT,
    cron_expression VARCHAR(100),
    start_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    next_run_time DATETIME NOT NULL,
    stop_condition TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_status (status),
    INDEX idx_next_run_time (next_run_time),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建实例表
CREATE TABLE IF NOT EXISTS instances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instance_name VARCHAR(100) NOT NULL,
    ip_address VARCHAR(50) NOT NULL,
    port INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    last_heartbeat DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_instance_name (instance_name),
    INDEX idx_status (status),
    INDEX idx_last_heartbeat (last_heartbeat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建任务分配表
CREATE TABLE IF NOT EXISTS task_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    instance_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_task_id (task_id),
    INDEX idx_instance_id (instance_id),
    FOREIGN KEY (task_id) REFERENCES tasks(task_id),
    FOREIGN KEY (instance_id) REFERENCES instances(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建任务日志表
CREATE TABLE IF NOT EXISTS task_logs (
    log_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    http_status INT,
    response_body TEXT,
    execution_time INT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_task_id (task_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (task_id) REFERENCES tasks(task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4; 