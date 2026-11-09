package com.pvca.app.data.repository

import com.pvca.app.data.model.ProductItem

/**
 * Repository interface providing access to the PVCA product catalogue.
 * Decouples the matching engine and UI from storage mechanisms (local JSON, SQLite, or future REST API).
 */
interface ProductCatalogueRepository {
    /**
     * Retrieves all products in the catalogue.
     */
    suspend fun getAllProducts(): List<ProductItem>

    /**
     * Retrieves a single product by its unique identifier.
     */
    suspend fun getProductById(id: String): ProductItem?

    /**
     * Filters products matching keyword tokens or categories.
     */
    suspend fun searchProducts(query: String): List<ProductItem>
}
