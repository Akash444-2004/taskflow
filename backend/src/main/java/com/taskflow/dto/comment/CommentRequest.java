package com.taskflow.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Comment content is required.")
        @Size(max = 5000, message = "Comment must be at most 5000 characters.")
        String content
) {
}
