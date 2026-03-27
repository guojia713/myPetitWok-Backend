CREATE TABLE recipe_categories (
    recipe_id BIGINT NOT NULL,
    category  VARCHAR(50) NOT NULL,
    CONSTRAINT pk_recipe_categories PRIMARY KEY (recipe_id, category),
    CONSTRAINT fk_recipe_categories_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE
);
