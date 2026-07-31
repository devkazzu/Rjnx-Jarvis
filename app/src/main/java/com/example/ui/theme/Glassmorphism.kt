package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassCard(
    cornerRadius: Dp = 24.dp,
    backgroundColor: Color = DeepSpaceSurface,
    borderColor: Color = GlassBorderColor,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                backgroundColor.copy(alpha = 0.80f),
                backgroundColor.copy(alpha = 0.55f)
            )
        )
    )
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                borderColor,
                Color.White.copy(alpha = 0.12f),
                GlassBorderSecondary
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

@Composable
fun JarvisVoiceOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransition")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val activeColor = when {
        isSpeaking -> GlowingMagenta
        isListening -> NeonCyan
        else -> ElectricBlue
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerPt = center
            val maxRadius = size.toPx() / 2f
            val baseRadius = maxRadius * 0.46f * (if (isListening || isSpeaking) (1f + audioLevel * 0.28f) else pulseScale)

            // Outer Glowing Radial Aura (Sleek Blue & Indigo)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.35f),
                        QuantumPurple.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = centerPt,
                    radius = maxRadius * 0.98f
                ),
                radius = maxRadius * 0.95f
            )

            // Rotating HUD dashed ring 1
            drawCircle(
                color = activeColor.copy(alpha = 0.7f),
                radius = baseRadius * 1.32f,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(35f, 20f, 15f, 20f),
                        rotationAngle * 2f
                    )
                )
            )

            // Rotating HUD dashed ring 2 (Counter-clockwise)
            drawCircle(
                color = GlassBorderColor,
                radius = baseRadius * 1.58f,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(50f, 35f),
                        -rotationAngle * 1.5f
                    )
                )
            )

            // Sleek Inner Glowing Globe
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        NeonCyan,
                        ElectricBlue,
                        VoidBackground
                    ),
                    center = centerPt,
                    radius = baseRadius
                ),
                radius = baseRadius
            )
        }
    }
}

@Composable
fun AudioWaveformVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    val waveColor = if (isSpeaking) GlowingMagenta else NeonCyan

    Canvas(modifier = modifier.fillMaxWidth().height(40.dp)) {
        val widthPx = size.width
        val heightPx = size.height
        val centerY = heightPx / 2f
        val barCount = 28
        val barWidth = widthPx / (barCount * 1.8f)

        for (i in 0 until barCount) {
            val normIndex = i.toFloat() / barCount
            val sinVal = Math.sin((normIndex * 12.0) + phase).toFloat()
            val dynamicAmp = if (isListening || isSpeaking) {
                (audioLevel * 0.8f + 0.2f) * Math.abs(sinVal) * (heightPx / 2.2f)
            } else {
                (0.15f + 0.1f * Math.abs(sinVal)) * (heightPx / 2.5f)
            }

            val barHeight = dynamicAmp.coerceAtLeast(6f)
            val x = i * (barWidth * 1.8f) + 10f

            drawRoundRect(
                color = waveColor.copy(alpha = 0.85f),
                topLeft = androidx.compose.ui.geometry.Offset(x, centerY - barHeight / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
    }
}
