package com.pvca.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pvca.app.data.analyzer.ImageAnalyzer
import com.pvca.app.data.analyzer.MlKitImageAnalyzer
import com.pvca.app.data.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing PVCA state: selected image and visual analysis results.
 */
class MainViewModel(
    private val imageAnalyzer: ImageAnalyzer = MlKitImageAnalyzer()
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
            val result = imageAnalyzer.analyze(context, uri)
            result.fold(
                onSuccess = { labels ->
                    _uiState.value = if (labels.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(labels)
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
     * Clears current selection and resets state back to Idle.
     */
    fun clearSelection() {
        _selectedImageUri.value = null
        _uiState.value = UiState.Idle
    }
}
