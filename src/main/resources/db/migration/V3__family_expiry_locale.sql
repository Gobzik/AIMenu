-- Family details and expiry bound; existing accounts keep safe defaults.
ALTER TABLE users ADD COLUMN display_name VARCHAR(80) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN household_name VARCHAR(80) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN adults INTEGER NOT NULL DEFAULT 1 CHECK (adults BETWEEN 1 AND 20);
ALTER TABLE users ADD COLUMN children INTEGER NOT NULL DEFAULT 0 CHECK (children BETWEEN 0 AND 20);
ALTER TABLE batches ADD CONSTRAINT batches_expiry_max CHECK (expires_at IS NULL OR expires_at <= DATE '2500-12-31') NOT VALID;

-- Translate the bundled demonstration catalog without touching user-owned data.
UPDATE products SET name = CASE name
    WHEN 'Egg' THEN 'Яйцо' WHEN 'Milk' THEN 'Молоко' WHEN 'Flour' THEN 'Мука'
    WHEN 'Rice' THEN 'Рис' WHEN 'Chicken' THEN 'Курица' WHEN 'Tomato' THEN 'Помидор'
    WHEN 'Pasta' THEN 'Макароны' WHEN 'Cheese' THEN 'Сыр' ELSE name END,
    unit = CASE unit WHEN 'piece' THEN 'шт' WHEN 'g' THEN 'г' WHEN 'ml' THEN 'мл' ELSE unit END;
UPDATE recipes SET title = CASE title
    WHEN 'Omelette' THEN 'Омлет' WHEN 'Chicken with rice' THEN 'Курица с рисом'
    WHEN 'Cheese pasta' THEN 'Паста с сыром' WHEN 'Pancakes' THEN 'Оладьи' ELSE title END;
UPDATE steps SET instruction = CASE instruction
    WHEN 'Beat eggs with milk.' THEN 'Взбейте яйца с молоком.'
    WHEN 'Cook in a nonstick pan over medium heat until fully set.' THEN 'Готовьте на сковороде на среднем огне до полной готовности.'
    WHEN 'Boil rice according to package directions.' THEN 'Сварите рис по инструкции на упаковке.'
    WHEN 'Cut chicken and tomato, cook chicken until core temperature reaches 74 C.' THEN 'Нарежьте курицу и помидор. Обжарьте курицу до полной готовности (внутри не менее 74 °C).'
    WHEN 'Add tomato and serve with rice.' THEN 'Добавьте помидор и подавайте с рисом.'
    WHEN 'Boil pasta according to package directions.' THEN 'Сварите макароны по инструкции на упаковке.'
    WHEN 'Chop tomatoes and grate cheese.' THEN 'Нарежьте помидоры и натрите сыр.'
    WHEN 'Combine drained pasta with tomatoes and cheese.' THEN 'Смешайте макароны с помидорами и сыром.'
    WHEN 'Whisk flour, milk and egg into a smooth batter.' THEN 'Смешайте муку, молоко и яйцо до однородного теста.'
    WHEN 'Cook small pancakes in a nonstick pan on both sides until cooked through.' THEN 'Обжарьте небольшие оладьи с обеих сторон до готовности.'
    ELSE instruction END;

