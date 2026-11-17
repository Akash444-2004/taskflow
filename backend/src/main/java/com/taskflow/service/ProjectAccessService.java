package com.taskflow.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskflow.entity.Project;
import com.taskflow.enums.ProjectRole;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.NotFoundException;
import com.taskflow.repository.ProjectMemberRepository;
import com.taskflow.repository.ProjectRepository;

@Service
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAccessService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional(readOnly = true)
    public Project requireMember(Long projectId, Long userId) {
        Project project = findProject(projectId);
        if (!isOwner(project, userId)
                && !projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ForbiddenException(
                    "PROJECT_ACCESS_DENIED",
                    "You do not have access to this project."
            );
        }
        return project;
    }

    @Transactional(readOnly = true)
    public Project requireOwner(Long projectId, Long userId) {
        Project project = findProject(projectId);
        if (!isOwner(project, userId)) {
            throw new ForbiddenException(
                    "PROJECT_OWNER_REQUIRED",
                    "Only the project owner can perform this action."
            );
        }
        return project;
    }

    public ProjectRole roleFor(Project project, Long userId) {
        if (isOwner(project, userId)) {
            return ProjectRole.OWNER;
        }
        return projectMemberRepository.findByProjectIdAndUserId(project.getId(), userId)
                .map(member -> member.getProjectRole())
                .orElse(null);
    }

    public static boolean isOwner(Project project, Long userId) {
        return project.getOwner().getId().equals(userId);
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException(
                        "PROJECT_NOT_FOUND",
                        "Project not found."
                ));
    }
}
