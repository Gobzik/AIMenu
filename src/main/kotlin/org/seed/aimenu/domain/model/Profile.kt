package org.seed.aimenu.domain.model
import java.util.UUID

import java.time.LocalDate
import java.math.BigDecimal

data class Profile(
    val allergyProductIds: Set<UUID> = emptySet(),
    val favoriteProductIds: Set<UUID> = emptySet(),
    val favoriteRecipeIds: Set<UUID> = emptySet(),
    val displayName: String = "",
    val householdName: String = "",
    val adults: Int = 1,
    val children: Int = 0,
    val email: String = ""
)

