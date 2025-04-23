package com.coze.timer.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InstanceCluster {
    private Long id;
    private String clusterName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 