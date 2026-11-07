package com.pvca.app.model

import android.graphics.Rect
import android.graphics.RectF
import com.pvca.app.data.model.DetectedObjectItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetectedObjectItemTest {

    @Test
    fun confidencePercentage_roundsCorrectly() {
        val item = DetectedObjectItem(
            id = 1,
            rawBoundingBox = Rect(10, 20, 110, 220),
            normalizedBoundingBox = RectF(0.1f, 0.1f, 0.5f, 0.5f),
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
            rawBoundingBox = Rect(0, 0, 50, 50),
            normalizedBoundingBox = RectF(0f, 0f, 0.2f, 0.2f),
            category = "General Object",
            confidence = null
        )

        assertNull(item.confidencePercentage)
    }

    @Test
    fun trackingId_isPreserved() {
        val item = DetectedObjectItem(
            id = 3,
            rawBoundingBox = Rect(5, 5, 25, 25),
            normalizedBoundingBox = RectF(0.05f, 0.05f, 0.25f, 0.25f),
            category = "Home good",
            trackingId = 42
        )

        assertEquals(42, item.trackingId)
    }
}
