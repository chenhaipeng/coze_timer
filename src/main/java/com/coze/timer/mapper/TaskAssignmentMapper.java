package com.coze.timer.mapper;

import com.coze.timer.model.TaskAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TaskAssignmentMapper {
    int insert(TaskAssignment assignment);
    
    TaskAssignment findByTaskId(@Param("taskId") String taskId);
    
    List<TaskAssignment> findByInstanceId(@Param("instanceId") Long instanceId);
    
    int update(TaskAssignment assignment);
    
    int deleteByTaskId(@Param("taskId") String taskId);
} 