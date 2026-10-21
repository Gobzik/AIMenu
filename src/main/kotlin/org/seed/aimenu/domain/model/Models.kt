package org.seed.aimenu.domain.model
import java.time.LocalDate
import java.math.BigDecimal
data class Profile(val allergyProductIds: Set<Long> = emptySet(), val favoriteProductIds: Set<Long> = emptySet(), val favoriteRecipeIds: Set<Long> = emptySet())
data class Credentials(val email: String, val password: String, val profile: Profile = Profile())
data class Product(val id: Long, val name: String, val unit: String, val perishable: Boolean)
data class Ingredient(val productId: Long, val quantity: BigDecimal)
data class Recipe(val id: Long, val title: String, val ingredients: List<Ingredient>, val steps: List<String>)
data class Purchase(val productId: Long, val quantity: BigDecimal, val expiresAt: LocalDate? = null)
data class Batch(val id: Long, val productId: Long, val quantity: BigDecimal, val expiresAt: LocalDate?)
data class ShoppingItem(val productId: Long, val required: BigDecimal, val available: BigDecimal, val missing: BigDecimal)
data class CookRequest(val idempotencyKey: String)
class BusinessException(val status: Int, override val message: String): RuntimeException(message)
