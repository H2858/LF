package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BioGreen
import com.example.ui.theme.BioViolet
import kotlin.math.sin

@Composable
fun SoundWaveVisualizer(
    isServiceActive: Boolean,
    isPlayingAudio: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(110.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlayingAudio) 1200 else 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        if (!isServiceActive) {
            // Dormant flatline with subtle pulse
            drawLine(
                color = Color(0x33444455),
                start = Offset(0f, midY),
                end = Offset(width, midY),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(
                color = Color(0x4464748B),
                radius = 4.dp.toPx(),
                center = Offset(width / 2f, midY)
            )
            return@Canvas
        }

        // Active baseline amplitude + sound amplitude boost
        val baseAmp = if (isPlayingAudio) {
            (28.dp.toPx() + amplitude * 22.dp.toPx())
        } else {
            10.dp.toPx()
        }

        // 1. Violet background wave (deeper harmonic)
        val pathViolet = Path()
        pathViolet.moveTo(0f, midY)
        for (x in 0..width.toInt() step 6) {
            val normalX = x / width
            val envelope = sin(normalX * Math.PI).toFloat() // window envelope tapering at edges
            val y = midY + sin((normalX * 3.5 * Math.PI) + phase * 0.8).toFloat() * (baseAmp * 0.7f) * envelope
            pathViolet.lineTo(x.toFloat(), y)
        }
        drawPath(
            path = pathViolet,
            color = BioViolet.copy(alpha = if (isPlayingAudio) 0.75f else 0.4f),
            style = Stroke(
                width = if (isPlayingAudio) 3.5.dp.toPx() else 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // 2. Cyan foreground wave (fast, primary sensory pulse)
        val pathCyan = Path()
        pathCyan.moveTo(0f, midY)
        for (x in 0..width.toInt() step 4) {
            val normalX = x / width
            val envelope = sin(normalX * Math.PI).toFloat()
            val y = midY + sin((normalX * 5.0 * Math.PI) - phase * 1.2).toFloat() * baseAmp * envelope
            pathCyan.lineTo(x.toFloat(), y)
        }
        drawPath(
            path = pathCyan,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    BioCyan.copy(alpha = 0.2f),
                    BioCyan.copy(alpha = 0.95f),
                    BioGreen.copy(alpha = 0.95f),
                    BioCyan.copy(alpha = 0.2f)
                )
            ),
            style = Stroke(
                width = if (isPlayingAudio) 4.dp.toPx() else 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // 3. Central pulsing Bio-Orb
        val orbRadius = if (isPlayingAudio) (7f + amplitude * 6f).dp.toPx() else 5.dp.toPx()
        val orbAlpha = if (isPlayingAudio) 0.9f else 0.6f

        // Glow halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    BioCyan.copy(alpha = orbAlpha * 0.6f),
                    Color.Transparent
                ),
                center = Offset(width / 2f, midY),
                radius = orbRadius * 3.5f
            ),
            radius = orbRadius * 3.5f,
            center = Offset(width / 2f, midY)
        )

        // Solid core
        drawCircle(
            color = Color.White,
            radius = orbRadius * 0.6f,
            center = Offset(width / 2f, midY)
        )
        drawCircle(
            color = BioCyan,
            radius = orbRadius,
            center = Offset(width / 2f, midY),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
