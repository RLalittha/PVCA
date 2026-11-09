package com.pvca.app.data.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.pvca.app.data.model.LabelItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Extracts and analyzes a visual sub-crop corresponding specifically to a detected object's bounding box.
 *
 * This prevents the critical flaw of assigning full-scene image labels to a specific object.
 */
class ObjectCropAnalyzer(
    private val options: ImageLabelerOptions = ImageLabelerOptions.DEFAULT_OPTIONS
) {

    /**
     * Extracts a crop of [rawBoundingBox] from [imageUri] and runs ML Kit Image Labeling
     * on the cropped sub-image, returning object-specific visual labels.
     */
    suspend fun analyzeCrop(
        context: Context,
        imageUri: Uri,
        rawBoundingBox: Rect
    ): List<LabelItem> = withContext(Dispatchers.IO) {
        val croppedBitmap = extractCroppedBitmap(context, imageUri, rawBoundingBox) ?: return@withContext emptyList()
        val labeler = ImageLabeling.getClient(options)
        try {
            val inputImage = InputImage.fromBitmap(croppedBitmap, 0)
            val mlKitLabels = labeler.process(inputImage).await()

            mlKitLabels
                .mapIndexed { index, label ->
                    LabelItem(
                        text = label.text,
                        confidence = label.confidence,
                        index = index
                    )
                }
                .sortedByDescending { it.confidence }
        } catch (e: Exception) {
            emptyList()
        } finally {
            labeler.close()
            if (!croppedBitmap.isRecycled) {
                croppedBitmap.recycle()
            }
        }
    }

    private fun extractCroppedBitmap(context: Context, uri: Uri, boundingBox: Rect): Bitmap? {
        return try {
            val resolver = context.contentResolver

            // 1. Determine image orientation
            var rotationDegrees = 0
            resolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                rotationDegrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }

            // 2. Decode full image with sensible bounds to prevent OOM
            val decodeOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var fullBitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Rotate bitmap if required by EXIF
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(fullBitmap, 0, 0, fullBitmap.width, fullBitmap.height, matrix, true)
                if (rotated != fullBitmap) {
                    fullBitmap.recycle()
                    fullBitmap = rotated
                }
            }

            // 4. Safely clamp bounding box to bitmap dimensions
            val left = boundingBox.left.coerceIn(0, fullBitmap.width - 1)
            val top = boundingBox.top.coerceIn(0, fullBitmap.height - 1)
            val right = boundingBox.right.coerceIn(left + 1, fullBitmap.width)
            val bottom = boundingBox.bottom.coerceIn(top + 1, fullBitmap.height)

            val cropWidth = right - left
            val cropHeight = bottom - top

            if (cropWidth <= 0 || cropHeight <= 0) {
                fullBitmap.recycle()
                return null
            }

            val cropped = Bitmap.createBitmap(fullBitmap, left, top, cropWidth, cropHeight)
            fullBitmap.recycle()
            cropped
        } catch (e: Exception) {
            null
        }
    }
}
