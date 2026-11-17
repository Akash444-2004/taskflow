package com.taskflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.taskflow.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "user")
    List<Comment> findAllByTaskIdOrderByCreatedAtAsc(Long taskId);

    Optional<Comment> findByIdAndUserId(Long commentId, Long userId);

    @EntityGraph(attributePaths = { "user", "task", "task.project", "task.project.owner" })
    @Query("select c from Comment c where c.id = :commentId")
    Optional<Comment> findDetailedById(@Param("commentId") Long commentId);
}
