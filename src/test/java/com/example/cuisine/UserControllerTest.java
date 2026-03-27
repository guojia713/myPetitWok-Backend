package com.example.cuisine;

import com.example.cuisine.dto.AuthDto;
import com.example.cuisine.dto.UserDto;
import com.example.cuisine.entity.Language;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── Helper ────────────────────────────────────────────────────────────────

    private String registerAndGetToken(String email, String displayName) throws Exception {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setEmail(email);
        req.setPassword("password123");
        req.setDisplayName(displayName);

        String body = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("token").asText();
    }

    // ── GET /api/v1/me ────────────────────────────────────────────────────────

    @Test
    void getProfile_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void getProfile_authenticated_returnsUserDetails() throws Exception {
        String token = registerAndGetToken("me-get@example.com", "Alice");

        mockMvc.perform(get("/api/v1/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("me-get@example.com")))
            .andExpect(jsonPath("$.displayName", is("Alice")))
            .andExpect(jsonPath("$.id", notNullValue()));
    }

    // ── PUT /api/v1/me ────────────────────────────────────────────────────────

    @Test
    void updateProfile_unauthenticated_returns4xx() throws Exception {
        UserDto.UpdateRequest req = new UserDto.UpdateRequest();
        req.setDisplayName("NewName");

        mockMvc.perform(put("/api/v1/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void updateProfile_displayName_isUpdated() throws Exception {
        String token = registerAndGetToken("me-update@example.com", "Bob");

        UserDto.UpdateRequest req = new UserDto.UpdateRequest();
        req.setDisplayName("Bobby");

        mockMvc.perform(put("/api/v1/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName", is("Bobby")));
    }

    @Test
    void updateProfile_preferredLanguage_isUpdated() throws Exception {
        String token = registerAndGetToken("me-lang@example.com", "Charlie");

        UserDto.UpdateRequest req = new UserDto.UpdateRequest();
        req.setPreferredLanguage(Language.FR);

        mockMvc.perform(put("/api/v1/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.preferredLanguage", is("FR")));
    }
}
