package com.taskflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.taskflow.config.JwtProperties;
import com.taskflow.entity.User;
import com.taskflow.enums.Role;
import com.taskflow.repository.RefreshTokenRepository;
import com.taskflow.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationIntegrationTest {

    private static final String REFRESH_COOKIE = "taskflow_refresh";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void csrfEndpointReturnsRawTokenMatchingCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");
        String cookie = cookieValue(result.getResponse(), CSRF_COOKIE);
        String setCookie = setCookieHeader(result.getResponse(), CSRF_COOKIE);

        assertThat(cookie).isEqualTo(token);
        assertThat(setCookie)
                .contains("Path=/")
                .doesNotContain("HttpOnly");

        // Servlet 6 cookies carry SameSite as an attribute. MockHttpServletResponse
        // does not serialize attributes into Set-Cookie, so inspect the cookie object.
        Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE);
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getAttribute("SameSite")).isEqualTo("Strict");
    }

    @Test
    void registersUserAndIssuesTokens() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Avery Stone",
                                "email", "Avery@Example.com",
                                "password", PASSWORD,
                                "phoneNumber", "+1 (555) 010-2000"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("avery@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andReturn();

        String setCookie = setCookieHeader(result.getResponse(), REFRESH_COOKIE);
        assertThat(setCookie)
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/api/auth");

        User user = userRepository.findByEmailIgnoreCase("avery@example.com").orElseThrow();
        assertThat(user.getEmail()).isEqualTo("avery@example.com");
        assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(user.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(refreshTokenRepository.countByUserIdAndRevokedAtIsNull(user.getId())).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateEmailCaseInsensitively() throws Exception {
        register("First Account", "duplicate@example.com", PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Second Account",
                                "email", "DUPLICATE@example.com",
                                "password", PASSWORD
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidRegistrationPayload() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "",
                                "email", "not-an-email",
                                "password", "short",
                                "phoneNumber", "abc"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void rejectsPasswordsThatExceedBcryptByteLimit() throws Exception {
        String longUtf8Password = "A1" + "\u00e9".repeat(40);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Long Password",
                                "email", "long-password@example.com",
                                "password", longUtf8Password
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_TOO_LONG"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void logsInWithNormalizedEmailAndRejectsBadCredentials() throws Exception {
        register("Login User", "login@example.com", PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "LOGIN@EXAMPLE.COM",
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("login@example.com"));

        MvcResult wrongPassword = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "login@example.com",
                                "password", "WrongPassword1"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();

        MvcResult unknownAccount = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "missing@example.com",
                                "password", PASSWORD
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();

        assertThat(message(wrongPassword)).isEqualTo(message(unknownAccount));
    }

    @Test
    void rejectsDisabledAccountForLoginAndExistingAccessToken() throws Exception {
        Session session = session(register("Disabled User", "disabled@example.com", PASSWORD));

        User user = userRepository.findByEmailIgnoreCase("disabled@example.com").orElseThrow();
        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "disabled@example.com",
                                "password", PASSWORD
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void requiresAuthenticationForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/users/me"));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsAndUpdatesCurrentProfile() throws Exception {
        Session session = session(register("Profile User", "profile@example.com", PASSWORD));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Profile User"))
                .andExpect(jsonPath("$.email").value("profile@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Profile Updated",
                                "phoneNumber", "+1 (555) 010-3000"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Profile Updated"))
                .andExpect(jsonPath("$.phoneNumber").value("+1 (555) 010-3000"));

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Profile Updated",
                                "phoneNumber", ""
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist());

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fullName", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());
    }

    @Test
    void rotatesRefreshTokensAndDetectsReuse() throws Exception {
        Session initial = session(register("Rotation User", "rotation@example.com", PASSWORD));
        Csrf csrf = csrf();
        Long userId = userRepository.findByEmailIgnoreCase("rotation@example.com")
                .orElseThrow()
                .getId();

        MvcResult rotatedResult = performRefresh(initial.refreshToken(), csrf)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Session rotated = session(rotatedResult);

        assertThat(rotated.refreshToken()).isNotEqualTo(initial.refreshToken());
        assertThat(refreshTokenRepository.countByUserIdAndRevokedAtIsNull(userId)).isEqualTo(1);

        performRefresh(initial.refreshToken(), csrf)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        assertThat(refreshTokenRepository.countByUserIdAndRevokedAtIsNull(userId)).isZero();

        performRefresh(rotated.refreshToken(), csrf)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void rejectsRefreshWithoutCsrfToken() throws Exception {
        Session session = session(register("Csrf User", "csrf@example.com", PASSWORD));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(REFRESH_COOKIE, session.refreshToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void rejectsRefreshWithoutRefreshCookie() throws Exception {
        Csrf csrf = csrf();

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(CSRF_COOKIE, csrf.cookieValue()))
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
        Session session = session(register("Logout User", "logout@example.com", PASSWORD));
        Csrf csrf = csrf();
        Long userId = userRepository.findByEmailIgnoreCase("logout@example.com")
                .orElseThrow()
                .getId();

        MvcResult logoutResult = performLogout(session.refreshToken(), csrf)
                .andExpect(status().isNoContent())
                .andReturn();

        String clearedCookie = setCookieHeader(logoutResult.getResponse(), REFRESH_COOKIE);
        assertThat(clearedCookie).contains("Max-Age=0");

        performRefresh(session.refreshToken(), csrf)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        assertThat(refreshTokenRepository.countByUserIdAndRevokedAtIsNull(userId)).isZero();

        performLogout(session.refreshToken(), csrf)
                .andExpect(status().isNoContent());
    }

    @Test
    void changesPasswordAndRevokesAllRefreshTokens() throws Exception {
        Session session = session(register("Password User", "password@example.com", PASSWORD));
        Csrf csrf = csrf();

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Password User"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "WrongPassword1",
                                "newPassword", "NewPassword2"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_INCORRECT"));

        mockMvc.perform(post("/api/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", PASSWORD,
                                "newPassword", PASSWORD
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_UNCHANGED"));

        MvcResult changed = mockMvc.perform(post("/api/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", PASSWORD,
                                "newPassword", "NewPassword2"
                        ))))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(setCookieHeader(changed.getResponse(), REFRESH_COOKIE)).contains("Max-Age=0");

        performRefresh(session.refreshToken(), csrf)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "password@example.com",
                                "password", PASSWORD
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "password@example.com",
                                "password", "NewPassword2"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsExpiredTamperedAndUnknownSubjectTokens() throws Exception {
        Session session = session(register("Token User", "token@example.com", PASSWORD));
        User user = userRepository.findByEmailIgnoreCase("token@example.com").orElseThrow();

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tamper(session.accessToken()))))
                .andExpect(status().isUnauthorized());

        String expired = forgeToken(
                user.getId(),
                "USER",
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(60)
        );
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().isUnauthorized());

        String unknownSubject = forgeToken(
                999_999L,
                "USER",
                Instant.now(),
                Instant.now().plusSeconds(300)
        );
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(unknownSubject)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enforcesAdminRoleFromDatabaseNotJwtClaims() throws Exception {
        Session normalUser = session(register("Normal User", "normal@example.com", PASSWORD));
        User user = userRepository.findByEmailIgnoreCase("normal@example.com").orElseThrow();

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(normalUser.accessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        String forgedAdminClaim = forgeToken(
                user.getId(),
                "ADMIN",
                Instant.now(),
                Instant.now().plusSeconds(300)
        );
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(forgedAdminClaim)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        User admin = new User();
        admin.setFullName("Admin User");
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode(PASSWORD));
        admin.setRole(Role.ADMIN);
        userRepository.saveAndFlush(admin);

        MvcResult adminLogin = login("admin@example.com", PASSWORD);
        String adminToken = JsonPath.read(
                adminLogin.getResponse().getContentAsString(),
                "$.accessToken"
        );

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void allowsConfiguredCorsOriginAndRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173"
                ));

        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    private MvcResult register(String fullName, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", fullName,
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();
    }

    private Csrf csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String headerName = JsonPath.read(body, "$.headerName");
        String token = JsonPath.read(body, "$.token");
        String cookie = cookieValue(result.getResponse(), CSRF_COOKIE);

        assertThat(cookie).isEqualTo(token);
        return new Csrf(headerName, token, cookie);
    }

    private ResultActions performRefresh(String refreshToken, Csrf csrf) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie(REFRESH_COOKIE, refreshToken))
                .cookie(new Cookie(CSRF_COOKIE, csrf.cookieValue()))
                .header(csrf.headerName(), csrf.token()));
    }

    private ResultActions performLogout(String refreshToken, Csrf csrf) throws Exception {
        return mockMvc.perform(post("/api/auth/logout")
                .cookie(new Cookie(REFRESH_COOKIE, refreshToken))
                .cookie(new Cookie(CSRF_COOKIE, csrf.cookieValue()))
                .header(csrf.headerName(), csrf.token()));
    }

    private static Session session(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        String accessToken = JsonPath.read(body, "$.accessToken");
        String refreshToken = cookieValue(result.getResponse(), REFRESH_COOKIE);
        return new Session(accessToken, refreshToken);
    }

    private String forgeToken(
            Long userId,
            String role,
            Instant issuedAt,
            Instant expiresAt
    ) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", role)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private static String tamper(String token) {
        // Change a meaningful bit inside the signature. Editing the final Base64URL
        // character can alter only padding bits, leaving the decoded signature intact.
        int index = token.lastIndexOf('.') + 5;
        char current = token.charAt(index);
        char replacement = current == 'a' ? 'b' : 'a';
        return token.substring(0, index) + replacement + token.substring(index + 1);
    }

    private static String message(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.message");
    }

    private static String setCookieHeader(MockHttpServletResponse response, String name) {
        for (String header : response.getHeaders(HttpHeaders.SET_COOKIE)) {
            if (header.startsWith(name + "=")) {
                return header;
            }
        }
        return null;
    }

    private static String cookieValue(MockHttpServletResponse response, String name) {
        String header = setCookieHeader(response, name);
        if (header == null) {
            return null;
        }

        int valueStart = name.length() + 1;
        int valueEnd = header.indexOf(';', valueStart);
        return valueEnd < 0
                ? header.substring(valueStart)
                : header.substring(valueStart, valueEnd);
    }

    private record Csrf(String headerName, String token, String cookieValue) {
    }

    private record Session(String accessToken, String refreshToken) {
    }
}
