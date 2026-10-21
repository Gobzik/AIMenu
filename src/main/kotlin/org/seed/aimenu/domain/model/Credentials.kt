package org.seed.aimenu.domain.model

import java.time.LocalDate
import java.math.BigDecimal

data class Credentials(val email: String, val password: String, val profile: Profile = Profile())

