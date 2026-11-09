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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
 *
 * NOTE: Resolves Day 2 label overlap issue by eliminating cluttered floating badges for
 * unselected objects. The selected object is highlighted clearly, and detailed information
 * is surfaced in the Product Discovery Card below.
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
                        // Highlighted fill for selected object
                        drawRoundRect(
                            color = PvcaCyan.copy(alpha = 0.12f),
                            topLeft = Offset(boxLeft, boxTop),
                            size = Size(boxWidth, boxHeight),
                            cornerRadius = cornerRadius
                        )
                        // Vibrant glowing solid stroke
                        drawRoundRect(
                            color = PvcaCyan,
                            topLeft = Offset(boxLeft, boxTop),
                            size = Size(boxWidth, boxHeight),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    } else {
                        // Unselected subtle dashed stroke that does not clutter the scene
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.50f),
                            topLeft = Offset(boxLeft, boxTop),
                            size = Size(boxWidth, boxHeight),
                            cornerRadius = cornerRadius,
                            style = Stroke(
                                width = 1.6.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )
                        )
                    }
                }
            }
        }

        // Layer 2: Interactive Blue Dots positioned at geometric centers of bounding boxes
        detectedObjects.forEach { obj ->
            val isSelected = (obj.id == selectedObjectId)
            val (screenCenterX, screenCenterY) = transformer.toScreenCenter(obj.rawBoundingBox)

            // 48.dp touch target centered at (screenCenterX, screenCenterY)
            val dotTouchTargetPx = with(density) { 48.dp.toPx() }
            val dotLeftPx = (screenCenterX - dotTouchTargetPx / 2f).roundToInt()
            val dotTopPx = (screenCenterY - dotTouchTargetPx / 2f).roundToInt()

            InteractiveBlueDot(
                isSelected = isSelected,
                objectId = obj.id,
                category = obj.category,
                onClick = { onObjectSelected(obj.id) },
                modifier = Modifier.offset { IntOffset(dotLeftPx, dotTopPx) }
            )
        }
    }
}

/**
 * Signature PVCA Interactive Blue Dot with numeric object identification badge.
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
        targetValue = if (isSelected) 1.45f else 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSelected) 1000 else 1500),
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
        // Outer pulsing aura
        Box(
            modifier = Modifier
                .size(if (isSelected) 40.dp else 32.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    if (isSelected) PvcaCyan.copy(alpha = 0.40f) else PvcaBlueDotGlow
                )
        )

        // Core indicator with #ID number inside
        Box(
            modifier = Modifier
                .size(if (isSelected) 24.dp else 20.dp)
                .clip(CircleShape)
                .background(if (isSelected) PvcaCyan else PvcaPrimaryBlue)
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color.White else PvcaCyan,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$objectId",
                color = if (isSelected) PvcaDeepBlue else Color.White,
                fontSize = if (isSelected) 11.sp else 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
