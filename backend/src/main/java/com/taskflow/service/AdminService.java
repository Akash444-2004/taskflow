package com.taskflow.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.taskflow.dto.admin.AdminStatsResponse;
import com.taskflow.dto.common.PageResponse;
import com.taskflow.dto.user.UserResponse;
import com.taskflow.entity.User;
import com.taskflow.enums.TaskStatus;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.NotFoundException;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;

@Service
public class AdminService {

    private static final Set<String> ALLOWED_SORTS =
            Set.of("fullName", "email", "createdAt", "role", "enabled");

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final RefreshTokenService refreshTokenService;

    public AdminService(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String search, Pageable pageable) {
        Page<User> page = userRepository.searchByPattern(
                normalizePattern(search),
                sanitize(pageable)
        );
        return PageResponse.from(page, UserResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByEnabledTrue(),
                projectRepository.count(),
                taskRepository.count(),
                taskRepository.countByStatus(TaskStatus.COMPLETED)
        );
    }

    @Transactional
    public UserResponse setEnabled(Long adminId, Long userId, boolean enabled) {
        if (adminId.equals(userId) && !enabled) {
            throw new BadRequestException(
                    "CANNOT_DISABLE_SELF",
                    "You cannot disable your own administrator account."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));

        user.setEnabled(enabled);
        userRepository.saveAndFlush(user);

        if (!enabled) {
            refreshTokenService.revokeAllForUser(userId);
        }

        return UserResponse.from(user);
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
            sanitizedSort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sanitizedSort);
    }
}
