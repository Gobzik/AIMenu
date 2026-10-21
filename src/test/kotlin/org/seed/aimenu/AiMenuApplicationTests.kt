package org.seed.aimenu

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.seed.aimenu.application.port.output.*
import org.seed.aimenu.application.service.MenuService
import org.seed.aimenu.application.service.AuthService
import org.seed.aimenu.domain.model.*
import java.math.BigDecimal
import java.time.LocalDate

class AiMenuApplicationTests {
    private val store = mock(MenuStore::class.java)
    private val service = MenuService(store, mock(RecommendationPort::class.java), AuthService(store))
    private val recipe = Recipe(1, "Eggs", listOf(Ingredient(1, BigDecimal("2"))), listOf("Cook"))

    @Test fun `expired inventory does not reduce shopping list`() {
        `when`(store.recipes()).thenReturn(listOf(recipe))
        `when`(store.profile(1)).thenReturn(Profile())
        `when`(store.pantry(1, false)).thenReturn(listOf(Batch(1, 1, BigDecimal("5"), LocalDate.now().minusDays(1))))
        assertEquals(0, service.shopping(1, 1).single().missing.compareTo(BigDecimal("2")))
    }

    @Test fun `cooking excluded ingredient fails before deduction`() {
        `when`(store.recipes()).thenReturn(listOf(recipe))
        `when`(store.profile(1)).thenReturn(Profile(allergyProductIds = setOf(1)))
        val error = assertThrows(BusinessException::class.java) { service.cook(1, 1, "request") }
        assertEquals(409, error.status)
        verify(store, never()).consume(1, BigDecimal("2"))
    }

    @Test fun `replayed cooking does not consume inventory again`() {
        `when`(store.cooking(1, "request")).thenReturn(1)
        assertEquals(true, service.cook(1, 1, "request")["replayed"])
        verify(store, never()).pantry(anyLong(), anyBoolean())
    }

    @Test fun `negative purchase rejected`() {
        `when`(store.products()).thenReturn(listOf(Product(1, "Egg", "piece", true)))
        assertThrows(BusinessException::class.java) { service.purchase(1, listOf(Purchase(1, BigDecimal("-1"), LocalDate.now()))) }
        verify(store, never()).addPurchase(1, Purchase(1, BigDecimal("-1"), LocalDate.now()))
    }

    @Test fun `cooking consumes earliest expiry batches first`() {
        `when`(store.recipes()).thenReturn(listOf(recipe))
        `when`(store.profile(1)).thenReturn(Profile())
        `when`(store.pantry(1, true)).thenReturn(listOf(
            Batch(10, 1, BigDecimal("1"), LocalDate.now()),
            Batch(11, 1, BigDecimal("3"), LocalDate.now().plusDays(3))
        ))
        `when`(store.cooking(1, "fefo-request")).thenReturn(null)
        service.cook(1, 1, "fefo-request")
        val order = inOrder(store)
        order.verify(store).consume(10, BigDecimal("1"))
        order.verify(store).consume(11, BigDecimal("1"))
        order.verify(store).recordCooking(1, "fefo-request", 1)
    }
}


