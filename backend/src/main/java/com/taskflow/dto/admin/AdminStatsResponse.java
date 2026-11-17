package com.taskflow.dto.admin;

public record AdminStatsResponse(
        long totalUsers,
        long enabledUsers,
        long totalProjects,
        long totalTasks,
        long completedTasks
) {
}
