package org.seed.aimenu.domain.model
import java.util.UUID

import java.time.LocalDate
import java.math.BigDecimal

data class ShoppingItem(val productId: UUID, val required: BigDecimal, val available: BigDecimal, val missing: BigDecimal)

