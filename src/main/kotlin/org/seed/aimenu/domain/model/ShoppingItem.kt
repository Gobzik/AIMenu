package org.seed.aimenu.domain.model

import java.time.LocalDate
import java.math.BigDecimal

data class ShoppingItem(val productId: Long, val required: BigDecimal, val available: BigDecimal, val missing: BigDecimal)

