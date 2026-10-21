# AIMenu backend

Kotlin/Spring Boot 4 API, PostgreSQL and separate FastAPI recommendations service with its own pgvector PostgreSQL database.

## Start

```sh
docker compose up --build -d --wait
python scripts/smoke.py
```

API: http://localhost:8080/api. Health: http://localhost:8080/actuator/health. Compose starts four services and keeps database data in separate volumes. The Docker build runs five Kotlin tests. The recommendations image tests can be run with `docker compose run --rm --no-deps recommendations python -m unittest test_app.py`.

## Flow

Fetch `GET /api/products` and `GET /api/recipes`. Every entity ID is a UUID. Register with `POST /api/auth/register`, sending `email`, `password` (10–72 bytes), and optional `profile` with `allergyProductIds`, `favoriteProductIds`, and `favoriteRecipeIds`. Use the returned signed HS256 JWT in `Authorization: Bearer TOKEN`. `POST /api/auth/login` returns a new JWT. Tokens expire after seven days; the signing key comes from `JWT_SECRET` in Compose. Set a private value before deployment.

`GET/PUT /api/profile` edits preferences. `GET /api/recipes/{id}` includes ingredients and ordered steps. `POST /api/pantry/purchases` accepts an array of `{productId, quantity, expiresAt}`; use it for existing stock and new purchases. `expiresAt` is required for perishable products, in YYYY-MM-DD format. `GET /api/recipes/{id}/shopping-list` returns required, available, and missing quantities. `POST /api/recipes/{id}/cook` accepts `{idempotencyKey}` and consumes available batches by nearest expiry atomically. `GET /api/pantry` lists stock. `GET /api/recommendations` calls the recommendations service.

Quantities use each product's catalog unit (g, ml, piece). Allergies match exact product IDs; allergen groups and cross contact are not modeled. The seeded catalog has four sample recipes. Recipe vectors are deterministic ingredient embeddings, computed locally without external ML APIs.

Swagger UI: http://localhost:8080/swagger-ui/index.html; OpenAPI JSON: http://localhost:8080/v3/api-docs. In Swagger UI use Authorize with the JWT returned by registration or login. Examples use valid UUID syntax; choose actual catalog IDs from `GET /api/products` and `GET /api/recipes`.

## PWA frontend

Run `docker compose up --build -d --wait` and open http://localhost:3000. The frontend is a buildless static PWA served by nginx; it proxies /api/ to the backend. Visual specification: frontend/DESIGN.md. The installable app shell is cached for offline opening; stock and recommendations require API connectivity. Food photography is bundled locally in frontend/assets. 

