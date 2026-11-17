package com.taskflow.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.taskflow.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByEnabledTrue();

    @Query("""
            select u
            from User u
            where :pattern is null
               or lower(u.fullName) like :pattern
               or lower(u.email) like :pattern
            """)
    Page<User> searchByPattern(@Param("pattern") String pattern, Pageable pageable);
}
