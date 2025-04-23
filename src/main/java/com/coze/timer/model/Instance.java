package com.coze.timer.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Instance {
    private Long id;
    private String instanceName;
    private String ipAddress;
    private Integer port;
    private String status;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 