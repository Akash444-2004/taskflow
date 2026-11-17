package com.taskflow.dto.project;

import java.time.Instant;
import java.time.LocalDate;

import com.taskflow.dto.user.UserSummary;
import com.taskflow.enums.ProjectRole;
import com.taskflow.enums.ProjectStatus;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        UserSummary owner,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate dueDate,
        ProjectRole myRole,
        long memberCount,
        long taskCount,
        long completedTaskCount,
        Instant createdAt,
        Instant updatedAt
) {
}
