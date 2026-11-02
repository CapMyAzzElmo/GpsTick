package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyanGlow
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.CyberNavySurfaceVariant
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ComposeJoyStick(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onDeflectionChanged: (dx: Float, dy: Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val knobOffsetX = remember { Animatable(0f) }
    val knobOffsetY = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .size(size)
            .testTag("in_app_joystick"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    val centerPx = size.toPx() / 2f
                    val maxRadius = centerPx * 0.72f

                    detectDragGestures(
                        onDragStart = { offset ->
                            val dx = offset.x - centerPx
                            val dy = offset.y - centerPx
                            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                            val (targetX, targetY) = if (dist <= maxRadius) {
                                dx to dy
                            } else {
                                val angle = atan2(dy.toDouble(), dx.toDouble())
                                (maxRadius * cos(angle)).toFloat() to (maxRadius * sin(angle)).toFloat()
                            }

                            coroutineScope.launch {
                                knobOffsetX.snapTo(targetX)
                                knobOffsetY.snapTo(targetY)
                            }

                            val normX = (targetX / maxRadius).coerceIn(-1f, 1f)
                            val normY = (targetY / maxRadius).coerceIn(-1f, 1f)
                            onDeflectionChanged(normX, normY)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val curX = knobOffsetX.value + dragAmount.x
                            val curY = knobOffsetY.value + dragAmount.y
                            val dist = sqrt((curX * curX + curY * curY).toDouble()).toFloat()

                            val (targetX, targetY) = if (dist <= maxRadius) {
                                curX to curY
                            } else {
                                val angle = atan2(curY.toDouble(), curX.toDouble())
                                (maxRadius * cos(angle)).toFloat() to (maxRadius * sin(angle)).toFloat()
                            }

                            coroutineScope.launch {
                                knobOffsetX.snapTo(targetX)
                                knobOffsetY.snapTo(targetY)
                            }

                            val normX = (targetX / maxRadius).coerceIn(-1f, 1f)
                            val normY = (targetY / maxRadius).coerceIn(-1f, 1f)
                            onDeflectionChanged(normX, normY)
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                launch { knobOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                launch { knobOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                            }
                            onDeflectionChanged(0f, 0f)
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                launch { knobOffsetX.animateTo(0f) }
                                launch { knobOffsetY.animateTo(0f) }
                            }
                            onDeflectionChanged(0f, 0f)
                        }
                    )
                }
        ) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = size.toPx() * 0.44f
            val knobRadius = baseRadius * 0.36f

            // 1. Draw outer base circle background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyberNavySurfaceVariant.copy(alpha = 0.85f),
                        Color(0xFF0F172A).copy(alpha = 0.95f)
                    ),
                    center = center,
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )

            // 2. Outer glowing ring
            drawCircle(
                color = CyberCyanPrimary.copy(alpha = 0.4f),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // 3. Subtle inner ring
            drawCircle(
                color = CyberCyanPrimary.copy(alpha = 0.15f),
                radius = baseRadius * 0.65f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 4. Directional crosshair ticks
            val tickLen = 14.dp.toPx()
            drawLine(
                color = CyberCyanGlow.copy(alpha = 0.6f),
                start = Offset(center.x, center.y - baseRadius),
                end = Offset(center.x, center.y - baseRadius + tickLen),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = CyberCyanPrimary.copy(alpha = 0.4f),
                start = Offset(center.x, center.y + baseRadius - tickLen),
                end = Offset(center.x, center.y + baseRadius),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = CyberCyanPrimary.copy(alpha = 0.4f),
                start = Offset(center.x - baseRadius, center.y),
                end = Offset(center.x - baseRadius + tickLen, center.y),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = CyberCyanPrimary.copy(alpha = 0.4f),
                start = Offset(center.x + baseRadius - tickLen, center.y),
                end = Offset(center.x + baseRadius, center.y),
                strokeWidth = 2.dp.toPx()
            )

            // 5. Connecting stem line from center to knob
            val knobCenter = Offset(center.x + knobOffsetX.value, center.y + knobOffsetY.value)
            drawLine(
                color = CyberCyanPrimary.copy(alpha = 0.4f),
                start = center,
                end = knobCenter,
                strokeWidth = 4.dp.toPx()
            )

            // 6. Knob shadow / glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyberCyanGlow.copy(alpha = 0.7f), Color.Transparent),
                    center = knobCenter,
                    radius = knobRadius * 1.5f
                ),
                radius = knobRadius * 1.5f,
                center = knobCenter
            )

            // 7. Knob body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyberCyanGlow,
                        CyberCyanPrimary,
                        Color(0xFF0284C7)
                    ),
                    center = knobCenter,
                    radius = knobRadius
                ),
                radius = knobRadius,
                center = knobCenter
            )

            // 8. Knob top shine
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = knobRadius * 0.35f,
                center = Offset(knobCenter.x - knobRadius * 0.25f, knobCenter.y - knobRadius * 0.25f)
            )
        }
    }
}
