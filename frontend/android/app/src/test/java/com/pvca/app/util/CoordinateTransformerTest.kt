package com.pvca.app.util

import android.graphics.Rect
import com.pvca.app.ui.util.CoordinateTransformer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateTransformerTest {

    private fun testRect(left: Int, top: Int, right: Int, bottom: Int): Rect =
        Rect().apply {
            this.left = left
            this.top = top
            this.right = right
            this.bottom = bottom
        }

    @Test
    fun squareImageInLandscapeContainer_isCenteredHorizontally() {
        // Container: 1000 x 500, Image: 500 x 500
        val transformer = CoordinateTransformer(
            imageWidth = 500,
            imageHeight = 500,
            containerWidth = 1000f,
            containerHeight = 500f
        )

        // Scale should be 1.0 because height fits exactly
        assertEquals(1.0f, transformer.scale, 0.001f)
        // Scaled width = 500, container = 1000, so offsetX = (1000 - 500) / 2 = 250
        assertEquals(250f, transformer.offsetX, 0.001f)
        assertEquals(0f, transformer.offsetY, 0.001f)

        // Test box at center of image [100, 100, 400, 400]
        val screenRect = transformer.toScreenRect(testRect(100, 100, 400, 400))
        assertEquals(350f, screenRect.left, 0.001f)
        assertEquals(100f, screenRect.top, 0.001f)
        assertEquals(650f, screenRect.right, 0.001f)
        assertEquals(400f, screenRect.bottom, 0.001f)

        // Center calculation
        val (centerX, centerY) = transformer.toScreenCenter(testRect(100, 100, 400, 400))
        assertEquals(500f, centerX, 0.001f)
        assertEquals(250f, centerY, 0.001f)
    }

    @Test
    fun portraitImageInLandscapeContainer_pillarboxesCorrectly() {
        // Container: 800 x 600, Image: 400 x 800 (ratio 1:2)
        val transformer = CoordinateTransformer(
            imageWidth = 400,
            imageHeight = 800,
            containerWidth = 800f,
            containerHeight = 600f
        )

        // Scale is limited by height: 600 / 800 = 0.75
        assertEquals(0.75f, transformer.scale, 0.001f)
        // Scaled image width = 400 * 0.75 = 300
        // Offset X = (800 - 300) / 2 = 250
        assertEquals(250f, transformer.offsetX, 0.001f)
        assertEquals(0f, transformer.offsetY, 0.001f)

        // Raw box [0, 0, 400, 800] (full image)
        val screenRect = transformer.toScreenRect(testRect(0, 0, 400, 800))
        assertEquals(250f, screenRect.left, 0.001f)
        assertEquals(0f, screenRect.top, 0.001f)
        assertEquals(550f, screenRect.right, 0.001f)
        assertEquals(600f, screenRect.bottom, 0.001f)
    }

    @Test
    fun landscapeImageInSquareContainer_letterboxesCorrectly() {
        // Container: 500 x 500, Image: 1000 x 500 (ratio 2:1)
        val transformer = CoordinateTransformer(
            imageWidth = 1000,
            imageHeight = 500,
            containerWidth = 500f,
            containerHeight = 500f
        )

        // Scale limited by width: 500 / 1000 = 0.5
        assertEquals(0.5f, transformer.scale, 0.001f)
        assertEquals(0f, transformer.offsetX, 0.001f)
        // Scaled height = 500 * 0.5 = 250, offsetY = (500 - 250) / 2 = 125
        assertEquals(125f, transformer.offsetY, 0.001f)
    }

    @Test
    fun toNormalizedRect_clampsToZeroOne() {
        val transformer = CoordinateTransformer(
            imageWidth = 1000,
            imageHeight = 800,
            containerWidth = 500f,
            containerHeight = 400f
        )

        val normalized = transformer.toNormalizedRect(testRect(250, 200, 750, 600))
        assertEquals(0.25f, normalized.left, 0.001f)
        assertEquals(0.25f, normalized.top, 0.001f)
        assertEquals(0.75f, normalized.right, 0.001f)
        assertEquals(0.75f, normalized.bottom, 0.001f)
    }

    @Test
    fun zeroOrNegativeDimensions_doesNotCrash() {
        val transformer = CoordinateTransformer(
            imageWidth = 0,
            imageHeight = 0,
            containerWidth = 0f,
            containerHeight = 0f
        )
        val (cx, cy) = transformer.toScreenCenter(testRect(0, 0, 10, 10))
        assertTrue(cx >= 0f)
        assertTrue(cy >= 0f)
    }
}
