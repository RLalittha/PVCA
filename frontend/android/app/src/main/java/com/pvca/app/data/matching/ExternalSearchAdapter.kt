package com.pvca.app.data.matching

import com.pvca.app.data.model.ProductItem

/**
 * Interface contract for external product-search providers.
 *
 * NOTE: For Day 3, external search is cleanly abstracted but NOT actively scraping.
 * In subsequent milestones (Milestone 6+), this adapter will connect to an authorized
 * merchant API or external search gateway.
 */
interface ExternalSearchAdapter {
    /**
     * Searches external verified commercial sources for candidate products.
     *
     * @param query Search query derived from visual labels or user description.
     * @param category Optional category filter.
     * @return List of verified candidate products.
     */
    suspend fun searchExternal(query: String, category: String? = null): List<ProductItem>
}
