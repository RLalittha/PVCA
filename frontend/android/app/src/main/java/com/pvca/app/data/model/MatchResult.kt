package com.pvca.app.data.model

/**
 * Confidence categorization for catalogue matching.
 */
enum class MatchConfidenceTier {
    HIGH_CONFIDENCE,   // Strong evidence from object-specific crop labels or exact keyword match
    POSSIBLE_MATCH,    // Plausible candidate based on category and general scene context
    NO_RELIABLE_MATCH  // Insufficient evidence; prompts user refinement
}

/**
 * A ranked product candidate with evidence explanation.
 */
data class MatchCandidate(
    val product: ProductItem,
    val score: Float,
    val confidenceTier: MatchConfidenceTier,
    val evidenceExplanation: String,
    val isUserRefined: Boolean = false
)

/**
 * Sealed outcome of a product matching operation.
 */
sealed interface MatchResult {
    /**
     * Found a credible matching candidate.
     */
    data class Matched(val candidate: MatchCandidate) : MatchResult

    /**
     * No candidate met the minimum reliability threshold.
     */
    data class NoMatch(
        val reason: String,
        val suggestedCategory: String? = null
    ) : MatchResult
}
