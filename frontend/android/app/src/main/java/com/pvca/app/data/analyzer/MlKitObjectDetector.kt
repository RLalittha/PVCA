package com.pvca.app.data.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.pvca.app.data.model.DetectedObjectItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Result returned by [MlKitObjectDetector], containing detected objects and upright image dimensions.
 */
data class ObjectDetectionResult(
    val objects: List<DetectedObjectItem>,
    val imageWidth: Int,
    val imageHeight: Int
)

/**
 * On-device object detector using Google ML Kit Object Detection and Tracking in static image mode.
 *
 * NOTE: Object detection identifies spatial bounding boxes and broad categories (e.g., "Fashion good", "Home good").
 * It does NOT identify specific product brands, models, or merchant listings.
 */
class MlKitObjectDetector(
    private val options: ObjectDetectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()
) {

    suspend fun detect(context: Context, uri: Uri): Result<ObjectDetectionResult> =
        withContext(Dispatchers.IO) {
            val detector = ObjectDetection.getClient(options)
            try {
                // 1. Construct InputImage (respects upright EXIF orientation)
                val inputImage = InputImage.fromFilePath(context, uri)
                processInputImage(detector, inputImage)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                detector.close()
            }
        }

    /**
     * Overloaded method to detect objects directly from an in-memory [Bitmap].
     * Used for real-time and screen-capture frames without disk I/O.
     */
    suspend fun detect(bitmap: Bitmap, rotationDegrees: Int = 0): Result<ObjectDetectionResult> =
        withContext(Dispatchers.IO) {
            val detector = ObjectDetection.getClient(options)
            try {
                val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
                processInputImage(detector, inputImage)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                detector.close()
            }
        }

    private suspend fun processInputImage(
        detector: com.google.mlkit.vision.objects.ObjectDetector,
        inputImage: InputImage
    ): Result<ObjectDetectionResult> {
        val width = inputImage.width.coerceAtLeast(1)
        val height = inputImage.height.coerceAtLeast(1)

        val mlKitObjects = detector.process(inputImage).await()

        val domainObjects = mlKitObjects.mapIndexed { index, obj ->
            val rawBox = obj.boundingBox
            val normalizedBox = RectF(
                (rawBox.left.toFloat() / width.toFloat()).coerceIn(0f, 1f),
                (rawBox.top.toFloat() / height.toFloat()).coerceIn(0f, 1f),
                (rawBox.right.toFloat() / width.toFloat()).coerceIn(0f, 1f),
                (rawBox.bottom.toFloat() / height.toFloat()).coerceIn(0f, 1f)
            )

            val topLabel = obj.labels.maxByOrNull { it.confidence }
            val category = topLabel?.text?.trim()?.ifEmpty { "General Object" } ?: "General Object"
            val confidence = topLabel?.confidence

            DetectedObjectItem(
                id = index + 1,
                rawBoundingBox = rawBox,
                normalizedBoundingBox = normalizedBox,
                category = category,
                confidence = confidence,
                trackingId = obj.trackingId
            )
        }

        return Result.success(
            ObjectDetectionResult(
                objects = domainObjects,
                imageWidth = width,
                imageHeight = height
            )
        )
    }
}
