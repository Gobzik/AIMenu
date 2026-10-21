-- Two hundred deterministic, Russian-language catalog recipes.
-- The grid keeps the migration readable and allows every variation to retain
-- its own ingredients and preparation steps.
WITH seed(name, unit, perishable) AS (
    VALUES
        ('Овсяные хлопья','г',false),('Пшено','г',false),('Гречка','г',false),
        ('Кускус','г',false),('Булгур','г',false),('Рисовая мука','г',false),
        ('Ржаная мука','г',false),('Цельнозерновая мука','г',false),('Овсяная мука','г',false),
        ('Картофель','г',false),('Лук','г',false),('Морковь','г',false),
        ('Огурец','г',true),('Болгарский перец','г',true),('Кабачок','г',true),
        ('Брокколи','г',true),('Шпинат','г',true),('Шампиньоны','г',true),
        ('Листья салата','г',true),('Кукуруза','г',false),('Авокадо','г',true),
        ('Яблоко','г',true),('Банан','г',true),('Груша','г',true),
        ('Ягоды','г',true),('Тыква','г',true),('Фасоль','г',false),
        ('Тунец консервированный','г',false),('Лосось','г',true),('Треска','г',true),
        ('Минтай','г',true),('Форель','г',true),('Индейка','г',true),
        ('Говядина','г',true),('Ветчина','г',true),('Тофу','г',true),
        ('Сливки','мл',true),('Песто','г',true),('Оливковое масло','мл',false),
        ('Хлеб','г',true),('Лапша','г',false),('Чеснок','г',false),('Лимон','г',true)
)
INSERT INTO products(id, name, unit, perishable)
SELECT md5('aimenu:v4:product:' || s.name)::uuid, s.name, s.unit, s.perishable
FROM seed s WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.name = s.name)
ON CONFLICT (id) DO NOTHING;

CREATE TEMP TABLE catalog_v4 (
    family integer NOT NULL,
    title text NOT NULL,
    first_product text NOT NULL,
    second_product text NOT NULL
) ON COMMIT DROP;

INSERT INTO catalog_v4
SELECT 1, 'Омлет с ' || a.label || ' и ' || b.label, a.product, b.product
FROM unnest(ARRAY['сыром','курицей','грибами','ветчиной'], ARRAY['Сыр','Курица','Шампиньоны','Ветчина']) AS a(label, product)
CROSS JOIN unnest(ARRAY['помидором','шпинатом','брокколи','болгарским перцем','кабачком'], ARRAY['Помидор','Шпинат','Брокколи','Болгарский перец','Кабачок']) AS b(label, product);

INSERT INTO catalog_v4
SELECT 2, 'Каша из ' || a.label || ' с ' || b.label, a.product, b.product
FROM unnest(ARRAY['овсяных хлопьев','пшена','гречки','риса'], ARRAY['Овсяные хлопья','Пшено','Гречка','Рис']) AS a(label, product)
CROSS JOIN unnest(ARRAY['яблоком','бананом','грушей','ягодами','тыквой'], ARRAY['Яблоко','Банан','Груша','Ягоды','Тыква']) AS b(label, product);

INSERT INTO catalog_v4
SELECT 3, 'Суп с ' || a.label || ' и ' || b.label, a.product, b.product
FROM unnest(ARRAY['курицей','индейкой','говядиной','фасолью'], ARRAY['Курица','Индейка','Говядина','Фасоль']) AS a(label, product)
CROSS JOIN unnest(ARRAY['рисом','картофелем','лапшой','гречкой','грибами'], ARRAY['Рис','Картофель','Лапша','Гречка','Шампиньоны']) AS b(label, product);

INSERT INTO catalog_v4
SELECT 4, 'Салат с ' || a.label || ' и ' || b.label, a.product, b.product
FROM unnest(ARRAY['курицей','тунцом','яйцом','сыром'], ARRAY['Курица','Тунец консервированный','Яйцо','Сыр']) AS a(label, product)
CROSS JOIN unnest(ARRAY['огурцом','помидором','болгарским перцем','кукурузой','авокадо'], ARRAY['Огурец','Помидор','Болгарский перец','Кукуруза','Авокадо']) AS b(label, product);

INSERT INTO catalog_v4
SELECT 5, 'Паста с ' || a.label || ' и ' || b.label, a.product, b.product
FROM unnest(ARRAY['курицей','индейкой','тунцом','грибами'], ARRAY['Курица','Индейка','Тунец консервированный','Шампиньоны']) AS a(label, product)
CROSS JOIN unnest(ARRAY['помидорами','сливками','песто','сыром','брокколи'], ARRAY['Помидор','Сливки','Песто','Сыр','Брокколи']) AS b(label, product);

INSERT INTO catalog_v4
SELECT 6, a.label || ' с ' || b.label, a.product, b.product
FROM unnest(ARRAY['Рис','Гречка','Кускус','Булгур'], ARRAY['Рис','Гречка','Кускус','Булгур']) AS a(label, product)
CROSS JOIN unnest(ARRAY['курицей','индейкой','говядиной','фасолью','тофу'], ARRAY['Курица','Индейка','Говядина','Фасоль','Тофу']) AS b(label, product);

INSERT INTO catalog_v4
SELECT 7, a.label || ' с ' || b.label, a.product, b.product
FROM unnest(ARRAY['Запечённый картофель','Жареный картофель','Картофельное пюре','Тушёный картофель'], ARRAY['Картофель','Картофель','Картофель','Картофель']) AS a(label, product)
CROSS JOIN unnest(ARRAY['курицей','говядиной','грибами','сыром','брокколи'], ARRAY['Курица','Говядина','Шампиньоны','Сыр','Брокколи']) AS b(label, product);

INSERT INTO catalog_v4
SELECT 8, a.label || ' с ' || b.label, a.product, b.product
FROM unnest(ARRAY['Лосось','Треска','Минтай','Форель'], ARRAY['Лосось','Треска','Минтай','Форель']) AS a(label, product)
CROSS JOIN unnest(ARRAY['картофелем','рисом','брокколи','кабачком','морковью'], ARRAY['Картофель','Рис','Брокколи','Кабачок','Морковь']) AS b(label, product);

INSERT INTO catalog_v4
SELECT 9, 'Оладьи из ' || a.label || ' с ' || b.label, a.product, b.product
FROM unnest(ARRAY['ржаной муки','цельнозерновой муки','рисовой муки','овсяной муки'], ARRAY['Ржаная мука','Цельнозерновая мука','Рисовая мука','Овсяная мука']) AS a(label, product)
CROSS JOIN unnest(ARRAY['яблоком','бананом','грушей','ягодами','тыквой'], ARRAY['Яблоко','Банан','Груша','Ягоды','Тыква']) AS b(label, product);

INSERT INTO catalog_v4
SELECT 10, 'Сэндвич с ' || a.label || ' и ' || b.label, a.product, b.product
FROM unnest(ARRAY['ветчиной','сыром','яйцом','тунцом'], ARRAY['Ветчина','Сыр','Яйцо','Тунец консервированный']) AS a(label, product)
CROSS JOIN unnest(ARRAY['огурцом','помидором','болгарским перцем','авокадо','листьями салата'], ARRAY['Огурец','Помидор','Болгарский перец','Авокадо','Листья салата']) AS b(label, product);

INSERT INTO recipes(id, title)
SELECT md5('aimenu:v4:recipe:' || title)::uuid, title FROM catalog_v4
ON CONFLICT (id) DO NOTHING;

-- For a serving of one or two people. Quantities are measured in the product's unit.
WITH recipe_products AS (
    SELECT title, family, first_product AS product_name FROM catalog_v4
    UNION ALL SELECT title, family, second_product FROM catalog_v4
    UNION ALL SELECT title, family, 'Яйцо' FROM catalog_v4 WHERE family IN (1,9)
    UNION ALL SELECT title, family, 'Молоко' FROM catalog_v4 WHERE family IN (1,2,9)
    UNION ALL SELECT title, family, 'Морковь' FROM catalog_v4 WHERE family IN (3,6)
    UNION ALL SELECT title, family, 'Лук' FROM catalog_v4 WHERE family IN (3,6)
    UNION ALL SELECT title, family, 'Листья салата' FROM catalog_v4 WHERE family = 4
    UNION ALL SELECT title, family, 'Макароны' FROM catalog_v4 WHERE family = 5
    UNION ALL SELECT title, family, 'Сливки' FROM catalog_v4 WHERE family = 7 AND title LIKE 'Картофельное пюре%'
    UNION ALL SELECT title, family, 'Лимон' FROM catalog_v4 WHERE family = 8
    UNION ALL SELECT title, family, 'Хлеб' FROM catalog_v4 WHERE family = 10
)
INSERT INTO ingredients(recipe_id, product_id, quantity)
SELECT md5('aimenu:v4:recipe:' || rp.title)::uuid, p.id,
    CASE rp.product_name
        WHEN 'Яйцо' THEN 2 WHEN 'Молоко' THEN 180 WHEN 'Сливки' THEN 80
        WHEN 'Оливковое масло' THEN 15 WHEN 'Песто' THEN 45
        WHEN 'Лимон' THEN 30 WHEN 'Морковь' THEN 60 WHEN 'Лук' THEN 50
        WHEN 'Листья салата' THEN 40 WHEN 'Хлеб' THEN 100
        WHEN 'Макароны' THEN 100 WHEN 'Лапша' THEN 80
        WHEN 'Сыр' THEN 60 WHEN 'Рис' THEN 90 WHEN 'Гречка' THEN 90
        WHEN 'Кускус' THEN 90 WHEN 'Булгур' THEN 90 WHEN 'Пшено' THEN 90
        WHEN 'Овсяные хлопья' THEN 80 WHEN 'Ржаная мука' THEN 100
        WHEN 'Цельнозерновая мука' THEN 100 WHEN 'Рисовая мука' THEN 100
        WHEN 'Овсяная мука' THEN 100 WHEN 'Картофель' THEN 220
        WHEN 'Фасоль' THEN 100 WHEN 'Кукуруза' THEN 80
        WHEN 'Ягоды' THEN 80 WHEN 'Шпинат' THEN 60
        WHEN 'Тунец консервированный' THEN 120
        ELSE 130 END
FROM (SELECT DISTINCT title, family, product_name FROM recipe_products) rp
JOIN products p ON p.name = rp.product_name
ON CONFLICT (recipe_id, product_id) DO NOTHING;

INSERT INTO steps(recipe_id, position, instruction)
SELECT md5('aimenu:v4:recipe:' || title)::uuid, 1,
    CASE family
        WHEN 1 THEN 'Взбейте яйца с молоком. Нарежьте дополнительные ингредиенты; курицу при необходимости приготовьте отдельно до полной готовности.'
        WHEN 2 THEN 'Промойте крупу, если это указано на упаковке. Сварите её в молоке с добавлением воды до мягкости.'
        WHEN 3 THEN 'Нарежьте лук и морковь. Мясо нарежьте небольшими кусочками, фасоль предварительно замочите и отварите.'
        WHEN 4 THEN 'Вымойте овощи и листья салата. Курицу при необходимости приготовьте до полной готовности, яйцо сварите вкрутую.'
        WHEN 5 THEN 'Сварите макароны по инструкции на упаковке. Нарежьте основной ингредиент и подготовьте добавку.'
        WHEN 6 THEN 'Промойте крупу и сварите по инструкции на упаковке. Нарежьте лук, морковь и основной ингредиент.'
        WHEN 7 THEN 'Очистите и нарежьте картофель. Подготовьте добавку: мясо нарежьте, грибы и брокколи промойте, сыр натрите.'
        WHEN 8 THEN 'Разогрейте духовку до 190 °C. Подготовьте рыбу и гарнир, сбрызните рыбу лимонным соком.'
        WHEN 9 THEN 'Смешайте муку, яйцо и молоко до однородности. Измельчите фрукт или ягоды и добавьте в тесто.'
        ELSE 'Подготовьте хлеб и начинку. Овощи вымойте, яйцо при необходимости сварите вкрутую.' END
FROM catalog_v4
ON CONFLICT (recipe_id, position) DO NOTHING;

INSERT INTO steps(recipe_id, position, instruction)
SELECT md5('aimenu:v4:recipe:' || title)::uuid, 2,
    CASE family
        WHEN 1 THEN 'Подготовленные добавки обжарьте на сковороде, залейте яйцами и готовьте под крышкой на среднем огне до полного застывания.'
        WHEN 2 THEN 'Добавьте нарезанный фрукт, ягоды или тыкву за несколько минут до готовности; перемешайте и подавайте тёплой.'
        WHEN 3 THEN 'Вскипятите воду. Добавьте мясо или фасоль, овощи и второй ингредиент; варите до полной готовности мяса и мягкости овощей.'
        WHEN 4 THEN 'Нарежьте подготовленные ингредиенты, соедините с листьями салата и перемешайте. Подавайте сразу.'
        WHEN 5 THEN 'Мясо и грибы при необходимости обжарьте до полной готовности. Прогрейте с добавкой, затем смешайте с макаронами.'
        WHEN 6 THEN 'Обжарьте лук, морковь и добавку; мясо доведите до полной готовности. Смешайте с готовой крупой.'
        WHEN 7 THEN CASE
            WHEN title LIKE 'Запечённый картофель%' THEN 'Запеките картофель при 200 °C до мягкости. Добавку приготовьте отдельно и подавайте вместе с картофелем.'
            WHEN title LIKE 'Жареный картофель%' THEN 'Обжарьте картофель на сковороде до мягкости. Добавку приготовьте отдельно и соедините перед подачей.'
            WHEN title LIKE 'Картофельное пюре%' THEN 'Отварите картофель до мягкости и разомните со сливками. Добавку приготовьте отдельно и подавайте с пюре.'
            ELSE 'Тушите картофель с небольшим количеством воды до мягкости. Добавку приготовьте до полной готовности и смешайте с картофелем.' END
        WHEN 8 THEN 'Запекайте рыбу до полной готовности (внутри не менее 63 °C). Гарнир отварите или запеките до мягкости и подавайте вместе.'
        WHEN 9 THEN 'Выкладывайте тесто небольшими порциями на разогретую сковороду. Обжарьте оладьи с обеих сторон до полной готовности.'
        ELSE 'Соберите сэндвич из хлеба и начинки. При желании слегка подрумяньте хлеб на сухой сковороде.' END
FROM catalog_v4
ON CONFLICT (recipe_id, position) DO NOTHING;
