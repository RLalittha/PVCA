package com.pvca.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.pvca.app.service.OverlayPermissionHelper
import com.pvca.app.service.ScreenCaptureService
import com.pvca.app.ui.PvcaApp
import com.pvca.app.ui.theme.PvcaTheme
import com.pvca.app.ui.viewmodel.MainViewModel

/**
 * Main activity for PVCA (PayPal Visual Commerce Assistant).
 *
 * Handles:
 * 1. Standard app lifecycle & Jetpack Compose UI.
 * 2. Android overlay & MediaProjection permission flow for system-wide visual shopping.
 * 3. PayPal Sandbox checkout deep links (pvca://checkout).
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Activity result launcher for MediaProjection screen-capture consent
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            ScreenCaptureService.start(this, result.resultCode, result.data!!)
        } else {
            Toast.makeText(
                this,
                "Screen capture permission is required for Visual Shopping.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Permission launcher for Android 13+ (API 33+) foreground service notifications
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestMediaProjection()
        } else {
            // Still attempt capture consent as notification permission is optional for some OEMs
            requestMediaProjection()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleCheckoutDeepLink(intent)

        setContent {
            PvcaTheme {
                val isVisualShoppingActive by ScreenCaptureService.isServiceRunning.collectAsState()

                PvcaApp(
                    viewModel = viewModel,
                    isVisualShoppingActive = isVisualShoppingActive,
                    onStartVisualShopping = { startVisualShoppingSession() },
                    onStopVisualShopping = { stopVisualShoppingSession() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleCheckoutDeepLink(intent)
    }

    /**
     * Initiates the supported Android permission sequence for System-Wide Visual Shopping:
     * Step 1: System Alert Window ("Display over other apps")
     * Step 2: POST_NOTIFICATIONS (Android 13+)
     * Step 3: MediaProjection screen-capture user consent dialog
     */
    private fun startVisualShoppingSession() {
        // Step 1: Check Display Over Other Apps permission
        if (!OverlayPermissionHelper.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Please enable 'Display over other apps' to allow PVCA markers.",
                Toast.LENGTH_LONG
            ).show()
            OverlayPermissionHelper.requestOverlayPermission(this)
            return
        }

        // Step 2: Check Notification Permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        // Step 3: Request Screen Capture Consent
        requestMediaProjection()
    }

    private fun requestMediaProjection() {
        OverlayPermissionHelper.requestMediaProjectionConsent(this, screenCaptureLauncher)
    }

    private fun stopVisualShoppingSession() {
        ScreenCaptureService.stop(this)
        Toast.makeText(this, "Visual Shopping stopped.", Toast.LENGTH_SHORT).show()
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
