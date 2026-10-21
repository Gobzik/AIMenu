package org.seed.aimenu.infrastructure.service
import org.seed.aimenu.application.port.output.RecommendationPort
import org.seed.aimenu.domain.model.*
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestClient
import org.springframework.http.client.JdkClientHttpRequestFactory
import java.net.http.HttpClient
import java.time.Duration
@Component
class HttpRecommendations(@Value("\${recommendations.url}") url:String):RecommendationPort {
    private val client=RestClient.builder().baseUrl(url).requestFactory(JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()).apply{
        setReadTimeout(Duration.ofSeconds(10))
    }).build()
    override fun recommend(user:Long,profile:Profile,pantry:List<Batch>,recipes:List<Recipe>):Any {
        val body=mapOf("user_id" to user.toString(),"allergies" to profile.allergyProductIds.map{
            it.toString()
        },"favorite_product_ids" to profile.favoriteProductIds.map{
            it.toString()
        },"favorite_recipe_ids" to profile.favoriteRecipeIds.map{
            it.toString()
        },"pantry" to pantry.map{
            mapOf("product_id" to it.productId.toString(),"quantity" to it.quantity,"expires_at" to it.expiresAt?.toString())
        },"recipes" to recipes.map{
            mapOf("id" to it.id.toString(),"title" to it.title,"ingredients" to it.ingredients.map{
                i->mapOf("product_id" to i.productId.toString(),"quantity" to i.quantity)
            })
        })
        return try {
            client.post().uri("/recommendations").body(body).retrieve().body(Map::class.java) ?: throw IllegalStateException("Empty response")
        } catch(e:Exception){
            throw BusinessException(503,"Recommendations temporarily unavailable")
        }
    }
}
