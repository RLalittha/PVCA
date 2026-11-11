package com.pvca.app.data.model

/**
 * UI state representing the PayPal Sandbox checkout lifecycle in the Android app.
 */
sealed interface CheckoutUiState {

    /** Default state: no active checkout. */
    object Idle : CheckoutUiState

    /** Order creation request in progress with the backend. */
    data class Creating(val productId: String) : CheckoutUiState

    /**
     * Order created successfully on PayPal Sandbox.
     * The browser or Custom Tab is opened for user authorization.
     */
    data class AwaitingApproval(
        val orderId: String,
        val approvalUrl: String,
        val productTitle: String,
        val price: Double,
        val currency: String
    ) : CheckoutUiState

    /** Order approval received; backend capture verification in progress. */
    data class Capturing(val orderId: String) : CheckoutUiState

    /**
     * Payment captured and verified by the FastAPI backend.
     */
    data class Success(
        val orderId: String,
        val payerEmail: String?,
        val message: String
    ) : CheckoutUiState

    /** Payment was cancelled by the user on the PayPal approval screen. */
    data class Cancelled(val orderId: String?) : CheckoutUiState

    /** Payment order creation or capture failed with an error message. */
    data class Error(val message: String) : CheckoutUiState
}
