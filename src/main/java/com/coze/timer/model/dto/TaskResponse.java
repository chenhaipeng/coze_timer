package com.coze.timer.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "任务响应")
public class TaskResponse {
    
    /**
     * 结果状态
     */
    @Schema(description = "任务状态: pending、running、completed、failed、stopped", example = "running", 
            allowableValues = {"pending", "running", "completed", "failed", "stopped"})
    private String status;
    
    /**
     * 任务ID
     */
    @Schema(description = "任务ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String taskId;
    
    /**
     * 任务状态
     */
    private String taskStatus;
    
    /**
     * 任务类型
     */
    private String type;
    
    /**
     * HTTP请求地址
     */
    @Schema(description = "HTTP请求地址", example = "https://api.example.com/execute")
    private String httpEndpoint;
    
    /**
     * 最近一次执行记录
     */
    @Schema(description = "最近一次执行记录")
    private ExecutionRecord lastExecution;
    
    /**
     * 下次执行时间
     */
    @Schema(description = "下次执行时间", example = "2025-04-22T10:00:20Z")
    private LocalDateTime nextRunTime;
    
    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2025-04-22T09:59:00Z")
    private LocalDateTime createdAt;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 通用响应构造器
     */
    public static TaskResponse success(String taskId, String message) {
        TaskResponse response = new TaskResponse();
        response.setTaskId(taskId);
        response.setStatus("success");
        return response;
    }
    
    /**
     * 执行记录
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "任务执行记录")
    public static class ExecutionRecord {
        
        /**
         * 执行时间
         */
        @Schema(description = "执行时间", example = "2025-04-22T10:00:00Z")
        private LocalDateTime timestamp;
        
        /**
         * HTTP状态码
         */
        @Schema(description = "HTTP状态码", example = "200")
        private Integer httpStatus;
        
        /**
         * 响应内容
         */
        @Schema(description = "响应内容", example = "OK")
        private String responseBody;
    }
} 