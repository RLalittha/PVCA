package com.pvca.app.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.pvca.app.data.model.CheckoutUiState
import com.pvca.app.data.model.DetectedObjectItem
import com.pvca.app.data.model.MatchCandidate
import com.pvca.app.data.model.MatchConfidenceTier
import com.pvca.app.data.model.MatchResult
import com.pvca.app.data.model.ProductItem
import com.pvca.app.ui.theme.PvcaCyan
import com.pvca.app.ui.theme.PvcaDeepBlue
import com.pvca.app.ui.theme.PvcaPrimaryBlue
import com.pvca.app.ui.theme.PvcaSuccess
import com.pvca.app.ui.theme.PvcaWarning

@Composable
fun ProductMatchCard(
    selectedObject: DetectedObjectItem,
    matchResult: MatchResult?,
    isMatching: Boolean,
    onUserQuerySubmitted: (String) -> Unit,
    onClearQuery: () -> Unit,
    checkoutState: CheckoutUiState = CheckoutUiState.Idle,
    onStartCheckout: (ProductItem) -> Unit = {},
    onCaptureCheckout: (String) -> Unit = {},
    onCancelCheckout: () -> Unit = {},
    onResetCheckout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var queryText by remember(selectedObject.id) { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, PvcaCyan.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Object ID + Category + Confidence Tier Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(PvcaPrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${selectedObject.id}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = selectedObject.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedObject.widthPx} × ${selectedObject.heightPx} px",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Match Status Badge with strictly non-misleading terminology
                when (matchResult) {
                    is MatchResult.Matched -> {
                        MatchTierBadge(candidate = matchResult.candidate)
                    }
                    is MatchResult.NoMatch -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "No Reliable Match",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    null -> {}
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body: Loading vs Matched vs NoMatch
            if (isMatching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = PvcaCyan,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Analyzing visual features & searching catalogue…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                when (matchResult) {
                    is MatchResult.Matched -> {
                        MatchedProductContent(
                            candidate = matchResult.candidate,
                            checkoutState = checkoutState,
                            onStartCheckout = onStartCheckout,
                            onCaptureCheckout = onCaptureCheckout,
                            onCancelCheckout = onCancelCheckout,
                            onResetCheckout = onResetCheckout
                        )
                    }
                    is MatchResult.NoMatch -> {
                        NoMatchContent(reason = matchResult.reason)
                    }
                    null -> {}
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // User-Assisted Search Fallback Input (Phase C)
            Text(
                text = "Help us identify this product",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter descriptive terms (e.g. \"ceiling fan remote\" or \"running shoe\") to refine local catalogue search.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = {
                        Text(
                            text = "Describe product...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (queryText.isNotBlank()) {
                                onUserQuerySubmitted(queryText)
                            }
                        }
                    ),
                    trailingIcon = {
                        if (queryText.isNotEmpty()) {
                            IconButton(onClick = {
                                queryText = ""
                                onClearQuery()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PvcaCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (queryText.isNotBlank()) {
                            onUserQuerySubmitted(queryText)
                        }
                    },
                    enabled = queryText.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PvcaPrimaryBlue)
                ) {
                    Text(text = "Refine", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Renders non-misleading confidence tier badges:
 * - HIGH_CONFIDENCE -> "Demo Match (High Confidence)"
 * - POSSIBLE_MATCH  -> "Possible Match"
 * - NO_RELIABLE_MATCH -> "No Reliable Match"
 */
@Composable
private fun MatchTierBadge(candidate: MatchCandidate) {
    val (bgColor, textColor, label) = when (candidate.confidenceTier) {
        MatchConfidenceTier.HIGH_CONFIDENCE -> {
            Triple(PvcaSuccess.copy(alpha = 0.2f), PvcaSuccess, "Demo Match (High Confidence)")
        }
        MatchConfidenceTier.POSSIBLE_MATCH -> {
            Triple(PvcaWarning.copy(alpha = 0.2f), PvcaWarning, "Possible Match")
        }
        MatchConfidenceTier.NO_RELIABLE_MATCH -> {
            Triple(Color.Gray.copy(alpha = 0.2f), Color.LightGray, "No Reliable Match")
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (candidate.isUserRefined) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PvcaDeepBlue)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "User Refined",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = PvcaCyan
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(bgColor)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun MatchedProductContent(
    candidate: MatchCandidate,
    checkoutState: CheckoutUiState,
    onStartCheckout: (ProductItem) -> Unit,
    onCaptureCheckout: (String) -> Unit,
    onCancelCheckout: () -> Unit,
    onResetCheckout: () -> Unit
) {
    val product = candidate.product
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // AI Identification Banner (Gemini Vision)
        if (!candidate.identifiedAs.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PvcaCyan.copy(alpha = 0.12f))
                    .border(1.dp, PvcaCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Identified",
                    tint = PvcaCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Identified: ${candidate.identifiedAs}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PvcaCyan
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Product Thumbnail with verified category placeholder
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = PvcaCyan
                                )
                            }
                        },
                        error = {
                            ProductCategoryPlaceholder(
                                category = product.category,
                                subcategory = product.subcategory
                            )
                        }
                    )
                } else {
                    ProductCategoryPlaceholder(
                        category = product.category,
                        subcategory = product.subcategory
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Text(
                    text = "Brand: ${product.brand} • ${product.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.formattedPrice,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PvcaCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Merchant Row explicitly marked as Demo Catalogue Product
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = product.merchant,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Demo Catalogue Product",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Match Evidence Explanation
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PvcaCyan,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Match Evidence",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = candidate.evidenceExplanation,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // -------------------------------------------------------------
        // Milestone 7: PayPal Sandbox Checkout Flow
        // -------------------------------------------------------------
        Spacer(modifier = Modifier.height(10.dp))

        when (checkoutState) {
            is CheckoutUiState.Idle -> {
                Button(
                    onClick = { onStartCheckout(product) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF003087), // PayPal Brand Navy
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PvcaCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Checkout with PayPal Sandbox",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            is CheckoutUiState.Creating -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = PvcaCyan
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Creating PayPal Sandbox order on backend…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            is CheckoutUiState.AwaitingApproval -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PvcaPrimaryBlue.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PvcaCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = PvcaCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Awaiting PayPal Approval",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = PvcaCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PayPal opened in browser. After approving payment, return here to complete.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onCaptureCheckout(checkoutState.orderId) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PvcaCyan,
                                    contentColor = PvcaDeepBlue
                                )
                            ) {
                                Text(
                                    text = "Complete Payment",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            OutlinedButton(
                                onClick = onCancelCheckout,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            is CheckoutUiState.Capturing -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = PvcaCyan
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Verifying & capturing transaction with backend…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            is CheckoutUiState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PvcaSuccess.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PvcaSuccess.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PvcaSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PayPal Sandbox Payment Verified! ✓",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = PvcaSuccess
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Order ID: ${checkoutState.orderId}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (checkoutState.payerEmail != null) {
                            Text(
                                text = "Payer: ${checkoutState.payerEmail}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Demonstration payment verified by FastAPI backend. No real funds charged.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onResetCheckout,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Done", fontSize = 12.sp)
                        }
                    }
                }
            }
            is CheckoutUiState.Cancelled -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PvcaWarning.copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PvcaWarning.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = PvcaWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Payment Cancelled",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = PvcaWarning
                            )
                            Text(
                                text = "Transaction was cancelled. No charges were made.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onResetCheckout,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PvcaWarning,
                                contentColor = PvcaDeepBlue
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Try Again", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            is CheckoutUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Checkout Error",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = checkoutState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onResetCheckout,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dismiss", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Non-Misleading Demo Notice & Responsive Store Button
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Demo Catalogue Product — For demonstration only",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    Toast.makeText(
                        context,
                        "External store links are disabled for demonstration products.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Store Link Disabled (Demo Catalogue Product)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Renders a clean, verified category-specific placeholder icon when a product image
 * is not available or is a demonstration entry without an exact verified photo.
 */
@Composable
fun ProductCategoryPlaceholder(
    category: String,
    subcategory: String,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when {
        subcategory.contains("Remote", ignoreCase = true) || category.contains("Remote", ignoreCase = true) -> Icons.Default.SettingsRemote
        subcategory.contains("Headphone", ignoreCase = true) || subcategory.contains("Audio", ignoreCase = true) -> Icons.Default.Headphones
        subcategory.contains("Speaker", ignoreCase = true) -> Icons.Default.Speaker
        subcategory.contains("Mouse", ignoreCase = true) -> Icons.Default.Mouse
        subcategory.contains("Keyboard", ignoreCase = true) -> Icons.Default.Keyboard
        category.contains("Footwear", ignoreCase = true) || subcategory.contains("Shoe", ignoreCase = true) || subcategory.contains("Sneaker", ignoreCase = true) || subcategory.contains("Boot", ignoreCase = true) -> Icons.Default.DirectionsRun
        category.contains("Bag", ignoreCase = true) || subcategory.contains("Bag", ignoreCase = true) || subcategory.contains("Tote", ignoreCase = true) || subcategory.contains("Backpack", ignoreCase = true) -> Icons.Default.ShoppingBag
        category.contains("Watch", ignoreCase = true) || subcategory.contains("Watch", ignoreCase = true) -> Icons.Default.Watch
        category.contains("Eyewear", ignoreCase = true) || subcategory.contains("Sunglasses", ignoreCase = true) || subcategory.contains("Glasses", ignoreCase = true) -> Icons.Default.Visibility
        category.contains("Apparel", ignoreCase = true) || category.contains("Clothing", ignoreCase = true) || subcategory.contains("Jacket", ignoreCase = true) || subcategory.contains("Hoodie", ignoreCase = true) -> Icons.Default.Checkroom
        category.contains("Drinkware", ignoreCase = true) || subcategory.contains("Bottle", ignoreCase = true) || subcategory.contains("Mug", ignoreCase = true) || subcategory.contains("Tumbler", ignoreCase = true) -> Icons.Default.LocalCafe
        else -> Icons.Default.Category
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PvcaPrimaryBlue.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$category placeholder",
            tint = PvcaCyan,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun NoMatchContent(reason: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "No Direct Match Found",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
