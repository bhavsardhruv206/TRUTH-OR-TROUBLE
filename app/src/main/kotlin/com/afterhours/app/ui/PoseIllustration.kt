package com.afterhours.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.afterhours.core.Pose

val PartnerPink = Color(0xFFF48FB1)
val PartnerBlue = Color(0xFF64B5F6)

/**
 * Two faceless mannequin figures, one pink and one blue, in the card's pose.
 * Drawn from the pose's joint coordinates, so every pose scales cleanly to any width.
 */
@Composable
fun PoseIllustration(pose: Pose, modifier: Modifier = Modifier) {
    Canvas(
        modifier.fillMaxWidth().aspectRatio(Pose.WIDTH / Pose.HEIGHT)
            .semantics { contentDescription = "Illustration: ${pose.title}" },
    ) {
        val s = size.width / Pose.WIDTH
        fun pt(xy: List<Float>) = Offset(xy[0] * s, xy[1] * s)
        drawLine(Hairline, Offset(20 * s, Pose.GROUND_Y * s), Offset(180 * s, Pose.GROUND_Y * s), strokeWidth = 1.5f * s)
        pose.seat?.let { (from, to, y) ->
            drawLine(HairlineStrong, Offset(from * s, y * s), Offset(to * s, y * s), strokeWidth = 4f * s, cap = StrokeCap.Round)
        }
        // Blue first so pink sits in front where they overlap.
        drawFigure(pose.blue, PartnerBlue, s) { pt(it) }
        drawFigure(pose.pink, PartnerPink, s) { pt(it) }
    }
}

private fun DrawScope.drawFigure(joints: Map<String, List<Float>>, color: Color, s: Float, pt: (List<Float>) -> Offset) {
    fun j(name: String) = pt(joints.getValue(name))
    Pose.LIMBS.forEach { (a, b) -> drawLine(color, j(a), j(b), strokeWidth = 6f * s, cap = StrokeCap.Round) }
    drawLine(color, j("n"), j("p"), strokeWidth = 12f * s, cap = StrokeCap.Round)
    drawCircle(color, radius = 9f * s, center = j("h"))
}
