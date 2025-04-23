package com.coze.timer.controller;

import com.coze.timer.model.dto.TaskRequest;
import com.coze.timer.model.dto.TaskResponse;
import com.coze.timer.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务管理控制器
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "任务管理", description = "定时任务的创建、查询、取消和删除")
public class TaskController {
    
    @Autowired
    private TaskService taskService;
    
    /**
     * 添加定时任务
     */
    @PostMapping("/tasks")
    @Operation(
        summary = "创建定时任务",
        description = "创建一个新的定时任务，支持interval、once和cron三种类型",
        responses = {
            @ApiResponse(responseCode = "200", description = "任务创建成功", 
                content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "无效的请求参数")
        }
    )
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 查看任务详情
     */
    @GetMapping("/tasks/{taskId}")
    @Operation(
        summary = "获取任务详情",
        description = "根据任务ID获取任务的详细信息",
        responses = {
            @ApiResponse(responseCode = "200", description = "成功获取任务信息", 
                content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "404", description = "任务不存在")
        }
    )
    public ResponseEntity<TaskResponse> getTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        TaskResponse response = taskService.getTask(taskId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 取消任务
     */
    @PutMapping("/tasks/{taskId}/cancel")
    @Operation(
        summary = "取消任务",
        description = "取消正在执行的定时任务",
        responses = {
            @ApiResponse(responseCode = "200", description = "任务取消成功", 
                content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "404", description = "任务不存在")
        }
    )
    public ResponseEntity<TaskResponse> cancelTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        TaskResponse response = taskService.cancelTask(taskId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/tasks/{taskId}")
    @Operation(
        summary = "删除任务",
        description = "永久删除一个任务及其相关数据",
        responses = {
            @ApiResponse(responseCode = "200", description = "任务删除成功", 
                content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "404", description = "任务不存在")
        }
    )
    public ResponseEntity<TaskResponse> deleteTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        TaskResponse response = taskService.deleteTask(taskId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 按用户查询任务列表
     */
    @GetMapping("/users/{userId}/tasks")
    @Operation(
        summary = "获取用户任务列表",
        description = "获取指定用户的任务列表，支持按状态筛选和分页",
        responses = {
            @ApiResponse(responseCode = "200", description = "成功获取任务列表")
        }
    )
    public ResponseEntity<Map<String, Object>> getUserTasks(
            @Parameter(description = "用户ID") @PathVariable Integer userId,
            @Parameter(description = "任务状态: pending, running, completed, failed, stopped") 
            @RequestParam(required = false) String status,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        
        List<TaskResponse> tasks = taskService.getUserTasks(userId, status, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("tasks", tasks);
        response.put("total", tasks.size()); // 这里应该返回总数，简化处理
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取用户运行任务数
     */
    @GetMapping("/users/{userId}/tasks/running-count")
    @Operation(
        summary = "获取用户运行中任务数量",
        description = "获取指定用户当前正在运行的任务数量",
        responses = {
            @ApiResponse(responseCode = "200", description = "成功获取运行中任务数量")
        }
    )
    public ResponseEntity<Map<String, Object>> getRunningTaskCount(
            @Parameter(description = "用户ID") @PathVariable Integer userId) {
        int count = taskService.getRunningTaskCount(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("user_id", userId);
        response.put("running_tasks", count);
        response.put("last_updated", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
} 