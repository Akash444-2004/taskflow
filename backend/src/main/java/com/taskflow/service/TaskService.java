package com.taskflow.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.taskflow.dto.common.PageResponse;
import com.taskflow.dto.task.TaskRequest;
import com.taskflow.dto.task.TaskResponse;
import com.taskflow.dto.user.UserSummary;
import com.taskflow.entity.Project;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.NotFoundException;
import com.taskflow.repository.ProjectMemberRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;

@Service
public class TaskService {

    private static final Set<String> ALLOWED_SORTS =
            Set.of("title", "createdAt", "updatedAt", "dueDate", "priority", "status");

    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;

    public TaskService(
            TaskRepository taskRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            ProjectAccessService projectAccessService
    ) {
        this.taskRepository = taskRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.projectAccessService = projectAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(
            Long projectId,
            Long userId,
            TaskStatus status,
            TaskPriority priority,
            Long assigneeId,
            String search,
            Pageable pageable
    ) {
        Project project = projectAccessService.requireMember(projectId, userId);

        Page<Task> page = taskRepository.search(
                projectId,
                status,
                priority,
                assigneeId,
                normalizePattern(search),
                sanitize(pageable)
        );

        return PageResponse.from(page, task -> toResponse(task, project.getName()));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> assignedToMe(
            Long userId,
            TaskStatus status,
            Pageable pageable
    ) {
        Page<Task> page = taskRepository.findAssignedToUser(
                userId,
                status,
                sanitize(pageable)
        );
        return PageResponse.from(page, task -> toResponse(task, task.getProject().getName()));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> upcoming(Long userId, int limit) {
        return taskRepository.findUpcomingForUser(
                        userId,
                        TaskStatus.COMPLETED,
                        PageRequest.of(0, Math.max(1, limit))
                )
                .stream()
                .map(task -> toResponse(task, task.getProject().getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long taskId, Long userId) {        Task task = findDetailed(taskId);
        projectAccessService.requireMember(task.getProject().getId(), userId);
        return toResponse(task, task.getProject().getName());
    }

    @Transactional
    public TaskResponse create(
            Long projectId,
            Long userId,
            TaskRequest request
    ) {
        Project project = projectAccessService.requireMember(projectId, userId);
        validateDates(request.startDate(), request.dueDate());

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));
        User assignee = resolveAssignee(project, request.assignedToId());

        Task task = new Task();
        task.setProject(project);
        task.setCreatedBy(creator);
        task.setAssignedTo(assignee);
        applyFields(task, request);
        task.setStatus(request.status() == null ? TaskStatus.TODO : request.status());

        taskRepository.saveAndFlush(task);
        return toResponse(task, project.getName());
    }

    @Transactional
    public TaskResponse update(Long taskId, Long userId, TaskRequest request) {
        Task task = findDetailed(taskId);
        Project project = projectAccessService.requireMember(task.getProject().getId(), userId);
        validateDates(request.startDate(), request.dueDate());

        User assignee = resolveAssignee(project, request.assignedToId());

        if (request.status() != null && request.status() != task.getStatus()) {
            validateTransition(task.getStatus(), request.status());
            task.setStatus(request.status());
        }

        task.setAssignedTo(assignee);
        applyFields(task, request);

        return toResponse(task, task.getProject().getName());
    }

    @Transactional
    public TaskResponse updateStatus(Long taskId, Long userId, TaskStatus newStatus) {
        Task task = findDetailed(taskId);
        projectAccessService.requireMember(task.getProject().getId(), userId);

        if (newStatus != task.getStatus()) {
            validateTransition(task.getStatus(), newStatus);
            task.setStatus(newStatus);
        }

        return toResponse(task, task.getProject().getName());
    }

    @Transactional
    public void delete(Long taskId, Long userId) {
        Task task = findDetailed(taskId);
        Project project = task.getProject();
        projectAccessService.requireMember(project.getId(), userId);

        boolean canDelete = ProjectAccessService.isOwner(project, userId)
                || task.getCreatedBy().getId().equals(userId);

        if (!canDelete) {
            throw new ForbiddenException(
                    "TASK_DELETE_FORBIDDEN",
                    "Only the project owner or task creator can delete this task."
            );
        }

        taskRepository.delete(task);
    }

    private Task findDetailed(Long taskId) {
        return taskRepository.findDetailedById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));
    }

    private User resolveAssignee(Project project, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new NotFoundException(
                        "ASSIGNEE_NOT_FOUND",
                        "Assigned user not found."
                ));

        if (!assignee.isEnabled()) {
            throw new BadRequestException(
                    "ASSIGNEE_DISABLED",
                    "Tasks cannot be assigned to a disabled account."
            );
        }

        boolean isProjectMember = ProjectAccessService.isOwner(project, assigneeId)
                || projectMemberRepository.existsByProjectIdAndUserId(project.getId(), assigneeId);

        if (!isProjectMember) {
            throw new BadRequestException(
                    "ASSIGNEE_NOT_MEMBER",
                    "Tasks can only be assigned to project members."
            );
        }

        return assignee;
    }

    private static void applyFields(Task task, TaskRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(normalizeText(request.description()));
        task.setPriority(request.priority() == null ? TaskPriority.MEDIUM : request.priority());
        task.setStartDate(request.startDate());
        task.setDueDate(request.dueDate());
    }

    static void validateTransition(TaskStatus from, TaskStatus to) {
        boolean allowed = switch (from) {
            case TODO -> to == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> to == TaskStatus.TODO || to == TaskStatus.COMPLETED;
            case COMPLETED -> to == TaskStatus.IN_PROGRESS;
        };

        if (!allowed) {
            throw new BadRequestException(
                    "INVALID_STATUS_TRANSITION",
                    "Tasks cannot move from " + from + " to " + to + "."
            );
        }
    }

    private static void validateDates(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            throw new BadRequestException(
                    "INVALID_DATE_RANGE",
                    "Due date cannot be before the start date."
            );
        }
    }

    private static TaskResponse toResponse(Task task, String projectName) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getProject().getId(),
                projectName,
                task.getAssignedTo() == null ? null : UserSummary.from(task.getAssignedTo()),
                UserSummary.from(task.getCreatedBy()),
                task.getPriority(),
                task.getStatus(),
                task.getStartDate(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
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
