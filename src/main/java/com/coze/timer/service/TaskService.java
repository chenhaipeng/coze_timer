package com.coze.timer.service;

import com.coze.timer.model.Task;
import com.coze.timer.model.dto.TaskRequest;
import com.coze.timer.model.dto.TaskResponse;

import java.util.List;

/**
 * 任务服务接口
 */
public interface TaskService {
    
    /**
     * 创建任务
     */
    TaskResponse createTask(TaskRequest request);
    
    /**
     * 根据ID查询任务
     */
    TaskResponse getTask(String taskId);
    
    /**
     * 取消任务
     */
    TaskResponse cancelTask(String taskId);
    
    /**
     * 删除任务
     */
    TaskResponse deleteTask(String taskId);
    
    /**
     * 获取用户任务列表
     */
    List<TaskResponse> getUserTasks(Integer userId, String status, int page, int size);
    
    /**
     * 获取用户运行中的任务数
     */
    int getRunningTaskCount(Integer userId);
    
    /**
     * 获取需要执行的任务
     */
    List<Task> getTasksToExecute(String taskId, int limit);
    
    /**
     * 更新任务状态
     */
    TaskResponse updateTaskStatus(String taskId, String status);
    
    /**
     * 获取未分配的任务
     * @param limit 最大获取数量
     * @return 未分配的任务列表
     */
    List<Task> getUnassignedTasks(int limit);
} 