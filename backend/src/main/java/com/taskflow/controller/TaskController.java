package com.taskflow.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.taskflow.dto.common.PageResponse;
import com.taskflow.dto.task.TaskRequest;
import com.taskflow.dto.task.TaskResponse;
import com.taskflow.dto.task.TaskStatusRequest;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import com.taskflow.security.TaskFlowUserPrincipal;
import com.taskflow.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/projects/{projectId}/tasks")
    public PageResponse<TaskResponse> list(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 100, sort = "createdAt", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return taskService.list(
                projectId,
                principal.id(),
                status,
                priority,
                assignedToId,
                search,
                pageable
        );
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long projectId,
            @Valid @RequestBody TaskRequest request
    ) {
        return taskService.create(projectId, principal.id(), request);
    }

    @GetMapping("/tasks/assigned-to-me")
    public PageResponse<TaskResponse> assignedToMe(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(size = 50, sort = "dueDate", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return taskService.assignedToMe(principal.id(), status, pageable);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse get(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long taskId
    ) {
        return taskService.get(taskId, principal.id());
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse update(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequest request
    ) {
        return taskService.update(taskId, principal.id(), request);
    }

    @PatchMapping("/tasks/{taskId}/status")
    public TaskResponse updateStatus(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskStatusRequest request
    ) {
        return taskService.updateStatus(taskId, principal.id(), request.status());
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long taskId
    ) {
        taskService.delete(taskId, principal.id());
    }
}
