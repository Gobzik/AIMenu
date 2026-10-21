package org.seed.aimenu.infrastructure.persistance.adapter
import org.seed.aimenu.application.port.output.MenuStore
import org.seed.aimenu.domain.model.*
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.math.BigDecimal
@Repository
class JdbcMenuStore(private val db:JdbcClient):MenuStore {
    override fun register(email:String,hash:String)=db.sql("INSERT INTO users(email,password_hash) VALUES (?,?) RETURNING id").params(email,hash).query { row, _ -> row.getLong(1) }.single()
    override fun account(email:String)=db.sql("SELECT id,password_hash FROM users WHERE email=?").param(email).query {
        r,_->r.getLong(1) to r.getString(2)
    }.optional().orElse(null)
    override fun session(user:Long,hash:String) {
        db.sql("INSERT INTO sessions(token_hash,user_id) VALUES (?,?)").params(hash,user).update()
    }
    override fun authenticate(hash:String)=db.sql("SELECT user_id FROM sessions WHERE token_hash=? AND expires_at>now()").param(hash).query(Long::class.java).optional().orElse(null)
    private fun ids(user:Long,kind:String)=db.sql("SELECT target_id FROM preferences WHERE user_id=? AND kind=?").params(user,kind).query { row, _ -> row.getLong(1) }.list().toSet()
    override fun profile(user:Long)=Profile(ids(user,"allergy"),ids(user,"product"),ids(user,"recipe"))
    override fun saveProfile(user:Long,profile:Profile) {
        db.sql("DELETE FROM preferences WHERE user_id=?").param(user).update()
        mapOf("allergy" to profile.allergyProductIds,"product" to profile.favoriteProductIds,"recipe" to profile.favoriteRecipeIds).forEach {
            (kind,ids)->ids.forEach {
                db.sql("INSERT INTO preferences VALUES (?,?,?)").params(user,kind,it).update()
            }
        }
    }
    override fun products()=db.sql("SELECT * FROM products ORDER BY id").query {
        r,_->Product(r.getLong("id"),r.getString("name"),r.getString("unit"),r.getBoolean("perishable"))
    }.list()
    override fun recipes()=db.sql("SELECT * FROM recipes ORDER BY id").query {
        r,_->
        val id=r.getLong("id")
        Recipe(id,r.getString("title"),db.sql("SELECT * FROM ingredients WHERE recipe_id=? ORDER BY product_id").param(id).query {
            i,_->Ingredient(i.getLong("product_id"),i.getBigDecimal("quantity"))
        }.list(),db.sql("SELECT instruction FROM steps WHERE recipe_id=? ORDER BY position").param(id).query { row, _ -> requireNotNull(row.getString(1)) }.list())
    }.list()
    override fun pantry(user:Long,lock:Boolean)=db.sql("SELECT * FROM batches WHERE user_id=? AND quantity>0 ORDER BY expires_at NULLS LAST,id" + if(lock) " FOR UPDATE" else "").param(user).query {
        r,_->Batch(r.getLong("id"),r.getLong("product_id"),r.getBigDecimal("quantity"),r.getDate("expires_at")?.toLocalDate())
    }.list()
    override fun addPurchase(user:Long,purchase:Purchase) {
        db.sql("INSERT INTO batches(user_id,product_id,quantity,expires_at) VALUES (:u,:p,:q,:e)").param("u",user).param("p",purchase.productId).param("q",purchase.quantity).param("e",purchase.expiresAt,java.sql.Types.DATE).update()
    }
    override fun consume(batch:Long,quantity:BigDecimal) {
        db.sql("UPDATE batches SET quantity=quantity-? WHERE id=?").params(quantity,batch).update()
    }
    override fun lockUser(user:Long) {
        db.sql("SELECT id FROM users WHERE id=? FOR UPDATE").param(user).query { row, _ -> row.getLong(1) }.single()
    }
    override fun cooking(user:Long,key:String)=db.sql("SELECT recipe_id FROM cooking_events WHERE user_id=? AND request_key=?").params(user,key).query(Long::class.java).optional().orElse(null)
    override fun recordCooking(user:Long,key:String,recipe:Long) {
        db.sql("INSERT INTO cooking_events VALUES (?,?,?,now())").params(user,key,recipe).update()
    }
}
