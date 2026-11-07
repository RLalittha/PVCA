package com.pvca.app.data.model

/**
 * Represents a single label returned from image analysis.
 *
 * @param text The descriptive name of the detected visual concept (e.g., "Shoe", "Watch").
 * @param confidence The model's confidence value, normalized between 0.0 and 1.0.
 * @param index The rank order index among detected labels.
 */
data class LabelItem(
    val text: String,
    val confidence: Float,
    val index: Int = 0
) {
    /**
     * Confidence represented as an integer percentage from 0 to 100.
     */
    val confidencePercentage: Int
        get() = (confidence * 100f).toInt().coerceIn(0, 100)
}
