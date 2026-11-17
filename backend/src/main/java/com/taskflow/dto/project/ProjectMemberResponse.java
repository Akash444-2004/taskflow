package com.taskflow.dto.project;

import java.time.Instant;

import com.taskflow.dto.user.UserSummary;
import com.taskflow.enums.ProjectRole;

public record ProjectMemberResponse(
        Long id,
        UserSummary user,
        ProjectRole projectRole,
        Instant joinedAt
) {
}
