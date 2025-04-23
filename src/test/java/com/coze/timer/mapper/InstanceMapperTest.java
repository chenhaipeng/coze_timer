package com.coze.timer.mapper;

import com.coze.timer.model.Instance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InstanceMapper 测试类
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InstanceMapperTest {

    @Autowired
    private InstanceMapper instanceMapper;

    @Test
    public void testInsertAndFindById() {
        // 创建测试实例
        Instance instance = new Instance();
        instance.setInstanceName("test-instance");
        instance.setIpAddress("127.0.0.1");
        instance.setPort(8080);
        instance.setStatus("active");
        instance.setLastHeartbeat(LocalDateTime.now());

        // 插入实例
        int result = instanceMapper.insert(instance);
        assertEquals(1, result);

        // 查询实例
        Instance foundInstance = instanceMapper.findById(instance.getId());
        assertNotNull(foundInstance);
        assertEquals(instance.getInstanceName(), foundInstance.getInstanceName());
        assertEquals(instance.getIpAddress(), foundInstance.getIpAddress());
        assertEquals(instance.getPort(), foundInstance.getPort());
    }

    @Test
    public void testFindByName() {
        // 创建测试实例
        Instance instance = new Instance();
        instance.setInstanceName("test-instance");
        instance.setIpAddress("127.0.0.1");
        instance.setPort(8080);
        instance.setStatus("active");
        instance.setLastHeartbeat(LocalDateTime.now());
        instanceMapper.insert(instance);

        // 按名称查询
        Instance foundInstance = instanceMapper.findByName("test-instance");
        assertNotNull(foundInstance);
        assertEquals(instance.getInstanceName(), foundInstance.getInstanceName());
    }

    @Test
    public void testFindByIpAndPort() {
        // 创建测试实例
        Instance instance = new Instance();
        instance.setInstanceName("test-instance");
        instance.setIpAddress("127.0.0.1");
        instance.setPort(8080);
        instance.setStatus("active");
        instance.setLastHeartbeat(LocalDateTime.now());
        instanceMapper.insert(instance);

        // 按IP和端口查询
        Instance foundInstance = instanceMapper.findByIpAndPort("127.0.0.1", 8080);
        assertNotNull(foundInstance);
        assertEquals(instance.getIpAddress(), foundInstance.getIpAddress());
        assertEquals(instance.getPort(), foundInstance.getPort());
    }

    @Test
    public void testFindActiveInstances() {
        // 创建测试实例
        Instance instance = new Instance();
        instance.setInstanceName("test-instance");
        instance.setIpAddress("127.0.0.1");
        instance.setPort(8080);
        instance.setStatus("active");
        instance.setLastHeartbeat(LocalDateTime.now());
        instanceMapper.insert(instance);

        // 查询活跃实例
        List<Instance> activeInstances = instanceMapper.findActiveInstances();
        assertFalse(activeInstances.isEmpty());
        assertEquals("active", activeInstances.get(0).getStatus());
    }

    @Test
    public void testUpdate() {
        // 创建测试实例
        Instance instance = new Instance();
        instance.setInstanceName("test-instance");
        instance.setIpAddress("127.0.0.1");
        instance.setPort(8080);
        instance.setStatus("active");
        instance.setLastHeartbeat(LocalDateTime.now());
        instanceMapper.insert(instance);

        // 更新实例
        instance.setStatus("inactive");
        int result = instanceMapper.update(instance);
        assertEquals(1, result);

        // 验证更新结果
        Instance updatedInstance = instanceMapper.findById(instance.getId());
        assertEquals("inactive", updatedInstance.getStatus());
    }

    @Test
    public void testUpdateStatus() {
        // 创建测试实例
        Instance instance = new Instance();
        instance.setInstanceName("test-instance");
        instance.setIpAddress("127.0.0.1");
        instance.setPort(8080);
        instance.setStatus("active");
        instance.setLastHeartbeat(LocalDateTime.now());
        instanceMapper.insert(instance);

        // 更新状态
        int result = instanceMapper.updateStatus(instance.getId(), "inactive");
        assertEquals(1, result);

        // 验证更新结果
        Instance updatedInstance = instanceMapper.findById(instance.getId());
        assertEquals("inactive", updatedInstance.getStatus());
    }

    @Test
    public void testUpdateHeartbeat() {
        // 创建测试实例
        Instance instance = new Instance();
        instance.setInstanceName("test-instance");
        instance.setIpAddress("127.0.0.1");
        instance.setPort(8080);
        instance.setStatus("active");
        instance.setLastHeartbeat(LocalDateTime.now().minusHours(1));
        instanceMapper.insert(instance);

        // 更新心跳
        int result = instanceMapper.updateHeartbeat(instance.getId());
        assertEquals(1, result);

        // 验证更新结果
        Instance updatedInstance = instanceMapper.findById(instance.getId());
        assertNotNull(updatedInstance.getLastHeartbeat());
        assertTrue(updatedInstance.getLastHeartbeat().isAfter(instance.getLastHeartbeat()));
    }

    @Test
    public void testDeleteById() {
        // 创建测试实例
        Instance instance = new Instance();
        instance.setInstanceName("test-instance");
        instance.setIpAddress("127.0.0.1");
        instance.setPort(8080);
        instance.setStatus("active");
        instance.setLastHeartbeat(LocalDateTime.now());
        instanceMapper.insert(instance);

        // 删除实例
        int result = instanceMapper.deleteById(instance.getId());
        assertEquals(1, result);

        // 验证删除结果
        Instance deletedInstance = instanceMapper.findById(instance.getId());
        assertNull(deletedInstance);
    }
} 