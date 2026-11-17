package com.taskflow.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.taskflow.entity.User;
import com.taskflow.enums.Role;
import com.taskflow.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Test
    void addsListsAndDeletesOwnComment() throws Exception {
        String ownerToken = register("Comment Owner", "commentowner@example.com");
        String memberToken = register("Comment Member", "commentmember@example.com");
        long projectId = createProject(ownerToken, "Comment Project");
        addMember(ownerToken, projectId, "commentmember@example.com");
        long taskId = createTask(ownerToken, projectId, "Discuss this");

        MvcResult created = mockMvc.perform(post("/api/tasks/{id}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "Looks good to me."
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Looks good to me."))
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.author.email").value("commentmember@example.com"))
                .andReturn();

        long commentId = id(created, "$.id");

        mockMvc.perform(get("/api/tasks/{id}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(commentId));

        mockMvc.perform(post("/api/tasks/{id}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(delete("/api/comments/{id}", commentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void enforcesCommentDeletionPermissions() throws Exception {
        String ownerToken = register("Mod Owner", "modowner@example.com");
        String memberOneToken = register("Mod Member One", "modmember1@example.com");
        String memberTwoToken = register("Mod Member Two", "modmember2@example.com");
        long projectId = createProject(ownerToken, "Moderation Project");
        addMember(ownerToken, projectId, "modmember1@example.com");
        addMember(ownerToken, projectId, "modmember2@example.com");
        long taskId = createTask(ownerToken, projectId, "Moderate comments");

        long memberCommentId = createComment(memberOneToken, taskId, "Member one says hi");

        mockMvc.perform(delete("/api/comments/{id}", memberCommentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberTwoToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_DELETE_FORBIDDEN"));

        mockMvc.perform(delete("/api/comments/{id}", memberCommentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent());

        long ownerCommentId = createComment(ownerToken, taskId, "Owner comment");

        mockMvc.perform(delete("/api/comments/{id}", ownerCommentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberOneToken)))
                .andExpect(status().isForbidden());

        long adminTargetCommentId = createComment(memberOneToken, taskId, "Admin will remove this");
        String adminToken = createAndLoginAdmin("commentadmin@example.com");

        mockMvc.perform(delete("/api/comments/{id}", adminTargetCommentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonMemberCannotReadOrCreateComments() throws Exception {
        String ownerToken = register("Private Comment Owner", "pcowner@example.com");
        String outsiderToken = register("Private Outsider", "pcoutside@example.com");
        long projectId = createProject(ownerToken, "Private Comment Project");
        long taskId = createTask(ownerToken, projectId, "Private task");

        mockMvc.perform(get("/api/tasks/{id}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/tasks/{id}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Sneaky"))))
                .andExpect(status().isForbidden());
    }

    private long createProject(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result, "$.id");
    }

    private long createTask(String token, long projectId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects/{id}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", title))))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result, "$.id");
    }

    private long createComment(String token, long taskId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks/{id}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result, "$.id");
    }

    private void addMember(String token, long projectId, String email) throws Exception {
        mockMvc.perform(post("/api/projects/{id}/members", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isCreated());
    }

    private String register(String fullName, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", fullName,
                                "email", email,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        assertThat(token).isNotBlank();
        return token;
    }

    private String createAndLoginAdmin(String email) throws Exception {
        User admin = new User();
        admin.setFullName("Comment Admin");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(PASSWORD));
        admin.setRole(Role.ADMIN);
        userRepository.saveAndFlush(admin);

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    }

    private static long id(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), path)).longValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
