package org.seed.aimenu.domain.model
import java.util.UUID

import java.time.LocalDate
import java.math.BigDecimal

data class Recipe(
    val id: UUID,
    val title: String,
    val ingredients: List<Ingredient>,
    val steps: List<String>,
    val ownerId: UUID? = null
)

