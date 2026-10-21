package org.seed.aimenu.domain.model

import java.math.BigDecimal

data class CreateProductRequest(
    val name: String,
    val unit: String,
    val perishable: Boolean,
    val defaultQuantity: BigDecimal
)
