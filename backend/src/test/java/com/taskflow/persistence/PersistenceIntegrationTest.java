package com.taskflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.taskflow.entity.Comment;
import com.taskflow.entity.Project;
import com.taskflow.entity.ProjectMember;
import com.taskflow.entity.RefreshToken;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.enums.ProjectRole;
import com.taskflow.enums.ProjectStatus;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import com.taskflow.repository.CommentRepository;
import com.taskflow.repository.ProjectMemberRepository;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.RefreshTokenRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    private static final String PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoO5Qn9Qn0p4Jm3j5o8z8x6i2gJY8Yh2O6";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsCompleteProjectGraphAndTimestamps() {
        User owner = saveUser("Avery Owner", "avery@example.com");
        User member = saveUser("Morgan Member", "morgan@example.com");
        Project project = saveProject(owner, "Website Redesign");

        ProjectMember ownerMembership = saveMembership(project, owner, ProjectRole.OWNER);
        ProjectMember memberMembership = saveMembership(project, member, ProjectRole.MEMBER);

        Task task = new Task();
        task.setTitle("Design the account settings page");
        task.setDescription("Cover profile, security, and notification settings.");
        task.setProject(project);
        task.setAssignedTo(member);
        task.setCreatedBy(owner);
        task.setPriority(TaskPriority.HIGH);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartDate(LocalDate.of(2026, 11, 17));
        task.setDueDate(LocalDate.of(2026, 11, 24));
        Task savedTask = taskRepository.saveAndFlush(task);

        Comment comment = new Comment();
        comment.setContent("The first design review is scheduled for Thursday.");
        comment.setTask(savedTask);
        comment.setUser(owner);
        Comment savedComment = commentRepository.saveAndFlush(comment);

        assertThat(ownerMembership.getJoinedAt()).isNotNull();
        assertThat(memberMembership.getJoinedAt()).isNotNull();
        assertThat(savedTask.getId()).isNotNull();
        assertThat(savedTask.getCreatedAt()).isNotNull();
        assertThat(savedTask.getUpdatedAt()).isNotNull();
        assertThat(savedComment.getId()).isNotNull();
        assertThat(savedComment.getCreatedAt()).isNotNull();

        assertThat(projectMemberRepository.findAllByProjectIdOrderByJoinedAtAsc(project.getId()))
                .extracting(membership -> membership.getUser().getEmail())
                .containsExactlyInAnyOrder("avery@example.com", "morgan@example.com");

        assertThat(taskRepository.findByProjectId(
                project.getId(),
                PageRequest.of(0, 10, Sort.by("createdAt").descending())
        ).getTotalElements()).isEqualTo(1);

        assertThat(commentRepository.findAllByTaskIdOrderByCreatedAtAsc(savedTask.getId()))
                .singleElement()
                .extracting(Comment::getContent)
                .isEqualTo("The first design review is scheduled for Thursday.");
    }

    @Test
    void enforcesCaseInsensitiveEmailUniqueness() {
        saveUser("First Account", "duplicate@example.com");

        User second = new User();
        second.setFullName("Second Account");
        second.setEmail("DUPLICATE@example.com");
        second.setPasswordHash(PASSWORD_HASH);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(second)
        );
    }

    @Test
    void preventsDuplicateProjectMembership() {
        User owner = saveUser("Owner", "owner-member@example.com");
        Project project = saveProject(owner, "Membership Validation");
        saveMembership(project, owner, ProjectRole.OWNER);

        ProjectMember duplicate = new ProjectMember();
        duplicate.setProject(project);
        duplicate.setUser(owner);
        duplicate.setProjectRole(ProjectRole.MEMBER);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> projectMemberRepository.saveAndFlush(duplicate)
        );
    }

    @Test
    void deletingProjectCascadesMembersTasksAndCommentsWithoutDeletingUsers() {
        User owner = saveUser("Cascade Owner", "cascade-owner@example.com");
        User member = saveUser("Cascade Member", "cascade-member@example.com");
        Project project = saveProject(owner, "Cascade Check");

        saveMembership(project, owner, ProjectRole.OWNER);
        saveMembership(project, member, ProjectRole.MEMBER);

        Task task = new Task();
        task.setTitle("Temporary task");
        task.setProject(project);
        task.setAssignedTo(member);
        task.setCreatedBy(owner);
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        Task savedTask = taskRepository.saveAndFlush(task);

        Comment comment = new Comment();
        comment.setContent("This record should be removed with its task.");
        comment.setTask(savedTask);
        comment.setUser(member);
        commentRepository.saveAndFlush(comment);

        Long projectId = project.getId();
        Long taskId = savedTask.getId();

        entityManager.flush();
        entityManager.clear();

        projectRepository.deleteById(projectId);
        projectRepository.flush();

        assertThat(projectRepository.findById(projectId)).isEmpty();
        assertThat(projectMemberRepository.countByProjectId(projectId)).isZero();
        assertThat(taskRepository.countByProjectId(projectId)).isZero();
        assertThat(commentRepository.findAllByTaskIdOrderByCreatedAtAsc(taskId)).isEmpty();
        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    void findsOnlyOwnedOrJoinedProjectsForUser() {
        User subject = saveUser("Project Subject", "subject@example.com");
        User other = saveUser("Other Owner", "other-owner@example.com");

        Project owned = saveProject(subject, "Owned Project");
        Project joined = saveProject(other, "Joined Project");
        Project privateProject = saveProject(other, "Private Project");

        saveMembership(owned, subject, ProjectRole.OWNER);
        saveMembership(joined, other, ProjectRole.OWNER);
        saveMembership(joined, subject, ProjectRole.MEMBER);

        Page<Project> result = projectRepository.findAccessibleByUserId(
                subject.getId(),
                PageRequest.of(0, 10, Sort.by("updatedAt").descending())
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Project::getName)
                .containsExactlyInAnyOrder("Owned Project", "Joined Project");
        assertThat(result.getContent())
                .extracting(Project::getName)
                .doesNotContain(privateProject.getName());
    }

    @Test
    void findsAssignedTasksWithoutLeakingOtherAssignments() {
        User subject = saveUser("Task Subject", "task-subject@example.com");
        User other = saveUser("Task Other", "task-other@example.com");
        Project project = saveProject(subject, "Task Assignment");

        saveMembership(project, subject, ProjectRole.OWNER);
        saveMembership(project, other, ProjectRole.MEMBER);

        Task subjectTask = saveTask(project, subject, subject, "Subject task");
        saveTask(project, other, subject, "Other task");

        Page<Task> result = taskRepository.findByAssignedToId(
                subject.getId(),
                PageRequest.of(0, 10, Sort.by("dueDate").ascending())
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(Task::getTitle)
                .containsExactly(subjectTask.getTitle());
    }

    @Test
    void preventsMultipleOwnerMembershipsForOneProject() {
        User owner = saveUser("First Owner", "first-owner@example.com");
        User secondOwner = saveUser("Second Owner", "second-owner@example.com");
        Project project = saveProject(owner, "Single Owner Rule");
        saveMembership(project, owner, ProjectRole.OWNER);

        ProjectMember conflictingOwner = new ProjectMember();
        conflictingOwner.setProject(project);
        conflictingOwner.setUser(secondOwner);
        conflictingOwner.setProjectRole(ProjectRole.OWNER);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> projectMemberRepository.saveAndFlush(conflictingOwner)
        );
    }

    @Test
    void preventsDeletingUserWithProjectHistory() {
        User owner = saveUser("Protected Owner", "protected-owner@example.com");
        Project project = saveProject(owner, "Protected History");
        saveMembership(project, owner, ProjectRole.OWNER);

        Long ownerId = owner.getId();
        entityManager.flush();
        entityManager.clear();

        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    userRepository.deleteById(ownerId);
                    userRepository.flush();
                }
        );
    }

    @Test
    void deletingTaskCascadesItsComments() {
        User owner = saveUser("Task Owner", "task-owner@example.com");
        Project project = saveProject(owner, "Task Cascade");
        saveMembership(project, owner, ProjectRole.OWNER);

        Task task = saveTask(project, owner, owner, "Task with comments");
        Comment comment = new Comment();
        comment.setContent("Removed with the task");
        comment.setTask(task);
        comment.setUser(owner);
        commentRepository.saveAndFlush(comment);

        Long taskId = task.getId();
        entityManager.flush();
        entityManager.clear();

        taskRepository.deleteById(taskId);
        taskRepository.flush();

        assertThat(commentRepository.findAllByTaskIdOrderByCreatedAtAsc(taskId)).isEmpty();
        assertThat(projectRepository.findById(project.getId())).isPresent();
        assertThat(userRepository.findById(owner.getId())).isPresent();
    }

    @Test
    void rotatesAndRevokesRefreshTokens() {
        User user = saveUser("Session User", "session@example.com");

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", ""));
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        RefreshToken saved = refreshTokenRepository.saveAndFlush(token);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(saved.getTokenHash()))
                .isPresent();

        int revoked = refreshTokenRepository.revokeAllActiveByUserId(
                user.getId(),
                Instant.now()
        );

        entityManager.flush();
        entityManager.clear();

        assertThat(revoked).isEqualTo(1);
        assertThat(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(saved.getTokenHash()))
                .isEmpty();
    }

    private User saveUser(String fullName, String email) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(PASSWORD_HASH);
        return userRepository.saveAndFlush(user);
    }

    private Project saveProject(User owner, String name) {
        Project project = new Project();
        project.setName(name);
        project.setDescription("Integration test project");
        project.setOwner(owner);
        project.setStartDate(LocalDate.of(2026, 11, 1));
        project.setDueDate(LocalDate.of(2026, 12, 31));
        project.setStatus(ProjectStatus.ACTIVE);
        return projectRepository.saveAndFlush(project);
    }

    private ProjectMember saveMembership(Project project, User user, ProjectRole role) {
        ProjectMember membership = new ProjectMember();
        membership.setProject(project);
        membership.setUser(user);
        membership.setProjectRole(role);
        return projectMemberRepository.saveAndFlush(membership);
    }

    private Task saveTask(Project project, User assignee, User creator, String title) {
        Task task = new Task();
        task.setTitle(title);
        task.setProject(project);
        task.setAssignedTo(assignee);
        task.setCreatedBy(creator);
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        task.setDueDate(LocalDate.of(2026, 12, 15));
        return taskRepository.saveAndFlush(task);
    }
}
