package com.coze.timer.scheduler;

import com.coze.timer.executor.HttpTaskExecutor;
import com.coze.timer.mapper.InstanceMapper;
import com.coze.timer.mapper.TaskAssignmentMapper;
import com.coze.timer.model.Instance;
import com.coze.timer.model.Task;
import com.coze.timer.model.TaskAssignment;
import com.coze.timer.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class ScheduledTasks {
    @Autowired
    private TaskService taskService;

    @Autowired
    private HttpTaskExecutor httpTaskExecutor;

    @Autowired
    private InstanceMapper instanceMapper;

    @Autowired
    private TaskAssignmentMapper taskAssignmentMapper;

    @Value("${timer.instance.name}")
    private String instanceName;

    @Scheduled(fixedRate = 1000)
    public void scanTasks() {
        log.debug("开始扫描任务...");
        try {
            // 获取当前实例信息
            Instance currentInstance = instanceMapper.findByName(instanceName);
            log.debug("当前实例信息: {}", currentInstance);

            if (currentInstance == null || !"active".equals(currentInstance.getStatus())) {
                log.warn("当前实例[{}]不存在或未激活", instanceName);
                return;
            }

            // 获取分配给当前实例的任务
            List<TaskAssignment> assignments = taskAssignmentMapper.findByInstanceId(currentInstance.getId());
            log.debug("分配给当前实例的任务数量: {}", assignments.size());

            if (assignments.isEmpty()) {
                log.debug("当前实例[{}]没有分配的任务", instanceName);
                return;
            }

            for (TaskAssignment assignment : assignments) {
                // 获取待执行的任务
                List<Task> tasksToExecute = taskService.getTasksToExecute(assignment.getTaskId(), 50);
                log.debug("待执行任务数量: {}", tasksToExecute.size());

                if (!tasksToExecute.isEmpty()) {
                    log.info("扫描到{}个待执行任务", tasksToExecute.size());

                    // 提交任务到执行器
                    for (Task task : tasksToExecute) {
                        httpTaskExecutor.executeAsync(task);
                    }
                }
            }
        } catch (Exception e) {
            log.error("扫描任务过程中发生异常", e);
        }
        log.debug("扫描任务完成");
    }

    /**
     * 更新实例心跳
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    //@SchedulerLock(name = "updateHeartbeat", lockAtLeastFor = "PT30S")
    public void updateHeartbeat() {
        try {
            Instance currentInstance = instanceMapper.findByName(instanceName);
            if (currentInstance != null) {
                instanceMapper.updateHeartbeat(currentInstance.getId());
                log.debug("更新实例[{}]心跳成功", instanceName);
            } else {
                log.warn("实例[{}]不存在，无法更新心跳", instanceName);
            }
        } catch (Exception e) {
            log.error("更新实例心跳失败", e);
        }
    }

    /**
     * 分配任务到实例
     * 每5秒执行一次
     */
    @Scheduled(fixedRate = 5000)
    @SchedulerLock(name = "assignTasks", lockAtLeastFor = "PT3S")
    public void assignTasks() {
        try {
            // 获取当前实例信息
            Instance currentInstance = instanceMapper.findByName(instanceName);
            if (currentInstance == null || !"active".equals(currentInstance.getStatus())) {
                return;
            }

            // 获取未分配的任务
            List<Task> unassignedTasks = taskService.getUnassignedTasks(50);
            if (!unassignedTasks.isEmpty()) {
                log.info("发现{}个未分配的任务", unassignedTasks.size());

                // 分配任务到当前实例
                for (Task task : unassignedTasks) {
                    TaskAssignment assignment = new TaskAssignment();
                    assignment.setTaskId(task.getTaskId());
                    assignment.setInstanceId(currentInstance.getId());
                    assignment.setCreatedAt(LocalDateTime.now());
                    assignment.setUpdatedAt(LocalDateTime.now());

                    taskAssignmentMapper.insert(assignment);
                    log.info("任务[{}]已分配到实例[{}]", task.getTaskId(), instanceName);
                }
            }

            // 检查并重新分配失效实例的任务
            List<Instance> inactiveInstances = instanceMapper.findInactiveInstances();
            for (Instance inactiveInstance : inactiveInstances) {
                List<TaskAssignment> assignments = taskAssignmentMapper.findByInstanceId(inactiveInstance.getId());
                if (!assignments.isEmpty()) {
                    log.info("发现实例[{}]有{}个未完成的任务需要重新分配", 
                            inactiveInstance.getInstanceName(), assignments.size());
                    
                    for (TaskAssignment assignment : assignments) {
                        // 删除旧的任务分配
                        taskAssignmentMapper.deleteByTaskId(assignment.getTaskId());
                        
                        // 创建新的任务分配
                        TaskAssignment newAssignment = new TaskAssignment();
                        newAssignment.setTaskId(assignment.getTaskId());
                        newAssignment.setInstanceId(currentInstance.getId());
                        newAssignment.setCreatedAt(LocalDateTime.now());
                        newAssignment.setUpdatedAt(LocalDateTime.now());
                        
                        taskAssignmentMapper.insert(newAssignment);
                        log.info("任务[{}]已从失效实例[{}]重新分配到实例[{}]", 
                                assignment.getTaskId(), 
                                inactiveInstance.getInstanceName(), 
                                instanceName);
                    }
                }
            }
        } catch (Exception e) {
            log.error("分配任务过程中发生异常", e);
        }
    }
}