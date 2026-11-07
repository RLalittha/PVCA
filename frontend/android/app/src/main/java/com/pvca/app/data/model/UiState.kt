package com.pvca.app.data.model

/**
 * Represents the complete UI state for the PVCA analysis screen.
 */
sealed interface UiState {
    /**
     * Initial state: No image selected yet.
     */
    data object Idle : UiState

    /**
     * Processing state: An image is being analyzed by ML Kit.
     */
    data object Loading : UiState

    /**
     * Success state: One or more visual labels were successfully detected.
     */
    data class Success(val labels: List<LabelItem>) : UiState

    /**
     * Empty state: The analyzer ran successfully, but found no confident labels.
     */
    data object Empty : UiState

    /**
     * Error state: Image analysis failed or image was unreadable.
     */
    data class Error(val message: String) : UiState
}
