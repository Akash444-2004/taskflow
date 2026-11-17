package com.taskflow.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.taskflow.dto.common.PageResponse;
import com.taskflow.dto.project.AddProjectMemberRequest;
import com.taskflow.dto.project.ProjectMemberResponse;
import com.taskflow.dto.project.ProjectRequest;
import com.taskflow.dto.project.ProjectResponse;
import com.taskflow.enums.ProjectStatus;
import com.taskflow.security.TaskFlowUserPrincipal;
import com.taskflow.service.ProjectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public PageResponse<ProjectResponse> list(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 12, sort = "updatedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return projectService.list(principal.id(), status, search, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @Valid @RequestBody ProjectRequest request
    ) {
        return projectService.create(principal.id(), request);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long projectId
    ) {
        return projectService.get(projectId, principal.id());
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequest request
    ) {
        return projectService.update(projectId, principal.id(), request);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long projectId
    ) {
        projectService.delete(projectId, principal.id());
    }

    @GetMapping("/{projectId}/members")
    public List<ProjectMemberResponse> listMembers(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long projectId
    ) {
        return projectService.listMembers(projectId, principal.id());
    }

    @PostMapping("/{projectId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse addMember(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long projectId,
            @Valid @RequestBody AddProjectMemberRequest request
    ) {
        return projectService.addMember(projectId, principal.id(), request);
    }

    @DeleteMapping("/{projectId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long projectId
    ) {
        projectService.leave(projectId, principal.id());
    }

    @DeleteMapping("/{projectId}/members/{memberUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @PathVariable Long projectId,
            @PathVariable Long memberUserId
    ) {
        projectService.removeMember(projectId, principal.id(), memberUserId);
    }
}
