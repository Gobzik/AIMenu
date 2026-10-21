package org.seed.aimenu.domain.model

import java.time.LocalDate
import java.math.BigDecimal

data class Recipe(val id: Long, val title: String, val ingredients: List<Ingredient>, val steps: List<String>)

