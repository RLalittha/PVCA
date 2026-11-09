package com.pvca.app.repository

import com.pvca.app.data.repository.LocalAssetCatalogueRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAssetCatalogueRepositoryTest {

    private val sampleJson = """
        [
          {
            "id": "PVCA-SHOE-001",
            "title": "AeroStride Velocity Running Shoe",
            "brand": "AeroStride",
            "category": "Footwear",
            "subcategory": "Running Shoes",
            "description": "Breathable mesh running shoe.",
            "keywords": ["shoe", "running", "sneaker"],
            "color": "Blue / White",
            "price": 89.99,
            "currency": "USD",
            "merchant": "Apex Store",
            "productUrl": "https://demo.pvca.app/1",
            "imageUrl": "https://images.unsplash.com/1",
            "verificationStatus": "demo_verified",
            "provenance": "Internal Catalogue",
            "lastVerified": "2026-11-01"
          },
          {
            "id": "PVCA-ELEC-001",
            "title": "AeroBreeze Ceiling Fan Remote",
            "brand": "AeroBreeze",
            "category": "Consumer Electronics",
            "subcategory": "Remote Controls",
            "description": "Universal fan remote.",
            "keywords": ["remote control", "fan remote", "electronics"],
            "color": "White",
            "price": 24.99,
            "currency": "USD",
            "merchant": "Breeze Store",
            "productUrl": "https://demo.pvca.app/2",
            "imageUrl": "https://images.unsplash.com/2",
            "verificationStatus": "demo_verified",
            "provenance": "Internal Catalogue",
            "lastVerified": "2026-11-01"
          },
          {
            "id": "",
            "title": "Malformed Entry Missing ID"
          }
        ]
    """.trimIndent()

    @Test
    fun parseProductsJson_filtersOutMalformedEntries() {
        val products = LocalAssetCatalogueRepository.parseProductsJson(sampleJson)
        // Should have 2 valid products, 3rd malformed is discarded
        assertEquals(2, products.size)
        assertEquals("PVCA-SHOE-001", products[0].id)
        assertEquals("PVCA-ELEC-001", products[1].id)
    }

    @Test
    fun getProductById_returnsCorrectProduct() = runBlocking {
        val repo = LocalAssetCatalogueRepository(rawJsonString = sampleJson)
        val product = repo.getProductById("PVCA-ELEC-001")
        assertNotNull(product)
        assertEquals("AeroBreeze Ceiling Fan Remote", product?.title)
        assertEquals(24.99, product?.price ?: 0.0, 0.01)

        val missing = repo.getProductById("NON-EXISTENT")
        assertNull(missing)
    }

    @Test
    fun searchProducts_matchesKeywordsAndCategory() = runBlocking {
        val repo = LocalAssetCatalogueRepository(rawJsonString = sampleJson)
        val searchShoe = repo.searchProducts("sneaker")
        assertEquals(1, searchShoe.size)
        assertEquals("PVCA-SHOE-001", searchShoe[0].id)

        val searchRemote = repo.searchProducts("fan remote")
        assertEquals(1, searchRemote.size)
        assertEquals("PVCA-ELEC-001", searchRemote[0].id)

        val emptySearch = repo.searchProducts("sunglasses")
        assertTrue(emptySearch.isEmpty())
    }
}
