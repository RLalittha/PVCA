package com.pvca.app.data.model

import java.util.Locale

/**
 * Represents a verified or demonstration product entry in the PVCA catalogue.
 *
 * @param id Unique identifier (e.g., "PVCA-SHOE-001").
 * @param title Full display title of the product.
 * @param brand Brand name, when known.
 * @param category Broad commercial category (e.g., "Footwear", "Consumer Electronics").
 * @param subcategory Specific product subcategory (e.g., "Running Shoes", "Remote Controls").
 * @param description Short narrative description.
 * @param keywords List of search keywords and visual synonyms.
 * @param color Colorway description.
 * @param price Demonstrated price.
 * @param currency Currency code (e.g., "USD").
 * @param merchant Seller or merchant name.
 * @param productUrl Permitted product destination URL.
 * @param imageUrl Product representative image URL.
 * @param verificationStatus Verification flag (e.g. "demo_verified").
 * @param provenance Source of data (e.g. "Internal PVCA Verified Demo Catalogue").
 * @param lastVerified Date of verification in YYYY-MM-DD.
 */
data class ProductItem(
    val id: String,
    val title: String,
    val brand: String,
    val category: String,
    val subcategory: String,
    val description: String,
    val keywords: List<String>,
    val color: String,
    val price: Double,
    val currency: String,
    val merchant: String,
    val productUrl: String,
    val imageUrl: String,
    val verificationStatus: String,
    val provenance: String,
    val lastVerified: String
) {
    /**
     * Formats price and currency for clean display (e.g., "$89.99 USD").
     */
    val formattedPrice: String
        get() = String.format(Locale.US, "$%.2f %s", price, currency)

    val isDemoData: Boolean
        get() = verificationStatus.contains("demo", ignoreCase = true)

    /**
     * Determines whether the product URL points to a legitimate, verified live destination
     * rather than an internal demonstration endpoint.
     */
    val hasVerifiedLiveUrl: Boolean
        get() = !isDemoData &&
                productUrl.isNotBlank() &&
                (productUrl.startsWith("http://") || productUrl.startsWith("https://")) &&
                !productUrl.contains("demo.", ignoreCase = true) &&
                !productUrl.contains(".commerce", ignoreCase = true)
}
