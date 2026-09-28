package com.varundev.backend.auth;

import java.util.UUID;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @LocalServerPort
    private int port;

    private static String jsonValue(String json, String name) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(name) + "\\\":\\\"([^\\\"]+)\\\"")
                .matcher(json);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static String setCookieValue(HttpResponse<?> response, String name) {
        String value = setCookieHeader(response, name);
        return value.substring((name + "=").length(), value.indexOf(';'));
    }

    private static String setCookieHeader(HttpResponse<?> response, String name) {
        String prefix = name + "=";
        return response.headers().allValues("Set-Cookie").stream()
                .filter(value -> value.startsWith(prefix))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing " + name + " Set-Cookie header"));
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerShouldAuthenticateUser() throws Exception {

        String email = "test-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(
                post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "secret123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    @Test
    void loginShouldAuthenticateUser() throws Exception {

        String email = "login-" + UUID.randomUUID() + "@example.com";
        String password = "secret123";

        mockMvc.perform(
                post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void loginShouldRejectInvalidPassword() throws Exception {

        String email
                = "wrong-password-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(
                post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "secret123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "wrong-password"
                                }
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginWithUnknownEmailShouldReturnStructuredInvalidCredentials() throws Exception {
        mockMvc.perform(
                post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing-%s@example.com","password":"secret123"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void unauthenticatedMeShouldReturnStructuredUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void stateChangingRequestWithoutCsrfShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionCookieShouldAuthenticateAndLogoutOverHttp() throws Exception {

        String email
                = "logout-" + UUID.randomUUID() + "@example.com";

        String password = "secret123";

        HttpClient client = HttpClient.newHttpClient();
        URI baseUri = URI.create("http://localhost:" + port);

        HttpResponse<String> csrfResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/auth/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(csrfResponse.statusCode()).isEqualTo(200);
        String csrfToken = jsonValue(csrfResponse.body(), "token");
        String csrfCookie = setCookieValue(csrfResponse, "XSRF-TOKEN");

        HttpResponse<String> registerResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/auth/register"))
                        .header("Content-Type", "application/json")
                        .header("Cookie", "XSRF-TOKEN=" + csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .POST(HttpRequest.BodyPublishers.ofString("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(registerResponse.statusCode()).isEqualTo(201);

        HttpResponse<String> loginResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .header("Cookie", "XSRF-TOKEN=" + csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .POST(HttpRequest.BodyPublishers.ofString("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        String sessionCookieHeader = setCookieHeader(loginResponse, "JSESSIONID");
        assertThat(sessionCookieHeader).contains("HttpOnly", "Path=/", "SameSite=Lax");
        assertThat(sessionCookieHeader).doesNotContain("Secure");
        String sessionCookie = setCookieValue(loginResponse, "JSESSIONID");

        HttpResponse<String> meResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/auth/me"))
                        .header("Cookie", "JSESSIONID=" + sessionCookie)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(meResponse.statusCode()).isEqualTo(200);
        assertThat(meResponse.body()).contains(email);

        HttpResponse<String> logoutResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/auth/logout"))
                        .header("Cookie", "JSESSIONID=" + sessionCookie + "; XSRF-TOKEN=" + csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(logoutResponse.statusCode()).isEqualTo(204);

        HttpResponse<String> afterLogoutResponse = client.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/auth/me"))
                        .header("Cookie", "JSESSIONID=" + sessionCookie)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(afterLogoutResponse.statusCode()).isEqualTo(401);
    }

    @Test
    void csrfEndpointShouldReturnToken() throws Exception {
        mockMvc.perform(
                get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").isNotEmpty());
    }

    @Test
    void corsShouldAllowReactOriginWithCredentialsAndPreflight() throws Exception {
        String origin = "http://localhost:5173";

        mockMvc.perform(get("/api/auth/csrf").header("Origin", origin))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("x-xsrf-token")));

        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:9999")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({
        "'', 'secret123'",
        "'userexample.com', 'secret123'",
        "'user@example.com', ''",
        "'user@example.com', '123'"
    })
    void registerShouldRejectInvalidRequest(
            String email,
            String password) throws Exception {

        mockMvc.perform(
                post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "%s",
                            "password": "%s"
                        }
                        """.formatted(email, password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "'', 'secret123'",
        "'userexample.com', 'secret123'",
        "'user@example.com', ''",
        "'user@example.com', '123'"
    })
    void loginShouldRejectInvalidRequest(
            String email,
            String password) throws Exception {

        mockMvc.perform(
                post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "%s",
                            "password": "%s"
                        }
                        """.formatted(email, password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void RegisterUsingSameEmailShouldFail() throws Exception {

        String email = "test-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(
                post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "secret123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "secret123"
                                }
                                """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void loginWithWrongPasswordShouldFail() throws Exception {

        String email = "test-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(
                post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                            {
                                "email": "%s",
                                "password": "secret123"
                            }
                            """.formatted(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                            {
                                "email": "%s",
                                "password": "wrongPassword"
                            }
                            """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }
}
