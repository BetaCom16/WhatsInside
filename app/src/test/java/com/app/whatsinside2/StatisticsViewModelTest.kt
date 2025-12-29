package com.app.whatsinside2

import com.app.whatsinside2.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsViewModelTest {

    @Test
    fun `calculateStats counts correctly`() {
        val now = 10000L
        val day = 24 * 60 * 60 * 1000L

        // Testdaten erstellen - abgelaufene, bald fällige, noch haltbare Produkte und welche ohne MHD
        val products = listOf(
            createProduct(expDate = 5000L),
            createProduct(expDate = now + day),
            createProduct(expDate = now + (10 * day)),
            createProduct(expDate = null)
        )

        // Ruft die Funktion aus dem Companion-Object auf
        val result = StatisticsViewModel.calculateStats(products, now)

        // Prüfung
        assertEquals("Gesamtanzahl", 4, result.totalItems)
        assertEquals("Abgelaufen", 1, result.expiredItems)
        assertEquals("Bald fällig", 1, result.expiringSoonItems)
        assertEquals("Gut", 2, result.goodItems)
    }

    private fun createProduct(expDate: Long?): ProductEntity {
        return ProductEntity(
            barcode = "123",
            name = "Test",
            imageUrl = null,
            calories = null,
            quantity = 1,
            expirationDate = expDate
        )
    }
}