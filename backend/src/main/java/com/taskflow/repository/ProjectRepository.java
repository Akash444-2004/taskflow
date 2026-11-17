package com.taskflow.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.taskflow.entity.Project;
import com.taskflow.enums.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @EntityGraph(attributePaths = "owner")
    @Query("""
            select p
            from Project p
            where (
                    p.owner.id = :userId
                    or exists (
                        select 1
                        from ProjectMember pm
                        where pm.project.id = p.id
                          and pm.user.id = :userId
                    )
            )
            """)
    Page<Project> findAccessibleByUserId(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    @Query("""
            select p
            from Project p
            where (
                    p.owner.id = :userId
                    or exists (
                        select 1
                        from ProjectMember pm
                        where pm.project.id = p.id
                          and pm.user.id = :userId
                    )
            )
              and (:status is null or p.status = :status)
              and (
                    :pattern is null
                    or lower(p.name) like :pattern
                    or lower(p.description) like :pattern
              )
            """)
    Page<Project> searchAccessible(
            @Param("userId") Long userId,
            @Param("status") ProjectStatus status,
            @Param("pattern") String pattern,
            Pageable pageable
    );

    Optional<Project> findByIdAndOwnerId(Long projectId, Long ownerId);

    boolean existsByIdAndOwnerId(Long projectId, Long ownerId);

    long countByOwnerId(Long ownerId);
}
