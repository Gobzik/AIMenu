package org.seed.aimenu.domain.model

import java.time.LocalDate
import java.math.BigDecimal

data class Purchase(val productId: Long, val quantity: BigDecimal, val expiresAt: LocalDate? = null)

