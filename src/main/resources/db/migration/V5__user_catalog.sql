ALTER TABLE products ADD COLUMN default_quantity NUMERIC(12,3) NOT NULL DEFAULT 1
    CHECK (default_quantity > 0 AND default_quantity <= 1000000);
ALTER TABLE products ADD COLUMN owner_user_id UUID REFERENCES users(id) ON DELETE CASCADE;
UPDATE products SET default_quantity = CASE unit
    WHEN 'г' THEN 200
    WHEN 'мл' THEN 250
    ELSE 1
END;
ALTER TABLE recipes ADD COLUMN owner_user_id UUID REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX products_owner_user_id_idx ON products(owner_user_id) WHERE owner_user_id IS NOT NULL;
CREATE INDEX recipes_owner_user_id_idx ON recipes(owner_user_id) WHERE owner_user_id IS NOT NULL;
CREATE UNIQUE INDEX products_owner_name_unique_idx ON products(owner_user_id, lower(name)) WHERE owner_user_id IS NOT NULL;
