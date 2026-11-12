package com.pvca.app.data.network

import com.pvca.app.data.model.DetectedObjectItem
import com.pvca.app.data.model.LabelItem
import com.pvca.app.data.model.MatchCandidate
import com.pvca.app.data.model.MatchConfidenceTier
import com.pvca.app.data.model.MatchResult
import com.pvca.app.data.model.ProductItem
import com.pvca.app.data.matching.ProductMatchingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Product matching service that calls the PVCA FastAPI backend
 * via POST /api/products/match.
 *
 * Uses java.net.HttpURLConnection (no extra dependencies needed).
 */
class BackendMatchingService : ProductMatchingService {

    override suspend fun findBestMatch(
        selectedObject: DetectedObjectItem,
        cropLabels: List<LabelItem>,
        sceneLabels: List<LabelItem>,
        userQuery: String?,
        cropImageBase64: String?
    ): MatchResult = withContext(Dispatchers.IO) {
        val url = URL("${BackendConfig.BASE_URL}/api/products/match")
        val conn = url.openConnection() as HttpURLConnection

        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = BackendConfig.CONNECT_TIMEOUT_MS
            conn.readTimeout = BackendConfig.READ_TIMEOUT_MS
            conn.doOutput = true

            // Build request JSON
            val requestBody = buildRequestJson(selectedObject, cropLabels, sceneLabels, userQuery, cropImageBase64)

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody.toString())
            }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                return@withContext MatchResult.NoMatch(
                    reason = "Backend returned HTTP $responseCode"
                )
            }

            val responseText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseMatchResponse(responseText)
        } finally {
            conn.disconnect()
        }
    }

    private fun buildRequestJson(
        selectedObject: DetectedObjectItem,
        cropLabels: List<LabelItem>,
        sceneLabels: List<LabelItem>,
        userQuery: String?,
        cropImageBase64: String?
    ): JSONObject {
        val json = JSONObject()
        json.put("detected_category", selectedObject.category)

        val cropArray = JSONArray()
        cropLabels.forEach { label ->
            cropArray.put(JSONObject().apply {
                put("text", label.text)
                put("confidence", label.confidence.toDouble())
            })
        }
        json.put("crop_labels", cropArray)

        val sceneArray = JSONArray()
        sceneLabels.forEach { label ->
            sceneArray.put(JSONObject().apply {
                put("text", label.text)
                put("confidence", label.confidence.toDouble())
            })
        }
        json.put("scene_labels", sceneArray)

        if (userQuery != null) {
            json.put("user_query", userQuery)
        }

        if (cropImageBase64 != null) {
            json.put("crop_image_base64", cropImageBase64)
        }

        return json
    }

    private fun parseMatchResponse(responseText: String): MatchResult {
        val json = JSONObject(responseText)
        val matched = json.optBoolean("matched", false)
        val identifiedAs = if (json.has("identified_as") && !json.isNull("identified_as")) {
            json.getString("identified_as")
        } else null

        if (!matched) {
            return MatchResult.NoMatch(
                reason = json.optString("no_match_reason", "No match from backend."),
                suggestedCategory = if (json.has("suggested_category")) json.optString("suggested_category") else null,
                identifiedAs = identifiedAs
            )
        }

        val candidateJson = json.optJSONObject("candidate")
            ?: return MatchResult.NoMatch(reason = "Backend returned matched=true but no candidate.", identifiedAs = identifiedAs)

        val productJson = candidateJson.getJSONObject("product")
        val product = parseProduct(productJson)

        val tierString = candidateJson.optString("confidence_tier", "POSSIBLE_MATCH")
        val tier = when (tierString) {
            "HIGH_CONFIDENCE" -> MatchConfidenceTier.HIGH_CONFIDENCE
            "POSSIBLE_MATCH" -> MatchConfidenceTier.POSSIBLE_MATCH
            else -> MatchConfidenceTier.NO_RELIABLE_MATCH
        }

        val explanation = candidateJson.optString("evidence_explanation", "")
        val matchSource = candidateJson.optString("match_source", "backend")
        val isUserRefined = candidateJson.optBoolean("is_user_refined", false)
        val candidateIdentified = if (candidateJson.has("identified_as") && !candidateJson.isNull("identified_as")) {
            candidateJson.getString("identified_as")
        } else identifiedAs

        return MatchResult.Matched(
            MatchCandidate(
                product = product,
                score = candidateJson.optDouble("score", 0.0).toFloat(),
                confidenceTier = tier,
                evidenceExplanation = if (matchSource == "backend") {
                    "$explanation [via backend]"
                } else {
                    explanation
                },
                isUserRefined = isUserRefined,
                identifiedAs = candidateIdentified
            )
        )
    }

    private fun parseProduct(obj: JSONObject): ProductItem {
        val keywordsArray = obj.optJSONArray("keywords")
        val keywords = mutableListOf<String>()
        if (keywordsArray != null) {
            for (i in 0 until keywordsArray.length()) {
                val kw = keywordsArray.optString(i, "").trim()
                if (kw.isNotEmpty()) keywords.add(kw)
            }
        }
        return ProductItem(
            id = obj.optString("id", ""),
            title = obj.optString("title", ""),
            brand = obj.optString("brand", ""),
            category = obj.optString("category", ""),
            subcategory = obj.optString("subcategory", ""),
            description = obj.optString("description", ""),
            keywords = keywords,
            color = obj.optString("color", ""),
            price = obj.optDouble("price", 0.0),
            currency = obj.optString("currency", "USD"),
            merchant = obj.optString("merchant", ""),
            productUrl = obj.optString("productUrl", ""),
            imageUrl = obj.optString("imageUrl", ""),
            verificationStatus = obj.optString("verificationStatus", "demo_data"),
            provenance = obj.optString("provenance", ""),
            lastVerified = obj.optString("lastVerified", "")
        )
    }
}
