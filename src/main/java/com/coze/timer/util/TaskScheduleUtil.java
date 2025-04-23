package com.coze.timer.util;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * 任务调度工具类
 */
@Component
public class TaskScheduleUtil {
    
    private CronParser cronParser;
    
    public TaskScheduleUtil() {
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING);
        this.cronParser = new CronParser(cronDefinition);
    }
    
    /**
     * 获取下一次执行时间
     */
    public LocalDateTime getNextRunTime(String cronExpression) {
        try {
            // 使用中国时区
            ZoneId chinaZone = ZoneId.of("Asia/Shanghai");
            ZonedDateTime now = ZonedDateTime.now(chinaZone);
            ExecutionTime executionTime = ExecutionTime.forCron(cronParser.parse(cronExpression));
            Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(now);
            
            return nextExecution.map(ZonedDateTime::toLocalDateTime).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 验证Cron表达式是否有效
     */
    public boolean isValidCronExpression(String cronExpression) {
        try {
            cronParser.parse(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 