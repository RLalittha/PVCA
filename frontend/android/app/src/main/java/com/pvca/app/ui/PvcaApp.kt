package com.pvca.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pvca.app.data.model.UiState
import com.pvca.app.ui.components.AnalysisResultsSection
import com.pvca.app.ui.components.ImagePreviewSection
import com.pvca.app.ui.components.PvcaHeader
import com.pvca.app.ui.viewmodel.MainViewModel

@Composable
fun PvcaApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val matchResult by viewModel.matchResult.collectAsState()
    val isMatching by viewModel.isMatchingProduct.collectAsState()
    val checkoutState by viewModel.checkoutState.collectAsState()

    // Android Photo Picker Launcher (zero runtime storage permissions needed)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onImageSelected(uri, context)
    }

    val successState = uiState as? UiState.Success
    val detectedObjects = successState?.detectedObjects ?: emptyList()
    val selectedObjectId = successState?.selectedObjectId
    val imageWidth = successState?.imageWidth ?: 1
    val imageHeight = successState?.imageHeight ?: 1

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Branded Application Header
            PvcaHeader()

            Spacer(modifier = Modifier.height(16.dp))

            // Image Picker and Preview Section with Blue Dots and Product Discovery Match Card
            ImagePreviewSection(
                imageUri = selectedImageUri,
                detectedObjects = detectedObjects,
                selectedObjectId = selectedObjectId,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                matchResult = matchResult,
                isMatching = isMatching,
                onSelectImageClicked = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onObjectSelected = { id ->
                    viewModel.selectObject(id, context)
                },
                onUserQuerySubmitted = { query ->
                    viewModel.submitUserRefinement(query, context)
                },
                onClearQuery = {
                    viewModel.clearUserRefinement(context)
                },
                checkoutState = checkoutState,
                onStartCheckout = { product ->
                    viewModel.startCheckout(product, context)
                },
                onCaptureCheckout = { orderId ->
                    viewModel.captureOrder(orderId)
                },
                onCancelCheckout = {
                    viewModel.cancelCheckout()
                },
                onResetCheckout = {
                    viewModel.resetCheckout()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Dedicated ML Kit Visual Analysis Results Section (Dual Tab: Objects & Labels)
            AnalysisResultsSection(
                uiState = uiState,
                onObjectSelected = { id ->
                    viewModel.selectObject(id, context)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
