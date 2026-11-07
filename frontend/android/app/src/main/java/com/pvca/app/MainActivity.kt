package com.pvca.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pvca.app.ui.PvcaApp
import com.pvca.app.ui.theme.PvcaTheme

/**
 * Main activity for PVCA (PayPal Visual Commerce Assistant).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PvcaTheme {
                PvcaApp()
            }
        }
    }
}
