
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(254) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL
);

CREATE TABLE sessions (
    token_hash TEXT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT now()+interval '7 days'
);

CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    unit TEXT NOT NULL,
    perishable BOOLEAN NOT NULL
);

CREATE TABLE recipes (
    id BIGINT PRIMARY KEY,
    title TEXT NOT NULL
);

CREATE TABLE ingredients (
    recipe_id BIGINT REFERENCES recipes(id),
    product_id BIGINT REFERENCES products(id),
    quantity NUMERIC(15,3) NOT NULL CHECK(quantity>0),
    PRIMARY KEY(recipe_id,product_id)
);

CREATE TABLE steps (
    recipe_id BIGINT REFERENCES recipes(id),
    position INT NOT NULL,
    instruction TEXT NOT NULL,
    PRIMARY KEY(recipe_id,position)
);

CREATE TABLE preferences (
    user_id BIGINT REFERENCES users(id),
    kind TEXT CHECK(kind IN ('allergy','product','recipe')),
    target_id BIGINT NOT NULL,
    PRIMARY KEY(user_id,kind,target_id)
);

CREATE TABLE batches (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity NUMERIC(15,3) NOT NULL CHECK(quantity>=0),
    expires_at DATE
);

CREATE INDEX batches_user ON batches(user_id);

CREATE TABLE cooking_events (
    user_id BIGINT REFERENCES users(id),
    request_key VARCHAR(128),
    recipe_id BIGINT NOT NULL REFERENCES recipes(id),
    cooked_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(user_id,request_key)
);

INSERT INTO products VALUES (1,'Egg','piece',true),(2,'Milk','ml',true),(3,'Flour','g',false),(4,'Rice','g',false),(5,'Chicken','g',true),(6,'Tomato','g',true),(7,'Pasta','g',false),(8,'Cheese','g',true);

INSERT INTO recipes VALUES (1,'Omelette'),(2,'Chicken with rice'),(3,'Cheese pasta'),(4,'Pancakes');

INSERT INTO ingredients VALUES (1,1,2),(1,2,100),(2,4,100),(2,5,150),(2,6,100),(3,7,100),(3,8,40),(3,6,100),(4,3,100),(4,2,200),(4,1,1);

INSERT INTO steps VALUES (1,1,'Beat eggs with milk.'),(1,2,'Cook in a nonstick pan over medium heat until fully set.'),(2,1,'Boil rice according to package directions.'),(2,2,'Cut chicken and tomato, cook chicken until core temperature reaches 74 C.'),(2,3,'Add tomato and serve with rice.'),(3,1,'Boil pasta according to package directions.'),(3,2,'Chop tomatoes and grate cheese.'),(3,3,'Combine drained pasta with tomatoes and cheese.'),(4,1,'Whisk flour, milk and egg into a smooth batter.'),(4,2,'Cook small pancakes in a nonstick pan on both sides until cooked through.');
