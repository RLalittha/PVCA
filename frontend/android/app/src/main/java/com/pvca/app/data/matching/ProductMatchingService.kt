package com.pvca.app.data.matching

import com.pvca.app.data.model.DetectedObjectItem
import com.pvca.app.data.model.LabelItem
import com.pvca.app.data.model.MatchCandidate
import com.pvca.app.data.model.MatchConfidenceTier
import com.pvca.app.data.model.MatchResult
import com.pvca.app.data.model.ProductItem
import com.pvca.app.data.repository.ProductCatalogueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Service responsible for ranking local catalogue products against a selected detected object.
 */
interface ProductMatchingService {
    suspend fun findBestMatch(
        selectedObject: DetectedObjectItem,
        cropLabels: List<LabelItem>,
        sceneLabels: List<LabelItem>,
        userQuery: String? = null,
        cropImageBase64: String? = null
    ): MatchResult
}

class DefaultProductMatchingService(
    private val repository: ProductCatalogueRepository
) : ProductMatchingService {

    override suspend fun findBestMatch(
        selectedObject: DetectedObjectItem,
        cropLabels: List<LabelItem>,
        sceneLabels: List<LabelItem>,
        userQuery: String?,
        cropImageBase64: String?
    ): MatchResult = withContext(Dispatchers.Default) {
        val allProducts = repository.getAllProducts()
        if (allProducts.isEmpty()) {
            return@withContext MatchResult.NoMatch("Product catalogue is empty.")
        }

        val hasUserQuery = !userQuery.isNullOrBlank()
        val cleanQuery = userQuery?.trim()?.lowercase() ?: ""
        val queryTokens = if (hasUserQuery) cleanQuery.split("\\s+".toRegex()).filter { it.isNotBlank() } else emptyList()

        val scoredCandidates = allProducts.map { product ->
            val scoreCat = evaluateCategoryAlignment(selectedObject.category, product.category)
            val (scoreCrop, matchedCropKeywords) = evaluateLabelOverlap(cropLabels, product)
            val (scoreScene, _) = evaluateLabelOverlap(sceneLabels, product)
            val scoreUser = if (hasUserQuery) evaluateUserQueryMatch(queryTokens, product) else 0f

            val totalScore = if (hasUserQuery) {
                (0.50f * scoreUser) + (0.30f * scoreCrop) + (0.15f * scoreCat) + (0.05f * scoreScene)
            } else if (cropLabels.isNotEmpty()) {
                (0.60f * scoreCrop) + (0.25f * scoreCat) + (0.15f * scoreScene)
            } else {
                (0.60f * scoreCat) + (0.40f * scoreScene)
            }

            val clampedScore = totalScore.coerceIn(0f, 1f)

            val explanation = buildExplanation(
                hasUserQuery = hasUserQuery,
                userQuery = userQuery,
                scoreUser = scoreUser,
                matchedCropKeywords = matchedCropKeywords,
                objectCategory = selectedObject.category,
                productCategory = product.category
            )

            Triple(product, clampedScore, explanation)
        }

        // Retrieve the strongest candidate
        val topCandidate = scoredCandidates.maxByOrNull { it.second }
            ?: return@withContext MatchResult.NoMatch("No candidates evaluated.")

        val (bestProduct, bestScore, explanation) = topCandidate

        // Evaluate thresholds
        when {
            bestScore >= 0.58f -> {
                MatchResult.Matched(
                    MatchCandidate(
                        product = bestProduct,
                        score = bestScore,
                        confidenceTier = MatchConfidenceTier.HIGH_CONFIDENCE,
                        evidenceExplanation = explanation,
                        isUserRefined = hasUserQuery
                    )
                )
            }
            bestScore >= 0.32f -> {
                MatchResult.Matched(
                    MatchCandidate(
                        product = bestProduct,
                        score = bestScore,
                        confidenceTier = MatchConfidenceTier.POSSIBLE_MATCH,
                        evidenceExplanation = explanation,
                        isUserRefined = hasUserQuery
                    )
                )
            }
            else -> {
                MatchResult.NoMatch(
                    reason = "No reliable match found for this ${selectedObject.category}. You can refine the search below.",
                    suggestedCategory = selectedObject.category
                )
            }
        }
    }

    private fun evaluateCategoryAlignment(mlKitCategory: String, catalogueCategory: String): Float {
        val ml = mlKitCategory.lowercase()
        val cat = catalogueCategory.lowercase()

        return when {
            ml.contains("fashion") -> {
                if (cat.contains("footwear") || cat.contains("bag") || cat.contains("watch") ||
                    cat.contains("eyewear") || cat.contains("apparel")) 1.0f else 0.1f
            }
            ml.contains("home") -> {
                if (cat.contains("drinkware") || cat.contains("home") || cat.contains("electronics")) 1.0f else 0.2f
            }
            ml.contains("food") -> {
                if (cat.contains("drinkware") || cat.contains("food")) 1.0f else 0.1f
            }
            ml.contains("place") || ml.contains("plant") -> 0.3f
            else -> 0.5f // "General Object"
        }
    }

    private fun evaluateLabelOverlap(labels: List<LabelItem>, product: ProductItem): Pair<Float, List<String>> {
        if (labels.isEmpty()) return Pair(0f, emptyList())

        var score = 0f
        val matchedKeywords = mutableListOf<String>()

        val productSearchText = "${product.title} ${product.subcategory} ${product.keywords.joinToString(" ")}".lowercase()

        labels.take(6).forEach { label ->
            val labelText = label.text.lowercase().trim()
            if (labelText.length > 2 && productSearchText.contains(labelText)) {
                score += label.confidence * 0.40f
                matchedKeywords.add(label.text)
            }
        }

        return Pair(min(score, 1.0f), matchedKeywords.distinct())
    }

    private fun evaluateUserQueryMatch(queryTokens: List<String>, product: ProductItem): Float {
        if (queryTokens.isEmpty()) return 0f

        val productText = "${product.title} ${product.brand} ${product.category} ${product.subcategory} ${product.description} ${product.keywords.joinToString(" ")}".lowercase()

        var matchedTokens = 0
        queryTokens.forEach { token ->
            if (productText.contains(token)) {
                matchedTokens++
            }
        }

        return matchedTokens.toFloat() / queryTokens.size.toFloat()
    }

    private fun buildExplanation(
        hasUserQuery: Boolean,
        userQuery: String?,
        scoreUser: Float,
        matchedCropKeywords: List<String>,
        objectCategory: String,
        productCategory: String
    ): String {
        return buildString {
            if (hasUserQuery && scoreUser > 0.4f) {
                append("Matched user description \"$userQuery\". ")
            }
            if (matchedCropKeywords.isNotEmpty()) {
                append("Visual crop matched keywords: ${matchedCropKeywords.take(3).joinToString(", ")}. ")
            } else {
                append("Category aligned with detected $objectCategory ($productCategory). ")
            }
        }.trim()
    }
}
