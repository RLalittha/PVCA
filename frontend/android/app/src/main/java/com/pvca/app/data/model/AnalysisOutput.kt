package com.pvca.app.data.model

/**
 * Combined output produced by PVCA's visual analysis engines.
 *
 * @param imageLabels High-level visual concepts identified by Google ML Kit Image Labeling.
 * @param detectedObjects Localized objects and bounding boxes identified by Google ML Kit Object Detection.
 * @param imageWidth Source image width in upright pixels (accounting for EXIF orientation).
 * @param imageHeight Source image height in upright pixels (accounting for EXIF orientation).
 */
data class AnalysisOutput(
    val imageLabels: List<LabelItem>,
    val detectedObjects: List<DetectedObjectItem>,
    val imageWidth: Int,
    val imageHeight: Int
)
