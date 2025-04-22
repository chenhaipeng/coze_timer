package com.coze.timer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * 定时器应用启动类
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.coze.timer.mapper")
public class TimerApplication {

    public static void main(String[] args) {
        // 设置JVM默认时区为中国时区
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(TimerApplication.class, args);
    }
} 