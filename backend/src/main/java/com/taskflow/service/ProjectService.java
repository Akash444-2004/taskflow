package com.taskflow.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.taskflow.dto.common.PageResponse;
import com.taskflow.dto.project.AddProjectMemberRequest;
import com.taskflow.dto.project.ProjectMemberResponse;
import com.taskflow.dto.project.ProjectRequest;
import com.taskflow.dto.project.ProjectResponse;
import com.taskflow.dto.user.UserSummary;
import com.taskflow.entity.Project;
import com.taskflow.entity.ProjectMember;
import com.taskflow.entity.User;
import com.taskflow.enums.ProjectRole;
import com.taskflow.enums.ProjectStatus;
import com.taskflow.enums.TaskStatus;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ConflictException;
import com.taskflow.exception.NotFoundException;
import com.taskflow.repository.ProjectMemberRepository;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.TaskRepository.ProjectTaskCount;
import com.taskflow.repository.UserRepository;

@Service
public class ProjectService {

    private static final Set<String> ALLOWED_SORTS =
            Set.of("name", "createdAt", "updatedAt", "dueDate", "status");

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectAccessService projectAccessService
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectAccessService = projectAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(
            Long userId,
            ProjectStatus status,
            String search,
            Pageable pageable
    ) {
        Page<Project> page = projectRepository.searchAccessible(
                userId,
                status,
                normalizePattern(search),
                sanitize(pageable)
        );

        List<Long> projectIds = page.getContent().stream().map(Project::getId).toList();
        Map<Long, Long> memberCounts = new HashMap<>();
        Map<Long, ProjectTaskCount> taskCounts = new HashMap<>();
        Map<Long, ProjectRole> roles = new HashMap<>();

        if (!projectIds.isEmpty()) {
            projectMemberRepository.countByProjectIds(projectIds)
                    .forEach(count -> memberCounts.put(count.getProjectId(), count.getMemberCount()));
            taskRepository.countByProjectIds(projectIds, TaskStatus.COMPLETED)
                    .forEach(count -> taskCounts.put(count.getProjectId(), count));
            projectMemberRepository.findAllByUserIdAndProjectIds(userId, projectIds)
                    .forEach(member -> roles.put(
                            member.getProject().getId(),
                            member.getProjectRole()
                    ));
        }

        List<ProjectResponse> content = page.getContent().stream()
                .map(project -> {
                    ProjectTaskCount counts = taskCounts.get(project.getId());
                    ProjectRole role = ProjectAccessService.isOwner(project, userId)
                            ? ProjectRole.OWNER
                            : roles.getOrDefault(project.getId(), ProjectRole.MEMBER);
                    return toResponse(
                            project,
                            role,
                            memberCounts.getOrDefault(project.getId(), 0L),
                            counts == null ? 0L : counts.getTaskCount(),
                            counts == null ? 0L : counts.getCompletedTaskCount()
                    );
                })
                .toList();

        return PageResponse.of(page, content);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long projectId, Long userId) {
        Project project = projectAccessService.requireMember(projectId, userId);
        return toDetailResponse(project, projectAccessService.roleFor(project, userId));
    }

    @Transactional
    public ProjectResponse create(Long userId, ProjectRequest request) {
        validateDates(request.startDate(), request.dueDate());

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        Project project = new Project();
        project.setName(request.name().trim());
        project.setDescription(normalizeText(request.description()));
        project.setStartDate(request.startDate());
        project.setDueDate(request.dueDate());
        project.setStatus(request.status() == null ? ProjectStatus.ACTIVE : request.status());
        project.setOwner(owner);
        projectRepository.saveAndFlush(project);

        ProjectMember ownerMembership = new ProjectMember();
        ownerMembership.setProject(project);
        ownerMembership.setUser(owner);
        ownerMembership.setProjectRole(ProjectRole.OWNER);
        projectMemberRepository.save(ownerMembership);

        return toResponse(project, ProjectRole.OWNER, 1L, 0L, 0L);
    }

    @Transactional
    public ProjectResponse update(Long projectId, Long userId, ProjectRequest request) {
        validateDates(request.startDate(), request.dueDate());

        Project project = projectAccessService.requireOwner(projectId, userId);
        project.setName(request.name().trim());
        project.setDescription(normalizeText(request.description()));
        project.setStartDate(request.startDate());
        project.setDueDate(request.dueDate());
        if (request.status() != null) {
            project.setStatus(request.status());
        }

        return toDetailResponse(project, ProjectRole.OWNER);
    }

    @Transactional
    public void delete(Long projectId, Long userId) {
        Project project = projectAccessService.requireOwner(projectId, userId);
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(Long projectId, Long userId) {
        projectAccessService.requireMember(projectId, userId);
        return projectMemberRepository.findAllByProjectIdOrderByJoinedAtAsc(projectId)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public ProjectMemberResponse addMember(
            Long projectId,
            Long userId,
            AddProjectMemberRequest request
    ) {
        projectAccessService.requireOwner(projectId, userId);

        User memberUser = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new NotFoundException(
                        "USER_NOT_FOUND",
                        "No account exists with that email address."
                ));

        if (!memberUser.isEnabled()) {
            throw new ConflictException(
                    "USER_DISABLED",
                    "That account is disabled and cannot be added to a project."
            );
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, memberUser.getId())) {
            throw new ConflictException(
                    "ALREADY_PROJECT_MEMBER",
                    "That user is already a member of this project."
            );
        }

        ProjectMember member = new ProjectMember();
        member.setProject(projectRepository.getReferenceById(projectId));
        member.setUser(memberUser);
        member.setProjectRole(ProjectRole.MEMBER);
        projectMemberRepository.saveAndFlush(member);

        return toMemberResponse(member);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, Long memberUserId) {
        Project project = projectAccessService.requireOwner(projectId, userId);

        ProjectMember member = projectMemberRepository
                .findByProjectIdAndUserId(projectId, memberUserId)
                .orElseThrow(() -> new NotFoundException(
                        "PROJECT_MEMBER_NOT_FOUND",
                        "Project member not found."
                ));

        if (member.getProjectRole() == ProjectRole.OWNER
                || ProjectAccessService.isOwner(project, memberUserId)) {
            throw new BadRequestException(
                    "CANNOT_REMOVE_OWNER",
                    "The project owner cannot be removed."
            );
        }

        projectMemberRepository.delete(member);
    }

    @Transactional
    public void leave(Long projectId, Long userId) {
        Project project = projectAccessService.requireMember(projectId, userId);

        if (ProjectAccessService.isOwner(project, userId)) {
            throw new BadRequestException(
                    "OWNER_CANNOT_LEAVE",
                    "Transfer or delete the project instead of leaving it."
            );
        }

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    private ProjectResponse toDetailResponse(Project project, ProjectRole role) {
        long memberCount = projectMemberRepository.countByProjectId(project.getId());
        long taskCount = taskRepository.countByProjectId(project.getId());
        long completedTaskCount = taskRepository.countByProjectIdAndStatus(
                project.getId(),
                TaskStatus.COMPLETED
        );
        return toResponse(project, role, memberCount, taskCount, completedTaskCount);
    }

    private ProjectResponse toResponse(
            Project project,
            ProjectRole role,
            long memberCount,
            long taskCount,
            long completedTaskCount
    ) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                UserSummary.from(project.getOwner()),
                project.getStatus(),
                project.getStartDate(),
                project.getDueDate(),
                role,
                memberCount,
                taskCount,
                completedTaskCount,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember member) {
        return new ProjectMemberResponse(
                member.getId(),
                UserSummary.from(member.getUser()),
                member.getProjectRole(),
                member.getJoinedAt()
        );
    }

    private static void validateDates(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            throw new BadRequestException(
                    "INVALID_DATE_RANGE",
                    "Due date cannot be before the start date."
            );
        }
    }

    private static String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String normalizePattern(String search) {
        return StringUtils.hasText(search)
                ? "%" + search.trim().toLowerCase(Locale.ROOT) + "%"
                : null;
    }

    private static Pageable sanitize(Pageable pageable) {
        Sort sanitizedSort = Sort.by(pageable.getSort().stream()
                .filter(order -> ALLOWED_SORTS.contains(order.getProperty()))
                .toList());

        if (!sanitizedSort.isSorted()) {
            sanitizedSort = Sort.by(Sort.Direction.DESC, "updatedAt");
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sanitizedSort);
    }
}
