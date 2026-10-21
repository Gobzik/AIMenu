package org.seed.aimenu.domain.model

import java.time.LocalDate
import java.math.BigDecimal

data class Product(val id: Long, val name: String, val unit: String, val perishable: Boolean)

