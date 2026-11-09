package com.pvca.app.ui.util

import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.min

/**
 * Maps coordinates between original image space and displayed screen space
 * when an image is rendered inside a viewport with ContentScale.Fit (letterboxed/pillarboxed).
 *
 * @param imageWidth Source image width in upright pixels.
 * @param imageHeight Source image height in upright pixels.
 * @param containerWidth Display container viewport width in pixels.
 * @param containerHeight Display container viewport height in pixels.
 */
class CoordinateTransformer(
    val imageWidth: Int,
    val imageHeight: Int,
    val containerWidth: Float,
    val containerHeight: Float
) {
    val scale: Float
    val offsetX: Float
    val offsetY: Float
    val scaledImageWidth: Float
    val scaledImageHeight: Float

    init {
        if (imageWidth <= 0 || imageHeight <= 0 || containerWidth <= 0f || containerHeight <= 0f) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            scaledImageWidth = containerWidth
            scaledImageHeight = containerHeight
        } else {
            scale = min(containerWidth / imageWidth.toFloat(), containerHeight / imageHeight.toFloat())
            scaledImageWidth = imageWidth.toFloat() * scale
            scaledImageHeight = imageHeight.toFloat() * scale
            offsetX = (containerWidth - scaledImageWidth) / 2f
            offsetY = (containerHeight - scaledImageHeight) / 2f
        }
    }

    /**
     * Converts a raw bounding box in source image pixels to screen coordinates.
     */
    fun toScreenRect(rawRect: Rect): RectF {
        val rect = RectF()
        rect.left = offsetX + rawRect.left.toFloat() * scale
        rect.top = offsetY + rawRect.top.toFloat() * scale
        rect.right = offsetX + rawRect.right.toFloat() * scale
        rect.bottom = offsetY + rawRect.bottom.toFloat() * scale
        return rect
    }

    /**
     * Calculates the exact center of a raw bounding box in screen coordinates.
     * Used for positioning interactive blue dots over detected objects.
     */
    fun toScreenCenter(rawRect: Rect): Pair<Float, Float> {
        val rawCenterX = (rawRect.left.toFloat() + rawRect.right.toFloat()) / 2f
        val rawCenterY = (rawRect.top.toFloat() + rawRect.bottom.toFloat()) / 2f
        val screenCenterX = offsetX + rawCenterX * scale
        val screenCenterY = offsetY + rawCenterY * scale
        return Pair(screenCenterX, screenCenterY)
    }

    /**
     * Normalizes a pixel bounding box into [0.0..1.0] coordinates relative to image bounds.
     */
    fun toNormalizedRect(rawRect: Rect): RectF {
        val rect = RectF()
        if (imageWidth > 0 && imageHeight > 0) {
            rect.left = (rawRect.left.toFloat() / imageWidth.toFloat()).coerceIn(0f, 1f)
            rect.top = (rawRect.top.toFloat() / imageHeight.toFloat()).coerceIn(0f, 1f)
            rect.right = (rawRect.right.toFloat() / imageWidth.toFloat()).coerceIn(0f, 1f)
            rect.bottom = (rawRect.bottom.toFloat() / imageHeight.toFloat()).coerceIn(0f, 1f)
        }
        return rect
    }
}
