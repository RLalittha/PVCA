package com.pvca.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pvca.app.data.analyzer.CombinedVisualAnalyzer
import com.pvca.app.data.analyzer.VisualAnalyzer
import com.pvca.app.data.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing PVCA state: selected image, on-device object detections,
 * image labels, and interactive object selection.
 */
class MainViewModel(
    private val visualAnalyzer: VisualAnalyzer = CombinedVisualAnalyzer()
) : ViewModel() {

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Triggered when the user picks an image or cancels the photo picker.
     *
     * @param uri The content Uri returned by Android Photo Picker, or null if cancelled.
     * @param context Android context used to decode the content Uri.
     */
    fun onImageSelected(uri: Uri?, context: Context) {
        if (uri == null) {
            // User cancelled picker: retain existing state without crashing.
            return
        }

        _selectedImageUri.value = uri
        _uiState.value = UiState.Loading

        viewModelScope.launch {
            val result = visualAnalyzer.analyze(context, uri)
            result.fold(
                onSuccess = { output ->
                    if (output.detectedObjects.isEmpty() && output.imageLabels.isEmpty()) {
                        _uiState.value = UiState.Empty
                    } else {
                        // Default selection to first detected object if present
                        val initialSelectedId = output.detectedObjects.firstOrNull()?.id
                        _uiState.value = UiState.Success(
                            imageLabels = output.imageLabels,
                            detectedObjects = output.detectedObjects,
                            imageWidth = output.imageWidth,
                            imageHeight = output.imageHeight,
                            selectedObjectId = initialSelectedId
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(
                        error.localizedMessage ?: "Failed to read or analyze the selected image."
                    )
                }
            )
        }
    }

    /**
     * Updates the currently active selected object (e.g. when the user taps an interactive blue dot).
     */
    fun selectObject(id: Int) {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(selectedObjectId = id)
        }
    }

    /**
     * Clears current selection and resets state back to Idle.
     */
    fun clearSelection() {
        _selectedImageUri.value = null
        _uiState.value = UiState.Idle
    }
}
