package org.seed.aimenu.domain.model

import java.time.LocalDate
import java.math.BigDecimal

class BusinessException(val status: Int, override val message: String): RuntimeException(message)

