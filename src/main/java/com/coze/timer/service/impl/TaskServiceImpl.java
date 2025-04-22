package com.coze.timer.service.impl;

import com.coze.timer.mapper.TaskLogMapper;
import com.coze.timer.mapper.TaskMapper;
import com.coze.timer.model.Task;
import com.coze.timer.model.TaskLog;
import com.coze.timer.model.dto.TaskRequest;
import com.coze.timer.model.dto.TaskResponse;
import com.coze.timer.service.TaskService;
import com.coze.timer.util.TaskScheduleUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 任务服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    
    private final TaskMapper taskMapper;
    private final TaskLogMapper taskLogMapper;
    private final ObjectMapper objectMapper;
    
    // 使用Spring Boot自动配置的stringRedisTemplate或customStringRedisTemplate
    private final RedisTemplate<String, String> stringRedisTemplate;
    
    private final TaskScheduleUtil taskScheduleUtil;
    
    private static final String TASK_LOCK_PREFIX = "task_lock:";
    
    /**
     * 创建任务
     */
    @Override
    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        try {
            // 1. 生成任务ID
            String taskId = UUID.randomUUID().toString();
            
            // 2. 计算下次执行时间
            LocalDateTime nextRunTime = calculateNextRunTime(request);
            if (nextRunTime == null) {
                TaskResponse response = new TaskResponse();
                response.setStatus("error");
                response.setMessage("无法计算下次执行时间，请检查任务参数");
                return response;
            }
            
            // 3. 构建任务对象
            Task task = new Task();
            task.setTaskId(taskId);
            task.setUserId(request.getUserId());
            task.setType(request.getType());
            task.setHttpEndpoint(request.getHttpEndpoint());
            task.setMethod(request.getMethod());
            task.setHeaders(objectMapper.writeValueAsString(request.getHeaders()));
            task.setRequestBody(objectMapper.writeValueAsString(request.getBody()));
            task.setIntervalSeconds(request.getIntervalSeconds());
            task.setCronExpression(request.getCronExpression());
            task.setStartTime(request.getStartTime());
            task.setStatus("pending");
            task.setNextRunTime(nextRunTime);
            
            // 4. 保存停止条件
            if (request.getStopCondition() != null) {
                task.setStopCondition(objectMapper.writeValueAsString(request.getStopCondition()));
            }
            
            // 5. 保存任务
            taskMapper.insert(task);
            
            // 6. 将任务加入Redis
            addTaskToRedis(task);
            
            // 7. 构建响应
            TaskResponse response = new TaskResponse();
            response.setStatus("success");
            response.setTaskId(taskId);
            response.setNextRunTime(nextRunTime);
            return response;
        } catch (Exception e) {
            log.error("创建任务失败", e);
            TaskResponse response = new TaskResponse();
            response.setStatus("error");
            response.setMessage("创建任务失败: " + e.getMessage());
            return response;
        }
    }
    
    /**
     * 根据ID查询任务
     */
    @Override
    public TaskResponse getTaskById(String taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            TaskResponse response = new TaskResponse();
            response.setStatus("error");
            response.setMessage("任务不存在");
            return response;
        }
        
        // 查询最近一次执行记录
        TaskLog latestLog = taskLogMapper.findLatestByTaskId(taskId);
        TaskResponse.ExecutionRecord executionRecord = null;
        
        if (latestLog != null) {
            executionRecord = new TaskResponse.ExecutionRecord();
            executionRecord.setTimestamp(latestLog.getCreatedAt());
            executionRecord.setHttpStatus(latestLog.getHttpStatus());
            executionRecord.setResponseBody(latestLog.getResponseBody());
        }
        
        TaskResponse response = new TaskResponse();
        response.setStatus("success");
        response.setTaskId(task.getTaskId());
        response.setTaskStatus(task.getStatus());
        response.setType(task.getType());
        response.setHttpEndpoint(task.getHttpEndpoint());
        response.setLastExecution(executionRecord);
        response.setNextRunTime(task.getNextRunTime());
        response.setCreatedAt(task.getCreatedAt());
        return response;
    }
    
    /**
     * 取消任务
     */
    @Override
    @Transactional
    public TaskResponse cancelTask(String taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            TaskResponse response = new TaskResponse();
            response.setStatus("error");
            response.setMessage("任务不存在");
            return response;
        }
        
        // 更新任务状态为stopped
        taskMapper.updateStatus(taskId, "stopped", null);
        
        // 从Redis中移除任务
        removeTaskFromRedis(taskId);
        
        TaskResponse response = new TaskResponse();
        response.setStatus("success");
        response.setMessage("任务已成功停止");
        return response;
    }
    
    /**
     * 删除任务
     */
    @Override
    @Transactional
    public TaskResponse deleteTask(String taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            TaskResponse response = new TaskResponse();
            response.setStatus("error");
            response.setMessage("任务不存在");
            return response;
        }
        
        // 删除任务
        taskMapper.deleteById(taskId);
        
        // 从Redis中移除任务
        removeTaskFromRedis(taskId);
        
        TaskResponse response = new TaskResponse();
        response.setStatus("success");
        response.setMessage("任务已永久删除");
        return response;
    }
    
    /**
     * 获取用户任务列表
     */
    @Override
    public List<TaskResponse> getUserTasks(Integer userId, String status, int page, int size) {
        int offset = (page - 1) * size;
        List<Task> tasks = taskMapper.findByUserId(userId, status, offset, size);
        List<TaskResponse> responses = new ArrayList<>();
        
        for (Task task : tasks) {
            TaskResponse response = new TaskResponse();
            response.setTaskId(task.getTaskId());
            response.setTaskStatus(task.getStatus());
            response.setType(task.getType());
            response.setHttpEndpoint(task.getHttpEndpoint());
            response.setNextRunTime(task.getNextRunTime());
            response.setCreatedAt(task.getCreatedAt());
            responses.add(response);
        }
        
        return responses;
    }
    
    /**
     * 获取用户运行中的任务数
     */
    @Override
    public int getRunningTaskCount(Integer userId) {
        return taskMapper.countRunningTasks(userId);
    }
    
    /**
     * 获取需要执行的任务
     */
    @Override
    public List<Task> getTasksToExecute(int limit) {
        return taskMapper.findTasksToExecute(LocalDateTime.now(), limit);
    }
    
    /**
     * 更新任务状态
     */
    @Override
    public boolean updateTaskStatus(String taskId, String status) {
        if (taskId == null || status == null) {
            return false;
        }
        
        String lockKey = TASK_LOCK_PREFIX + taskId;
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        
        if (Boolean.TRUE.equals(acquired)) {
            try {
                Task task = taskMapper.findById(taskId);
                if (task == null) {
                    return false;
                }
                
                // 计算下次执行时间
                LocalDateTime nextRunTime = null;
                // 使用中国时区
                ZoneId chinaZone = ZoneId.of("Asia/Shanghai");
                
                if ("running".equals(status) && "interval".equals(task.getType())) {
                    nextRunTime = LocalDateTime.now(chinaZone).plusSeconds(task.getIntervalSeconds());
                } else if ("running".equals(status) && "cron".equals(task.getType())) {
                    nextRunTime = taskScheduleUtil.getNextRunTime(task.getCronExpression());
                }
                
                return taskMapper.updateStatus(taskId, status, nextRunTime) > 0;
            } finally {
                // 释放锁
                stringRedisTemplate.delete(lockKey);
            }
        }
        return false;
    }
    
    /**
     * 计算下次执行时间
     */
    private LocalDateTime calculateNextRunTime(TaskRequest request) {
        // 使用中国时区
        ZoneId chinaZone = ZoneId.of("Asia/Shanghai");
        LocalDateTime nowInChina = LocalDateTime.now(chinaZone);
        
        LocalDateTime startTime = request.getStartTime();
        if (startTime == null) {
            startTime = nowInChina;
        }
        
        if (startTime.isBefore(nowInChina)) {
            startTime = nowInChina;
        }
        
        switch (request.getType()) {
            case "once":
                return startTime;
            case "interval":
                if (request.getIntervalSeconds() == null || request.getIntervalSeconds() <= 0) {
                    return null;
                }
                return startTime;
            case "cron":
                if (request.getCronExpression() == null || request.getCronExpression().isEmpty()) {
                    return null;
                }
                return taskScheduleUtil.getNextRunTime(request.getCronExpression());
            default:
                return null;
        }
    }
    
    /**
     * 将任务添加到Redis
     */
    private void addTaskToRedis(Task task) throws JsonProcessingException {
        String taskJson = objectMapper.writeValueAsString(task);
        String key = "task:" + task.getTaskId();
        stringRedisTemplate.opsForValue().set(key, taskJson);
    }
    
    /**
     * 从Redis中移除任务
     */
    private void removeTaskFromRedis(String taskId) {
        String key = "task:" + taskId;
        stringRedisTemplate.delete(key);
    }
} 