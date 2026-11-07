package com.pvca.app.model

import com.pvca.app.data.model.LabelItem
import org.junit.Assert.assertEquals
import org.junit.Test

class LabelItemTest {

    @Test
    fun confidencePercentage_roundsCorrectly() {
        val item = LabelItem(text = "Shoe", confidence = 0.942f)
        assertEquals(94, item.confidencePercentage)
    }

    @Test
    fun confidencePercentage_clampsBetweenZeroAndHundred() {
        val lowItem = LabelItem(text = "Unknown", confidence = -0.1f)
        assertEquals(0, lowItem.confidencePercentage)

        val highItem = LabelItem(text = "Definite", confidence = 1.05f)
        assertEquals(100, highItem.confidencePercentage)
    }

    @Test
    fun labelItem_preservesTextAndIndex() {
        val item = LabelItem(text = "Wristwatch", confidence = 0.88f, index = 2)
        assertEquals("Wristwatch", item.text)
        assertEquals(2, item.index)
        assertEquals(0.88f, item.confidence, 0.001f)
    }
}
