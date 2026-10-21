package org.seed.aimenu.domain.model
import java.util.UUID

import java.time.LocalDate
import java.math.BigDecimal

data class Batch(val id: UUID, val productId: UUID, val quantity: BigDecimal, val expiresAt: LocalDate?)

