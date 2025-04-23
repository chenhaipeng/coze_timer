package com.coze.timer.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskAssignment {
    private Long id;
    private String taskId;
    private Long instanceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 