package com.taskflow.dto.comment;

import java.time.Instant;

import com.taskflow.dto.user.UserSummary;

public record CommentResponse(
        Long id,
        String content,
        Long taskId,
        UserSummary author,
        Instant createdAt
) {
}
