package org.seed.aimenu.presentation.controller
import java.util.UUID

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as ApiRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.seed.aimenu.application.port.input.MenuUseCases
import org.seed.aimenu.domain.model.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Tag(name = "AIMenu", description = "Каталог, профиль, запасы, приготовление и рекомендации")
class MenuController(private val service: MenuUseCases) {
    @PostMapping("/auth/register")
    @Operation(summary = "Регистрация", description = "Создает учетную запись, сохраняет предпочтения и возвращает JWT на 7 дней. Все ID имеют формат UUID. Пароль: от 10 до 72 байт.")
    @ApiRequestBody(content = [Content(examples = [ExampleObject(value = """{"email":"user@example.com","password":"secure-password-123","profile":{"allergyProductIds":["449d3b88-076a-abd6-84b6-a50656441e58"],"favoriteProductIds":["cc73019d-da30-5f9e-c773-5c4e1a2d1da2"],"favoriteRecipeIds":["246fd25e-facd-7c75-cd31-dd1590099fe2"]}}""")])])
    @ApiResponse(responseCode = "200", description = "JWT и UUID пользователя", content = [Content(examples = [ExampleObject(value = """{"token":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MTY1NDBmMC0wZDk0LTQ4MDYtOTk3YS02ODc0ZjJjMGI0ZTQifQ.signature","userId":"416540f0-0d94-4806-997a-6874f2c0b4e4","expiresIn":604800}""")])])
    @ApiResponse(responseCode = "400", description = "Некорректные данные")
    @ApiResponse(responseCode = "409", description = "Email уже занят")
    fun register(@RequestBody credentials: Credentials) = service.register(credentials)

    @PostMapping("/auth/login")
    @Operation(summary = "Вход", description = "Проверяет email и пароль, возвращает новый токен.")
    @ApiRequestBody(content = [Content(examples = [ExampleObject(value = """{"email":"user@example.com","password":"secure-password-123"}""")])])
    @ApiResponse(responseCode = "200", description = "Токен")
    @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    fun login(@RequestBody credentials: Credentials) = service.login(credentials)

    @GetMapping("/products")
    @Operation(summary = "Каталог продуктов", description = "ID, название, единица измерения и признак скоропортящегося продукта.")
    @ApiResponse(responseCode = "200", content = [Content(examples = [ExampleObject(value = """[{"id":"449d3b88-076a-abd6-84b6-a50656441e58","name":"Молоко","unit":"ml","perishable":true}]""")])])
    fun products(@Parameter(hidden = true) @RequestHeader("Authorization", required = false) auth: String?) = service.products(auth?.let(service::authenticate))

    @GetMapping("/recipes")
    @Operation(summary = "Каталог рецептов", description = "Каждый рецепт содержит ингредиенты с количествами и упорядоченные шаги.")
    fun recipes(@Parameter(hidden = true) @RequestHeader("Authorization", required = false) auth: String?) = service.recipes(auth?.let(service::authenticate))

    @GetMapping("/recipes/{id}")
    @Operation(summary = "Рецепт и шаги", description = "Получить ингредиенты и последовательность приготовления.")
    @ApiResponse(responseCode = "200", content = [Content(examples = [ExampleObject(value = """{"id":"c41cbbec-fb8e-9550-6d85-a037d6edde2a","title":"Омлет","ingredients":[{"productId":"91e156f7-ef64-aa62-5632-de7388039980","quantity":2},{"productId":"449d3b88-076a-abd6-84b6-a50656441e58","quantity":100}],"steps":["Взбейте яйца с молоком.","Готовьте на сковороде на среднем огне до полной готовности."]}""")])])
    @ApiResponse(responseCode = "404", description = "Рецепт не найден")
    fun recipe(@PathVariable id: UUID, @Parameter(hidden = true) @RequestHeader("Authorization", required = false) auth: String?) = service.recipe(id,auth?.let(service::authenticate))

    @GetMapping("/profile")
    @Operation(summary = "Мой профиль", security = [SecurityRequirement(name = "bearerAuth")])
    @ApiResponse(responseCode = "401", description = "Нет или истек токен")
    fun profile(@Parameter(hidden = true) @RequestHeader("Authorization", required = false) auth: String?) = service.profile(service.authenticate(auth))

    @PutMapping("/profile")
    @Operation(summary = "Изменить предпочтения и аллергии", description = "Передайте все три списка ID; неизвестные ID отклоняются.", security = [SecurityRequirement(name = "bearerAuth")])
    @ApiRequestBody(content = [Content(examples = [ExampleObject(value = """{"allergyProductIds":["449d3b88-076a-abd6-84b6-a50656441e58"],"favoriteProductIds":["cc73019d-da30-5f9e-c773-5c4e1a2d1da2"],"favoriteRecipeIds":["246fd25e-facd-7c75-cd31-dd1590099fe2"]}""")])])
    @ApiResponse(responseCode = "400", description = "Неизвестный ID")
    @ApiResponse(responseCode = "401", description = "Нет или истек токен")
    fun profile(@Parameter(hidden = true) @RequestHeader("Authorization", required = false) auth: String?, @RequestBody profile: Profile) = service.updateProfile(service.authenticate(auth), profile)

    @GetMapping("/pantry")
    @Operation(summary = "Мои остатки", description = "Партии продуктов с количеством и сроком годности. Просроченные партии могут отображаться, но не используются при готовке.", security = [SecurityRequirement(name = "bearerAuth")])
    @ApiResponse(responseCode = "200", content = [Content(examples = [ExampleObject(value = """[{"id":"416540f0-0d94-4806-997a-6874f2c0b4e4","productId":"449d3b88-076a-abd6-84b6-a50656441e58","quantity":500,"expiresAt":"2030-10-25"}]""")])])
    fun pantry(@Parameter(hidden = true) @RequestHeader("Authorization", required = false) auth: String?) = service.pantry(service.authenticate(auth))

    @PostMapping("/pantry/purchases")
    @Operation(summary = "Добавить продукты", description = "Укажите купленное или уже имеющееся количество. Для скоропортящихся продуктов срок годности обязателен; дата должна быть от сегодняшнего дня до 31.12.2500.", security = [SecurityRequirement(name = "bearerAuth")])
    @ApiRequestBody(content = [Content(examples = [ExampleObject(value = """[{"productId":"449d3b88-076a-abd6-84b6-a50656441e58","quantity":500,"expiresAt":"2030-10-25"},{"productId":"cc73019d-da30-5f9e-c773-5c4e1a2d1da2","quantity":300}]""")])])
    @ApiResponse(responseCode = "200", description = "Обновленные партии")
    @ApiResponse(responseCode = "400", description = "Количество или срок годности некорректны")
    fun purchase(@Parameter(hidden = true) @RequestHeader("Authorization", required = false) auth: String?, @RequestBody items: List<Purchase>) = service.purchase(service.authenticate(auth), items)

    @GetMapping("/recipes/{id}/shopping-list")
    @Operation(summary = "Список покупок для блюда", description = "required, available и missing указаны в единицах продукта. Просроченные остатки исключены. Блюдо с аллергеном отклоняется.", security = [SecurityRequirement(name = "bearerAuth")])
    @ApiResponse(responseCode = "200", content = [Content(examples = [ExampleObject(value = """[{"productId":"cc73019d-da30-5f9e-c773-5c4e1a2d1da2","required":100,"available":50,"missing":50}]""")])])
    @ApiResponse(responseCode = "404", description = "Рецепт не найден")
    @ApiResponse(responseCode = "409", description = "Блюдо содержит исключенный продукт")
    fun shopping(@Parameter(hidden = true) @RequestHeader("Authorization", required = false) auth: String?, @PathVariable id: UUID) = service.shopping(service.authenticate(auth), id)

    @PostMapping("/recipes/{id}/cook")
    @Operation(summary = "Приготовить блюдо", description = "Списывает ингредиенты атомарно, начиная с партий с ближайшим сроком годности. Повтор того же idempotencyKey не списывает их снова.", security = [SecurityRequirement(name = "bearerAuth")])
    @ApiRequestBody(content = [Content(examples = [ExampleObject(value = """{"idempotencyKey":"dinner-2026-10-21"}""")])])
    @ApiResponse(responseCode = "200", content = [Content(examples = [ExampleObject(value = """{"recipeId":"246fd25e-facd-7c75-cd31-dd1590099fe2","replayed":false}""")])])
    @ApiResponse(responseCode = "409", description = "Недостаточно продуктов, аллерген или повторное использование ключа для другого блюда")
    fun cook(@Parameter(hidden = true) @RequestHeader("Authorization", required = false) auth: String?, @PathVariable id: UUID, @RequestBody request: CookRequest) = service.cook(service.authenticate(auth), id, request.idempotencyKey)

    @GetMapping("/recommendations")
    @Operation(summary = "Рекомендации из остатков", description = "Сортирует блюда по доступности ингредиентов, минимальным покупкам, предпочтениям и сроку годности. Аллергенные блюда исключаются.", security = [SecurityRequirement(name = "bearerAuth")])
    @ApiResponse(responseCode = "200", content = [Content(examples = [ExampleObject(value = """{"recommendations":[{"recipe_id":"246fd25e-facd-7c75-cd31-dd1590099fe2","score":105.0,"missing_ingredients":[],"reason":"Все продукты уже есть"}]}""")])])
    @ApiResponse(responseCode = "503", description = "Сервис рекомендаций недоступен")
    fun recommendations(@Parameter(hidden = true) @RequestHeader("Authorization", required = false) auth: String?) = service.recommend(service.authenticate(auth))
}


