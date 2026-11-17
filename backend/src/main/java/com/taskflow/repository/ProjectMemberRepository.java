package com.taskflow.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.taskflow.entity.ProjectMember;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @EntityGraph(attributePaths = "user")
    List<ProjectMember> findAllByProjectIdOrderByJoinedAtAsc(Long projectId);

    @EntityGraph(attributePaths = "user")
    List<ProjectMember> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    long countByProjectId(Long projectId);

    long countByUserId(Long userId);

    void deleteByProjectIdAndUserId(Long projectId, Long userId);

    @Query("""
            select pm
            from ProjectMember pm
            where pm.user.id = :userId
              and pm.project.id in :projectIds
            """)
    List<ProjectMember> findAllByUserIdAndProjectIds(
            @Param("userId") Long userId,
            @Param("projectIds") Collection<Long> projectIds
    );

    @Query("""
            select pm.project.id as projectId, count(pm) as memberCount
            from ProjectMember pm
            where pm.project.id in :projectIds
            group by pm.project.id
            """)
    List<ProjectMemberCount> countByProjectIds(@Param("projectIds") Collection<Long> projectIds);

    interface ProjectMemberCount {
        Long getProjectId();

        Long getMemberCount();
    }
}
