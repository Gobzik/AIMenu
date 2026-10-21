"""Ingredient recommendation service; quantities must use each product's canonical unit."""
import hashlib
import math
import os
from datetime import date
from contextlib import asynccontextmanager
from typing import Protocol

import psycopg
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

DIMENSIONS = 64

class Ingredient(BaseModel):
    product_id: str
    quantity: float = Field(gt=0, le=1e12, allow_inf_nan=False)

class PantryItem(Ingredient):
    expires_at: date | None = None

class Recipe(BaseModel):
    id: str
    title: str
    ingredients: list[Ingredient] = Field(min_length=1, max_length=1000)

class RecommendationRequest(BaseModel):
    user_id: str | None = None
    allergies: list[str] = Field(default_factory=list)
    favorite_product_ids: list[str] = Field(default_factory=list)
    favorite_recipe_ids: list[str] = Field(default_factory=list)
    pantry: list[PantryItem] = Field(default_factory=list)
    recipes: list[Recipe] = Field(default_factory=list, max_length=10000)
    limit: int = Field(default=10, ge=1, le=100)

class Recommendation(BaseModel):
    recipe_id: str
    score: float
    missing_ingredients: list[Ingredient]
    reason: str

class RecommendationResponse(BaseModel):
    recommendations: list[Recommendation]


def embedding(products: list[str]) -> list[float]:
    vector = [0.0] * DIMENSIONS
    for product in set(products):
        digest = hashlib.sha256(product.encode()).digest()
        vector[int.from_bytes(digest[:4], 'big') % DIMENSIONS] += 1.0 if digest[4] % 2 else -1.0
    norm = math.sqrt(sum(value * value for value in vector))
    return [value / norm for value in vector] if norm else vector

class SimilarityRepository(Protocol):
    def similarities(self, recipes: list[Recipe], preferred: list[str]) -> dict[str, float]: ...

class PgVectorRepository:
    def __init__(self, dsn: str):
        self.dsn = dsn

    def initialize(self):
        with psycopg.connect(self.dsn, connect_timeout=5, options='-c statement_timeout=10000') as conn:
            conn.execute('CREATE EXTENSION IF NOT EXISTS vector')
            conn.execute('CREATE TABLE IF NOT EXISTS recipe_vectors (id text PRIMARY KEY, embedding vector(64) NOT NULL)')

    def similarities(self, recipes: list[Recipe], preferred: list[str]) -> dict[str, float]:
        if not recipes:
            return {}
        query = str(embedding(preferred))
        with psycopg.connect(self.dsn, connect_timeout=5, options='-c statement_timeout=10000') as conn:
            for recipe in recipes:
                conn.execute('INSERT INTO recipe_vectors (id, embedding) VALUES (%s, %s::vector) ON CONFLICT (id) DO UPDATE SET embedding = EXCLUDED.embedding',
                             (recipe.id, str(embedding([i.product_id for i in recipe.ingredients]))))
            # Inner product is well-defined even when the preference vector is zero.
            rows = conn.execute('SELECT id, -(embedding <#> %s::vector) FROM recipe_vectors WHERE id = ANY(%s)',
                                (query, [r.id for r in recipes])).fetchall()
        return dict(rows)

class RecommendationService:
    def __init__(self, repository: SimilarityRepository):
        self.repository = repository

    def recommend(self, request: RecommendationRequest, today: date | None = None):
        today = today or date.today()
        forbidden = set(request.allergies)
        safe = [r for r in request.recipes if not any(i.product_id in forbidden for i in r.ingredients)]
        similarities = self.repository.similarities(safe, request.favorite_product_ids)
        available: dict[str, float] = {}
        urgent = set()
        for item in request.pantry:
            if item.expires_at is not None and item.expires_at < today:
                continue
            available[item.product_id] = available.get(item.product_id, 0) + item.quantity
            if item.expires_at and (item.expires_at - today).days <= 3:
                urgent.add(item.product_id)
        results = []
        for recipe in safe:
            needed: dict[str, float] = {}
            for item in recipe.ingredients:
                needed[item.product_id] = needed.get(item.product_id, 0) + item.quantity
            missing = [Ingredient(product_id=pid, quantity=round(qty - available.get(pid, 0), 9))
                       for pid, qty in needed.items() if qty - available.get(pid, 0) > 1e-8]
            coverage = sum(min(available.get(pid, 0) / qty, 1) for pid, qty in needed.items()) / len(needed)
            urgency = len(set(needed) & urgent) / len(needed)
            score = 100 * coverage + 5 * urgency + 3 * similarities.get(recipe.id, 0) + (2 if recipe.id in request.favorite_recipe_ids else 0)
            results.append(Recommendation(recipe_id=recipe.id, score=round(score, 4), missing_ingredients=missing,
                         reason='Все продукты уже есть' if not missing else f'Нужно докупить: {len(missing)}; есть {coverage:.0%} продуктов'))
        results.sort(key=lambda r: (len(r.missing_ingredients), -r.score, r.recipe_id))
        return RecommendationResponse(recommendations=results[:request.limit])

repository = PgVectorRepository(os.getenv('DATABASE_URL', 'postgresql://recommendations:recommendations@recommendations-db:5432/recommendations'))

@asynccontextmanager
async def lifespan(app: FastAPI):
    repository.initialize()
    yield

app = FastAPI(title='AIMenu Recommendations', lifespan=lifespan)

@app.get('/health')
def health():
    try:
        with psycopg.connect(repository.dsn, connect_timeout=5, options='-c statement_timeout=10000') as conn:
            conn.execute('SELECT 1 FROM recipe_vectors LIMIT 1')
        return {'status': 'ok'}
    except psycopg.Error as exc:
        raise HTTPException(status_code=503, detail='Recommendation storage unavailable') from exc

@app.post('/recommendations', response_model=RecommendationResponse)
def recommend(request: RecommendationRequest):
    try:
        return RecommendationService(repository).recommend(request)
    except psycopg.Error as exc:
        raise HTTPException(status_code=503, detail='Recommendation storage unavailable') from exc

