package com.pvca.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pvca.app.data.analyzer.CombinedVisualAnalyzer
import com.pvca.app.data.analyzer.VisualAnalyzer
import com.pvca.app.data.crop.ObjectCropAnalyzer
import com.pvca.app.data.matching.DefaultProductMatchingService
import com.pvca.app.data.matching.ProductMatchingService
import com.pvca.app.data.model.DetectedObjectItem
import com.pvca.app.data.model.LabelItem
import com.pvca.app.data.model.MatchResult
import com.pvca.app.data.model.UiState
import com.pvca.app.data.repository.LocalAssetCatalogueRepository
import com.pvca.app.data.repository.ProductCatalogueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating PVCA visual commerce flows:
 * - Image acquisition
 * - Concurrent object detection & scene labeling
 * - Object-specific crop analysis
 * - Local product catalogue search & ranking
 * - User-assisted search refinement
 */
class MainViewModel(
    private val visualAnalyzer: VisualAnalyzer = CombinedVisualAnalyzer(),
    private val catalogueRepository: ProductCatalogueRepository = LocalAssetCatalogueRepository(),
    private val matchingService: ProductMatchingService = DefaultProductMatchingService(catalogueRepository),
    private val cropAnalyzer: ObjectCropAnalyzer = ObjectCropAnalyzer()
) : ViewModel() {

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _matchResult = MutableStateFlow<MatchResult?>(null)
    val matchResult: StateFlow<MatchResult?> = _matchResult.asStateFlow()

    private val _isMatchingProduct = MutableStateFlow(false)
    val isMatchingProduct: StateFlow<Boolean> = _isMatchingProduct.asStateFlow()

    private val _userRefinementQuery = MutableStateFlow<String?>(null)
    val userRefinementQuery: StateFlow<String?> = _userRefinementQuery.asStateFlow()

    /**
     * Triggered when an image is picked or cancelled.
     */
    fun onImageSelected(uri: Uri?, context: Context) {
        if (uri == null) {
            // User cancelled picker: retain existing state safely
            return
        }

        // Attach application context to catalogue repository if using local assets
        (catalogueRepository as? LocalAssetCatalogueRepository)?.attachContext(context)

        _selectedImageUri.value = uri
        _uiState.value = UiState.Loading
        _matchResult.value = null
        _userRefinementQuery.value = null

        viewModelScope.launch {
            val result = visualAnalyzer.analyze(context, uri)
            result.fold(
                onSuccess = { output ->
                    if (output.detectedObjects.isEmpty() && output.imageLabels.isEmpty()) {
                        _uiState.value = UiState.Empty
                    } else {
                        val initialSelectedId = output.detectedObjects.firstOrNull()?.id
                        _uiState.value = UiState.Success(
                            imageLabels = output.imageLabels,
                            detectedObjects = output.detectedObjects,
                            imageWidth = output.imageWidth,
                            imageHeight = output.imageHeight,
                            selectedObjectId = initialSelectedId
                        )

                        // If an object is detected, trigger matching immediately
                        val firstObj = output.detectedObjects.firstOrNull()
                        if (firstObj != null) {
                            triggerProductMatching(context, uri, firstObj, output.imageLabels, null)
                        }
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
     * Triggered when the user taps an interactive blue dot or selects an object row.
     */
    fun selectObject(id: Int, context: Context) {
        val currentState = _uiState.value as? UiState.Success ?: return
        if (currentState.selectedObjectId == id) return

        _uiState.value = currentState.copy(selectedObjectId = id)
        _userRefinementQuery.value = null

        val selectedObj = currentState.detectedObjects.firstOrNull { it.id == id } ?: return
        val currentUri = _selectedImageUri.value ?: return

        triggerProductMatching(context, currentUri, selectedObj, currentState.imageLabels, null)
    }

    /**
     * Triggered when the user submits a description refinement (Phase C).
     */
    fun submitUserRefinement(query: String, context: Context) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return

        _userRefinementQuery.value = cleanQuery
        val currentState = _uiState.value as? UiState.Success ?: return
        val selectedObj = currentState.selectedObject ?: return
        val currentUri = _selectedImageUri.value ?: return

        triggerProductMatching(context, currentUri, selectedObj, currentState.imageLabels, cleanQuery)
    }

    /**
     * Clears user refinement and restores purely visual matching.
     */
    fun clearUserRefinement(context: Context) {
        _userRefinementQuery.value = null
        val currentState = _uiState.value as? UiState.Success ?: return
        val selectedObj = currentState.selectedObject ?: return
        val currentUri = _selectedImageUri.value ?: return

        triggerProductMatching(context, currentUri, selectedObj, currentState.imageLabels, null)
    }

    /**
     * Resets everything to Idle.
     */
    fun clearSelection() {
        _selectedImageUri.value = null
        _uiState.value = UiState.Idle
        _matchResult.value = null
        _userRefinementQuery.value = null
    }

    private fun triggerProductMatching(
        context: Context,
        imageUri: Uri,
        selectedObject: DetectedObjectItem,
        sceneLabels: List<LabelItem>,
        userQuery: String?
    ) {
        _isMatchingProduct.value = true
        viewModelScope.launch {
            // 1. Analyze object-specific crop
            val cropLabels = cropAnalyzer.analyzeCrop(context, imageUri, selectedObject.rawBoundingBox)

            // 2. Query catalogue and rank candidates
            val result = matchingService.findBestMatch(
                selectedObject = selectedObject,
                cropLabels = cropLabels,
                sceneLabels = sceneLabels,
                userQuery = userQuery
            )

            _matchResult.value = result
            _isMatchingProduct.value = false
        }
    }
}
