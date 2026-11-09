package com.pvca.app.data.repository

import android.content.Context
import com.pvca.app.data.model.ProductItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loads and caches product catalogue entries bundled in the Android assets.
 */
class LocalAssetCatalogueRepository(
    private var context: Context? = null,
    private val rawJsonString: String? = null
) : ProductCatalogueRepository {

    private var cachedProducts: List<ProductItem>? = null

    /**
     * Attaches an Android context to access assets if not provided at construction time.
     */
    fun attachContext(context: Context) {
        if (this.context == null) {
            this.context = context.applicationContext
        }
    }

    override suspend fun getAllProducts(): List<ProductItem> = withContext(Dispatchers.IO) {
        cachedProducts?.let { return@withContext it }

        val json = rawJsonString ?: loadJsonFromAssets() ?: return@withContext emptyList()
        val products = parseProductsJson(json)
        cachedProducts = products
        products
    }

    override suspend fun getProductById(id: String): ProductItem? = withContext(Dispatchers.IO) {
        getAllProducts().firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    override suspend fun searchProducts(query: String): List<ProductItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return@withContext emptyList()

        val tokens = cleanQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }

        getAllProducts().filter { product ->
            val productText = "${product.title} ${product.brand} ${product.category} ${product.subcategory} ${product.keywords.joinToString(" ")}".lowercase()
            tokens.any { token -> productText.contains(token) }
        }
    }

    private fun loadJsonFromAssets(): String? {
        val ctx = context ?: return null
        return try {
            ctx.assets.open("demo_catalogue.json").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun parseProductsJson(jsonString: String): List<ProductItem> {
            val list = mutableListOf<ProductItem>()
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val id = obj.optString("id").trim()
                    val title = obj.optString("title").trim()
                    if (id.isEmpty() || title.isEmpty()) continue

                    val keywordsArray = obj.optJSONArray("keywords")
                    val keywords = mutableListOf<String>()
                    if (keywordsArray != null) {
                        for (k in 0 until keywordsArray.length()) {
                            val kw = keywordsArray.optString(k).trim()
                            if (kw.isNotEmpty()) keywords.add(kw)
                        }
                    }

                    list.add(
                        ProductItem(
                            id = id,
                            title = title,
                            brand = obj.optString("brand", "Generic").trim(),
                            category = obj.optString("category", "General").trim(),
                            subcategory = obj.optString("subcategory", "").trim(),
                            description = obj.optString("description", "").trim(),
                            keywords = keywords,
                            color = obj.optString("color", "").trim(),
                            price = obj.optDouble("price", 0.0),
                            currency = obj.optString("currency", "USD").trim(),
                            merchant = obj.optString("merchant", "Demo Merchant").trim(),
                            productUrl = obj.optString("productUrl", "").trim(),
                            imageUrl = obj.optString("imageUrl", "").trim(),
                            verificationStatus = obj.optString("verificationStatus", "demo_data").trim(),
                            provenance = obj.optString("provenance", "Internal Demo Catalogue").trim(),
                            lastVerified = obj.optString("lastVerified", "").trim()
                        )
                    )
                }
            } catch (e: Exception) {
                // Malformed entries gracefully ignored
            }
            return list
        }
    }
}
