package com.pvca.app.matching

import android.graphics.Rect
import android.graphics.RectF
import com.pvca.app.data.matching.DefaultProductMatchingService
import com.pvca.app.data.model.DetectedObjectItem
import com.pvca.app.data.model.LabelItem
import com.pvca.app.data.model.MatchConfidenceTier
import com.pvca.app.data.model.MatchResult
import com.pvca.app.data.repository.LocalAssetCatalogueRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductMatchingEngineTest {

    private val sampleJson = """
        [
          {
            "id": "PVCA-SHOE-001",
            "title": "AeroStride Velocity Running Shoe",
            "brand": "AeroStride",
            "category": "Footwear",
            "subcategory": "Running Shoes",
            "description": "Breathable running shoe.",
            "keywords": ["shoe", "running shoe", "sneaker", "footwear"],
            "color": "Blue",
            "price": 89.99,
            "currency": "USD",
            "merchant": "Apex Store",
            "productUrl": "https://demo.pvca.app/1",
            "imageUrl": "https://images.unsplash.com/1",
            "verificationStatus": "demo_verified",
            "provenance": "Internal",
            "lastVerified": "2026-11-01"
          },
          {
            "id": "PVCA-ELEC-001",
            "title": "AeroBreeze Ceiling Fan Remote Control",
            "brand": "AeroBreeze",
            "category": "Consumer Electronics",
            "subcategory": "Remote Controls",
            "description": "Ceiling fan remote controller.",
            "keywords": ["remote control", "fan remote", "controller", "electronics", "home good"],
            "color": "White",
            "price": 24.99,
            "currency": "USD",
            "merchant": "Breeze Store",
            "productUrl": "https://demo.pvca.app/2",
            "imageUrl": "https://images.unsplash.com/2",
            "verificationStatus": "demo_verified",
            "provenance": "Internal",
            "lastVerified": "2026-11-01"
          },
          {
            "id": "PVCA-BAG-001",
            "title": "Moderna Leather Work Tote",
            "brand": "Moderna",
            "category": "Bags & Luggage",
            "subcategory": "Tote Bags",
            "description": "Pebbled leather handbag tote.",
            "keywords": ["handbag", "bag", "tote", "leather bag", "purse"],
            "color": "Brown",
            "price": 165.00,
            "currency": "USD",
            "merchant": "Moderna Store",
            "productUrl": "https://demo.pvca.app/3",
            "imageUrl": "https://images.unsplash.com/3",
            "verificationStatus": "demo_verified",
            "provenance": "Internal",
            "lastVerified": "2026-11-01"
          }
        ]
    """.trimIndent()

    private val repo = LocalAssetCatalogueRepository(rawJsonString = sampleJson)
    private val matchingService = DefaultProductMatchingService(repo)

    @Test
    fun cropLabelsMatchingRemote_returnsRemoteWithHighConfidence() = runBlocking {
        val detectedRemote = DetectedObjectItem(
            id = 1,
            rawBoundingBox = Rect(100, 100, 300, 500),
            normalizedBoundingBox = RectF(0.2f, 0.2f, 0.6f, 0.8f),
            category = "Home good"
        )
        val cropLabels = listOf(
            LabelItem(text = "Remote control", confidence = 0.92f, index = 0),
            LabelItem(text = "Electronics", confidence = 0.85f, index = 1)
        )
        val sceneLabels = listOf(
            LabelItem(text = "Table", confidence = 0.70f, index = 0)
        )

        val result = matchingService.findBestMatch(
            selectedObject = detectedRemote,
            cropLabels = cropLabels,
            sceneLabels = sceneLabels
        )

        assertTrue(result is MatchResult.Matched)
        val matched = result as MatchResult.Matched
        assertEquals("PVCA-ELEC-001", matched.candidate.product.id)
        assertEquals("AeroBreeze Ceiling Fan Remote Control", matched.candidate.product.title)
        assertEquals(MatchConfidenceTier.HIGH_CONFIDENCE, matched.candidate.confidenceTier)
        assertTrue(matched.candidate.evidenceExplanation.contains("Remote control", ignoreCase = true))
    }

    @Test
    fun userAssistedQuery_boostsAndRefinesCandidate() = runBlocking {
        val detectedBag = DetectedObjectItem(
            id = 2,
            rawBoundingBox = Rect(50, 50, 200, 200),
            normalizedBoundingBox = RectF(0.1f, 0.1f, 0.4f, 0.4f),
            category = "Fashion good"
        )

        val result = matchingService.findBestMatch(
            selectedObject = detectedBag,
            cropLabels = emptyList(), // no specific crop labels
            sceneLabels = emptyList(),
            userQuery = "leather handbag tote"
        )

        assertTrue(result is MatchResult.Matched)
        val matched = result as MatchResult.Matched
        assertEquals("PVCA-BAG-001", matched.candidate.product.id)
        assertTrue(matched.candidate.isUserRefined)
        assertTrue(matched.candidate.evidenceExplanation.contains("leather handbag tote", ignoreCase = true))
    }

    @Test
    fun unrelatedCategoryAndNoCrop_returnsNoMatch() = runBlocking {
        val detectedPlant = DetectedObjectItem(
            id = 3,
            rawBoundingBox = Rect(10, 10, 50, 50),
            normalizedBoundingBox = RectF(0f, 0f, 0.1f, 0.1f),
            category = "Plant"
        )

        val result = matchingService.findBestMatch(
            selectedObject = detectedPlant,
            cropLabels = emptyList(),
            sceneLabels = emptyList()
        )

        assertTrue(result is MatchResult.NoMatch)
        val noMatch = result as MatchResult.NoMatch
        assertTrue(noMatch.reason.contains("No reliable match", ignoreCase = true))
    }

    @Test
    fun matchCandidate_preservesIdentifiedAsField() = runBlocking {
        val candidate = com.pvca.app.data.model.MatchCandidate(
            product = repo.getAllProducts().first(),
            score = 0.95f,
            confidenceTier = MatchConfidenceTier.HIGH_CONFIDENCE,
            evidenceExplanation = "Matched via Gemini Vision",
            isUserRefined = false,
            identifiedAs = "ceiling fan remote control"
        )
        assertEquals("ceiling fan remote control", candidate.identifiedAs)
        assertEquals(false, candidate.isUserRefined)
    }

    @Test
    fun manualUserRefinement_alwaysMarksUserRefined() = runBlocking {
        val detectedRemote = DetectedObjectItem(
            id = 4,
            rawBoundingBox = Rect(10, 10, 100, 100),
            normalizedBoundingBox = RectF(0.1f, 0.1f, 0.4f, 0.4f),
            category = "General Object"
        )

        val result = matchingService.findBestMatch(
            selectedObject = detectedRemote,
            cropLabels = emptyList(),
            sceneLabels = emptyList(),
            userQuery = "ceiling fan remote controller",
            cropImageBase64 = null
        )

        assertTrue(result is MatchResult.Matched)
        val matched = result as MatchResult.Matched
        assertEquals("PVCA-ELEC-001", matched.candidate.product.id)
        assertTrue(matched.candidate.isUserRefined)
    }
}
