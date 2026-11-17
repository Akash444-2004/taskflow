package com.taskflow.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.taskflow.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createsListsFiltersAndGetsTasks() throws Exception {
        String ownerToken = register("Task Owner", "taskowner@example.com");
        String memberToken = register("Task Member", "taskmember@example.com");
        long projectId = createProject(ownerToken, "Task Project");
        addMember(ownerToken, projectId, "taskmember@example.com");
        long memberId = userId("taskmember@example.com");

        MvcResult created = mockMvc.perform(post("/api/projects/{id}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Design landing page",
                                "description", "Hero, pricing, CTA",
                                "assignedToId", memberId,
                                "priority", "HIGH",
                                "startDate", "2026-05-01",
                                "dueDate", "2026-05-10"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Design landing page"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.assignedTo.email").value("taskmember@example.com"))
                .andExpect(jsonPath("$.createdBy.email").value("taskowner@example.com"))
                .andReturn();

        long taskId = id(created, "$.id");

        mockMvc.perform(get("/api/projects/{id}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(taskId));

        mockMvc.perform(get("/api/projects/{id}/tasks", projectId)
                        .param("status", "COMPLETED")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/projects/{id}/tasks", projectId)
                        .param("assignedToId", String.valueOf(memberId))
                        .param("search", "landing")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Hero, pricing, CTA"));
    }

    @Test
    void assignsOnlyProjectMembersAndValidatesDates() throws Exception {
        String ownerToken = register("Assign Owner", "assignowner@example.com");
        String outsiderToken = register("Assign Outsider", "assignoutsider@example.com");
        long projectId = createProject(ownerToken, "Assign Project");

        long outsiderId = userId("assignoutsider@example.com");

        mockMvc.perform(post("/api/projects/{id}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Assign to outsider",
                                "assignedToId", outsiderId
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASSIGNEE_NOT_MEMBER"));

        mockMvc.perform(post("/api/projects/{id}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Bad dates",
                                "startDate", "2026-06-10",
                                "dueDate", "2026-06-01"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));

        mockMvc.perform(get("/api/projects/{id}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void enforcesStatusTransitionsAndUpdatesTask() throws Exception {
        String ownerToken = register("Status Owner", "statusowner@example.com");
        long projectId = createProject(ownerToken, "Status Project");
        long taskId = createTask(ownerToken, projectId, "Move me");

        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "COMPLETED"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "IN_PROGRESS"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "COMPLETED"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Renamed task",
                                "description", "Updated",
                                "priority", "LOW",
                                "status", "COMPLETED",
                                "startDate", "2026-07-01",
                                "dueDate", "2026-07-15"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed task"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void onlyOwnerOrCreatorCanDeleteTask() throws Exception {
        String ownerToken = register("Delete Owner", "deleteowner@example.com");
        String memberToken = register("Delete Member", "deletemember@example.com");
        long projectId = createProject(ownerToken, "Delete Project");
        addMember(ownerToken, projectId, "deletemember@example.com");

        long memberCreatedTaskId = createTask(memberToken, projectId, "Member task");

        mockMvc.perform(delete("/api/tasks/{id}", memberCreatedTaskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isNoContent());

        long ownerCreatedTaskId = createTask(ownerToken, projectId, "Owner task");

        mockMvc.perform(delete("/api/tasks/{id}", ownerCreatedTaskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TASK_DELETE_FORBIDDEN"));

        mockMvc.perform(delete("/api/tasks/{id}", ownerCreatedTaskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void listsTasksAssignedToCurrentUser() throws Exception {
        String ownerToken = register("Mine Owner", "mineowner@example.com");
        String memberToken = register("Mine Member", "minemember@example.com");
        long projectId = createProject(ownerToken, "Mine Project");
        addMember(ownerToken, projectId, "minemember@example.com");
        long memberId = userId("minemember@example.com");

        createTask(ownerToken, projectId, "Assigned to member", memberId);
        createTask(ownerToken, projectId, "Unassigned");

        mockMvc.perform(get("/api/tasks/assigned-to-me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Assigned to member"));
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
        return createTask(token, projectId, title, null);
    }

    private long createTask(String token, long projectId, String title, Long assigneeId)
            throws Exception {
        Map<String, Object> body = assigneeId == null
                ? Map.of("title", title)
                : Map.of("title", title, "assignedToId", assigneeId);

        MvcResult result = mockMvc.perform(post("/api/projects/{id}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
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
