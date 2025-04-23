package com.coze.timer.mapper;

import com.coze.timer.model.TaskAssignment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskAssignmentMapper 测试类
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TaskAssignmentMapperTest {

    @Autowired
    private TaskAssignmentMapper taskAssignmentMapper;

    @Test
    public void testInsertAndFindByTaskId() {
        // 创建测试任务分配
        TaskAssignment assignment = new TaskAssignment();
        assignment.setTaskId(UUID.randomUUID().toString());
        assignment.setInstanceId(1L);

        // 插入任务分配
        int result = taskAssignmentMapper.insert(assignment);
        assertEquals(1, result);

        // 查询任务分配
        TaskAssignment foundAssignment = taskAssignmentMapper.findByTaskId(assignment.getTaskId());
        assertNotNull(foundAssignment);
        assertEquals(assignment.getTaskId(), foundAssignment.getTaskId());
        assertEquals(assignment.getInstanceId(), foundAssignment.getInstanceId());
    }

    @Test
    public void testFindByInstanceId() {
        // 创建测试任务分配
        TaskAssignment assignment = new TaskAssignment();
        assignment.setTaskId(UUID.randomUUID().toString());
        assignment.setInstanceId(1L);
        taskAssignmentMapper.insert(assignment);

        // 查询实例的任务分配
        List<TaskAssignment> assignments = taskAssignmentMapper.findByInstanceId(1L);
        assertFalse(assignments.isEmpty());
        assertEquals(assignment.getTaskId(), assignments.get(0).getTaskId());
    }

    @Test
    public void testUpdate() {
        // 创建测试任务分配
        TaskAssignment assignment = new TaskAssignment();
        assignment.setTaskId(UUID.randomUUID().toString());
        assignment.setInstanceId(1L);
        taskAssignmentMapper.insert(assignment);

        // 更新任务分配
        assignment.setInstanceId(2L);
        int result = taskAssignmentMapper.update(assignment);
        assertEquals(1, result);

        // 验证更新结果
        TaskAssignment updatedAssignment = taskAssignmentMapper.findByTaskId(assignment.getTaskId());
        assertEquals(2L, updatedAssignment.getInstanceId());
    }

    @Test
    public void testDeleteByTaskId() {
        // 创建测试任务分配
        TaskAssignment assignment = new TaskAssignment();
        assignment.setTaskId(UUID.randomUUID().toString());
        assignment.setInstanceId(1L);
        taskAssignmentMapper.insert(assignment);

        // 删除任务分配
        int result = taskAssignmentMapper.deleteByTaskId(assignment.getTaskId());
        assertEquals(1, result);

        // 验证删除结果
        TaskAssignment deletedAssignment = taskAssignmentMapper.findByTaskId(assignment.getTaskId());
        assertNull(deletedAssignment);
    }
} 