package com.taskflow.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
class AdminDashboardIntegrationTest {

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
    void dashboardSummarisesAssignedWorkAndProjects() throws Exception {
        String token = register("Dashboard Owner", "dashboard@example.com");
        long userId = userId("dashboard@example.com");

        long projectId = createProject(token, "Dashboard Project");
        createTask(token, projectId, "Assigned task", userId, LocalDate.now().plusDays(3));
        createTask(token, projectId, "Backlog task", null, null);

        mockMvc.perform(get("/api/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectCount").value(1))
                .andExpect(jsonPath("$.assignedTaskCount").value(1))
                .andExpect(jsonPath("$.completedTaskCount").value(0))
                .andExpect(jsonPath("$.dueSoonTaskCount").value(1))
                .andExpect(jsonPath("$.overdueTaskCount").value(0))
                .andExpect(jsonPath("$.tasksByStatus.TODO").value(1))
                .andExpect(jsonPath("$.upcomingTasks.length()").value(1))
                .andExpect(jsonPath("$.upcomingTasks[0].title").value("Assigned task"))
                .andExpect(jsonPath("$.recentProjects.length()").value(1))
                .andExpect(jsonPath("$.recentProjects[0].id").value(projectId));
    }

    @Test
    void adminEndpointsRequireAdministratorRole() throws Exception {
        String token = register("Regular User", "regular@example.com");

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminListsSearchesAndDisablesAccounts() throws Exception {
        register("Member Account", "member-account@example.com");
        String adminToken = createAndLoginAdmin("admin@example.com");
        long adminId = userId("admin@example.com");
        long memberId = userId("member-account@example.com");

        mockMvc.perform(get("/api/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.enabledUsers").value(2));

        mockMvc.perform(get("/api/admin/users")
                        .param("search", "MEMBER-ACCOUNT")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("member-account@example.com"));

        mockMvc.perform(patch("/api/admin/users/{id}/enabled", memberId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "member-account@example.com",
                                "password", PASSWORD
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/admin/users/{id}/enabled", adminId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_DISABLE_SELF"));

        mockMvc.perform(patch("/api/admin/users/{id}/enabled", memberId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "member-account@example.com",
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("member-account@example.com"));
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

    private void createTask(
            String token,
            long projectId,
            String title,
            Long assigneeId,
            LocalDate dueDate
    ) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", title);
        if (assigneeId != null) {
            body.put("assignedToId", assigneeId);
        }
        if (dueDate != null) {
            body.put("dueDate", dueDate.toString());
        }

        mockMvc.perform(post("/api/projects/{id}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    private String createAndLoginAdmin(String email) throws Exception {
        User admin = new User();
        admin.setFullName("Platform Admin");
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

    private long userId(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    private static long id(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), path)).longValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
