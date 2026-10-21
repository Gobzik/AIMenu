package org.seed.aimenu

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.seed.aimenu.application.port.output.*
import org.seed.aimenu.application.service.MenuService
import org.seed.aimenu.application.service.AuthService
import org.seed.aimenu.domain.exception.BusinessException
import org.seed.aimenu.domain.model.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class AiMenuApplicationTests {
    private val store = mock(MenuStore::class.java)
    private val service = MenuService(store, mock(RecommendationPort::class.java), AuthService(store, "test-jwt-secret-with-at-least-32-bytes-2026"))
    private val userId = UUID.randomUUID()
    private val recipeId = UUID.randomUUID()
    private val productId = UUID.randomUUID()
    private val batchA = UUID.randomUUID()
    private val batchB = UUID.randomUUID()
    private val recipe = Recipe(recipeId, "Eggs", listOf(Ingredient(productId, BigDecimal("2"))), listOf("Cook"))

    @Test fun `expired inventory does not reduce shopping list`() {
        `when`(store.recipes(userId)).thenReturn(listOf(recipe))
        `when`(store.profile(userId)).thenReturn(Profile())
        `when`(store.pantry(userId, false)).thenReturn(listOf(Batch(batchA, productId, BigDecimal("5"), LocalDate.now().minusDays(1))))
        assertEquals(0, service.shopping(userId, recipeId).single().missing.compareTo(BigDecimal("2")))
    }

    @Test fun `cooking excluded ingredient fails before deduction`() {
        `when`(store.recipes(userId)).thenReturn(listOf(recipe))
        `when`(store.profile(userId)).thenReturn(Profile(allergyProductIds = setOf(productId)))
        val error = assertThrows(BusinessException::class.java) { service.cook(userId, recipeId, "request") }
        assertEquals(409, error.status)
        verify(store, never()).consume(batchA, BigDecimal("2"))
    }

    @Test fun `replayed cooking does not consume inventory again`() {
        `when`(store.cooking(userId, "request")).thenReturn(recipeId)
        assertEquals(true, service.cook(userId, recipeId, "request")["replayed"])
        verify(store, never()).pantry(userId, true)
    }

    @Test fun `negative purchase rejected`() {
        `when`(store.products(userId)).thenReturn(listOf(Product(productId, "Egg", "piece", true)))
        assertThrows(BusinessException::class.java) { service.purchase(userId, listOf(Purchase(productId, BigDecimal("-1"), LocalDate.now()))) }
        verify(store, never()).addPurchase(userId, Purchase(productId, BigDecimal("-1"), LocalDate.now()))
    }

    @Test fun `cooking consumes earliest expiry batches first`() {
        `when`(store.recipes(userId)).thenReturn(listOf(recipe))
        `when`(store.profile(userId)).thenReturn(Profile())
        `when`(store.pantry(userId, true)).thenReturn(listOf(
            Batch(batchA, productId, BigDecimal("1"), LocalDate.now()),
            Batch(batchB, productId, BigDecimal("3"), LocalDate.now().plusDays(3))
        ))
        `when`(store.cooking(userId, "fefo-request")).thenReturn(null)
        service.cook(userId, recipeId, "fefo-request")
        val order = inOrder(store)
        order.verify(store).consume(batchA, BigDecimal("1"))
        order.verify(store).consume(batchB, BigDecimal("1"))
        order.verify(store).recordCooking(userId, "fefo-request", recipeId)
    }
}




