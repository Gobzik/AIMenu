package org.seed.aimenu.application.port.input
import java.util.UUID
import org.seed.aimenu.domain.model.*
interface MenuUseCases {
    fun register(credentials: Credentials): Map<String, Any>
    fun login(credentials: Credentials): Map<String, Any>
    fun authenticate(header: String?): UUID
    fun profile(user: UUID): Profile
    fun updateProfile(user: UUID, profile: Profile): Profile
    fun products(user: UUID? = null): List<Product>
    fun recipes(user: UUID? = null): List<Recipe>
    fun recipe(id: UUID, user: UUID? = null): Recipe
    fun createProduct(user: UUID, request: CreateProductRequest): Product
    fun createRecipe(user: UUID, request: CreateRecipeRequest): Recipe
    fun pantry(user: UUID): List<Batch>
    fun purchase(user: UUID, items: List<Purchase>): List<Batch>
    fun shopping(user: UUID, id: UUID): List<ShoppingItem>
    fun cook(user: UUID, id: UUID, key: String): Map<String, Any>
    fun recommend(user: UUID): Any
}
