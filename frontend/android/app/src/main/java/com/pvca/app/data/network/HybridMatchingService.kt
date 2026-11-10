package com.pvca.app.data.network

import com.pvca.app.data.matching.DefaultProductMatchingService
import com.pvca.app.data.matching.ProductMatchingService
import com.pvca.app.data.model.DetectedObjectItem
import com.pvca.app.data.model.LabelItem
import com.pvca.app.data.model.MatchResult
import com.pvca.app.data.repository.ProductCatalogueRepository

/**
 * Hybrid matching service that tries the backend API first,
 * then falls back to the local on-device matching engine if
 * the backend is unreachable or returns an error.
 *
 * This ensures the app works offline / without the backend running.
 */
class HybridMatchingService(
    catalogueRepository: ProductCatalogueRepository
) : ProductMatchingService {

    private val backendService = BackendMatchingService()
    private val localService = DefaultProductMatchingService(catalogueRepository)

    override suspend fun findBestMatch(
        selectedObject: DetectedObjectItem,
        cropLabels: List<LabelItem>,
        sceneLabels: List<LabelItem>,
        userQuery: String?
    ): MatchResult {
        return try {
            val backendResult = backendService.findBestMatch(
                selectedObject, cropLabels, sceneLabels, userQuery
            )
            backendResult
        } catch (e: Exception) {
            // Backend unreachable or error — fall back to local matching
            val localResult = localService.findBestMatch(
                selectedObject, cropLabels, sceneLabels, userQuery
            )
            // Append fallback note to the evidence explanation
            when (localResult) {
                is MatchResult.Matched -> {
                    val updatedCandidate = localResult.candidate.copy(
                        evidenceExplanation = "${localResult.candidate.evidenceExplanation} [local fallback]"
                    )
                    MatchResult.Matched(updatedCandidate)
                }
                is MatchResult.NoMatch -> {
                    MatchResult.NoMatch(
                        reason = "${localResult.reason} [local fallback]",
                        suggestedCategory = localResult.suggestedCategory
                    )
                }
            }
        }
    }
}
