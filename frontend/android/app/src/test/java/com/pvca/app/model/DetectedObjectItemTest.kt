package com.pvca.app.model

import android.graphics.Rect
import android.graphics.RectF
import com.pvca.app.data.model.DetectedObjectItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetectedObjectItemTest {

    private fun testRect(left: Int, top: Int, right: Int, bottom: Int): Rect =
        Rect().apply {
            this.left = left
            this.top = top
            this.right = right
            this.bottom = bottom
        }

    @Test
    fun confidencePercentage_roundsCorrectly() {
        val item = DetectedObjectItem(
            id = 1,
            rawBoundingBox = testRect(10, 20, 110, 220),
            normalizedBoundingBox = RectF().apply { left = 0.1f; top = 0.1f; right = 0.5f; bottom = 0.5f },
            category = "Fashion good",
            confidence = 0.887f
        )

        assertEquals(88, item.confidencePercentage)
        assertEquals(100, item.widthPx)
        assertEquals(200, item.heightPx)
    }

    @Test
    fun nullConfidence_returnsNullPercentage() {
        val item = DetectedObjectItem(
            id = 2,
            rawBoundingBox = testRect(0, 0, 50, 50),
            normalizedBoundingBox = RectF().apply { left = 0f; top = 0f; right = 0.2f; bottom = 0.2f },
            category = "General Object",
            confidence = null
        )

        assertNull(item.confidencePercentage)
    }

    @Test
    fun trackingId_isPreserved() {
        val item = DetectedObjectItem(
            id = 3,
            rawBoundingBox = testRect(5, 5, 25, 25),
            normalizedBoundingBox = RectF().apply { left = 0.05f; top = 0.05f; right = 0.25f; bottom = 0.25f },
            category = "Home good",
            trackingId = 42
        )

        assertEquals(42, item.trackingId)
    }
}
