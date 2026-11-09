package com.pvca.app.data.model

import android.graphics.Rect
import android.graphics.RectF

/**
 * Represents an individual object localized by the Google ML Kit Object Detector.
 *
 * @param id Unique 1-based numeric identifier for this detection in the image.
 * @param rawBoundingBox Pixel bounding-box coordinates in source image space.
 * @param normalizedBoundingBox Normalized bounding box in [0.0..1.0] range relative to image width/height.
 * @param category Broad category classification returned by the detector (e.g., "Fashion good", "Home good", "Food", "General Object").
 * @param confidence Confidence score for the broad category classification (if provided by ML Kit).
 * @param trackingId Optional tracking identifier assigned by detector.
 */
data class DetectedObjectItem(
    val id: Int,
    val rawBoundingBox: Rect,
    val normalizedBoundingBox: RectF,
    val category: String,
    val confidence: Float? = null,
    val trackingId: Int? = null
) {
    /**
     * Integer percentage representation (0-100%) if confidence is available.
     */
    val confidencePercentage: Int?
        get() = confidence?.let { (it * 100f).toInt().coerceIn(0, 100) }

    /**
     * Bounding box width in pixels.
     */
    val widthPx: Int
        get() = rawBoundingBox.right - rawBoundingBox.left

    /**
     * Bounding box height in pixels.
     */
    val heightPx: Int
        get() = rawBoundingBox.bottom - rawBoundingBox.top
}
