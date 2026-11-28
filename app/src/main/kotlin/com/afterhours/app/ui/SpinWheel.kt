package com.afterhours.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afterhours.core.Wheel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Long, soft stop: fast start, gentle coast into place. */
private val SpinEasing = CubicBezierEasing(0.12f, 0.8f, 0.2f, 1f)

/**
 * An animated wheel of [labels] with a fixed pointer at 12 o'clock.
 * Each time [spinKey] changes to a non-null value the wheel spins and stops on [target],
 * then calls [onLanded]. The result is decided by the game engine; the wheel only shows it.
 */
@Composable
fun SpinWheel(
    labels: List<String>,
    target: Int,
    spinKey: Any?,
    onTick: () -> Unit,
    onLanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    val measurer = rememberTextMeasurer()
    val segments = labels.size.coerceAtLeast(1)

    LaunchedEffect(spinKey) {
        if (spinKey == null) return@LaunchedEffect
        val end = Wheel.targetRotation(rotation.value, segments, target, fullTurns = 5, offset = Wheel.randomOffset(Random.Default))
        var lastSlice = Wheel.indexAt(rotation.value, segments)
        rotation.animateTo(end, tween(3600, easing = SpinEasing)) {
            val slice = Wheel.indexAt(value, segments)
            if (slice != lastSlice) { lastSlice = slice; onTick() }
        }
        onLanded()
    }

    val labelStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = if (segments > 5) 14.sp else 17.sp, color = Ivory)

    val p = LocalPalette.current
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val pointerRoom = 18.dp.toPx()
            val radius = size.minDimension / 2f - pointerRoom
            val center = Offset(size.width / 2f, size.height / 2f + pointerRoom / 2f)
            val sweep = Wheel.sweep(segments)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            rotate(rotation.value, pivot = center) {
                for (i in 0 until segments) {
                    // Slices run clockwise from 12 o'clock (-90 degrees in canvas terms).
                    val start = -90f + i * sweep
                    drawArc(if (i % 2 == 0) p.surfaceHigh else p.surface, start, sweep, useCenter = true, topLeft = topLeft, size = arcSize)
                    drawArc(p.hairline, start, sweep, useCenter = true, topLeft = topLeft, size = arcSize, style = Stroke(1.dp.toPx()))

                    val mid = Math.toRadians((start + sweep / 2f).toDouble())
                    val labelRadius = radius * 0.62f
                    val pos = Offset(center.x + (labelRadius * cos(mid)).toFloat(), center.y + (labelRadius * sin(mid)).toFloat())
                    val maxWidth = (radius * 0.62f).toInt().coerceAtLeast(1)
                    val layout = measurer.measure(labels.getOrElse(i) { "" }, labelStyle, overflow = TextOverflow.Ellipsis, maxLines = 1, constraints = Constraints(maxWidth = maxWidth))
                    // Text reads outward along the slice's centre line.
                    rotate(start + sweep / 2f, pivot = pos) {
                        translate(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f) { drawText(layout) }
                    }
                }
            }
            // Rim and hub
            drawCircle(p.accent, radius, center, style = Stroke(2.dp.toPx()))
            drawCircle(p.background, radius * 0.14f, center)
            drawCircle(p.accent, radius * 0.14f, center, style = Stroke(1.5.dp.toPx()))
            drawCircle(p.accent, radius * 0.04f, center)
            // Pointer at 12 o'clock
            val tipY = center.y - radius + 10.dp.toPx()
            val half = 11.dp.toPx()
            val pointer = Path().apply {
                moveTo(center.x, tipY)
                lineTo(center.x - half, center.y - radius - pointerRoom + 2.dp.toPx())
                lineTo(center.x + half, center.y - radius - pointerRoom + 2.dp.toPx())
                close()
            }
            drawPath(pointer, p.accent)
        }
    }
}
