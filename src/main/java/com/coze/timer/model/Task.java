package com.coze.timer.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务实体类
 */
@Data
public class Task {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 用户ID
     */
    private Integer userId;
    
    /**
     * 任务类型: interval(间隔执行)、once(一次性)、cron(计划任务)
     */
    private String type;
    
    /**
     * HTTP请求地址
     */
    private String httpEndpoint;
    
    /**
     * HTTP请求方法
     */
    private String method;
    
    /**
     * HTTP请求头(JSON格式)
     */
    private String headers;
    
    /**
     * 请求体内容(JSON格式)
     */
    private String requestBody;
    
    /**
     * 间隔时间(秒)
     */
    private Integer intervalSeconds;
    
    /**
     * Cron表达式
     */
    private String cronExpression;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 任务状态: pending、running、completed、failed、stopped
     */
    private String status;
    
    /**
     * 下次执行时间
     */
    private LocalDateTime nextRunTime;
    
    /**
     * 停止条件(JSON格式)
     */
    private String stopCondition;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
} 