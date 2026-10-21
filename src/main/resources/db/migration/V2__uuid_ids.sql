-- Preserve every existing row while replacing numeric identifiers with UUIDs.
-- Seed identifiers are deterministic so references survive upgrades.
ALTER TABLE users ADD COLUMN uuid_id UUID;
UPDATE users SET uuid_id = md5('aimenu:user:' || id)::uuid;
ALTER TABLE products ADD COLUMN uuid_id UUID;
UPDATE products SET uuid_id = md5('aimenu:product:' || id)::uuid;
ALTER TABLE recipes ADD COLUMN uuid_id UUID;
UPDATE recipes SET uuid_id = md5('aimenu:recipe:' || id)::uuid;
ALTER TABLE batches ADD COLUMN uuid_id UUID;
UPDATE batches SET uuid_id = md5('aimenu:batch:' || id)::uuid;

ALTER TABLE ingredients ADD COLUMN uuid_recipe_id UUID, ADD COLUMN uuid_product_id UUID;
UPDATE ingredients i SET uuid_recipe_id = r.uuid_id, uuid_product_id = p.uuid_id
FROM recipes r, products p WHERE i.recipe_id = r.id AND i.product_id = p.id;
ALTER TABLE steps ADD COLUMN uuid_recipe_id UUID;
UPDATE steps s SET uuid_recipe_id = r.uuid_id FROM recipes r WHERE s.recipe_id = r.id;
ALTER TABLE preferences ADD COLUMN uuid_user_id UUID, ADD COLUMN uuid_target_id UUID;
UPDATE preferences pref SET uuid_user_id = u.uuid_id FROM users u WHERE pref.user_id = u.id;
UPDATE preferences pref SET uuid_target_id = p.uuid_id FROM products p
WHERE pref.kind IN ('allergy','product') AND pref.target_id = p.id;
UPDATE preferences pref SET uuid_target_id = r.uuid_id FROM recipes r
WHERE pref.kind = 'recipe' AND pref.target_id = r.id;
ALTER TABLE batches ADD COLUMN uuid_user_id UUID, ADD COLUMN uuid_product_id UUID;
UPDATE batches b SET uuid_user_id = u.uuid_id, uuid_product_id = p.uuid_id
FROM users u, products p WHERE b.user_id = u.id AND b.product_id = p.id;
ALTER TABLE cooking_events ADD COLUMN uuid_user_id UUID, ADD COLUMN uuid_recipe_id UUID;
UPDATE cooking_events c SET uuid_user_id = u.uuid_id, uuid_recipe_id = r.uuid_id
FROM users u, recipes r WHERE c.user_id = u.id AND c.recipe_id = r.id;

DROP TABLE sessions;
ALTER TABLE ingredients DROP CONSTRAINT ingredients_pkey, DROP CONSTRAINT ingredients_recipe_id_fkey, DROP CONSTRAINT ingredients_product_id_fkey;
ALTER TABLE steps DROP CONSTRAINT steps_pkey, DROP CONSTRAINT steps_recipe_id_fkey;
ALTER TABLE preferences DROP CONSTRAINT preferences_pkey, DROP CONSTRAINT preferences_user_id_fkey;
ALTER TABLE batches DROP CONSTRAINT batches_user_id_fkey, DROP CONSTRAINT batches_product_id_fkey;
ALTER TABLE cooking_events DROP CONSTRAINT cooking_events_pkey, DROP CONSTRAINT cooking_events_user_id_fkey, DROP CONSTRAINT cooking_events_recipe_id_fkey;
ALTER TABLE users DROP CONSTRAINT users_pkey;
ALTER TABLE products DROP CONSTRAINT products_pkey;
ALTER TABLE recipes DROP CONSTRAINT recipes_pkey;
ALTER TABLE batches DROP CONSTRAINT batches_pkey;

ALTER TABLE users DROP COLUMN id;
ALTER TABLE products DROP COLUMN id;
ALTER TABLE recipes DROP COLUMN id;
ALTER TABLE batches DROP COLUMN id, DROP COLUMN user_id, DROP COLUMN product_id;
ALTER TABLE ingredients DROP COLUMN recipe_id, DROP COLUMN product_id;
ALTER TABLE steps DROP COLUMN recipe_id;
ALTER TABLE preferences DROP COLUMN user_id, DROP COLUMN target_id;
ALTER TABLE cooking_events DROP COLUMN user_id, DROP COLUMN recipe_id;

ALTER TABLE users RENAME COLUMN uuid_id TO id;
ALTER TABLE products RENAME COLUMN uuid_id TO id;
ALTER TABLE recipes RENAME COLUMN uuid_id TO id;
ALTER TABLE batches RENAME COLUMN uuid_id TO id;
ALTER TABLE batches RENAME COLUMN uuid_user_id TO user_id;
ALTER TABLE batches RENAME COLUMN uuid_product_id TO product_id;
ALTER TABLE ingredients RENAME COLUMN uuid_recipe_id TO recipe_id;
ALTER TABLE ingredients RENAME COLUMN uuid_product_id TO product_id;
ALTER TABLE steps RENAME COLUMN uuid_recipe_id TO recipe_id;
ALTER TABLE preferences RENAME COLUMN uuid_user_id TO user_id;
ALTER TABLE preferences RENAME COLUMN uuid_target_id TO target_id;
ALTER TABLE cooking_events RENAME COLUMN uuid_user_id TO user_id;
ALTER TABLE cooking_events RENAME COLUMN uuid_recipe_id TO recipe_id;

ALTER TABLE users ALTER COLUMN id SET NOT NULL, ALTER COLUMN id SET DEFAULT gen_random_uuid(), ADD PRIMARY KEY(id);
ALTER TABLE products ALTER COLUMN id SET NOT NULL, ALTER COLUMN id SET DEFAULT gen_random_uuid(), ADD PRIMARY KEY(id);
ALTER TABLE recipes ALTER COLUMN id SET NOT NULL, ALTER COLUMN id SET DEFAULT gen_random_uuid(), ADD PRIMARY KEY(id);
ALTER TABLE batches ALTER COLUMN id SET NOT NULL, ALTER COLUMN id SET DEFAULT gen_random_uuid(), ADD PRIMARY KEY(id);
ALTER TABLE ingredients ALTER COLUMN recipe_id SET NOT NULL, ALTER COLUMN product_id SET NOT NULL,
  ADD PRIMARY KEY(recipe_id, product_id),
  ADD FOREIGN KEY(recipe_id) REFERENCES recipes(id), ADD FOREIGN KEY(product_id) REFERENCES products(id);
ALTER TABLE steps ALTER COLUMN recipe_id SET NOT NULL,
  ADD PRIMARY KEY(recipe_id, position), ADD FOREIGN KEY(recipe_id) REFERENCES recipes(id);
ALTER TABLE preferences ALTER COLUMN user_id SET NOT NULL, ALTER COLUMN target_id SET NOT NULL,
  ADD PRIMARY KEY(user_id, kind, target_id), ADD FOREIGN KEY(user_id) REFERENCES users(id);
ALTER TABLE batches ALTER COLUMN user_id SET NOT NULL, ALTER COLUMN product_id SET NOT NULL,
  ADD FOREIGN KEY(user_id) REFERENCES users(id), ADD FOREIGN KEY(product_id) REFERENCES products(id);
CREATE INDEX batches_user ON batches(user_id);
ALTER TABLE cooking_events ALTER COLUMN user_id SET NOT NULL, ALTER COLUMN recipe_id SET NOT NULL,
  ADD PRIMARY KEY(user_id, request_key), ADD FOREIGN KEY(user_id) REFERENCES users(id),
  ADD FOREIGN KEY(recipe_id) REFERENCES recipes(id);
