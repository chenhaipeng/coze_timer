package com.coze.timer;

import com.coze.timer.model.dto.TaskRequest;
import com.coze.timer.model.dto.TaskResponse;
import com.coze.timer.util.MockHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * 定时任务执行测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TaskExecutionTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    
    private MockHttpServer mockServer;
    
    @BeforeEach
    public void setup() throws IOException {
        // 启动模拟HTTP服务器
        mockServer = new MockHttpServer();
        mockServer.start(18080, "{\"status\":\"success\"}", 200);
    }
    
    @AfterEach
    public void tearDown() {
        // 关闭模拟HTTP服务器
        if (mockServer != null) {
            mockServer.stop();
        }
    }
    
    /**
     * 测试一次性任务的完整执行流程
     */
    @Test
    public void testOnceTaskExecution() {
        // 创建一个即将执行的一次性任务
        TaskRequest request = new TaskRequest();
        request.setType("once");
        request.setHttpEndpoint(mockServer.getUrl() + "/api/callback");
        request.setMethod("GET");
        request.setUserId(1);
        // 设置为5秒后执行
        request.setStartTime(LocalDateTime.now().plusSeconds(5));
        
        // 创建任务
        String url = "http://localhost:" + port + "/api/tasks";
        ResponseEntity<TaskResponse> response = restTemplate.postForEntity(url, request, TaskResponse.class);
        
        // 断言创建成功
        assertTrue(response.getStatusCode().is2xxSuccessful());
        String taskId = response.getBody().getTaskId();
        
        // 等待任务执行完成，最多等待15秒
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(() -> {
                ResponseEntity<TaskResponse> taskResponse = restTemplate.getForEntity(
                        "http://localhost:" + port + "/api/tasks/" + taskId, 
                        TaskResponse.class);
                
                // 任务执行完成后状态应该为completed
                return taskResponse.getBody() != null && 
                       "completed".equals(taskResponse.getBody().getStatus());
            });
        
        // 获取最终的任务状态
        ResponseEntity<TaskResponse> finalResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/tasks/" + taskId, 
                TaskResponse.class);
        
        // 断言任务已完成，并且有执行记录
        assertEquals("completed", finalResponse.getBody().getStatus());
        assertNotNull(finalResponse.getBody().getLastExecution());
        assertEquals(200, finalResponse.getBody().getLastExecution().getHttpStatus());
    }
    
    /**
     * 测试间隔任务的执行
     */
    @Test
    public void testIntervalTaskExecution() {
        // 创建一个间隔执行的任务
        TaskRequest request = new TaskRequest();
        request.setType("interval");
        request.setHttpEndpoint(mockServer.getUrl() + "/api/callback");
        request.setMethod("GET");
        request.setUserId(1);
        request.setIntervalSeconds(10); // 每10秒执行一次
        request.setStartTime(LocalDateTime.now().plusSeconds(2));
        
        // 创建任务
        String url = "http://localhost:" + port + "/api/tasks";
        ResponseEntity<TaskResponse> response = restTemplate.postForEntity(url, request, TaskResponse.class);
        
        // 断言创建成功
        assertTrue(response.getStatusCode().is2xxSuccessful());
        String taskId = response.getBody().getTaskId();
        
        // 等待任务至少执行一次，最多等待15秒
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(() -> {
                ResponseEntity<TaskResponse> taskResponse = restTemplate.getForEntity(
                        "http://localhost:" + port + "/api/tasks/" + taskId, 
                        TaskResponse.class);
                
                // 检查是否有执行记录
                return taskResponse.getBody() != null && 
                       taskResponse.getBody().getLastExecution() != null;
            });
        
        // 获取执行中的任务状态
        ResponseEntity<TaskResponse> runningResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/tasks/" + taskId, 
                TaskResponse.class);
        
        // 断言任务状态为运行中，并且有执行记录
        assertEquals("running", runningResponse.getBody().getStatus());
        assertNotNull(runningResponse.getBody().getLastExecution());
        assertEquals(200, runningResponse.getBody().getLastExecution().getHttpStatus());
        
        // 取消任务
        restTemplate.put(
                "http://localhost:" + port + "/api/tasks/" + taskId + "/cancel", 
                null);
        
        // 确认任务已取消
        ResponseEntity<TaskResponse> cancelledResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/tasks/" + taskId, 
                TaskResponse.class);
        
        assertEquals("stopped", cancelledResponse.getBody().getStatus());
    }
} 