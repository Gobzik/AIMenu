package org.seed.aimenu.application.service
import java.util.UUID
import org.seed.aimenu.application.port.output.*
import org.seed.aimenu.application.port.input.MenuUseCases
import org.seed.aimenu.domain.exception.BusinessException
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

    override fun profile(user:UUID)=store.profile(user)
    @Transactional override fun updateProfile(user:UUID,p:Profile):Profile {
        if (p.displayName.length > 80 || p.householdName.length > 80 || p.adults !in 1..20 || p.children !in 0..20) {
            fail("Укажите имя и название семьи до 80 символов; взрослых от 1 до 20, детей от 0 до 20")
        }
        val products=store.products(user).map{
            it.id
        }.toSet()
        val recipes=store.recipes(user).map{
            it.id
        }.toSet()
        if(!(p.allergyProductIds+p.favoriteProductIds).all{
            it in products
        } || !p.favoriteRecipeIds.all{
            it in recipes
        }) fail("Неизвестный продукт или рецепт")
        store.lockUser(user)
        store.saveProfile(user,p)
        return store.profile(user)
    }
    override fun products(user:UUID?)=store.products(user)
    override fun recipes(user:UUID?)=store.recipes(user)
    override fun recipe(id:UUID,user:UUID?)=recipes(user).find{
        it.id==id
    } ?: fail("Рецепт не найден",404)
    @Transactional override fun createProduct(user:UUID,request:CreateProductRequest):Product {
        val name=request.name.trim()
        val unit=request.unit.trim()
        if(name.length !in 2..100 || name.any { it.isISOControl() }) fail("Название продукта должно содержать от 2 до 100 символов")
        if(unit !in setOf("г","мл","шт")) fail("Выберите единицу измерения: г, мл или шт")
        if(request.defaultQuantity <= BigDecimal.ZERO || request.defaultQuantity > BigDecimal("1000000") || request.defaultQuantity.scale() > 3)
            fail("Укажите положительный объём до 1 000 000 с точностью до тысячных")
        if(products(user).any { it.name.equals(name,ignoreCase=true) }) fail("Продукт с таким названием уже есть в каталоге",409)
        return store.addProduct(user,request.copy(name=name,unit=unit))
    }
    @Transactional override fun createRecipe(user:UUID,request:CreateRecipeRequest):Recipe {
        val title=request.title.trim()
        if(title.length !in 2..120 || title.any { it.isISOControl() }) fail("Название рецепта должно содержать от 2 до 120 символов")
        if(request.ingredients.isEmpty() || request.ingredients.size > 30) fail("Добавьте от 1 до 30 ингредиентов")
        if(request.steps.isEmpty() || request.steps.size > 30 || request.steps.any { it.trim().length !in 3..1000 || it.any(Char::isISOControl) })
            fail("Добавьте от 1 до 30 шагов длиной от 3 до 1000 символов")
        val available=products(user).map { it.id }.toSet()
        if(request.ingredients.any { it.productId !in available }) fail("Ингредиент не найден")
        if(request.ingredients.map { it.productId }.toSet().size != request.ingredients.size) fail("Повторяющийся ингредиент")
        if(request.ingredients.any { it.quantity <= BigDecimal.ZERO || it.quantity > BigDecimal("1000000") || it.quantity.scale() > 3 })
            fail("Количество ингредиента должно быть от 0,001 до 1 000 000")
        return store.addRecipe(user,request.copy(title=title,steps=request.steps.map(String::trim)))
    }
    override fun pantry(user:UUID)=store.pantry(user)
    private fun usable(b:Batch)=b.expiresAt==null ||
        (!b.expiresAt.isBefore(LocalDate.now()) && !b.expiresAt.isAfter(LocalDate.of(2500, 12, 31)))
    @Transactional override fun purchase(user:UUID,items:List<Purchase>):List<Batch> {
        if(items.isEmpty() || items.size>100) fail("Добавьте от 1 до 100 продуктов")
        val products=products(user).associateBy{
            it.id
        }
        items.forEach {
            val product=products[it.productId] ?: fail("Продукт не найден")
            if(it.quantity<=BigDecimal.ZERO || it.quantity>BigDecimal("1000000") || it.quantity.scale()>3) fail("Укажите положительное количество до 1 000 000 с точностью до тысячных")
            if(product.perishable && it.expiresAt==null) fail("Для скоропортящегося продукта укажите срок годности")
            if(it.expiresAt?.isBefore(LocalDate.now())==true) fail("Срок годности уже истёк")
            if(it.expiresAt?.isAfter(LocalDate.of(2500, 12, 31))==true) fail("Срок годности не может быть позже 2500 года")
        }
        store.lockUser(user)
        items.forEach{
            store.addPurchase(user,it)
        }
        return pantry(user)
    }
    override fun shopping(user:UUID,id:UUID):List<ShoppingItem> {
        if(recipe(id,user).ingredients.any{
            it.productId in profile(user).allergyProductIds
        }) fail("Рецепт содержит продукт из списка исключений",409)
        val stock=pantry(user).filter(::usable).groupBy{
            it.productId
        }.mapValues{
            (_,v)->v.fold(BigDecimal.ZERO){
                a,b->a+b.quantity
            }
        }
        return recipe(id,user).ingredients.map{
            val available=stock[it.productId]?:BigDecimal.ZERO
            ShoppingItem(it.productId,it.quantity,available,(it.quantity-available).max(BigDecimal.ZERO))
        }
    }
    @Transactional override fun cook(user:UUID,id:UUID,key:String):Map<String,Any> {
        if(key.isBlank() || key.length>128) fail("Ключ приготовления должен содержать от 1 до 128 символов")
        store.lockUser(user)
        store.cooking(user,key)?.let{
            if(it!=id) fail("Этот ключ уже использован для другого рецепта",409)
            return mapOf("recipeId" to id,"replayed" to true)
        }
        val recipe=recipe(id,user)
        if(recipe.ingredients.any{
            it.productId in profile(user).allergyProductIds
        }) fail("Рецепт содержит продукт из списка исключений",409)
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
            if(remaining>BigDecimal.ZERO) fail("Недостаточно продуктов для приготовления",409)
        }
        store.recordCooking(user,key,id)
        return mapOf("recipeId" to id,"replayed" to false)
    }
    override fun recommend(user:UUID)=recommendations.recommend(user,profile(user),pantry(user).filter(::usable),recipes(user))
}
