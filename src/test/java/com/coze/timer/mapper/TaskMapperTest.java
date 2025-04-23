package com.coze.timer.mapper;

import com.coze.timer.model.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskMapper 测试类
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TaskMapperTest {

    @Autowired
    private TaskMapper taskMapper;

    @Test
    public void testInsertAndFindById() {
        // 创建测试任务
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setUserId(1);
        task.setType("once");
        task.setHttpEndpoint("http://example.com/api/test");
        task.setMethod("GET");
        task.setHeaders("{\"Content-Type\":\"application/json\"}");
        task.setRequestBody("{\"data\":\"test\"}");
        task.setStartTime(LocalDateTime.now().plusMinutes(5));
        task.setStatus("pending");
        task.setNextRunTime(LocalDateTime.now().plusMinutes(5));

        // 插入任务
        int result = taskMapper.insert(task);
        assertEquals(1, result);

        // 查询任务
        Task foundTask = taskMapper.findById(task.getTaskId());
        assertNotNull(foundTask);
        assertEquals(task.getTaskId(), foundTask.getTaskId());
        assertEquals(task.getUserId(), foundTask.getUserId());
        assertEquals(task.getType(), foundTask.getType());
        assertEquals(task.getHttpEndpoint(), foundTask.getHttpEndpoint());
    }

    @Test
    public void testUpdateStatus() {
        // 创建测试任务
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setUserId(1);
        task.setType("once");
        task.setHttpEndpoint("http://example.com/api/test");
        task.setMethod("GET");
        task.setStatus("pending");
        task.setNextRunTime(LocalDateTime.now().plusMinutes(5));
        taskMapper.insert(task);

        // 更新状态
        LocalDateTime newNextRunTime = LocalDateTime.now().plusMinutes(10);
        int result = taskMapper.updateStatus(task.getTaskId(), "running", newNextRunTime);
        assertEquals(1, result);

        // 验证更新结果
        Task updatedTask = taskMapper.findById(task.getTaskId());
        assertEquals("running", updatedTask.getStatus());
        assertEquals(newNextRunTime, updatedTask.getNextRunTime());
    }

    @Test
    public void testDeleteById() {
        // 创建测试任务
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setUserId(1);
        task.setType("once");
        task.setHttpEndpoint("http://example.com/api/test");
        task.setMethod("GET");
        task.setStatus("pending");
        task.setNextRunTime(LocalDateTime.now().plusMinutes(5));
        taskMapper.insert(task);

        // 删除任务
        int result = taskMapper.deleteById(task.getTaskId());
        assertEquals(1, result);

        // 验证删除结果
        Task deletedTask = taskMapper.findById(task.getTaskId());
        assertNull(deletedTask);
    }

    @Test
    public void testFindTasksToExecute() {
        // 创建测试任务
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setUserId(1);
        task.setType("once");
        task.setHttpEndpoint("http://example.com/api/test");
        task.setMethod("GET");
        task.setStatus("pending");
        task.setNextRunTime(LocalDateTime.now().minusMinutes(5));
        taskMapper.insert(task);

        // 查询待执行任务
        List<Task> tasks = taskMapper.findTasksToExecute(null, LocalDateTime.now(), 10);
        assertFalse(tasks.isEmpty());
        assertEquals(task.getTaskId(), tasks.get(0).getTaskId());
    }

    @Test
    public void testFindByUserId() {
        // 创建测试任务
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setUserId(1);
        task.setType("once");
        task.setHttpEndpoint("http://example.com/api/test");
        task.setMethod("GET");
        task.setStatus("pending");
        task.setNextRunTime(LocalDateTime.now().plusMinutes(5));
        taskMapper.insert(task);

        // 查询用户任务
        List<Task> tasks = taskMapper.findByUserId(1, "pending", 0, 10);
        assertFalse(tasks.isEmpty());
        assertEquals(task.getTaskId(), tasks.get(0).getTaskId());
    }

    @Test
    public void testCountRunningTasks() {
        // 创建测试任务
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setUserId(1);
        task.setType("once");
        task.setHttpEndpoint("http://example.com/api/test");
        task.setMethod("GET");
        task.setStatus("running");
        task.setNextRunTime(LocalDateTime.now().plusMinutes(5));
        taskMapper.insert(task);

        // 统计运行中任务
        int count = taskMapper.countRunningTasks(1);
        assertEquals(1, count);
    }
} 