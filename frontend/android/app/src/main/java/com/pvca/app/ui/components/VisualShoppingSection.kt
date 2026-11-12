package com.pvca.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pvca.app.ui.theme.PvcaCyan
import com.pvca.app.ui.theme.PvcaPrimaryBlue

/**
 * System-Wide Visual Shopping banner card.
 *
 * Provides a clear "Start Visual Shopping" action enabling users to scan other Android apps
 * (Chrome, YouTube, Instagram) using an interactive floating blue dot overlay and PayPal Sandbox checkout.
 */
@Composable
fun VisualShoppingSection(
    isActive: Boolean,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isActive) Color(0xFF10B981) else PvcaPrimaryBlue,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Status & Title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Color(0xFF10B981) else PvcaCyan)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "System-Wide Visual Shopping",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                // Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) Color(0x3310B981) else Color(0x3300CFE8))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isActive) "ACTIVE" else "ORIGINAL VISION",
                        color = if (isActive) Color(0xFF10B981) else PvcaCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isActive) {
                    "PVCA overlay is active! Switch to Chrome, YouTube, or Instagram. Tap 'Scan' on the floating pill to place interactive blue dots over items."
                } else {
                    "Shop for products you notice while using other apps. When activated, PVCA displays interactive blue dots over detected objects on your screen with 1-tap PayPal Sandbox checkout."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            if (!isActive) {
                Button(
                    onClick = onStartClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PvcaPrimaryBlue
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "🚀 Start Visual Shopping",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onStopClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "⏹ Stop Visual Shopping",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Requires system overlay & screen capture permissions. Zero persistent image storage.",
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
