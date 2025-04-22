package com.coze.timer.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务创建请求DTO
 */
@Data
@Schema(description = "任务创建请求")
public class TaskRequest {
    
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "1001", required = true)
    private Integer userId;
    
    /**
     * 任务类型: interval、once、cron
     */
    @NotBlank(message = "任务类型不能为空")
    @Pattern(regexp = "interval|once|cron", message = "任务类型只能是interval、once或cron")
    @Schema(description = "任务类型: interval(间隔任务)、once(一次性任务)、cron(计划任务)", example = "interval", required = true)
    private String type;
    
    /**
     * HTTP请求地址
     */
    @NotBlank(message = "HTTP请求地址不能为空")
    @Schema(description = "HTTP请求地址", example = "https://api.example.com/execute", required = true)
    private String httpEndpoint;
    
    /**
     * HTTP请求方法: GET、POST
     */
    @NotBlank(message = "请求方法不能为空")
    @Pattern(regexp = "GET|POST", message = "请求方法只能是GET或POST")
    @Schema(description = "HTTP请求方法", example = "POST", required = true, allowableValues = {"GET", "POST"})
    private String method;
    
    /**
     * HTTP请求头
     */
    @Schema(description = "HTTP请求头", example = "{\"Authorization\": \"Bearer token123\"}")
    private Map<String, String> headers;
    
    /**
     * 请求体数据
     */
    @Schema(description = "请求体数据", example = "{\"data\": \"example\"}")
    private Map<String, Object> body;
    
    /**
     * 间隔时间(秒)，间隔任务(interval)必填
     */
    @Min(value = 1, message = "间隔时间必须大于0")
    @Schema(description = "间隔时间(秒)，仅type=interval时必填", example = "20", minimum = "1")
    private Integer intervalSeconds;
    
    /**
     * Cron表达式，计划任务(cron)必填
     */
    @Schema(description = "Cron表达式，仅type=cron时必填", example = "0 0 12 * * ?")
    private String cronExpression;
    
    /**
     * 首次执行时间
     */
    @Schema(description = "首次执行时间", example = "2025-04-22T10:00:00Z")
    private LocalDateTime startTime;
    
    /**
     * 中止条件
     */
    @Schema(description = "中止条件")
    private StopCondition stopCondition;
    
    /**
     * 中止条件
     */
    @Data
    @Schema(description = "任务中止条件")
    public static class StopCondition {
        /**
         * 最大执行次数
         */
        @Schema(description = "最大执行次数", example = "300")
        private Integer maxCnt;
        
        /**
         * 完成条件
         */
        @Schema(description = "完成标志", example = "true")
        private Boolean finish;
    }
} 