package com.coze.timer.mapper;

import com.coze.timer.model.Instance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface InstanceMapper {
    int insert(Instance instance);
    
    Instance findById(@Param("id") Long id);
    
    Instance findByName(@Param("instanceName") String instanceName);
    
    Instance findByIpAndPort(@Param("ipAddress") String ipAddress, @Param("port") Integer port);
    
    List<Instance> findActiveInstances();
    
    int update(Instance instance);
    
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    
    int updateHeartbeat(@Param("id") Long id);
    
    int deleteById(@Param("id") Long id);
    
    /**
     * 查找失效的实例（超过2分钟未发送心跳的实例）
     */
    List<Instance> findInactiveInstances();
} 