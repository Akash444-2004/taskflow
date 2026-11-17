package com.taskflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.taskflow.dto.comment.CommentRequest;
import com.taskflow.dto.comment.CommentResponse;
import com.taskflow.enums.Role;
import com.taskflow.security.TaskFlowUserPrincipal;
import com.taskflow.service.CommentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/tasks/{taskId}/comments")
    public List<CommentResponse> list(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long taskId
    ) {
        return commentService.list(taskId, principal.id());
    }

    @PostMapping("/tasks/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long taskId,
            @Valid @RequestBody CommentRequest request
    ) {
        return commentService.create(taskId, principal.id(), request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long commentId
    ) {
        commentService.delete(commentId, principal.id(), principal.role() == Role.ADMIN);
    }
}
