package org.seed.aimenu.application.port.input
import org.seed.aimenu.domain.model.*
interface MenuUseCases {
    fun register(credentials: Credentials): Map<String, Any>
    fun login(credentials: Credentials): Map<String, Any>
    fun authenticate(header: String?): Long
    fun profile(user: Long): Profile
    fun updateProfile(user: Long, profile: Profile): Profile
    fun products(): List<Product>
    fun recipes(): List<Recipe>
    fun recipe(id: Long): Recipe
    fun pantry(user: Long): List<Batch>
    fun purchase(user: Long, items: List<Purchase>): List<Batch>
    fun shopping(user: Long, id: Long): List<ShoppingItem>
    fun cook(user: Long, id: Long, key: String): Map<String, Any>
    fun recommend(user: Long): Any
}
