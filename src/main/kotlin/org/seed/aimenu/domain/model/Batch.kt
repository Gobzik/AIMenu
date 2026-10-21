package org.seed.aimenu.domain.model

import java.time.LocalDate
import java.math.BigDecimal

data class Batch(val id: Long, val productId: Long, val quantity: BigDecimal, val expiresAt: LocalDate?)

