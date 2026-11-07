package com.pvca.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pvca.app.data.model.DetectedObjectItem
import com.pvca.app.ui.theme.PvcaBlueDotGlow
import com.pvca.app.ui.theme.PvcaCyan
import com.pvca.app.ui.theme.PvcaDeepBlue
import com.pvca.app.ui.theme.PvcaPrimaryBlue
import com.pvca.app.ui.util.CoordinateTransformer
import kotlin.math.roundToInt

/**
 * Renders real detection bounding boxes and interactive glowing blue dots over the photograph.
 *
 * Coordinates are mapped from source image pixel space to screen display space,
 * taking into account ContentScale.Fit letterboxing and pillarboxing.
 */
@Composable
fun DetectionBoundingBoxOverlay(
    detectedObjects: List<DetectedObjectItem>,
    selectedObjectId: Int?,
    imageWidth: Int,
    imageHeight: Int,
    containerWidthPx: Float,
    containerHeightPx: Float,
    onObjectSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (detectedObjects.isEmpty() || imageWidth <= 0 || imageHeight <= 0) {
        return
    }

    val transformer = remember(imageWidth, imageHeight, containerWidthPx, containerHeightPx) {
        CoordinateTransformer(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            containerWidth = containerWidthPx,
            containerHeight = containerHeightPx
        )
    }

    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Canvas drawing bounding boxes
        Canvas(modifier = Modifier.fillMaxSize()) {
            detectedObjects.forEach { obj ->
                val screenRect = transformer.toScreenRect(obj.rawBoundingBox)
                val isSelected = (obj.id == selectedObjectId)

                val boxLeft = screenRect.left
                val boxTop = screenRect.top
                val boxWidth = (screenRect.right - screenRect.left).coerceAtLeast(0f)
                val boxHeight = (screenRect.bottom - screenRect.top).coerceAtLeast(0f)

                if (boxWidth > 0f && boxHeight > 0f) {
                    val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

                    if (isSelected) {
                        // Highlighted fill
                        drawRoundRect(
                            color = PvcaCyan.copy(alpha = 0.12f),
                            topLeft = Offset(boxLeft, boxTop),
                            size = Size(boxWidth, boxHeight),
                            cornerRadius = cornerRadius
                        )
                        // Vibrant glowing stroke
                        drawRoundRect(
                            color = PvcaCyan,
                            topLeft = Offset(boxLeft, boxTop),
                            size = Size(boxWidth, boxHeight),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    } else {
                        // Unselected subtle dashed or semi-transparent stroke
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.65f),
                            topLeft = Offset(boxLeft, boxTop),
                            size = Size(boxWidth, boxHeight),
                            cornerRadius = cornerRadius,
                            style = Stroke(
                                width = 1.8.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        )
                    }
                }
            }
        }

        // Layer 2: Interactive Blue Dots positioned at actual centers of detected bounding boxes
        detectedObjects.forEach { obj ->
            val isSelected = (obj.id == selectedObjectId)
            val (screenCenterX, screenCenterY) = transformer.toScreenCenter(obj.rawBoundingBox)

            val screenRect = transformer.toScreenRect(obj.rawBoundingBox)

            // Touch target size is 48.dp
            val dotTouchTargetPx = with(density) { 48.dp.toPx() }
            val dotLeftPx = (screenCenterX - dotTouchTargetPx / 2f).roundToInt()
            val dotTopPx = (screenCenterY - dotTouchTargetPx / 2f).roundToInt()

            // Interactive Glowing Blue Dot
            InteractiveBlueDot(
                isSelected = isSelected,
                objectId = obj.id,
                category = obj.category,
                onClick = { onObjectSelected(obj.id) },
                modifier = Modifier.offset { IntOffset(dotLeftPx, dotTopPx) }
            )

            // Category Badge over the top-left of the bounding box
            val badgeLeftPx = screenRect.left.roundToInt().coerceAtLeast(4)
            val badgeTopPx = (screenRect.top - with(density) { 24.dp.toPx() }).roundToInt().coerceAtLeast(4)

            ObjectBadge(
                id = obj.id,
                category = obj.category,
                isSelected = isSelected,
                onClick = { onObjectSelected(obj.id) },
                modifier = Modifier.offset { IntOffset(badgeLeftPx, badgeTopPx) }
            )
        }
    }
}

/**
 * Signature PVCA Interactive Blue Dot.
 * Positioned using true object coordinates. Features a soft outer halo and responsive tap handling.
 */
@Composable
private fun InteractiveBlueDot(
    isSelected: Boolean,
    objectId: Int,
    category: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BlueDotPulse-$objectId")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (isSelected) 1.0f else 0.95f,
        targetValue = if (isSelected) 1.50f else 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSelected) 900 else 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale-$objectId"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(48.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Detected object $objectId: $category"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 28.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing glowing aura
        Box(
            modifier = Modifier
                .size(if (isSelected) 38.dp else 30.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    if (isSelected) PvcaCyan.copy(alpha = 0.45f) else PvcaBlueDotGlow
                )
        )

        // Core dot
        Box(
            modifier = Modifier
                .size(if (isSelected) 20.dp else 16.dp)
                .clip(CircleShape)
                .background(if (isSelected) PvcaCyan else PvcaPrimaryBlue)
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color.White else PvcaCyan,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                // Inner bright pip for active selection
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

/**
 * Compact pill badge labeling the detected object above its bounding box.
 */
@Composable
private fun ObjectBadge(
    id: Int,
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) PvcaCyan else PvcaDeepBlue.copy(alpha = 0.85f)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = "#$id $category",
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PvcaDeepBlue else Color.White,
            maxLines = 1
        )
    }
}
