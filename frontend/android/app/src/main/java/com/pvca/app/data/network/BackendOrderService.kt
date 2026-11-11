package com.pvca.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result wrapper for PayPal Sandbox order creation.
 */
sealed interface CreateOrderResult {
    data class Success(
        val orderId: String,
        val approvalUrl: String,
        val status: String
    ) : CreateOrderResult

    data class Error(val message: String) : CreateOrderResult
}

/**
 * Result wrapper for PayPal Sandbox order capture.
 */
sealed interface CaptureOrderResult {
    data class Success(
        val orderId: String,
        val status: String,
        val payerEmail: String?
    ) : CaptureOrderResult

    data class Error(val message: String) : CaptureOrderResult
}

/**
 * Android client service for PayPal Sandbox orders, communicating with
 * the PVCA FastAPI backend.
 *
 * Adheres strictly to security requirements:
 * - Zero PayPal secrets on the Android client.
 * - Authoritative pricing, order creation, and capture happen server-side.
 * - Uses native HttpURLConnection (lightweight, zero bloated dependencies).
 */
class BackendOrderService {

    /**
     * Creates a PayPal Sandbox order on the backend for the given product.
     */
    suspend fun createOrder(
        productId: String,
        quantity: Int = 1,
        returnUrl: String? = null,
        cancelUrl: String? = null
    ): CreateOrderResult = withContext(Dispatchers.IO) {
        val endpoint = "${BackendConfig.BASE_URL}/api/orders/create"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection

        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = BackendConfig.CONNECT_TIMEOUT_MS
            conn.readTimeout = BackendConfig.READ_TIMEOUT_MS
            conn.doOutput = true

            val requestJson = JSONObject().apply {
                put("product_id", productId)
                put("quantity", quantity)
                if (returnUrl != null) put("return_url", returnUrl)
                if (cancelUrl != null) put("cancel_url", cancelUrl)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestJson.toString())
            }

            val responseCode = conn.responseCode
            val responseStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                val errorDetail = try {
                    JSONObject(responseText).optString("detail", "HTTP $responseCode: $responseText")
                } catch (e: Exception) {
                    "Backend error HTTP $responseCode"
                }
                return@withContext CreateOrderResult.Error(errorDetail)
            }

            val json = JSONObject(responseText)
            CreateOrderResult.Success(
                orderId = json.getString("order_id"),
                approvalUrl = json.getString("approval_url"),
                status = json.optString("status", "CREATED")
            )
        } catch (e: Exception) {
            CreateOrderResult.Error(e.localizedMessage ?: "Failed to connect to backend at ${BackendConfig.BASE_URL}")
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Captures an approved PayPal order on the backend.
     */
    suspend fun captureOrder(orderId: String): CaptureOrderResult = withContext(Dispatchers.IO) {
        val endpoint = "${BackendConfig.BASE_URL}/api/orders/$orderId/capture"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection

        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = BackendConfig.CONNECT_TIMEOUT_MS
            conn.readTimeout = BackendConfig.READ_TIMEOUT_MS

            val responseCode = conn.responseCode
            val responseStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                val errorDetail = try {
                    JSONObject(responseText).optString("detail", "HTTP $responseCode: $responseText")
                } catch (e: Exception) {
                    "Capture error HTTP $responseCode"
                }
                return@withContext CaptureOrderResult.Error(errorDetail)
            }

            val json = JSONObject(responseText)
            val status = json.optString("status", "COMPLETED")
            val payerEmail = json.optString("payer_email").takeIf { it.isNotBlank() && it != "null" }

            CaptureOrderResult.Success(
                orderId = json.getString("order_id"),
                status = status,
                payerEmail = payerEmail
            )
        } catch (e: Exception) {
            CaptureOrderResult.Error(e.localizedMessage ?: "Failed to capture order on backend")
        } finally {
            conn.disconnect()
        }
    }
}
