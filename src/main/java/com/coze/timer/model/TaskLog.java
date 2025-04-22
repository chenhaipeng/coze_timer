package com.coze.timer.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务执行日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskLog {
    
    /**
     * 日志ID
     */
    private String logId;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 用户ID
     */
    private Integer userId;
    
    /**
     * HTTP状态码
     */
    private Integer httpStatus;
    
    /**
     * 响应内容
     */
    private String responseBody;
    
    /**
     * 执行耗时(毫秒)
     */
    private Integer executionTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
} 