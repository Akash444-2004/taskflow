package com.taskflow.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.taskflow.entity.Task;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = { "assignedTo", "createdBy" })
    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = { "project", "assignedTo", "createdBy" })
    Page<Task> findByAssignedToId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = { "project", "assignedTo", "createdBy" })
    Optional<Task> findByIdAndProjectId(Long taskId, Long projectId);

    @EntityGraph(attributePaths = { "project", "project.owner", "assignedTo", "createdBy" })
    @Query("select t from Task t where t.id = :taskId")
    Optional<Task> findDetailedById(@Param("taskId") Long taskId);

    @EntityGraph(attributePaths = { "assignedTo", "createdBy" })
    @Query("""
            select t
            from Task t
            where t.project.id = :projectId
              and (:status is null or t.status = :status)
              and (:priority is null or t.priority = :priority)
              and (:assigneeId is null or t.assignedTo.id = :assigneeId)
              and (
                    :pattern is null
                    or lower(t.title) like :pattern
                    or lower(t.description) like :pattern
              )
            """)
    Page<Task> search(
            @Param("projectId") Long projectId,
            @Param("status") TaskStatus status,
            @Param("priority") TaskPriority priority,
            @Param("assigneeId") Long assigneeId,
            @Param("pattern") String pattern,
            Pageable pageable
    );

    @EntityGraph(attributePaths = { "project", "project.owner", "assignedTo", "createdBy" })
    @Query("""
            select t
            from Task t
            where t.assignedTo.id = :userId
              and (:status is null or t.status = :status)
            """)
    Page<Task> findAssignedToUser(
            @Param("userId") Long userId,
            @Param("status") TaskStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = { "project", "project.owner", "assignedTo", "createdBy" })
    @Query("""
            select t
            from Task t
            where t.assignedTo.id = :userId
              and t.status <> :completed
              and t.dueDate is not null
            order by t.dueDate asc
            """)
    List<Task> findUpcomingForUser(
            @Param("userId") Long userId,
            @Param("completed") TaskStatus completed,
            Pageable pageable
    );

    long countByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, TaskStatus status);

    long countByAssignedToId(Long userId);

    long countByAssignedToIdAndStatus(Long userId, TaskStatus status);

    long countByAssignedToIdAndDueDateBetweenAndStatusNot(
            Long userId,
            LocalDate from,
            LocalDate to,
            TaskStatus status
    );

    long countByStatus(TaskStatus status);

    long countByAssignedToIdAndDueDateBeforeAndStatusNot(
            Long userId,
            LocalDate date,
            TaskStatus status
    );

    @Query("""
            select t.project.id as projectId,
                   count(t) as taskCount,
                   sum(case when t.status = :completed then 1 else 0 end) as completedTaskCount
            from Task t
            where t.project.id in :projectIds
            group by t.project.id
            """)
    List<ProjectTaskCount> countByProjectIds(
            @Param("projectIds") Collection<Long> projectIds,
            @Param("completed") TaskStatus completed
    );

    interface ProjectTaskCount {
        Long getProjectId();

        Long getTaskCount();

        Long getCompletedTaskCount();
    }
}
