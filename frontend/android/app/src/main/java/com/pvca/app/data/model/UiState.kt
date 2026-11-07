package com.pvca.app.data.model

/**
 * Represents the complete UI state for the PVCA visual commerce screen.
 */
sealed interface UiState {
    /**
     * Initial state: No image selected yet.
     */
    data object Idle : UiState

    /**
     * Processing state: An image is being analyzed by ML Kit (both object detection and labeling).
     */
    data object Loading : UiState

    /**
     * Success state: Analysis completed successfully.
     *
     * @param imageLabels Preserved high-level image labels with confidence values.
     * @param detectedObjects Localized objects with bounding boxes and broad categories.
     * @param imageWidth Upright pixel width of the source image.
     * @param imageHeight Upright pixel height of the source image.
     * @param selectedObjectId The ID of the currently selected object (or null if none selected).
     */
    data class Success(
        val imageLabels: List<LabelItem>,
        val detectedObjects: List<DetectedObjectItem>,
        val imageWidth: Int,
        val imageHeight: Int,
        val selectedObjectId: Int? = null
    ) : UiState {
        /**
         * The currently selected object item, if one is active.
         */
        val selectedObject: DetectedObjectItem?
            get() = detectedObjects.firstOrNull { it.id == selectedObjectId }
    }

    /**
     * Empty state: Neither labels nor objects were detected.
     */
    data object Empty : UiState

    /**
     * Error state: Image decoding, processing, or file reading failed.
     */
    data class Error(val message: String) : UiState
}
