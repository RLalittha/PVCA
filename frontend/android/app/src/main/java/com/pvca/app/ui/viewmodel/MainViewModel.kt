package com.pvca.app.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pvca.app.data.analyzer.CombinedVisualAnalyzer
import com.pvca.app.data.analyzer.VisualAnalyzer
import com.pvca.app.data.crop.ObjectCropAnalyzer
import com.pvca.app.data.matching.ProductMatchingService
import com.pvca.app.data.network.BackendConfig
import com.pvca.app.data.network.BackendOrderService
import com.pvca.app.data.network.CaptureOrderResult
import com.pvca.app.data.network.CreateOrderResult
import com.pvca.app.data.network.HybridMatchingService
import com.pvca.app.data.model.CheckoutUiState
import com.pvca.app.data.model.DetectedObjectItem
import com.pvca.app.data.model.LabelItem
import com.pvca.app.data.model.MatchResult
import com.pvca.app.data.model.ProductItem
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
 * - Local & server product catalogue search & ranking
 * - User-assisted search refinement
 * - PayPal Sandbox checkout creation and capture
 */
class MainViewModel(
    private val visualAnalyzer: VisualAnalyzer = CombinedVisualAnalyzer(),
    private val catalogueRepository: ProductCatalogueRepository = LocalAssetCatalogueRepository(),
    private val matchingService: ProductMatchingService = HybridMatchingService(catalogueRepository),
    private val cropAnalyzer: ObjectCropAnalyzer = ObjectCropAnalyzer(),
    private val orderService: BackendOrderService = BackendOrderService()
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

    private val _checkoutState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val checkoutState: StateFlow<CheckoutUiState> = _checkoutState.asStateFlow()

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
        _checkoutState.value = CheckoutUiState.Idle

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
        _checkoutState.value = CheckoutUiState.Idle

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
        _checkoutState.value = CheckoutUiState.Idle
    }

    /**
     * Initiates PayPal Sandbox checkout by creating an order via the FastAPI backend.
     * When successful, opens the browser to PayPal's approval URL.
     */
    fun startCheckout(product: ProductItem, context: Context) {
        if (_checkoutState.value is CheckoutUiState.Creating) return

        _checkoutState.value = CheckoutUiState.Creating(product.id)

        viewModelScope.launch {
            val returnUrl = "${BackendConfig.BASE_URL}/api/orders/web-return?status=success"
            val cancelUrl = "${BackendConfig.BASE_URL}/api/orders/web-return?status=cancelled"

            val result = orderService.createOrder(
                productId = product.id,
                quantity = 1,
                returnUrl = returnUrl,
                cancelUrl = cancelUrl
            )

            when (result) {
                is CreateOrderResult.Success -> {
                    _checkoutState.value = CheckoutUiState.AwaitingApproval(
                        orderId = result.orderId,
                        approvalUrl = result.approvalUrl,
                        productTitle = product.title,
                        price = product.price,
                        currency = product.currency
                    )

                    // Open PayPal approval flow in browser
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(result.approvalUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {
                        _checkoutState.value = CheckoutUiState.Error(
                            "Order created, but could not open browser: ${e.localizedMessage}"
                        )
                    }
                }
                is CreateOrderResult.Error -> {
                    _checkoutState.value = CheckoutUiState.Error(result.message)
                }
            }
        }
    }

    /**
     * Captures an approved PayPal order by calling the backend capture endpoint.
     * Invoked either via deep link (pvca://checkout?token=...) or manual confirmation button.
     */
    fun captureOrder(orderId: String) {
        if (orderId.isBlank()) return
        _checkoutState.value = CheckoutUiState.Capturing(orderId)

        viewModelScope.launch {
            val result = orderService.captureOrder(orderId)
            when (result) {
                is CaptureOrderResult.Success -> {
                    if (result.status.equals("COMPLETED", ignoreCase = true)) {
                        _checkoutState.value = CheckoutUiState.Success(
                            orderId = result.orderId,
                            payerEmail = result.payerEmail,
                            message = "Payment captured successfully! Demo transaction confirmed."
                        )
                    } else {
                        _checkoutState.value = CheckoutUiState.Error(
                            "Order status is ${result.status}. Please complete approval on PayPal."
                        )
                    }
                }
                is CaptureOrderResult.Error -> {
                    _checkoutState.value = CheckoutUiState.Error(result.message)
                }
            }
        }
    }

    /**
     * Marks the checkout as cancelled by the user.
     */
    fun cancelCheckout(orderId: String? = null) {
        _checkoutState.value = CheckoutUiState.Cancelled(orderId)
    }

    /**
     * Resets checkout state back to Idle.
     */
    fun resetCheckout() {
        _checkoutState.value = CheckoutUiState.Idle
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
            // 1. Analyze object-specific crop with on-device ML Kit
            val cropLabels = cropAnalyzer.analyzeCrop(context, imageUri, selectedObject.rawBoundingBox)

            // 2. If no manual user query, prepare compressed crop image for Gemini Vision
            val cropBase64 = if (userQuery.isNullOrBlank()) {
                cropAnalyzer.extractCroppedJpegBase64(context, imageUri, selectedObject.rawBoundingBox)
            } else {
                null
            }

            // 3. Query catalogue and rank candidates (uses Gemini Vision when available, with ML Kit fallback)
            val result = matchingService.findBestMatch(
                selectedObject = selectedObject,
                cropLabels = cropLabels,
                sceneLabels = sceneLabels,
                userQuery = userQuery,
                cropImageBase64 = cropBase64
            )

            _matchResult.value = result
            _isMatchingProduct.value = false
        }
    }
}
