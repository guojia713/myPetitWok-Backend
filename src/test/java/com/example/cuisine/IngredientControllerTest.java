package com.example.cuisine;

import com.example.cuisine.dto.IngredientDto;
import com.example.cuisine.entity.Language;
import com.example.cuisine.entity.User;
import com.example.cuisine.repository.UserRepository;
import com.example.cuisine.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class IngredientControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private String adminToken;

    @BeforeEach
    void setupAdmin() {
        if (!userRepository.existsByEmail("admin-ingredients@example.com")) {
            User admin = new User();
            admin.setEmail("admin-ingredients@example.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setDisplayName("Admin");
            admin.setRole("ROLE_ADMIN");
            admin.setPreferredLanguage(Language.EN);
            userRepository.save(admin);
        }
        adminToken = jwtService.generateToken("admin-ingredients@example.com", "ROLE_ADMIN");
    }

    private IngredientDto.CreateRequest buildCreateRequest(String asianName) {
        IngredientDto.CreateRequest req = new IngredientDto.CreateRequest();
        req.setAsianName(asianName);
        req.setCategory("SAUCE");
        IngredientDto.TranslationInput t = new IngredientDto.TranslationInput();
        t.setName("Test Sauce");
        t.setDescription("A test sauce");
        req.setTranslations(Map.of("EN", t));
        return req;
    }

    // ── GET /api/v1/ingredients ───────────────────────────────────────────────

    @Test
    void getAll_public_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/ingredients"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    void getAll_withLangFr_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/ingredients").param("lang", "FR"))
            .andExpect(status().isOk());
    }

    @Test
    void getAll_withSearch_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/ingredients").param("search", "sauce"))
            .andExpect(status().isOk());
    }

    @Test
    void getAll_withCategory_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/ingredients").param("category", "SAUCE"))
            .andExpect(status().isOk());
    }

    // ── GET /api/v1/ingredients/{id} ─────────────────────────────────────────

    @Test
    void getById_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/ingredients/99999"))
            .andExpect(status().isNotFound());
    }

    // ── POST /api/v1/ingredients ─────────────────────────────────────────────

    @Test
    void create_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/ingredients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildCreateRequest("酱油"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_asAdmin_returns201WithBody() throws Exception {
        mockMvc.perform(post("/api/v1/ingredients")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildCreateRequest("鱼露"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.asianName", is("鱼露")))
            .andExpect(jsonPath("$.name", is("Test Sauce")));
    }

    @Test
    void create_missingRequiredField_returns400() throws Exception {
        IngredientDto.CreateRequest req = new IngredientDto.CreateRequest();
        // asianName is @NotBlank — omit it intentionally
        req.setTranslations(Map.of());

        mockMvc.perform(post("/api/v1/ingredients")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    // ── PUT /api/v1/ingredients/{id} ─────────────────────────────────────────

    @Test
    void update_nonExistent_returns404() throws Exception {
        mockMvc.perform(put("/api/v1/ingredients/99999")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildCreateRequest("味噌"))))
            .andExpect(status().isNotFound());
    }

    // ── DELETE /api/v1/ingredients/{id} ──────────────────────────────────────

    @Test
    void delete_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/ingredients/1"))
            .andExpect(status().isForbidden());
    }

    @Test
    void delete_nonExistent_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/ingredients/99999")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void createThenDelete_asAdmin_returns204() throws Exception {
        // Create
        String body = mockMvc.perform(post("/api/v1/ingredients")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildCreateRequest("豆瓣酱"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).get("id").asLong();

        // Delete
        mockMvc.perform(delete("/api/v1/ingredients/" + id)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        // Confirm gone
        mockMvc.perform(get("/api/v1/ingredients/" + id))
            .andExpect(status().isNotFound());
    }
}
