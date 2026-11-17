package com.taskflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.taskflow.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "user")
    List<Comment> findAllByTaskIdOrderByCreatedAtAsc(Long taskId);

    Optional<Comment> findByIdAndUserId(Long commentId, Long userId);
}
