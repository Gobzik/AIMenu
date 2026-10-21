package org.seed.aimenu.application.service
import org.seed.aimenu.application.port.output.*
import org.seed.aimenu.application.port.input.MenuUseCases
import org.seed.aimenu.domain.model.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
@Service
class MenuService(private val store:MenuStore,private val recommendations:RecommendationPort, private val auth: AuthService): MenuUseCases {
    private fun fail(message: String, status: Int = 400): Nothing = throw BusinessException(status, message)

    @Transactional
    override fun register(c: Credentials): Map<String, Any> {
        val user = auth.createAccount(c)
        updateProfile(user, c.profile)
        return auth.token(user)
    }

    override fun login(c: Credentials) = auth.login(c)
    override fun authenticate(header: String?) = auth.authenticate(header)

    override fun profile(user:Long)=store.profile(user)
    @Transactional override fun updateProfile(user:Long,p:Profile):Profile {
        val products=store.products().map{
            it.id
        }.toSet()
        val recipes=store.recipes().map{
            it.id
        }.toSet()
        if(!(p.allergyProductIds+p.favoriteProductIds).all{
            it in products
        } || !p.favoriteRecipeIds.all{
            it in recipes
        }) fail("Unknown preference ID")
        store.lockUser(user)
        store.saveProfile(user,p)
        return p
    }
    override fun products()=store.products()
    override fun recipes()=store.recipes()
    override fun recipe(id:Long)=recipes().find{
        it.id==id
    } ?: fail("Recipe not found",404)
    override fun pantry(user:Long)=store.pantry(user)
    private fun usable(b:Batch)=b.expiresAt==null || !b.expiresAt.isBefore(LocalDate.now())
    @Transactional override fun purchase(user:Long,items:List<Purchase>):List<Batch> {
        if(items.isEmpty() || items.size>100) fail("Supply 1 to 100 purchases")
        val products=products().associateBy{
            it.id
        }
        items.forEach {
            val product=products[it.productId] ?: fail("Unknown product")
            if(it.quantity<=BigDecimal.ZERO || it.quantity>BigDecimal("1000000") || it.quantity.scale()>3) fail("Quantity must be positive, at most 1000000 with 3 decimal places")
            if(product.perishable && it.expiresAt==null) fail("Expiry required for perishable product")
            if(it.expiresAt?.isBefore(LocalDate.now())==true) fail("Purchase already expired")
        }
        store.lockUser(user)
        items.forEach{
            store.addPurchase(user,it)
        }
        return pantry(user)
    }
    override fun shopping(user:Long,id:Long):List<ShoppingItem> {
        if(recipe(id).ingredients.any{
            it.productId in profile(user).allergyProductIds
        }) fail("Recipe contains an excluded allergen",409)
        val stock=pantry(user).filter(::usable).groupBy{
            it.productId
        }.mapValues{
            (_,v)->v.fold(BigDecimal.ZERO){
                a,b->a+b.quantity
            }
        }
        return recipe(id).ingredients.map{
            val available=stock[it.productId]?:BigDecimal.ZERO
            ShoppingItem(it.productId,it.quantity,available,(it.quantity-available).max(BigDecimal.ZERO))
        }
    }
    @Transactional override fun cook(user:Long,id:Long,key:String):Map<String,Any> {
        if(key.isBlank() || key.length>128) fail("Idempotency key must be 1 to 128 characters")
        store.lockUser(user)
        store.cooking(user,key)?.let{
            if(it!=id) fail("Idempotency key belongs to another recipe",409)
            return mapOf("recipeId" to id,"replayed" to true)
        }
        val recipe=recipe(id)
        if(recipe.ingredients.any{
            it.productId in profile(user).allergyProductIds
        }) fail("Recipe contains an excluded allergen",409)
        val batches=store.pantry(user,true).filter(::usable)
        recipe.ingredients.forEach {
            ingredient->var remaining=ingredient.quantity
            for(batch in batches.filter{
                it.productId==ingredient.productId
            }) {
                val used=remaining.min(batch.quantity)
                if(used>BigDecimal.ZERO) store.consume(batch.id,used)
                remaining-=used
            }
            if(remaining>BigDecimal.ZERO) fail("Insufficient ingredients",409)
        }
        store.recordCooking(user,key,id)
        return mapOf("recipeId" to id,"replayed" to false)
    }
    override fun recommend(user:Long)=recommendations.recommend(user,profile(user),pantry(user).filter(::usable),recipes())
}
