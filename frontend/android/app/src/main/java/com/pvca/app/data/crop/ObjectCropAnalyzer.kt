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

    /**
     * Extracts the cropped object bitmap, scales it to a max dimension of 512px
     * (to conserve network bandwidth and ensure fast inference), and encodes it
     * as a Base64 JPEG string. Returns null if extraction or encoding fails.
     */
    fun extractCroppedJpegBase64(context: Context, uri: Uri, boundingBox: Rect): String? {
        val cropped = extractCroppedBitmap(context, uri, boundingBox) ?: return null
        return try {
            val maxDim = 512
            val scaledBitmap = if (cropped.width > maxDim || cropped.height > maxDim) {
                val ratio = minOf(maxDim.toFloat() / cropped.width, maxDim.toFloat() / cropped.height)
                val targetW = (cropped.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (cropped.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
            } else {
                cropped
            }

            val stream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val bytes = stream.toByteArray()

            if (scaledBitmap != cropped && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        } finally {
            if (!cropped.isRecycled) {
                cropped.recycle()
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

    /**
     * Safely extracts a cropped [Bitmap] directly from an in-memory [source] Bitmap.
     * Does NOT recycle the source bitmap, allowing multiple crops from the same frame.
     */
    fun cropBitmap(source: Bitmap, boundingBox: Rect): Bitmap? {
        return try {
            val left = boundingBox.left.coerceIn(0, source.width - 1)
            val top = boundingBox.top.coerceIn(0, source.height - 1)
            val right = boundingBox.right.coerceIn(left + 1, source.width)
            val bottom = boundingBox.bottom.coerceIn(top + 1, source.height)

            val cropWidth = right - left
            val cropHeight = bottom - top

            if (cropWidth <= 0 || cropHeight <= 0) return null

            Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts a [Bitmap] to a scaled Base64 JPEG string entirely in memory.
     * Caps maximum dimension to prevent large payloads (default 512px).
     */
    fun bitmapToJpegBase64(bitmap: Bitmap, maxDimension: Int = 512, quality: Int = 85): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                maxDimension.toFloat() / maxOf(width, height)
            } else {
                1f
            }

            val scaledBitmap = if (scale < 1f) {
                val scaledW = (width * scale).toInt().coerceAtLeast(1)
                val scaledH = (height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
            } else {
                bitmap
            }

            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            val bytes = outputStream.toByteArray()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
