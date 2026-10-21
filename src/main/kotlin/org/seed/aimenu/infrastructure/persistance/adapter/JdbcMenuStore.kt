package org.seed.aimenu.infrastructure.persistance.adapter
import java.util.UUID
import org.seed.aimenu.application.port.output.MenuStore
import org.seed.aimenu.domain.model.*
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.math.BigDecimal
@Repository
class JdbcMenuStore(private val db:JdbcClient):MenuStore {
    override fun register(email:String,hash:String)=db.sql("INSERT INTO users(email,password_hash) VALUES (?,?) RETURNING id").params(email,hash).query { row, _ -> row.getObject(1, UUID::class.java) }.single()
    override fun account(email:String)=db.sql("SELECT id,password_hash FROM users WHERE email=?").param(email).query {
        r,_->r.getObject(1, UUID::class.java) to r.getString(2)
    }.optional().orElse(null)
    private fun ids(user:UUID,kind:String)=db.sql("SELECT target_id FROM preferences WHERE user_id=? AND kind=?").params(user,kind).query { row, _ -> row.getObject(1, UUID::class.java) }.list().toSet()
    override fun profile(user:UUID):Profile {
        val details = db.sql("SELECT email, display_name, household_name, adults, children FROM users WHERE id=?")
            .param(user).query { row, _ ->
                Profile(
                    allergyProductIds = ids(user,"allergy"),
                    favoriteProductIds = ids(user,"product"),
                    favoriteRecipeIds = ids(user,"recipe"),
                    displayName = row.getString("display_name") ?: "",
                    householdName = row.getString("household_name") ?: "",
                    adults = row.getInt("adults"),
                    children = row.getInt("children"),
                    email = row.getString("email") ?: ""
                )
            }.single()
        return details
    }
    override fun saveProfile(user:UUID,profile:Profile) {
        db.sql("UPDATE users SET display_name=?, household_name=?, adults=?, children=? WHERE id=?")
            .params(profile.displayName.trim(), profile.householdName.trim(), profile.adults, profile.children, user).update()
        db.sql("DELETE FROM preferences WHERE user_id=?").param(user).update()
        mapOf("allergy" to profile.allergyProductIds,"product" to profile.favoriteProductIds,"recipe" to profile.favoriteRecipeIds).forEach {
            (kind,ids)->ids.forEach {
                db.sql("INSERT INTO preferences(user_id,kind,target_id) VALUES (?::uuid,?,?::uuid)").params(user,kind,it).update()
            }
        }
    }
    override fun products(user:UUID?)=db.sql("SELECT * FROM products WHERE owner_user_id IS NULL OR owner_user_id=? ORDER BY name,id").param(user).query {
        r,_->Product(r.getObject("id", UUID::class.java),r.getString("name"),r.getString("unit"),r.getBoolean("perishable"),r.getBigDecimal("default_quantity"),r.getObject("owner_user_id",UUID::class.java))
    }.list()
    override fun recipes(user:UUID?)=db.sql("SELECT * FROM recipes WHERE owner_user_id IS NULL OR owner_user_id=? ORDER BY title,id").param(user).query {
        r,_->
        val id=r.getObject("id", UUID::class.java)
        Recipe(id,r.getString("title"),db.sql("SELECT * FROM ingredients WHERE recipe_id=? ORDER BY product_id").param(id).query {
            i,_->Ingredient(i.getObject("product_id", UUID::class.java),i.getBigDecimal("quantity"))
        }.list(),db.sql("SELECT instruction FROM steps WHERE recipe_id=? ORDER BY position").param(id).query { row, _ -> requireNotNull(row.getString(1)) }.list(),r.getObject("owner_user_id",UUID::class.java))
    }.list()
    override fun addProduct(user:UUID,request:CreateProductRequest):Product {
        val id=db.sql("INSERT INTO products(name,unit,perishable,default_quantity,owner_user_id) VALUES (?,?,?,?,?) RETURNING id")
            .params(request.name.trim(),request.unit.trim(),request.perishable,request.defaultQuantity,user)
            .query { row,_->row.getObject(1,UUID::class.java) }.single()
        return Product(id,request.name.trim(),request.unit.trim(),request.perishable,request.defaultQuantity,user)
    }
    override fun addRecipe(user:UUID,request:CreateRecipeRequest):Recipe {
        val id=db.sql("INSERT INTO recipes(title,owner_user_id) VALUES (?,?) RETURNING id")
            .params(request.title.trim(),user).query { row,_->row.getObject(1,UUID::class.java) }.single()
        request.ingredients.forEach { ingredient ->
            db.sql("INSERT INTO ingredients(recipe_id,product_id,quantity) VALUES (?,?,?)")
                .params(id,ingredient.productId,ingredient.quantity).update()
        }
        request.steps.forEachIndexed { index, step ->
            db.sql("INSERT INTO steps(recipe_id,position,instruction) VALUES (?,?,?)")
                .params(id,index+1,step.trim()).update()
        }
        return Recipe(id,request.title.trim(),request.ingredients,request.steps.map(String::trim),user)
    }
    override fun pantry(user:UUID,lock:Boolean)=db.sql("SELECT * FROM batches WHERE user_id=? AND quantity>0 ORDER BY expires_at NULLS LAST,id" + if(lock) " FOR UPDATE" else "").param(user).query {
        r,_->Batch(r.getObject("id", UUID::class.java),r.getObject("product_id", UUID::class.java),r.getBigDecimal("quantity"),r.getDate("expires_at")?.toLocalDate())
    }.list()
    override fun addPurchase(user:UUID,purchase:Purchase) {
        db.sql("INSERT INTO batches(user_id,product_id,quantity,expires_at) VALUES (CAST(:u AS uuid),CAST(:p AS uuid),:q,:e)").param("u",user).param("p",purchase.productId).param("q",purchase.quantity).param("e",purchase.expiresAt,java.sql.Types.DATE).update()
    }
    override fun consume(batch:UUID,quantity:BigDecimal) {
        db.sql("UPDATE batches SET quantity=quantity-? WHERE id=?").params(quantity,batch).update()
    }
    override fun lockUser(user:UUID) {
        db.sql("SELECT id FROM users WHERE id=? FOR UPDATE").param(user).query { row, _ -> row.getObject(1, UUID::class.java) }.single()
    }
    override fun cooking(user:UUID,key:String)=db.sql("SELECT recipe_id FROM cooking_events WHERE user_id=? AND request_key=?").params(user,key).query(UUID::class.java).optional().orElse(null)
    override fun recordCooking(user:UUID,key:String,recipe:UUID) {
        db.sql("INSERT INTO cooking_events(user_id,request_key,recipe_id,cooked_at) VALUES (?::uuid,?,?::uuid,now())").params(user,key,recipe).update()
    }
}

