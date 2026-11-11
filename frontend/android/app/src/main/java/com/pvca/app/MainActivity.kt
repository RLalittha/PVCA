package com.pvca.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.pvca.app.ui.PvcaApp
import com.pvca.app.ui.theme.PvcaTheme
import com.pvca.app.ui.viewmodel.MainViewModel

/**
 * Main activity for PVCA (PayPal Visual Commerce Assistant).
 * Handles standard app lifecycle and PayPal checkout deep links (pvca://checkout).
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleCheckoutDeepLink(intent)

        setContent {
            PvcaTheme {
                PvcaApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleCheckoutDeepLink(intent)
    }

    private fun handleCheckoutDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "pvca" && data.host == "checkout") {
            val token = data.getQueryParameter("token").orEmpty()
            val status = data.getQueryParameter("status").orEmpty()

            if (status.equals("success", ignoreCase = true) && token.isNotBlank()) {
                viewModel.captureOrder(token)
            } else if (status.equals("cancelled", ignoreCase = true)) {
                viewModel.cancelCheckout(token.takeIf { it.isNotBlank() })
            }
        }
    }
}
