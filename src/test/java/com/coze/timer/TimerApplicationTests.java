package com.coze.timer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 定时任务应用程序基本测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class TimerApplicationTests {

    /**
     * 测试应用程序上下文加载
     */
    @Test
    public void contextLoads() {
        // 测试应用程序上下文是否成功加载
    }
} 