package com.taskflow.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.taskflow.dto.comment.CommentRequest;
import com.taskflow.dto.comment.CommentResponse;
import com.taskflow.dto.user.UserSummary;
import com.taskflow.entity.Comment;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.NotFoundException;
import com.taskflow.repository.CommentRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;

    public CommentService(
            CommentRepository commentRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectAccessService projectAccessService
    ) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectAccessService = projectAccessService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long taskId, Long userId) {
        Task task = findTask(taskId);
        projectAccessService.requireMember(task.getProject().getId(), userId);

        return commentRepository.findAllByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(CommentService::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse create(Long taskId, Long userId, CommentRequest request) {
        Task task = findTask(taskId);
        projectAccessService.requireMember(task.getProject().getId(), userId);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setUser(author);
        comment.setContent(request.content().trim());
        commentRepository.saveAndFlush(comment);

        return toResponse(comment);
    }

    @Transactional
    public void delete(Long commentId, Long userId, boolean admin) {
        Comment comment = commentRepository.findDetailedById(commentId)
                .orElseThrow(() -> new NotFoundException(
                        "COMMENT_NOT_FOUND",
                        "Comment not found."
                ));

        Long projectId = comment.getTask().getProject().getId();
        if (!admin) {
            projectAccessService.requireMember(projectId, userId);
        }

        boolean isAuthor = comment.getUser().getId().equals(userId);
        boolean isProjectOwner = ProjectAccessService.isOwner(comment.getTask().getProject(), userId);

        if (!admin && !isAuthor && !isProjectOwner) {
            throw new ForbiddenException(
                    "COMMENT_DELETE_FORBIDDEN",
                    "Only the author or project owner can delete this comment."
            );
        }

        commentRepository.delete(comment);
    }

    private Task findTask(Long taskId) {
        return taskRepository.findDetailedById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found."));
    }

    private static CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getTask().getId(),
                UserSummary.from(comment.getUser()),
                comment.getCreatedAt()
        );
    }
}
