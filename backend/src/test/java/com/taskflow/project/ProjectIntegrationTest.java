package com.taskflow.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class ProjectIntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createsProjectWithOwnerMembershipAndListsIt() throws Exception {
        String ownerToken = register("Owner One", "owner1@example.com");

        MvcResult created = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Website Redesign",
                                "description", "Refresh the marketing site.",
                                "startDate", "2026-01-05",
                                "dueDate", "2026-02-15",
                                "status", "ACTIVE"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Website Redesign"))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.taskCount").value(0))
                .andExpect(jsonPath("$.completedTaskCount").value(0))
                .andExpect(jsonPath("$.owner.email").value("owner1@example.com"))
                .andReturn();

        long projectId = ((Number) JsonPath.read(
                created.getResponse().getContentAsString(),
                "$.id"
        )).longValue();

        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(projectId))
                .andExpect(jsonPath("$.content[0].myRole").value("OWNER"));

        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Refresh the marketing site."));

        mockMvc.perform(get("/api/projects/{id}/members", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].projectRole").value("OWNER"))
                .andExpect(jsonPath("$[0].user.email").value("owner1@example.com"));
    }

    @Test
    void validatesProjectInput() throws Exception {
        String token = register("Validation Owner", "validation@example.com");

        mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "",
                                "startDate", "2026-03-10",
                                "dueDate", "2026-03-01"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Invalid Dates",
                                "startDate", "2026-03-10",
                                "dueDate", "2026-03-01"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    void nonMemberCannotAccessOrModifyProject() throws Exception {
        String ownerToken = register("Owner Two", "owner2@example.com");
        String outsiderToken = register("Outsider", "outsider@example.com");
        long projectId = createProject(ownerToken, "Private Project");

        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROJECT_ACCESS_DENIED"));

        mockMvc.perform(get("/api/projects/{id}/members", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hijacked"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void ownerManagesMembersAndMembersCanViewButNotEdit() throws Exception {
        String ownerToken = register("Owner Three", "owner3@example.com");
        String memberToken = register("Team Member", "member3@example.com");
        long projectId = createProject(ownerToken, "Team Project");

        mockMvc.perform(post("/api/projects/{id}/members", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "MEMBER3@example.com"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectRole").value("MEMBER"))
                .andExpect(jsonPath("$.user.email").value("member3@example.com"));

        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.memberCount").value(2));

        mockMvc.perform(put("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Renamed by member"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROJECT_OWNER_REQUIRED"));

        mockMvc.perform(post("/api/projects/{id}/members", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "member3@example.com"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_PROJECT_MEMBER"));

        mockMvc.perform(post("/api/projects/{id}/members", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "nobody@example.com"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        long memberId = userRepository.findByEmailIgnoreCase("member3@example.com")
                .orElseThrow()
                .getId();

        mockMvc.perform(delete("/api/projects/{id}/members/{userId}", projectId, memberId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanLeaveButOwnerCannot() throws Exception {
        String ownerToken = register("Owner Four", "owner4@example.com");
        String memberToken = register("Leaving Member", "leaver@example.com");
        long projectId = createProject(ownerToken, "Leave Project");

        mockMvc.perform(post("/api/projects/{id}/members", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "leaver@example.com"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/projects/{id}/members/me", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/projects/{id}/members/me", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OWNER_CANNOT_LEAVE"));
    }

    @Test
    void ownerCanUpdateAndDeleteProject() throws Exception {
        String ownerToken = register("Owner Five", "owner5@example.com");
        long projectId = createProject(ownerToken, "Mutable Project");

        mockMvc.perform(put("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Renamed Project",
                                "description", "Updated description",
                                "status", "ON_HOLD",
                                "startDate", "2026-04-01",
                                "dueDate", "2026-05-01"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Project"))
                .andExpect(jsonPath("$.status").value("ON_HOLD"))
                .andExpect(jsonPath("$.myRole").value("OWNER"));

        mockMvc.perform(delete("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{id}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    private long createProject(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id"))
                .longValue();
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

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
