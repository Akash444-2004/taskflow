package com.taskflow.dto.dashboard;

import java.util.List;
import java.util.Map;

import com.taskflow.dto.project.ProjectResponse;
import com.taskflow.dto.task.TaskResponse;
import com.taskflow.enums.TaskStatus;

public record DashboardResponse(
        long projectCount,
        long assignedTaskCount,
        long completedTaskCount,
        long overdueTaskCount,
        long dueSoonTaskCount,
        Map<TaskStatus, Long> tasksByStatus,
        List<TaskResponse> upcomingTasks,
        List<ProjectResponse> recentProjects
) {
}
