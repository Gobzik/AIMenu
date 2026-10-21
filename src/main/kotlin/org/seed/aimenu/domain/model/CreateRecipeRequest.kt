package org.seed.aimenu.domain.model

data class CreateRecipeRequest(
    val title: String,
    val ingredients: List<Ingredient>,
    val steps: List<String>
)
