-- 插入测试用户
INSERT INTO users (user_id, username, password, email, api_key) 
VALUES (1, 'test_user', 'password', 'test@example.com', 'test-api-key');

-- 插入测试实例
INSERT INTO instance (instance_name, ip_address, port, status, last_heartbeat)
VALUES ('test-instance', '127.0.0.1', 8080, 'active', NOW()); 