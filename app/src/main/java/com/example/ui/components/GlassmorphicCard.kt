package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassCardBackground
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardBorderActive

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    isActive: Boolean = false,
    activeBorderColor: Color = GlassCardBorderActive,
    borderColor: Color = GlassCardBorder,
    backgroundColor: Color = GlassCardBackground,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val currentBorderColor = if (isActive) activeBorderColor else borderColor
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            backgroundColor.copy(alpha = 0.28f),
            backgroundColor.copy(alpha = 0.16f)
        )
    )

    Surface(
        modifier = modifier
            .clip(shape)
            .border(
                border = BorderStroke(
                    width = borderWidth,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            currentBorderColor.copy(alpha = if (isActive) 0.8f else 0.4f),
                            currentBorderColor.copy(alpha = if (isActive) 0.3f else 0.15f)
                        )
                    )
                ),
                shape = shape
            ),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
        ) {
            content()
        }
    }
}
