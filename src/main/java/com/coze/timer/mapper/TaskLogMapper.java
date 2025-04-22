package com.coze.timer.mapper;

import com.coze.timer.model.TaskLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务日志数据访问接口
 */
@Mapper
public interface TaskLogMapper {
    
    /**
     * 插入任务执行日志
     */
    int insert(TaskLog taskLog);
    
    /**
     * 查询任务最新的执行日志
     */
    TaskLog findLatestByTaskId(String taskId);
    
    /**
     * 查询任务的所有执行日志
     */
    List<TaskLog> findByTaskId(@Param("taskId") String taskId, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 统计任务执行次数
     */
    int countByTaskId(String taskId);
} 