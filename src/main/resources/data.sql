-- 初始化实例
INSERT INTO instance (instance_name, ip_address, port, status, last_heartbeat, created_at, updated_at)
VALUES ('coze-timer-1', '127.0.0.1', 8080, 'active', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    status = 'active',
    last_heartbeat = NOW(),
    updated_at = NOW(); 