# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run all tests (H2 in-memory — no Postgres or AWS needed)
mvn test

# Run a single test class
mvn test -Dtest=RecipeControllerTest

# Run a single test method
mvn test -Dtest=RecipeControllerTest#getById_nonExistent_returns404

# Build jar (skip tests)
mvn clean package -DskipTests

# Run locally (requires Postgres on localhost:5432)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Start full stack with Docker
docker-compose up --build
```

## Architecture

### Multilingual content model
All user-facing text is separated from data entities. Each translatable entity has a companion `*Translation` entity with a `Language` enum (`EN`, `FR`, `ZH_CN`). The pattern repeats across:
- `Recipe` + `RecipeTranslation` (name, description, phonetic)
- `RecipeStep` + `RecipeStepTranslation` (instruction, tip)
- `Ingredient` + `IngredientTranslation` (name, description, whereToFind)

Controllers accept a `?lang=EN|FR|ZH_CN` param, pass it to services as `Language`, and the service calls `entity.getTranslation(lang)` which falls back to EN if the requested language is missing.

### Request flow
`Controller` → `Service` → `Repository` → DB. Services map entities to DTOs. The `RecipeService` is the most complex — it does language-aware mapping in `toCard()` and `toDetail()`.

### Authentication
JWT-based. `JwtAuthFilter` runs on every request, validates the token, and sets the `SecurityContext`. `SecurityUtils.getCurrentUserId()` is a static helper that reads from the context — it's wired to `UserRepository` via `AppConfig.@PostConstruct`. Public endpoints (GET recipes, auth) are open; favourites require any JWT; recipe mutations require `ROLE_ADMIN`.

### Hibernate fetch strategy
`RecipeRepository` uses custom `@Query` methods with `LEFT JOIN FETCH` for the detail view. **Important constraint:** Hibernate 6 throws `MultipleBagFetchException` if multiple `@OneToMany List` collections are JOIN FETCHed in a single query. The current queries only JOIN FETCH `recipeIngredients` + `ingredient`; `Recipe.translations` and `Ingredient.translations` load lazily within the `@Transactional` service call.

### AWS / config data bootstrap
`application.properties` has `spring.config.import=optional:aws-parameterstore:/mypetitwok/prod/` which runs during Spring's bootstrap phase (before profiles load). This requires `spring.cloud.aws.region.static` to be set in `application.properties` itself — not in a profile file — otherwise the SSM client creation fails at bootstrap. The `local` and `test` profiles override `spring.config.import=` to disable Parameter Store.

### Test setup quirks
- Tests use `@ActiveProfiles("test")` which activates `application-test.properties` (H2, Flyway disabled, DDL create-drop)
- `TestConfig` provides `@Primary` mocks for `S3Client` and `SsmClient` — `spring.main.allow-bean-definition-overriding=true` is required for this to work
- Maven Surefire is configured with `-Dnet.bytebuddy.experimental=true` because the project builds on JDK 25 and Mockito's ByteBuddy needs experimental mode for that JDK
- Lombok requires explicit `annotationProcessorPaths` configuration in `maven-compiler-plugin` and version `1.18.44+` for JDK 25 compatibility

### Database migrations
Flyway runs automatically on startup. Migration files are in `src/main/resources/db/migration/`. The test profile disables Flyway and uses Hibernate `create-drop` instead.

## Key design decisions
- **Ingredients are reusable** across recipes and can reference a `substitute` ingredient (self-referencing FK) for hard-to-find Asian ingredients
- **Recipes start as drafts** (`published=false`) and must be explicitly published via `PUT /api/v1/recipes/{id}/publish`
- **`spiceLevel`** is an integer 0–5 stored on `Recipe`, included in both `CardResponse` and `DetailResponse`
- The `AppConfig` bean uses `@Autowired @Lazy` for `UserRepository` to break a circular dependency with `entityManagerFactory`