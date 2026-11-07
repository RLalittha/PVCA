package com.pvca.app.data.analyzer

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.pvca.app.data.model.LabelItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * On-device image analyzer implementation using Google ML Kit Image Labeling.
 *
 * NOTE: Google ML Kit Image Labeling identifies overall visual concepts in the image
 * (image-level labels). It does NOT produce object bounding boxes or exact product coordinates.
 * Real object localization with bounding boxes will be introduced in Milestone 2.
 */
class MlKitImageAnalyzer(
    private val options: ImageLabelerOptions = ImageLabelerOptions.DEFAULT_OPTIONS
) : ImageAnalyzer {

    override suspend fun analyze(context: Context, uri: Uri): Result<List<LabelItem>> =
        withContext(Dispatchers.IO) {
            val labeler = ImageLabeling.getClient(options)
            try {
                // 1. Construct InputImage from the provided content Uri
                val inputImage = InputImage.fromFilePath(context, uri)

                // 2. Process image with ML Kit asynchronously
                val mlKitLabels = labeler.process(inputImage).await()

                // 3. Map to domain model, sorted by confidence descending
                val domainLabels = mlKitLabels
                    .mapIndexed { index, label ->
                        LabelItem(
                            text = label.text,
                            confidence = label.confidence,
                            index = index
                        )
                    }
                    .sortedByDescending { it.confidence }

                Result.success(domainLabels)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                labeler.close()
            }
        }
}
