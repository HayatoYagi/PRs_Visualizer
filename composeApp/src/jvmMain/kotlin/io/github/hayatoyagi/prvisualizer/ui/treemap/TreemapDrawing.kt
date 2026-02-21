package io.github.hayatoyagi.prvisualizer.ui.treemap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.ui.theme.prColor

fun DrawScope.drawPrBorder(
    topLeft: Offset,
    size: Size,
    prs: List<PullRequest>,
    colorMap: Map<String, Color>,
    fallback: Color,
    borderWidth: Float,
) {
    val uniquePrs = prs.distinctBy { it.id }
    when {
        uniquePrs.isEmpty() -> {
            drawRect(
                color = fallback,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = borderWidth),
            )
        }

        uniquePrs.size == 1 -> {
            drawRect(
                color = prColor(uniquePrs.first(), colorMap),
                topLeft = topLeft,
                size = size,
                style = Stroke(width = borderWidth),
            )
        }

        else -> {
            drawDashedMulticolorRectBorder(
                topLeft = topLeft,
                size = size,
                colors = uniquePrs.map { prColor(it, colorMap) },
                strokeWidth = borderWidth,
                dashLength = 14f,
                gapLength = 8f,
            )
        }
    }
}

private fun DrawScope.drawDashedMulticolorRectBorder(
    topLeft: Offset,
    size: Size,
    colors: List<Color>,
    strokeWidth: Float,
    dashLength: Float,
    gapLength: Float,
) {
    if (colors.isEmpty()) return
    val perimeter = (size.width + size.height) * 2f
    if (perimeter <= 0f) return

    var cursor = 0f
    var dashIndex = 0
    while (cursor < perimeter) {
        val dashStart = cursor
        val dashEnd = (cursor + dashLength).coerceAtMost(perimeter)
        val color = colors[dashIndex % colors.size]
        drawPerimeterSegment(
            topLeft = topLeft,
            size = size,
            startDistance = dashStart,
            endDistance = dashEnd,
            color = color,
            strokeWidth = strokeWidth,
        )
        dashIndex += 1
        cursor += dashLength + gapLength
    }
}

private fun DrawScope.drawPerimeterSegment(
    topLeft: Offset,
    size: Size,
    startDistance: Float,
    endDistance: Float,
    color: Color,
    strokeWidth: Float,
) {
    val breaks = listOf(
        0f,
        size.width,
        size.width + size.height,
        size.width * 2f + size.height,
        size.width * 2f + size.height * 2f,
    )

    var segmentStart = startDistance
    while (segmentStart < endDistance) {
        val edgeEnd = breaks.first { it > segmentStart }
        val segmentEnd = minOf(endDistance, edgeEnd)
        val from = pointOnRectPerimeter(topLeft, size, segmentStart)
        val to = pointOnRectPerimeter(topLeft, size, segmentEnd)
        drawLine(
            color = color,
            start = from,
            end = to,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt,
        )
        segmentStart = segmentEnd
    }
}

private fun pointOnRectPerimeter(
    topLeft: Offset,
    size: Size,
    distance: Float,
): Offset {
    val w = size.width
    val h = size.height
    val p = (2f * (w + h)).coerceAtLeast(1f)
    val d = ((distance % p) + p) % p

    return when {
        d <= w -> Offset(topLeft.x + d, topLeft.y)
        d <= w + h -> Offset(topLeft.x + w, topLeft.y + (d - w))
        d <= 2f * w + h -> Offset(topLeft.x + (2f * w + h - d), topLeft.y + h)
        else -> Offset(topLeft.x, topLeft.y + (p - d))
    }
}
