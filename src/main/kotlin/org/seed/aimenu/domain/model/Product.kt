package org.seed.aimenu.domain.model
import java.util.UUID

import java.time.LocalDate
import java.math.BigDecimal

data class Product(
    val id: UUID,
    val name: String,
    val unit: String,
    val perishable: Boolean,
    val defaultQuantity: BigDecimal = BigDecimal.ONE,
    val ownerId: UUID? = null
)

