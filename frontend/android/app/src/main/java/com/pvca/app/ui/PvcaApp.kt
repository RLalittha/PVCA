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

    // Android Photo Picker Launcher
    // Standard system-provided picker: does not require intrusive storage permissions
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onImageSelected(uri, context)
    }

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

            // Image Picker and Preview Section
            ImagePreviewSection(
                imageUri = selectedImageUri,
                onSelectImageClicked = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Dedicated ML Kit Visual Analysis Results Section
            AnalysisResultsSection(
                uiState = uiState
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
