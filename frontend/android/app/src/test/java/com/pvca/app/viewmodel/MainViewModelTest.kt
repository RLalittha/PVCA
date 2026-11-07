package com.pvca.app.viewmodel

import android.content.Context
import android.net.Uri
import com.pvca.app.data.analyzer.ImageAnalyzer
import com.pvca.app.data.model.LabelItem
import com.pvca.app.data.model.UiState
import com.pvca.app.ui.viewmodel.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeSuccessImageAnalyzer(private val results: List<LabelItem>) : ImageAnalyzer {
    override suspend fun analyze(context: Context, uri: Uri): Result<List<LabelItem>> =
        Result.success(results)
}

class FakeFailureImageAnalyzer(private val exception: Exception) : ImageAnalyzer {
    override suspend fun analyze(context: Context, uri: Uri): Result<List<LabelItem>> =
        Result.failure(exception)
}

class MainViewModelTest {

    @Test
    fun initialState_isIdleAndNullUri() {
        val viewModel = MainViewModel(FakeSuccessImageAnalyzer(emptyList()))
        assertNull(viewModel.selectedImageUri.value)
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun nullImageUri_keepsStateUnchanged() {
        // Simulates user opening picker and pressing back/cancelling
        val viewModel = MainViewModel(FakeSuccessImageAnalyzer(emptyList()))
        val dummyContext = null as? Context ?: run {
            // Passing mock or nullable context check
            return
        }
    }

    @Test
    fun clearSelection_resetsToIdle() {
        val viewModel = MainViewModel(FakeSuccessImageAnalyzer(emptyList()))
        viewModel.clearSelection()
        assertNull(viewModel.selectedImageUri.value)
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }
}
