package com.coze.timer.scheduler;

import com.coze.timer.executor.HttpTaskExecutor;
import com.coze.timer.model.Task;
import com.coze.timer.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 任务调度器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskScheduler {
    
    private final TaskService taskService;
    private final HttpTaskExecutor httpTaskExecutor;
    private final StringRedisTemplate stringRedisTemplate; // 使用Spring Boot提供的StringRedisTemplate
    
    @Value("${timer.distributed:true}")
    private boolean distributedMode;
    
    private static final String SCHEDULER_LOCK = "scheduler_lock";
    private static final long LOCK_EXPIRE_TIME = 10; // 锁过期时间(秒)
    
    /**
     * 定时扫描待执行任务
     * 每秒执行一次
     */
    @Scheduled(fixedRate = 1000)
    public void scanTasks() {
        // 如果启用分布式模式，需要获取分布式锁
        if (distributedMode) {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(SCHEDULER_LOCK, "1", LOCK_EXPIRE_TIME, TimeUnit.SECONDS);
            
            if (Boolean.FALSE.equals(acquired)) {
                // 未获取到锁，可能其他实例正在执行
                return;
            }
        }
        
        try {
            // 获取待执行的任务(状态为pending且执行时间已到)
            List<Task> tasksToExecute = taskService.getTasksToExecute(50);
            
            if (!tasksToExecute.isEmpty()) {
                log.info("扫描到{}个待执行任务", tasksToExecute.size());
                
                // 提交任务到执行器
                for (Task task : tasksToExecute) {
                    httpTaskExecutor.executeAsync(task);
                }
            }
        } catch (Exception e) {
            log.error("扫描任务过程中发生异常", e);
        } finally {
            // 释放分布式锁
            if (distributedMode) {
                stringRedisTemplate.delete(SCHEDULER_LOCK);
            }
        }
    }
} 