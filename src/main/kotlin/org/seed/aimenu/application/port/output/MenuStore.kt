package org.seed.aimenu.application.port.output
import org.seed.aimenu.domain.model.*
import java.math.BigDecimal
interface MenuStore {
    fun register(email:String, hash:String):Long
    fun account(email:String):Pair<Long,String>?
    fun session(user:Long, hash:String)
    fun authenticate(hash:String):Long?
    fun profile(user:Long):Profile
    fun saveProfile(user:Long, profile:Profile)
    fun products():List<Product>
    fun recipes():List<Recipe>
    fun pantry(user:Long, lock:Boolean=false):List<Batch>
    fun addPurchase(user:Long, purchase:Purchase)
    fun consume(batch:Long, quantity:BigDecimal)
    fun lockUser(user:Long)
    fun cooking(user:Long, key:String):Long?
    fun recordCooking(user:Long, key:String, recipe:Long)
}
interface RecommendationPort {
    fun recommend(user:Long, profile:Profile, pantry:List<Batch>, recipes:List<Recipe>):Any
}
