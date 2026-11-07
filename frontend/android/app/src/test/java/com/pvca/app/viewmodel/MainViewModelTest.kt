package com.pvca.app.viewmodel

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.pvca.app.data.analyzer.VisualAnalyzer
import com.pvca.app.data.model.AnalysisOutput
import com.pvca.app.data.model.DetectedObjectItem
import com.pvca.app.data.model.LabelItem
import com.pvca.app.data.model.UiState
import com.pvca.app.ui.viewmodel.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakeVisualAnalyzer(private val output: AnalysisOutput) : VisualAnalyzer {
    override suspend fun analyze(context: Context, uri: Uri): Result<AnalysisOutput> =
        Result.success(output)
}

class MainViewModelTest {

    @Test
    fun initialState_isIdleAndNullUri() {
        val fakeOutput = AnalysisOutput(
            imageLabels = emptyList(),
            detectedObjects = emptyList(),
            imageWidth = 100,
            imageHeight = 100
        )
        val viewModel = MainViewModel(FakeVisualAnalyzer(fakeOutput))
        assertNull(viewModel.selectedImageUri.value)
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun clearSelection_resetsToIdle() {
        val fakeOutput = AnalysisOutput(
            imageLabels = emptyList(),
            detectedObjects = emptyList(),
            imageWidth = 100,
            imageHeight = 100
        )
        val viewModel = MainViewModel(FakeVisualAnalyzer(fakeOutput))
        viewModel.clearSelection()
        assertNull(viewModel.selectedImageUri.value)
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun selectObject_updatesSelectedObjectId() {
        val testObject1 = DetectedObjectItem(
            id = 1,
            rawBoundingBox = Rect(0, 0, 10, 10),
            normalizedBoundingBox = RectF(0f, 0f, 0.1f, 0.1f),
            category = "Fashion good"
        )
        val testObject2 = DetectedObjectItem(
            id = 2,
            rawBoundingBox = Rect(20, 20, 50, 50),
            normalizedBoundingBox = RectF(0.2f, 0.2f, 0.5f, 0.5f),
            category = "Home good"
        )

        val successState = UiState.Success(
            imageLabels = emptyList(),
            detectedObjects = listOf(testObject1, testObject2),
            imageWidth = 100,
            imageHeight = 100,
            selectedObjectId = 1
        )

        // Verify selectedObject helper
        assertEquals(1, successState.selectedObjectId)
        assertEquals("Fashion good", successState.selectedObject?.category)

        // Copy with new selection
        val updatedState = successState.copy(selectedObjectId = 2)
        assertEquals(2, updatedState.selectedObjectId)
        assertEquals("Home good", updatedState.selectedObject?.category)
    }
}
