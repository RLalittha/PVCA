package com.pvca.app.data.analyzer

import android.content.Context
import android.net.Uri
import com.pvca.app.data.model.LabelItem

/**
 * Contract for image visual analysis.
 * Decouples the UI and ViewModel from ML Kit implementation details,
 * enabling straightforward testing and future model substitution.
 */
interface ImageAnalyzer {
    /**
     * Analyzes the image provided at [uri] and returns a [Result] containing
     * the detected [LabelItem] list, or a failure exception.
     */
    suspend fun analyze(context: Context, uri: Uri): Result<List<LabelItem>>
}
