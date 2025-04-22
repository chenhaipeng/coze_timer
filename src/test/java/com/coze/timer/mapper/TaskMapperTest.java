package com.coze.timer.mapper;

import com.coze.timer.model.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务数据访问接口测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class TaskMapperTest {

    @Autowired
    private TaskMapper taskMapper;
    
    /**
     * 测试插入和查询任务
     */
    @Test
    public void testInsertAndFindById() {
        Task task = createTestTask();
        
        // 插入任务
        int result = taskMapper.insert(task);
        assertEquals(1, result);
        
        // 根据ID查询任务
        Task foundTask = taskMapper.findById(task.getTaskId());
        assertNotNull(foundTask);
        assertEquals(task.getTaskId(), foundTask.getTaskId());
        assertEquals(task.getHttpEndpoint(), foundTask.getHttpEndpoint());
        assertEquals(task.getType(), foundTask.getType());
    }
    
    /**
     * 测试更新任务状态
     */
    @Test
    public void testUpdateStatus() {
        Task task = createTestTask();
        taskMapper.insert(task);
        
        // 更新任务状态
        LocalDateTime nextRunTime = LocalDateTime.now().plusHours(1);
        int result = taskMapper.updateStatus(task.getTaskId(), "running", nextRunTime);
        assertEquals(1, result);
        
        // 验证状态已更新
        Task updatedTask = taskMapper.findById(task.getTaskId());
        assertEquals("running", updatedTask.getStatus());
        assertNotNull(updatedTask.getNextRunTime());
    }
    
    /**
     * 测试删除任务
     */
    @Test
    public void testDeleteById() {
        Task task = createTestTask();
        taskMapper.insert(task);
        
        // 删除任务
        int result = taskMapper.deleteById(task.getTaskId());
        assertEquals(1, result);
        
        // 验证任务已删除
        Task deletedTask = taskMapper.findById(task.getTaskId());
        assertNull(deletedTask);
    }
    
    /**
     * 测试查询待执行任务
     */
    @Test
    public void testFindTasksToExecute() {
        // 先清理所有任务（防止干扰）
        List<Task> pendingTasks = taskMapper.findTasksToExecute(LocalDateTime.now().plusDays(1), 100);
        for (Task task : pendingTasks) {
            taskMapper.deleteById(task.getTaskId());
        }
        
        // 创建几个测试任务
        Task task1 = createTestTask();
        task1.setNextRunTime(LocalDateTime.now().minusMinutes(5));
        task1.setStatus("pending");
        taskMapper.insert(task1);
        
        Task task2 = createTestTask();
        task2.setNextRunTime(LocalDateTime.now().minusMinutes(3));
        task2.setStatus("pending");
        taskMapper.insert(task2);
        
        Task task3 = createTestTask();
        task3.setNextRunTime(LocalDateTime.now().plusMinutes(5));
        task3.setStatus("pending");
        taskMapper.insert(task3);
        
        // 查询待执行任务
        List<Task> tasksToExecute = taskMapper.findTasksToExecute(LocalDateTime.now(), 10);
        assertEquals(2, tasksToExecute.size());
    }
    
    /**
     * 测试按用户ID查询任务
     */
    @Test
    public void testFindByUserId() {
        // 创建用户任务
        Task task = createTestTask();
        task.setUserId(999);
        taskMapper.insert(task);
        
        // 查询用户任务
        List<Task> userTasks = taskMapper.findByUserId(999, null, 0, 10);
        assertTrue(userTasks.size() >= 1);
        
        // 测试状态筛选
        List<Task> pendingTasks = taskMapper.findByUserId(999, "pending", 0, 10);
        assertTrue(pendingTasks.size() >= 1);
    }
    
    /**
     * 创建测试任务
     */
    private Task createTestTask() {
        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setUserId(1);
        task.setType("once");
        task.setHttpEndpoint("http://example.com/api/test");
        task.setMethod("GET");
        task.setStatus("pending");
        task.setNextRunTime(LocalDateTime.now().plusMinutes(10));
        return task;
    }
} 