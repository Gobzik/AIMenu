# Recommendation service

`POST /recommendations` accepts the main backend's current catalog and pantry snapshot. All product/recipe IDs are strings; quantities use the product canonical unit. `expires_at` is an ISO date, usable through that date. Allergies exclude matching product IDs before ranking and storage; the main backend must expand allergen groups to product IDs.

A dedicated PostgreSQL instance with pgvector stores deterministic 64-dimensional ingredient feature-hash vectors. This is a local baseline requiring no AI API key; hash collisions are possible, so similarity is only a small ranking bonus and never an allergy filter. Ranking prioritizes number of missing products, then quantity coverage, near-expiry pantry products, ingredient affinity and favorite recipes. Missing quantities aggregate duplicate ingredients and ignore expired pantry batches. No user profile or pantry is persisted here.

Environment: `DATABASE_URL`. HTTP port: 8000. Readiness: `GET /health`. OpenAPI: `/docs`. Run tests: `python -m unittest test_app.py` from this directory after installing requirements. Database tables initialize at startup; Compose must wait for database health.

Database connections time out after 5 seconds; individual SQL statements after 10 seconds. Containers use UTC for expiry dates (same as the default backend). Quantities are bounded at 1e12 per ingredient.
