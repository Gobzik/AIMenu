package org.seed.aimenu.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.parameters.RequestBody as ApiRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.seed.aimenu.application.port.input.MenuUseCases
import org.seed.aimenu.domain.model.CreateProductRequest
import org.seed.aimenu.domain.model.CreateRecipeRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@Tag(name = "Личный каталог", description = "Собственные продукты и рецепты, доступные только владельцу")
class CustomCatalogController(private val service: MenuUseCases) {
    @PostMapping("/products")
    @Operation(summary = "Добавить свой продукт", description = "Объём по умолчанию предлагается при добавлении в запасы. Пользователь может изменить его перед покупкой. Поддерживаются единицы: г, мл, шт. Свой продукт виден только владельцу.", security = [SecurityRequirement(name = "bearerAuth")])
    @ApiRequestBody(content = [Content(examples = [ExampleObject(value = """{"name":"Миндальное молоко","unit":"мл","perishable":true,"defaultQuantity":250}""")])])
    @ApiResponse(responseCode = "200", description = "Продукт с UUID и ownerId", content = [Content(examples = [ExampleObject(value = """{"id":"8c6feab4-e69c-4784-ab04-891ca5bf6b18","name":"Миндальное молоко","unit":"мл","perishable":true,"defaultQuantity":250,"ownerId":"416540f0-0d94-4806-997a-6874f2c0b4e4"}""")])])
    @ApiResponse(responseCode = "400", description = "Некорректные название, единица или объём")
    @ApiResponse(responseCode = "401", description = "Нет или истёк токен")
    @ApiResponse(responseCode = "409", description = "Продукт с таким названием уже доступен")
    fun product(@Parameter(hidden = true) @RequestHeader("Authorization",required = false) auth:String?, @RequestBody request:CreateProductRequest) =
        service.createProduct(service.authenticate(auth),request)

    @PostMapping("/recipes")
    @Operation(summary = "Сохранить свой рецепт", description = "От 1 до 30 разных ингредиентов и шагов. ID продуктов должны принадлежать общему каталогу или текущему пользователю. Рецепт сразу доступен в личном каталоге и рекомендациях.", security = [SecurityRequirement(name = "bearerAuth")])
    @ApiRequestBody(content = [Content(examples = [ExampleObject(value = """{"title":"Тост с авокадо","ingredients":[{"productId":"8c6feab4-e69c-4784-ab04-891ca5bf6b18","quantity":1}],"steps":["Подготовьте продукты.","Соберите тост и подавайте."]}""")])])
    @ApiResponse(responseCode = "200", description = "Рецепт с UUID и ownerId")
    @ApiResponse(responseCode = "400", description = "Некорректные данные или чужой продукт")
    @ApiResponse(responseCode = "401", description = "Нет или истёк токен")
    fun recipe(@Parameter(hidden = true) @RequestHeader("Authorization",required = false) auth:String?, @RequestBody request:CreateRecipeRequest) =
        service.createRecipe(service.authenticate(auth),request)
}
