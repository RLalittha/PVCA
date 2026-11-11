package com.pvca.app.viewmodel

import com.pvca.app.data.model.AnalysisOutput
import com.pvca.app.data.model.CheckoutUiState
import com.pvca.app.ui.viewmodel.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutViewModelTest {

    private val fakeOutput = AnalysisOutput(
        imageLabels = emptyList(),
        detectedObjects = emptyList(),
        imageWidth = 100,
        imageHeight = 100
    )

    @Test
    fun initialCheckoutState_isIdle() {
        val viewModel = MainViewModel(visualAnalyzer = FakeVisualAnalyzer(fakeOutput))
        assertEquals(CheckoutUiState.Idle, viewModel.checkoutState.value)
    }

    @Test
    fun cancelCheckout_setsCancelledState() {
        val viewModel = MainViewModel(visualAnalyzer = FakeVisualAnalyzer(fakeOutput))
        viewModel.cancelCheckout("ORDER-123")
        assertTrue(viewModel.checkoutState.value is CheckoutUiState.Cancelled)
        val cancelled = viewModel.checkoutState.value as CheckoutUiState.Cancelled
        assertEquals("ORDER-123", cancelled.orderId)
    }

    @Test
    fun resetCheckout_restoresIdle() {
        val viewModel = MainViewModel(visualAnalyzer = FakeVisualAnalyzer(fakeOutput))
        viewModel.cancelCheckout("ORDER-123")
        viewModel.resetCheckout()
        assertEquals(CheckoutUiState.Idle, viewModel.checkoutState.value)
    }

    @Test
    fun clearSelection_resetsCheckoutState() {
        val viewModel = MainViewModel(visualAnalyzer = FakeVisualAnalyzer(fakeOutput))
        viewModel.cancelCheckout("ORDER-123")
        viewModel.clearSelection()
        assertEquals(CheckoutUiState.Idle, viewModel.checkoutState.value)
    }
}
