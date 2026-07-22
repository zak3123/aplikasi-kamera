package com.fatih.adaptivecompositioncamera.utility

import android.util.Size
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.round

data class FloatPoint(val x: Float, val y: Float)

data class FloatBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    fun contains(point: FloatPoint): Boolean = point.x in left..right && point.y in top..bottom
}

data class GoldenSpiralArc(
    val square: FloatBounds,
    val oval: FloatBounds,
    val startAngleDegrees: Float,
    val sweepAngleDegrees: Float = 90f,
)

enum class AdaptiveLayout { PhonePortrait, PhoneLandscape, TabletPortrait, TabletLandscape }

object CameraMath {
    const val PHI = 1.61803398875f

    fun megapixels(width: Int, height: Int): Double {
        if (width <= 0 || height <= 0) return 0.0
        return round((width.toDouble() * height.toDouble() / 1_000_000.0) * 10.0) / 10.0
    }

    fun aspectRatioLabel(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return "Unknown"
        val gcd = gcd(width, height)
        return "${width / gcd}:${height / gcd}"
    }

    fun sortResolutions(sizes: List<Size>, format: String): List<CameraResolution> {
        return sortResolutionDimensions(sizes.map { it.width to it.height }, format)
    }

    fun sortResolutionDimensions(sizes: List<Pair<Int, Int>>, format: String): List<CameraResolution> {
        val unique = sizes.filter { it.first > 0 && it.second > 0 }.distinct()
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.first.toLong() * it.second.toLong() }.thenByDescending { it.first })
        val recommendedIndex = unique.indexOfFirstRecommended()
        return unique.mapIndexed { index, size ->
            CameraResolution(
                width = size.first,
                height = size.second,
                format = format,
                megapixels = megapixels(size.first, size.second),
                aspectRatioLabel = aspectRatioLabel(size.first, size.second),
                recommended = index == recommendedIndex,
                maximum = index == 0,
            )
        }
    }

    fun filterByAspectRatio(
        resolutions: List<CameraResolution>,
        targetWidth: Int,
        targetHeight: Int,
        tolerance: Float = 0.02f,
    ): List<CameraResolution> {
        if (targetWidth <= 0 || targetHeight <= 0) return emptyList()
        val target = targetWidth.toFloat() / targetHeight
        return resolutions.filter { abs(it.width.toFloat() / it.height - target) <= tolerance }
    }

    fun ruleOfThirds(width: Float, height: Float): List<FloatPoint> = listOf(
        FloatPoint(width / 3f, 0f), FloatPoint(width / 3f, height),
        FloatPoint(width * 2f / 3f, 0f), FloatPoint(width * 2f / 3f, height),
        FloatPoint(0f, height / 3f), FloatPoint(width, height / 3f),
        FloatPoint(0f, height * 2f / 3f), FloatPoint(width, height * 2f / 3f),
    )

    fun fitGoldenRectangle(width: Float, height: Float): FloatBounds {
        if (width <= 0f || height <= 0f) return FloatBounds(0f, 0f, 0f, 0f)
        val targetWidth: Float
        val targetHeight: Float
        if (width / height >= PHI) {
            targetHeight = height
            targetWidth = height * PHI
        } else {
            targetWidth = width
            targetHeight = width / PHI
        }
        val left = (width - targetWidth) / 2f
        val top = (height - targetHeight) / 2f
        return FloatBounds(left, top, left + targetWidth, top + targetHeight)
    }

    fun goldenSpiralArcs(width: Float, height: Float, iterations: Int = 10): List<GoldenSpiralArc> {
        val fitted = fitGoldenRectangle(width, height)
        if (fitted.width <= 0f || fitted.height <= 0f) return emptyList()
        var working = fitted
        return buildList {
            repeat(iterations.coerceIn(1, 14)) { step ->
                if (working.width < 0.75f || working.height < 0.75f) return@repeat
                val direction = step % 4
                val side: Float
                val square: FloatBounds
                when (direction) {
                    0 -> {
                        side = working.height
                        square = FloatBounds(working.left, working.top, working.left + side, working.bottom)
                        working = FloatBounds(square.right, working.top, working.right, working.bottom)
                    }
                    1 -> {
                        side = working.width
                        square = FloatBounds(working.left, working.top, working.right, working.top + side)
                        working = FloatBounds(working.left, square.bottom, working.right, working.bottom)
                    }
                    2 -> {
                        side = working.height
                        square = FloatBounds(working.right - side, working.top, working.right, working.bottom)
                        working = FloatBounds(working.left, working.top, square.left, working.bottom)
                    }
                    else -> {
                        side = working.width
                        square = FloatBounds(working.left, working.bottom - side, working.right, working.bottom)
                        working = FloatBounds(working.left, working.top, working.right, square.top)
                    }
                }
                val center = when (direction) {
                    0 -> FloatPoint(square.right, square.bottom)
                    1 -> FloatPoint(square.left, square.bottom)
                    2 -> FloatPoint(square.left, square.top)
                    else -> FloatPoint(square.right, square.top)
                }
                add(
                    GoldenSpiralArc(
                        square = square,
                        oval = FloatBounds(
                            center.x - side,
                            center.y - side,
                            center.x + side,
                            center.y + side,
                        ),
                        startAngleDegrees = when (direction) {
                            0 -> 180f
                            1 -> 270f
                            2 -> 0f
                            else -> 90f
                        },
                    ),
                )
            }
        }
    }

    fun transformPoint(
        point: FloatPoint,
        bounds: FloatBounds,
        horizontalFlip: Boolean,
        verticalFlip: Boolean,
    ): FloatPoint = FloatPoint(
        x = if (horizontalFlip) bounds.right - (point.x - bounds.left) else point.x,
        y = if (verticalFlip) bounds.bottom - (point.y - bounds.top) else point.y,
    )

    fun mirrorX(x: Float, width: Float, mirrored: Boolean): Float = if (mirrored) width - x else x

    fun mapFillCenterPoint(
        point: FloatPoint,
        sourceWidth: Float,
        sourceHeight: Float,
        destinationWidth: Float,
        destinationHeight: Float,
    ): FloatPoint {
        if (sourceWidth <= 0f || sourceHeight <= 0f || destinationWidth <= 0f || destinationHeight <= 0f) {
            return FloatPoint(0f, 0f)
        }
        val scale = maxOf(destinationWidth / sourceWidth, destinationHeight / sourceHeight)
        val scaledWidth = sourceWidth * scale
        val scaledHeight = sourceHeight * scale
        val cropX = (scaledWidth - destinationWidth) / 2f
        val cropY = (scaledHeight - destinationHeight) / 2f
        return FloatPoint(point.x * scale - cropX, point.y * scale - cropY)
    }

    fun adaptiveLayout(widthDp: Int, heightDp: Int): AdaptiveLayout {
        val tablet = minOf(widthDp, heightDp) >= 600
        return when {
            tablet && widthDp >= heightDp -> AdaptiveLayout.TabletLandscape
            tablet -> AdaptiveLayout.TabletPortrait
            widthDp >= heightDp -> AdaptiveLayout.PhoneLandscape
            else -> AdaptiveLayout.PhonePortrait
        }
    }

    fun horizonRollDegrees(gravityX: Float, gravityY: Float): Float {
        if (gravityX == 0f && gravityY == 0f) return 0f
        return Math.toDegrees(atan2(gravityX.toDouble(), gravityY.toDouble())).toFloat()
    }

    fun isFrameRateValid(candidateFps: Int, ranges: List<IntRange>): Boolean {
        return ranges.any { candidateFps in it }
    }

    fun isHighSpeedVisible(options: List<com.fatih.adaptivecompositioncamera.domain.model.HighSpeedVideoOption>): Boolean {
        return options.any { it.maxFps >= 120 }
    }

    fun digitalZoomLabel(current: Float, opticalBreakpoints: List<Float>): String {
        val optical = opticalBreakpoints.any { abs(current - it) < 0.08f }
        return if (optical) "Optical position" else "May be digital zoom"
    }

    private fun List<Pair<Int, Int>>.indexOfFirstRecommended(): Int {
        if (isEmpty()) return -1
        val twelveMp = indexOfFirst {
            val pixels = it.first.toLong() * it.second.toLong()
            pixels in 8_000_000L..14_000_000L
        }
        return if (twelveMp >= 0) twelveMp else lastIndex.coerceAtLeast(0)
    }

    private tailrec fun gcd(a: Int, b: Int): Int {
        return if (b == 0) abs(a) else gcd(b, a % b)
    }
}
