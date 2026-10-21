package org.seed.aimenu.domain.model
import java.util.UUID

import java.time.LocalDate
import java.math.BigDecimal

data class Purchase(val productId: UUID, val quantity: BigDecimal, val expiresAt: LocalDate? = null)

