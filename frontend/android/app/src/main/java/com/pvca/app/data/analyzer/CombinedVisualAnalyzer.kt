package com.pvca.app.data.analyzer

import android.content.Context
import android.net.Uri
import com.pvca.app.data.model.AnalysisOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Contract for full PVCA visual analysis (both image-level labeling and spatial object detection).
 */
interface VisualAnalyzer {
    suspend fun analyze(context: Context, uri: Uri): Result<AnalysisOutput>
}

/**
 * Coordinates concurrent on-device image analysis using Google ML Kit:
 * 1. Image Labeling (high-level concept classification preserved from Day 1)
 * 2. Object Detection & Tracking (spatial bounding boxes & localization for Day 2)
 */
class CombinedVisualAnalyzer(
    private val imageAnalyzer: ImageAnalyzer = MlKitImageAnalyzer(),
    private val objectDetector: MlKitObjectDetector = MlKitObjectDetector()
) : VisualAnalyzer {

    override suspend fun analyze(context: Context, uri: Uri): Result<AnalysisOutput> =
        withContext(Dispatchers.IO) {
            try {
                // Run both detectors concurrently in parallel coroutines
                val labelsDeferred = async { imageAnalyzer.analyze(context, uri) }
                val objectsDeferred = async { objectDetector.detect(context, uri) }

                val labelsResult = labelsDeferred.await()
                val objectsResult = objectsDeferred.await()

                // If both failed, propagate failure
                if (labelsResult.isFailure && objectsResult.isFailure) {
                    val error = objectsResult.exceptionOrNull() ?: labelsResult.exceptionOrNull()
                    return@withContext Result.failure(
                        error ?: Exception("Failed to perform visual analysis on the image.")
                    )
                }

                val imageLabels = labelsResult.getOrDefault(emptyList())
                val objectOutput = objectsResult.getOrNull()

                val detectedObjects = objectOutput?.objects ?: emptyList()
                val imageWidth = objectOutput?.imageWidth ?: 1
                val imageHeight = objectOutput?.imageHeight ?: 1

                Result.success(
                    AnalysisOutput(
                        imageLabels = imageLabels,
                        detectedObjects = detectedObjects,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
