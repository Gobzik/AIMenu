package org.seed.aimenu.application.port.output
import java.util.UUID
import org.seed.aimenu.domain.model.*
import java.math.BigDecimal
interface MenuStore {
    fun register(email:String, hash:String):UUID
    fun account(email:String):Pair<UUID,String>?
    fun profile(user:UUID):Profile
    fun saveProfile(user:UUID, profile:Profile)
    fun products(user: UUID? = null):List<Product>
    fun recipes(user: UUID? = null):List<Recipe>
    fun addProduct(user: UUID, request: CreateProductRequest): Product
    fun addRecipe(user: UUID, request: CreateRecipeRequest): Recipe
    fun pantry(user:UUID, lock:Boolean=false):List<Batch>
    fun addPurchase(user:UUID, purchase:Purchase)
    fun consume(batch:UUID, quantity:BigDecimal)
    fun lockUser(user:UUID)
    fun cooking(user:UUID, key:String):UUID?
    fun recordCooking(user:UUID, key:String, recipe:UUID)
}
interface RecommendationPort {
    fun recommend(user:UUID, profile:Profile, pantry:List<Batch>, recipes:List<Recipe>):Any
}
