package com.taskflow.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.taskflow.entity.Task;
import com.taskflow.enums.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = { "assignedTo", "createdBy" })
    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = { "project", "assignedTo", "createdBy" })
    Page<Task> findByAssignedToId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = { "project", "assignedTo", "createdBy" })
    Optional<Task> findByIdAndProjectId(Long taskId, Long projectId);

    long countByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, TaskStatus status);

    long countByAssignedToId(Long userId);

    long countByAssignedToIdAndStatus(Long userId, TaskStatus status);

    long countByAssignedToIdAndDueDateBeforeAndStatusNot(
            Long userId,
            LocalDate date,
            TaskStatus status
    );
}
