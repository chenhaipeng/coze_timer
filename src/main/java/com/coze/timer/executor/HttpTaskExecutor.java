package com.coze.timer.executor;

import com.coze.timer.mapper.TaskLogMapper;
import com.coze.timer.model.Task;
import com.coze.timer.model.TaskLog;
import com.coze.timer.service.TaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * HTTP任务执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpTaskExecutor {
    
    private final TaskService taskService;
    private final TaskLogMapper taskLogMapper;
    private final ObjectMapper objectMapper;
    
    @Value("${timer.executor.http-pool-size:200}")
    private int httpPoolSize;
    
    @Value("${timer.executor.connect-timeout:5000}")
    private int connectTimeout;
    
    @Value("${timer.executor.request-timeout:15000}")
    private int requestTimeout;
    
    @Value("${timer.executor.retry-count:3}")
    private int retryCount;
    
    @Value("${timer.executor.rate-limiter.enabled:true}")
    private boolean rateLimiterEnabled;
    
    @Value("${timer.executor.rate-limiter.capacity:100}")
    private int rateLimiterCapacity;
    
    @Value("${timer.executor.rate-limiter.refill-rate:10}")
    private int rateLimiterRefillRate;
    
    private OkHttpClient httpClient;
    private ExecutorService executorService;
    private Bucket rateLimiter;
    
    @PostConstruct
    public void init() {
        // 初始化HTTP客户端
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(requestTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(requestTimeout, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(httpPoolSize, 5, TimeUnit.MINUTES))
                .build();
        
        // 初始化线程池
        executorService = Executors.newFixedThreadPool(httpPoolSize);
        
        // 初始化令牌桶限流器
        if (rateLimiterEnabled) {
            Bandwidth limit = Bandwidth.classic(rateLimiterCapacity, 
                    Refill.greedy(rateLimiterRefillRate, Duration.ofSeconds(1)));
            rateLimiter = Bucket4j.builder().addLimit(limit).build();
        }
    }
    
    /**
     * 异步执行HTTP任务
     */
    public CompletableFuture<Void> executeAsync(Task task) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 检查是否达到流量限制
                if (rateLimiterEnabled && !rateLimiter.tryConsume(1)) {
                    log.warn("任务[{}]被限流，将在下一周期重试", task.getTaskId());
                    return;
                }
                
                // 尝试更新任务状态为running
                if (!taskService.updateTaskStatus(task.getTaskId(), "running")) {
                    log.warn("任务[{}]状态更新失败，可能已被其他节点处理", task.getTaskId());
                    return;
                }
                
                // 构建请求
                Request request = buildRequest(task);
                
                // 记录开始时间
                LocalDateTime startDateTime = LocalDateTime.now();
                
                // 执行HTTP请求
                try (Response response = httpClient.newCall(request).execute()) {
                    // 计算执行耗时
                    long executionTime = ChronoUnit.MILLIS.between(startDateTime, LocalDateTime.now());
                    
                    // 解析响应
                    int statusCode = response.code();
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    // 记录执行日志
                    TaskLog taskLog = TaskLog.builder()
                            .logId(UUID.randomUUID().toString())
                            .taskId(task.getTaskId())
                            .userId(task.getUserId())
                            .httpStatus(statusCode)
                            .responseBody(responseBody)
                            .executionTime((int) executionTime)
                            .build();
                    taskLogMapper.insert(taskLog);
                    
                    // 检查是否达到停止条件
                    if (checkStopCondition(task, statusCode, responseBody)) {
                        taskService.updateTaskStatus(task.getTaskId(), "completed");
                    }
                }
            } catch (Exception e) {
                log.error("执行任务[{}]失败", task.getTaskId(), e);
                try {
                    // 记录失败日志
                    TaskLog taskLog = TaskLog.builder()
                            .logId(UUID.randomUUID().toString())
                            .taskId(task.getTaskId())
                            .userId(task.getUserId())
                            .httpStatus(500)
                            .responseBody("执行异常: " + e.getMessage())
                            .executionTime(0)
                            .build();
                    taskLogMapper.insert(taskLog);
                    
                    // 尝试重试
                    if (handleRetry(task)) {
                        return;
                    }
                    
                    // 如果不能重试，更新任务状态为失败
                    taskService.updateTaskStatus(task.getTaskId(), "failed");
                } catch (Exception ex) {
                    log.error("记录任务[{}]失败日志异常", task.getTaskId(), ex);
                }
            }
        }, executorService);
    }
    
    /**
     * 构建HTTP请求
     */
    private Request buildRequest(Task task) throws JsonProcessingException {
        // 构建请求URL
        HttpUrl url = HttpUrl.parse(task.getHttpEndpoint());
        if (url == null) {
            throw new IllegalArgumentException("无效的HTTP URL: " + task.getHttpEndpoint());
        }
        
        // 构建请求构造器
        Request.Builder requestBuilder = new Request.Builder().url(url);
        
        // 添加请求头
        if (task.getHeaders() != null && !task.getHeaders().isEmpty()) {
            Map<String, String> headers = objectMapper.readValue(task.getHeaders(), 
                    new TypeReference<Map<String, String>>() {});
            headers.forEach(requestBuilder::addHeader);
        }
        
        // 根据HTTP方法构建请求体
        if ("POST".equalsIgnoreCase(task.getMethod())) {
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(task.getRequestBody(), mediaType);
            requestBuilder.post(requestBody);
        } else if ("GET".equalsIgnoreCase(task.getMethod())) {
            requestBuilder.get();
        }
        
        return requestBuilder.build();
    }
    
    /**
     * 检查是否达到停止条件
     */
    private boolean checkStopCondition(Task task, int statusCode, String responseBody) {
        if (task.getStopCondition() == null || task.getStopCondition().isEmpty()) {
            return false;
        }
        
        try {
            Map<String, Object> stopCondition = objectMapper.readValue(task.getStopCondition(), 
                    new TypeReference<Map<String, Object>>() {});
            
            // 检查最大执行次数
            if (stopCondition.containsKey("maxCnt")) {
                Integer maxCount = (Integer) stopCondition.get("maxCnt");
                int executionCount = taskLogMapper.countByTaskId(task.getTaskId());
                if (maxCount != null && executionCount >= maxCount) {
                    return true;
                }
            }
            
            // TODO: 实现更复杂的停止条件逻辑，如根据响应内容判断
            
            return false;
        } catch (Exception e) {
            log.error("解析停止条件失败", e);
            return false;
        }
    }
    
    /**
     * 处理重试逻辑
     */
    private boolean handleRetry(Task task) {
        // TODO: 实现指数退避重试逻辑
        return false;
    }
    
    /**
     * 关闭执行器
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
} 