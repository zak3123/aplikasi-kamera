package com.fatih.adaptivecompositioncamera.composition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.fatih.adaptivecompositioncamera.domain.model.CompositionGuide
import com.fatih.adaptivecompositioncamera.domain.model.ForegroundZone
import com.fatih.adaptivecompositioncamera.domain.model.GuideLineStyle
import com.fatih.adaptivecompositioncamera.domain.model.GuideStyle
import com.fatih.adaptivecompositioncamera.domain.model.LevelReading
import com.fatih.adaptivecompositioncamera.domain.model.SpiralOrientation
import com.fatih.adaptivecompositioncamera.utility.CameraMath
import kotlin.math.roundToInt

@Composable
fun CompositionGuideOverlay(
    guide: CompositionGuide,
    mirrored: Boolean,
    modifier: Modifier = Modifier,
    style: GuideStyle = GuideStyle(),
    levelReading: LevelReading = LevelReading(),
    vanishingPoint: Offset = Offset(0.5f, 0.45f),
    frameBounds: Rect = Rect(0.18f, 0.20f, 0.82f, 0.80f),
    eyeLineFraction: Float = 0.36f,
) {
    if (guide == CompositionGuide.None) return
    Canvas(modifier.fillMaxSize()) {
        val color = Color(style.colorArgb).copy(alpha = style.opacity.coerceIn(0.15f, 1f))
        val width = style.thicknessDp.coerceIn(0.7f, 5f).dp.toPx()
        val pathEffect = if (style.lineStyle == GuideLineStyle.Dashed) {
            PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx()))
        } else null
        val stroke = Stroke(width = width, pathEffect = pathEffect)
        val visibleBounds = Rect(0f, 0f, size.width, size.height)
        val effectiveMirrored = mirrored xor style.overlayMirrorHorizontal

        fun line(start: Offset, end: Offset) = guideLine(start, end, color, stroke, style.outline)
        fun mappedX(value: Float): Float = CameraMath.mirrorX(value, size.width, effectiveMirrored)

        clipRect(visibleBounds.left, visibleBounds.top, visibleBounds.right, visibleBounds.bottom) {
            withTransform({
                rotate(style.overlayRotationDegrees.toFloat(), Offset(size.width / 2f, size.height / 2f))
            }) {
            when (guide) {
                CompositionGuide.RuleOfThirds -> {
                    val xs = listOf(size.width / 3f, size.width * 2f / 3f)
                    val ys = listOf(size.height / 3f, size.height * 2f / 3f)
                    xs.forEach { line(Offset(mappedX(it), 0f), Offset(mappedX(it), size.height)) }
                    ys.forEach { line(Offset(0f, it), Offset(size.width, it)) }
                    if (style.intersections) xs.forEach { x -> ys.forEach { y ->
                        drawCircle(color, 3.2.dp.toPx(), Offset(mappedX(x), y))
                    } }
                }

                CompositionGuide.LeadingLines -> {
                    val target = Offset(mappedX(size.width * 0.5f), size.height * 0.34f)
                    listOf(0f, 0.18f, 0.82f, 1f).forEach { fraction ->
                        line(Offset(mappedX(size.width * fraction), size.height), target)
                    }
                    line(
                        Offset(mappedX(0f), size.height * 0.72f),
                        Offset(mappedX(size.width), size.height * 0.72f),
                    )
                }

                CompositionGuide.VanishingPoint -> {
                    val point = Offset(mappedX(size.width * vanishingPoint.x), size.height * vanishingPoint.y)
                    val lineCount = style.vanishingLineCount.coerceIn(3, 13)
                    repeat(lineCount) { index ->
                        val fraction = index / (lineCount - 1f)
                        line(Offset(size.width * fraction, 0f), point)
                        line(Offset(size.width * fraction, size.height), point)
                    }
                    drawCircle(color, 8.dp.toPx(), point, style = stroke)
                    drawCircle(color, 2.5.dp.toPx(), point)
                }

                CompositionGuide.GoldenRatio -> {
                    val first = 1f / (CameraMath.PHI * CameraMath.PHI)
                    listOf(size.width * first, size.width * (1f - first)).forEach {
                        line(Offset(mappedX(it), 0f), Offset(mappedX(it), size.height))
                    }
                    listOf(size.height * first, size.height * (1f - first)).forEach {
                        line(Offset(0f, it), Offset(size.width, it))
                    }
                }

                CompositionGuide.GoldenSpiral -> drawGoldenSpiral(color, stroke, style, effectiveMirrored)

                CompositionGuide.FrameInFrame -> {
                    val left = size.width * frameBounds.left
                    val right = size.width * frameBounds.right
                    val top = size.height * frameBounds.top
                    val bottom = size.height * frameBounds.bottom
                    if (style.frameDimOutside) {
                        drawRect(Color.Black.copy(alpha = 0.22f), Offset.Zero, Size(size.width, top))
                        drawRect(Color.Black.copy(alpha = 0.22f), Offset(0f, bottom), Size(size.width, size.height - bottom))
                        drawRect(Color.Black.copy(alpha = 0.22f), Offset(0f, top), Size(left, bottom - top))
                        drawRect(Color.Black.copy(alpha = 0.22f), Offset(right, top), Size(size.width - right, bottom - top))
                    }
                    val rect = Rect(mappedX(left).coerceAtMost(mappedX(right)), top, mappedX(left).coerceAtLeast(mappedX(right)), bottom)
                    guideRect(rect, color, stroke, style.outline, style.frameCornerDp.coerceIn(0f, 32f).dp.toPx())
                    drawCircle(color, 6.dp.toPx(), rect.bottomRight)
                }

                CompositionGuide.Centered -> {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    line(Offset(center.x, 0f), Offset(center.x, size.height))
                    line(Offset(0f, center.y), Offset(size.width, center.y))
                    if (style.centeredTarget) drawCircle(color, size.minDimension * 0.12f, center, style = stroke)
                    drawCircle(color, 4.dp.toPx(), center)
                }

                CompositionGuide.Symmetry -> {
                    val centerX = size.width / 2f
                    line(Offset(centerX, 0f), Offset(centerX, size.height))
                    line(Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f))
                    listOf(0.18f, 0.32f).forEach { inset ->
                        line(
                            Offset(size.width * inset, 0f),
                            Offset(centerX, size.height),
                        )
                        line(
                            Offset(size.width * (1f - inset), 0f),
                            Offset(centerX, size.height),
                        )
                    }
                }

                CompositionGuide.Diagonal -> {
                    line(Offset(mappedX(0f), 0f), Offset(mappedX(size.width), size.height))
                    line(Offset(mappedX(size.width), 0f), Offset(mappedX(0f), size.height))
                    line(Offset(mappedX(size.width * 0.5f), 0f), Offset(mappedX(0f), size.height * 0.5f))
                    line(Offset(mappedX(size.width * 0.5f), 0f), Offset(mappedX(size.width), size.height * 0.5f))
                    line(Offset(mappedX(0f), size.height * 0.5f), Offset(mappedX(size.width * 0.5f), size.height))
                    line(Offset(mappedX(size.width), size.height * 0.5f), Offset(mappedX(size.width * 0.5f), size.height))
                }

                CompositionGuide.GoldenTriangle -> {
                    val denominator = size.width * size.width + size.height * size.height
                    val topRightProjection = if (denominator > 0f) size.width * size.width / denominator else 0.5f
                    val bottomLeftProjection = if (denominator > 0f) size.height * size.height / denominator else 0.5f
                    val first = Offset(size.width * topRightProjection, size.height * topRightProjection)
                    val second = Offset(size.width * bottomLeftProjection, size.height * bottomLeftProjection)
                    line(Offset(mappedX(0f), 0f), Offset(mappedX(size.width), size.height))
                    line(Offset(mappedX(size.width), 0f), Offset(mappedX(first.x), first.y))
                    line(Offset(mappedX(0f), size.height), Offset(mappedX(second.x), second.y))
                }

                CompositionGuide.TextureRepetition -> {
                    val cells = style.textureGridSize.coerceIn(4, 10)
                    for (index in 1 until cells) {
                        line(Offset(size.width * index / cells, 0f), Offset(size.width * index / cells, size.height))
                        line(Offset(0f, size.height * index / cells), Offset(size.width, size.height * index / cells))
                    }
                    if (style.textureDiagonals) {
                        line(Offset(mappedX(0f), 0f), Offset(mappedX(size.width), size.height))
                        line(Offset(mappedX(size.width), 0f), Offset(mappedX(0f), size.height))
                    }
                }

                CompositionGuide.Foreground -> {
                    val fraction = style.foregroundFraction.coerceIn(0.18f, 0.48f)
                    when (style.foregroundZone) {
                        ForegroundZone.Bottom -> {
                            val edge = size.height * (1f - fraction)
                            drawRect(color.copy(alpha = color.alpha * 0.18f), Offset(0f, edge), Size(size.width, size.height - edge))
                            line(Offset(0f, edge), Offset(size.width, edge))
                        }
                        ForegroundZone.Left -> {
                            val edge = size.width * fraction
                            drawRect(color.copy(alpha = color.alpha * 0.18f), Offset.Zero, Size(edge, size.height))
                            line(Offset(edge, 0f), Offset(edge, size.height))
                        }
                        ForegroundZone.Right -> {
                            val edge = size.width * (1f - fraction)
                            drawRect(color.copy(alpha = color.alpha * 0.18f), Offset(edge, 0f), Size(size.width - edge, size.height))
                            line(Offset(edge, 0f), Offset(edge, size.height))
                        }
                    }
                    val subject = Offset(size.width / 2f, size.height * 0.46f)
                    drawCircle(color, size.minDimension * 0.08f, subject, style = stroke)
                    line(Offset(subject.x - 10.dp.toPx(), subject.y), Offset(subject.x + 10.dp.toPx(), subject.y))
                    line(Offset(subject.x, subject.y - 10.dp.toPx()), Offset(subject.x, subject.y + 10.dp.toPx()))
                }

                CompositionGuide.EyeLine -> {
                    val y = size.height * eyeLineFraction.coerceIn(0.18f, 0.70f)
                    line(Offset(size.width * 0.12f, y), Offset(size.width * 0.88f, y))
                    if (style.faceSafeArea) {
                        drawOval(
                            color = color.copy(alpha = color.alpha * 0.65f),
                            topLeft = Offset(size.width * 0.18f, size.height * 0.10f),
                            size = Size(size.width * 0.64f, size.height * 0.72f),
                            style = stroke,
                        )
                    }
                }

                CompositionGuide.HorizonLevel -> {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val pitchOffset = (levelReading.pitchDegrees / 30f).coerceIn(-1f, 1f) * size.height * 0.16f
                    withTransform({ rotate(-levelReading.rollDegrees, center) }) {
                        val horizonColor = if (levelReading.isLevel) Color(0xFF63E6BE) else color
                        guideLine(
                            Offset(size.width * 0.14f, center.y),
                            Offset(size.width * 0.86f, center.y),
                            horizonColor,
                            Stroke(width = stroke.width * 1.5f, pathEffect = stroke.pathEffect),
                            style.outline,
                        )
                        drawCircle(horizonColor, 4.dp.toPx(), center)
                    }
                    val pitchCenter = Offset(center.x, center.y + pitchOffset)
                    guideLine(
                        Offset(pitchCenter.x, pitchCenter.y - 22.dp.toPx()),
                        Offset(pitchCenter.x, pitchCenter.y + 22.dp.toPx()),
                        if (levelReading.isLevel) Color(0xFF63E6BE) else color,
                        Stroke(width = stroke.width * 1.3f),
                        style.outline,
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            this.color = color.toArgb()
                            textSize = 13.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(
                            "R ${levelReading.rollDegrees.roundToInt()}°  P ${levelReading.pitchDegrees.roundToInt()}°",
                            center.x,
                            center.y - 14.dp.toPx(),
                            paint,
                        )
                    }
                }

                CompositionGuide.None -> Unit
            }
            }
        }
    }
}

private fun DrawScope.drawGoldenSpiral(
    color: Color,
    stroke: Stroke,
    style: GuideStyle,
    mirrored: Boolean,
) {
    val fitted = CameraMath.fitGoldenRectangle(size.width, size.height)
    val path = Path()
    val arcs = CameraMath.goldenSpiralArcs(size.width, size.height)
    arcs.forEachIndexed { index, arc ->
        path.arcTo(
            Rect(arc.oval.left, arc.oval.top, arc.oval.right, arc.oval.bottom),
            arc.startAngleDegrees,
            arc.sweepAngleDegrees,
            forceMoveTo = index == 0,
        )
    }
    val orientationFlipX = style.spiralOrientation == SpiralOrientation.TopRight ||
        style.spiralOrientation == SpiralOrientation.BottomRight
    val orientationFlipY = style.spiralOrientation == SpiralOrientation.BottomLeft ||
        style.spiralOrientation == SpiralOrientation.BottomRight
    val flipX = orientationFlipX xor mirrored xor !style.spiralClockwise xor style.spiralHorizontalFlip
    val flipY = orientationFlipY xor style.spiralVerticalFlip
    val pivot = Offset((fitted.left + fitted.right) / 2f, (fitted.top + fitted.bottom) / 2f)
    withTransform({
        scale(if (flipX) -1f else 1f, if (flipY) -1f else 1f, pivot)
    }) {
        clipRect(fitted.left, fitted.top, fitted.right, fitted.bottom) {
            guideRect(Rect(fitted.left, fitted.top, fitted.right, fitted.bottom), color.copy(alpha = color.alpha * 0.45f), stroke, style.outline)
            arcs.forEach { arc ->
                guideRect(
                    Rect(arc.square.left, arc.square.top, arc.square.right, arc.square.bottom),
                    color.copy(alpha = color.alpha * 0.32f),
                    Stroke(width = stroke.width * 0.75f, pathEffect = stroke.pathEffect),
                    false,
                )
            }
            if (style.outline) drawPath(path, Color.Black.copy(alpha = 0.68f), style = Stroke(width = stroke.width + 2.2.dp.toPx(), pathEffect = stroke.pathEffect))
            drawPath(path, color, style = stroke)
        }
    }
}

private fun DrawScope.guideLine(
    start: Offset,
    end: Offset,
    color: Color,
    stroke: Stroke,
    outline: Boolean,
) {
    if (outline) drawLine(Color.Black.copy(alpha = 0.68f), start, end, stroke.width + 2.2.dp.toPx(), pathEffect = stroke.pathEffect)
    drawLine(color, start, end, stroke.width, pathEffect = stroke.pathEffect)
}

private fun DrawScope.guideRect(
    rect: Rect,
    color: Color,
    stroke: Stroke,
    outline: Boolean,
    cornerRadius: Float = 0f,
) {
    if (outline) {
        drawRoundRect(
            Color.Black.copy(alpha = 0.68f),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
            style = Stroke(width = stroke.width + 2.2.dp.toPx(), pathEffect = stroke.pathEffect),
        )
    }
    drawRoundRect(
        color,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
        style = stroke,
    )
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt(),
    (red * 255).roundToInt(),
    (green * 255).roundToInt(),
    (blue * 255).roundToInt(),
)
