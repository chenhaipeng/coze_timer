package com.coze.timer;

import com.coze.timer.model.dto.TaskRequest;
import com.coze.timer.model.dto.TaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 定时任务系统集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TaskIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * 测试创建一次性任务
     */
    @Test
    public void testCreateOnceTask() {
        // 创建一次性任务请求
        TaskRequest request = createSampleOnceTaskRequest();
        
        // 发送创建任务请求
        String url = "http://localhost:" + port + "/api/tasks";
        ResponseEntity<TaskResponse> response = restTemplate.postForEntity(url, request, TaskResponse.class);
        
        // 断言响应成功
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getTaskId());
        assertEquals("pending", response.getBody().getStatus());
        
        // 验证任务已保存到数据库
        String taskId = response.getBody().getTaskId();
        String getUrl = "http://localhost:" + port + "/api/tasks/" + taskId;
        ResponseEntity<TaskResponse> getResponse = restTemplate.getForEntity(getUrl, TaskResponse.class);
        
        assertTrue(getResponse.getStatusCode().is2xxSuccessful());
        assertNotNull(getResponse.getBody());
        assertEquals(request.getHttpEndpoint(), getResponse.getBody().getHttpEndpoint());
    }
    
    /**
     * 测试获取任务详情
     */
    @Test
    public void testGetTaskDetails() {
        // 先创建一个任务
        TaskRequest request = createSampleOnceTaskRequest();
        String url = "http://localhost:" + port + "/api/tasks";
        ResponseEntity<TaskResponse> createResponse = restTemplate.postForEntity(url, request, TaskResponse.class);
        String taskId = createResponse.getBody().getTaskId();
        
        // 获取任务详情
        String getUrl = "http://localhost:" + port + "/api/tasks/" + taskId;
        ResponseEntity<TaskResponse> getResponse = restTemplate.getForEntity(getUrl, TaskResponse.class);
        
        // 断言获取成功
        assertTrue(getResponse.getStatusCode().is2xxSuccessful());
        assertNotNull(getResponse.getBody());
        assertEquals(taskId, getResponse.getBody().getTaskId());
    }
    
    /**
     * 测试取消任务
     */
    @Test
    public void testCancelTask() {
        // 先创建一个任务
        TaskRequest request = createSampleOnceTaskRequest();
        String url = "http://localhost:" + port + "/api/tasks";
        ResponseEntity<TaskResponse> createResponse = restTemplate.postForEntity(url, request, TaskResponse.class);
        String taskId = createResponse.getBody().getTaskId();
        
        // 取消任务
        String cancelUrl = "http://localhost:" + port + "/api/tasks/" + taskId + "/cancel";
        ResponseEntity<TaskResponse> cancelResponse = restTemplate.exchange(
                cancelUrl, HttpMethod.PUT, new HttpEntity<>(new HttpHeaders()), TaskResponse.class);
        
        // 断言取消成功
        assertTrue(cancelResponse.getStatusCode().is2xxSuccessful());
        assertNotNull(cancelResponse.getBody());
        assertEquals("stopped", cancelResponse.getBody().getStatus());
        
        // 验证数据库中任务状态已更新
        String getUrl = "http://localhost:" + port + "/api/tasks/" + taskId;
        ResponseEntity<TaskResponse> getResponse = restTemplate.getForEntity(getUrl, TaskResponse.class);
        assertTrue(getResponse.getStatusCode().is2xxSuccessful());
        assertEquals("stopped", getResponse.getBody().getStatus());
    }
    
    /**
     * 创建一个样例一次性任务请求
     */
    private TaskRequest createSampleOnceTaskRequest() {
        TaskRequest request = new TaskRequest();
        request.setType("once");
        request.setHttpEndpoint("http://example.com/api/test");
        request.setMethod("GET");
        request.setUserId(1);
        request.setStartTime(LocalDateTime.now().plusMinutes(5));
        return request;
    }
} 