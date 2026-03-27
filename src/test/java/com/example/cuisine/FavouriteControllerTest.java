package com.example.cuisine;

import com.example.cuisine.dto.AuthDto;
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
class FavouriteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── Helper ────────────────────────────────────────────────────────────────

    private String registerAndGetToken(String email) throws Exception {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setEmail(email);
        req.setPassword("password123");
        req.setDisplayName("Test User");

        String body = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("token").asText();
    }

    // ── GET /api/v1/favourites ────────────────────────────────────────────────

    @Test
    void getMyFavourites_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/v1/favourites"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void getMyFavourites_authenticated_returnsEmptyList() throws Exception {
        String token = registerAndGetToken("fav-list@example.com");

        mockMvc.perform(get("/api/v1/favourites")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── GET /api/v1/favourites/recipes ────────────────────────────────────────

    @Test
    void getFavouriteRecipes_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/v1/favourites/recipes"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void getFavouriteRecipes_authenticated_returnsEmptyPage() throws Exception {
        String token = registerAndGetToken("fav-recipes@example.com");

        mockMvc.perform(get("/api/v1/favourites/recipes")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // ── POST /api/v1/favourites/{recipeId} ────────────────────────────────────

    @Test
    void addFavourite_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(post("/api/v1/favourites/1"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void addFavourite_nonExistentRecipe_returns404() throws Exception {
        String token = registerAndGetToken("fav-add@example.com");

        mockMvc.perform(post("/api/v1/favourites/99999")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    // ── DELETE /api/v1/favourites/{recipeId} ──────────────────────────────────

    @Test
    void removeFavourite_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(delete("/api/v1/favourites/1"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void removeFavourite_nonExistent_returns204() throws Exception {
        String token = registerAndGetToken("fav-remove@example.com");

        // Removing a favourite that doesn't exist is a no-op — returns 204
        mockMvc.perform(delete("/api/v1/favourites/99999")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }
}
