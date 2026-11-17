package com.taskflow.service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskflow.dto.dashboard.DashboardResponse;
import com.taskflow.enums.TaskStatus;
import com.taskflow.repository.ProjectMemberRepository;
import com.taskflow.repository.TaskRepository;

@Service
public class DashboardService {

    private static final int UPCOMING_LIMIT = 5;
    private static final int RECENT_PROJECT_LIMIT = 5;
    private static final int DUE_SOON_DAYS = 7;

    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskService taskService;
    private final ProjectService projectService;

    public DashboardService(
            TaskRepository taskRepository,
            ProjectMemberRepository projectMemberRepository,
            TaskService taskService,
            ProjectService projectService
    ) {
        this.taskRepository = taskRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskService = taskService;
        this.projectService = projectService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(Long userId) {
        LocalDate today = LocalDate.now();

        Map<TaskStatus, Long> tasksByStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            tasksByStatus.put(status, taskRepository.countByAssignedToIdAndStatus(userId, status));
        }

        return new DashboardResponse(
                projectMemberRepository.countByUserId(userId),
                taskRepository.countByAssignedToId(userId),
                taskRepository.countByAssignedToIdAndStatus(userId, TaskStatus.COMPLETED),
                taskRepository.countByAssignedToIdAndDueDateBeforeAndStatusNot(
                        userId,
                        today,
                        TaskStatus.COMPLETED
                ),
                taskRepository.countByAssignedToIdAndDueDateBetweenAndStatusNot(
                        userId,
                        today,
                        today.plusDays(DUE_SOON_DAYS),
                        TaskStatus.COMPLETED
                ),
                tasksByStatus,
                taskService.upcoming(userId, UPCOMING_LIMIT),
                projectService.list(
                        userId,
                        null,
                        null,
                        PageRequest.of(
                                0,
                                RECENT_PROJECT_LIMIT,
                                Sort.by(Sort.Direction.DESC, "updatedAt")
                        )
                ).content()
        );
    }
}
