package com.travelai.domain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelai.domain.auth.dto.LoginRequest;
import com.travelai.domain.auth.dto.RegisterRequest;
import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'integració per a AuthController.
 * Utilitza H2 en memòria (perfil "test") i mocks per a Redis, MinIO i Ollama.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController — tests d'integració")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Mocks per a serveis externs no disponibles en tests
    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private MinioClient minioClient;

    @MockBean
    private OllamaChatModel ollamaChatModel;

    private static final String BASE_URL = "/api/v1/auth";

    // ── Helpers ──────────────────────────────────────────────────────────────

    private RegisterRequest buildValidRegisterRequest(String username, String email) {
        return new RegisterRequest(
                username,
                email,
                "Password1!",
                LocalDate.of(1995, 6, 15),
                "1.0",
                true,
                true,
                true,
                false
        );
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/register → 201 amb JWT quan les dades són vàlides")
    void register_validRequest_returns201WithJwt() throws Exception {
        RegisterRequest request = buildValidRegisterRequest("newuser", "newuser@example.com");

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /auth/login → 200 amb tokens quan les credencials són correctes")
    void login_validCredentials_returns200WithTokens() throws Exception {
        // Arrange: crear usuari directament a la BD
        User user = User.builder()
                .email("logintest@example.com")
                .username("logintest")
                .passwordHash(passwordEncoder.encode("Password1!"))
                .role(Role.USER)
                .active(true)
                .consentVersion("1.0")
                .consentAt(java.time.Instant.now())
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest("logintest@example.com", "Password1!");

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value("logintest"));
    }

    @Test
    @DisplayName("POST /auth/login → 401 quan la contrasenya és incorrecta")
    void login_wrongPassword_returns401() throws Exception {
        // Arrange: crear usuari directament a la BD
        User user = User.builder()
                .email("wrongpass@example.com")
                .username("wrongpassuser")
                .passwordHash(passwordEncoder.encode("CorrectPass1!"))
                .role(Role.USER)
                .active(true)
                .consentVersion("1.0")
                .consentAt(java.time.Instant.now())
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest("wrongpass@example.com", "WrongPassword!");

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
