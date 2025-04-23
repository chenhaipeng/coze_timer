package com.coze.timer.mapper;

import com.coze.timer.model.TaskLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskLogMapper 测试类
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TaskLogMapperTest {

    @Autowired
    private TaskLogMapper taskLogMapper;

    @Test
    public void testInsertAndFindLatestByTaskId() {
        // 创建测试任务日志
        TaskLog taskLog = new TaskLog();
        taskLog.setLogId(UUID.randomUUID().toString());
        taskLog.setTaskId(UUID.randomUUID().toString());
        taskLog.setUserId(1);
        taskLog.setHttpStatus(200);
        taskLog.setResponseBody("{\"status\":\"success\"}");
        taskLog.setExecutionTime(100);

        // 插入任务日志
        int result = taskLogMapper.insert(taskLog);
        assertEquals(1, result);

        // 查询最新日志
        TaskLog foundLog = taskLogMapper.findLatestByTaskId(taskLog.getTaskId());
        assertNotNull(foundLog);
        assertEquals(taskLog.getLogId(), foundLog.getLogId());
        assertEquals(taskLog.getTaskId(), foundLog.getTaskId());
        assertEquals(taskLog.getHttpStatus(), foundLog.getHttpStatus());
    }

    @Test
    public void testFindByTaskId() {
        // 创建测试任务日志
        TaskLog taskLog = new TaskLog();
        taskLog.setLogId(UUID.randomUUID().toString());
        taskLog.setTaskId(UUID.randomUUID().toString());
        taskLog.setUserId(1);
        taskLog.setHttpStatus(200);
        taskLog.setResponseBody("{\"status\":\"success\"}");
        taskLog.setExecutionTime(100);
        taskLogMapper.insert(taskLog);

        // 查询任务日志列表
        List<TaskLog> logs = taskLogMapper.findByTaskId(taskLog.getTaskId(), 0, 10);
        assertFalse(logs.isEmpty());
        assertEquals(taskLog.getLogId(), logs.get(0).getLogId());
    }

    @Test
    public void testCountByTaskId() {
        // 创建测试任务日志
        TaskLog taskLog = new TaskLog();
        taskLog.setLogId(UUID.randomUUID().toString());
        taskLog.setTaskId(UUID.randomUUID().toString());
        taskLog.setUserId(1);
        taskLog.setHttpStatus(200);
        taskLog.setResponseBody("{\"status\":\"success\"}");
        taskLog.setExecutionTime(100);
        taskLogMapper.insert(taskLog);

        // 统计任务日志数量
        int count = taskLogMapper.countByTaskId(taskLog.getTaskId());
        assertEquals(1, count);
    }
} 