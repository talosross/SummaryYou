package com.talosross.summaryyou.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LogoIcon(modifier: Modifier = Modifier, size: Dp, isRotating: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRotating) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "rotation angle"
    )

    val innerBoxSize = size / 1.6f

    Box(
        modifier = modifier
            .size(size)
            .rotate(-angle)
            .clip(MaterialShapes.VerySunny.toShape())
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(innerBoxSize)
                .rotate(angle * 1.5f)
                .clip(MaterialShapes.Flower.toShape())
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {}
    }
}

@Preview
@Composable
fun LogoIconPreview() {
    LogoIcon(size = 250.dp)
}

