package com.example.cuisine.controller;

import com.example.cuisine.dto.RecipeDto;
import com.example.cuisine.entity.Language;
import com.example.cuisine.service.FavouriteService;
import com.example.cuisine.service.RecipeService;
import com.example.cuisine.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/favourites")
@RequiredArgsConstructor
public class FavouriteController {

    private final FavouriteService favouriteService;
    private final RecipeService recipeService;

    /**
     * GET /api/v1/favourites
     * Returns list of recipe IDs saved by the logged-in user.
     */
    @GetMapping
    public ResponseEntity<List<Long>> getMyFavourites() {
        Long userId = SecurityUtils.getRequiredCurrentUserId();
        return ResponseEntity.ok(favouriteService.getFavouriteRecipeIds(userId));
    }

    /**
     * GET /api/v1/favourites/recipes
     * Returns paginated full recipe cards for the logged-in user's favourites.
     */
    @GetMapping("/recipes")
    public ResponseEntity<RecipeDto.PagedResponse> getFavouriteRecipes(
            @RequestParam(defaultValue = "EN") String lang,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getRequiredCurrentUserId();
        Language language;
        try {
            language = Language.valueOf(lang.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            language = Language.EN;
        }
        return ResponseEntity.ok(recipeService.findFavourites(userId, language, page, size, userId));
    }

    /**
     * POST /api/v1/favourites/{recipeId}
     * Save a recipe to favourites. Idempotent — safe to call multiple times.
     */
    @PostMapping("/{recipeId}")
    public ResponseEntity<Map<String, String>> addFavourite(@PathVariable Long recipeId) {
        Long userId = SecurityUtils.getRequiredCurrentUserId();
        favouriteService.addFavourite(userId, recipeId);
        return ResponseEntity.ok(Map.of("status", "saved"));
    }

    /**
     * DELETE /api/v1/favourites/{recipeId}
     * Remove a recipe from favourites.
     */
    @DeleteMapping("/{recipeId}")
    public ResponseEntity<Void> removeFavourite(@PathVariable Long recipeId) {
        Long userId = SecurityUtils.getRequiredCurrentUserId();
        favouriteService.removeFavourite(userId, recipeId);
        return ResponseEntity.noContent().build();
    }
}
