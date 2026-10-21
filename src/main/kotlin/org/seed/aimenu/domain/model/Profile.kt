package org.seed.aimenu.domain.model

import java.time.LocalDate
import java.math.BigDecimal

data class Profile(val allergyProductIds: Set<Long> = emptySet(), val favoriteProductIds: Set<Long> = emptySet(), val favoriteRecipeIds: Set<Long> = emptySet())

