package com.coze.timer.config;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ZooKeeper配置类
 */
@Configuration
public class ZookeeperConfig {
    
    @Value("${timer.zookeeper.host:localhost}")
    private String host;
    
    @Value("${timer.zookeeper.port:2181}")
    private String port;
    
    @Value("${timer.zookeeper.session-timeout:60000}")
    private int sessionTimeout;
    
    @Value("${timer.zookeeper.connection-timeout:15000}")
    private int connectionTimeout;
    
    @Value("${timer.zookeeper.base-path:/coze/timer}")
    private String basePath;
    
    @Bean(initMethod = "start", destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        String connectString = host + ":" + port;
        
        return CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .sessionTimeoutMs(sessionTimeout)
                .connectionTimeoutMs(connectionTimeout)
                .retryPolicy(retryPolicy)
                .namespace(basePath.startsWith("/") ? basePath.substring(1) : basePath)
                .build();
    }
} 