package org.seed.aimenu.presentation.controller
import org.seed.aimenu.application.port.input.MenuUseCases
import org.seed.aimenu.domain.model.*
import org.springframework.web.bind.annotation.*
@RestController
@RequestMapping("/api")
class MenuController(private val service:MenuUseCases) {
    @PostMapping("/auth/register") fun register(@RequestBody c:Credentials)=service.register(c)
    @PostMapping("/auth/login") fun login(@RequestBody c:Credentials)=service.login(c)
    @GetMapping("/products") fun products()=service.products()
    @GetMapping("/recipes") fun recipes()=service.recipes()
    @GetMapping("/recipes/{id}") fun recipe(@PathVariable id:Long)=service.recipe(id)
    @GetMapping("/profile") fun profile(@RequestHeader("Authorization",required=false) auth:String?)=service.profile(service.authenticate(auth))
    @PutMapping("/profile") fun profile(@RequestHeader("Authorization",required=false) auth:String?,@RequestBody p:Profile)=service.updateProfile(service.authenticate(auth),p)
    @GetMapping("/pantry") fun pantry(@RequestHeader("Authorization",required=false) auth:String?)=service.pantry(service.authenticate(auth))
    @PostMapping("/pantry/purchases") fun purchase(@RequestHeader("Authorization",required=false) auth:String?,@RequestBody items:List<Purchase>)=service.purchase(service.authenticate(auth),items)
    @GetMapping("/recipes/{id}/shopping-list") fun shopping(@RequestHeader("Authorization",required=false) auth:String?,@PathVariable id:Long)=service.shopping(service.authenticate(auth),id)
    @PostMapping("/recipes/{id}/cook") fun cook(@RequestHeader("Authorization",required=false) auth:String?,@PathVariable id:Long,@RequestBody request:CookRequest)=service.cook(service.authenticate(auth),id,request.idempotencyKey)
    @GetMapping("/recommendations") fun recommendations(@RequestHeader("Authorization",required=false) auth:String?)=service.recommend(service.authenticate(auth))
}
