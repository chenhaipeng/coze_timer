package com.coze.timer.service.impl;

import com.coze.timer.mapper.TaskLogMapper;
import com.coze.timer.mapper.TaskMapper;
import com.coze.timer.mapper.InstanceMapper;
import com.coze.timer.mapper.TaskAssignmentMapper;
import com.coze.timer.model.Task;
import com.coze.timer.model.Instance;
import com.coze.timer.model.TaskAssignment;
import com.coze.timer.model.dto.TaskRequest;
import com.coze.timer.model.dto.TaskResponse;
import com.coze.timer.service.TaskService;
import com.coze.timer.util.TaskScheduleUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 任务服务实现类
 */
@Slf4j
@Service
public class TaskServiceImpl implements TaskService {
    
    @Autowired
    private TaskMapper taskMapper;
    
    @Autowired
    private TaskLogMapper taskLogMapper;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private TaskScheduleUtil taskScheduleUtil;
    
    @Autowired
    private InstanceMapper instanceMapper;
    
    @Autowired
    private TaskAssignmentMapper taskAssignmentMapper;
    
    @Value("${timer.instance.name}")
    private String instanceName;
    
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
            
            // 6. 立即分配任务到当前实例
            Instance currentInstance = instanceMapper.findByName(instanceName);
            if (currentInstance != null && "active".equals(currentInstance.getStatus())) {
                TaskAssignment assignment = new TaskAssignment();
                assignment.setTaskId(taskId);
                assignment.setInstanceId(currentInstance.getId());
                assignment.setCreatedAt(LocalDateTime.now());
                assignment.setUpdatedAt(LocalDateTime.now());
                taskAssignmentMapper.insert(assignment);
                log.info("任务[{}]已分配到实例[{}]", taskId, instanceName);
            }
            
            // 7. 构建响应
            TaskResponse response = new TaskResponse();
            response.setTaskId(taskId);
            response.setTaskStatus("pending");
            response.setType(request.getType());
            response.setHttpEndpoint(request.getHttpEndpoint());
            response.setNextRunTime(nextRunTime);
            response.setCreatedAt(LocalDateTime.now());
            
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
    public TaskResponse getTask(String taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            TaskResponse response = new TaskResponse();
            response.setStatus("error");
            response.setMessage("任务不存在");
            return response;
        }
        
        TaskResponse response = new TaskResponse();
        response.setStatus("success");
        response.setTaskId(task.getTaskId());
        response.setTaskStatus(task.getStatus());
        response.setType(task.getType());
        response.setHttpEndpoint(task.getHttpEndpoint());
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
    public List<Task> getTasksToExecute(String taskId, int limit) {
        return taskMapper.findTasksToExecute(taskId, LocalDateTime.now(), limit);
    }
    
    /**
     * 更新任务状态
     */
    @Override
    @SchedulerLock(name = "updateTaskStatus", lockAtLeastFor = "PT5S")
    public TaskResponse updateTaskStatus(String taskId, String status) {
        if (taskId == null || status == null) {
            return TaskResponse.builder()
                    .status("error")
                    .message("Task ID and status cannot be null")
                    .build();
        }
        
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            return TaskResponse.builder()
                    .status("error")
                    .message("Task not found")
                    .build();
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
        
        boolean success = taskMapper.updateStatus(taskId, status, nextRunTime) > 0;
        if (success) {
            return TaskResponse.builder()
                    .status("success")
                    .taskId(taskId)
                    .taskStatus(status)
                    .nextRunTime(nextRunTime)
                    .build();
        } else {
            return TaskResponse.builder()
                    .status("error")
                    .message("Failed to update task status")
                    .build();
        }
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
     * 获取未分配的任务
     */
    @Override
    public List<Task> getUnassignedTasks(int limit) {
        return taskMapper.findUnassignedTasks(limit);
    }
} 