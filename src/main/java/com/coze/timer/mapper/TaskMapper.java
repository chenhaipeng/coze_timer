package com.coze.timer.mapper;

import com.coze.timer.model.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务数据访问接口
 */
@Mapper
public interface TaskMapper {
    
    /**
     * 插入新任务
     */
    int insert(Task task);
    
    /**
     * 根据ID查询任务
     */
    Task findById(String taskId);
    
    /**
     * 更新任务状态
     */
    int updateStatus(@Param("taskId") String taskId, @Param("status") String status, 
                     @Param("nextRunTime") LocalDateTime nextRunTime);
    
    /**
     * 删除任务
     */
    int deleteById(String taskId);
    
    /**
     * 查询指定时间之前需要执行的任务
     */
    List<Task> findTasksToExecute(@Param("taskId") String taskId, 
                                 @Param("time") LocalDateTime time, 
                                 @Param("limit") int limit);
    
    /**
     * 按用户ID查询任务列表
     */
    List<Task> findByUserId(@Param("userId") Integer userId, @Param("status") String status,
                            @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 统计用户运行中的任务数
     */
    int countRunningTasks(@Param("userId") Integer userId);
    
    /**
     * 查询未分配的任务
     */
    List<Task> findUnassignedTasks(@Param("limit") int limit);
} 