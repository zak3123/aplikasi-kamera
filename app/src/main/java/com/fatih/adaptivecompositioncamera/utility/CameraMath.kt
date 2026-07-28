package com.fatih.adaptivecompositioncamera.utility

import android.util.Size
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.PhotoQualityPreset
import com.fatih.adaptivecompositioncamera.domain.model.PhotoAspectRatio
import com.fatih.adaptivecompositioncamera.domain.model.StabilizationSupport
import com.fatih.adaptivecompositioncamera.domain.model.VideoQualitySetting
import com.fatih.adaptivecompositioncamera.domain.model.VideoStabilizationMode
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
    private const val GOLDEN_GUIDE_MARGIN = 0.025f

    fun megapixels(width: Int, height: Int): Double {
        if (width <= 0 || height <= 0) return 0.0
        return round((width.toDouble() * height.toDouble() / 1_000_000.0) * 10.0) / 10.0
    }

    fun aspectRatioLabel(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return "Unknown"
        val ratio = maxOf(width, height).toDouble() / minOf(width, height).toDouble()
        val common = listOf(
            1.0 to "1:1",
            4.0 / 3.0 to "4:3",
            3.0 / 2.0 to "3:2",
            16.0 / 9.0 to "16:9",
        ).minByOrNull { abs(ratio - it.first) }
        if (common != null && abs(ratio - common.first) <= 0.06) return common.second
        return if (ratio > 1.9) "Wide crop" else "Full"
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

    fun selectPhotoQuality(
        resolutions: List<CameraResolution>,
        preset: PhotoQualityPreset,
    ): CameraResolution? {
        if (resolutions.isEmpty() || preset == PhotoQualityPreset.Custom) return null
        val sorted = resolutions.distinctBy { "${it.width}:${it.height}:${it.format}" }
            .sortedByDescending { it.width.toLong() * it.height }
        if (preset == PhotoQualityPreset.Maximum) return sorted.first()
        val target = when (preset) {
            PhotoQualityPreset.High -> 14.0
            PhotoQualityPreset.Medium -> 8.0
            PhotoQualityPreset.StorageSaver -> 4.0
            else -> return sorted.first()
        }
        val preferredRange = when (preset) {
            PhotoQualityPreset.High -> 10.0..18.0
            PhotoQualityPreset.Medium -> 6.0..10.0
            PhotoQualityPreset.StorageSaver -> 2.5..5.5
            else -> 0.0..Double.MAX_VALUE
        }
        val normalOutputs = sorted.filterNot { it.maximumSensorMode || it.highResolution }
        return normalOutputs.filter { it.megapixels in preferredRange }
            .minByOrNull { abs(it.megapixels - target) }
            ?: normalOutputs.minByOrNull { abs(it.megapixels - target) }
            ?: sorted.minByOrNull { abs(it.megapixels - target) }
    }

    fun estimatedJpegBytes(resolution: CameraResolution): Long =
        (resolution.width.toLong() * resolution.height * 0.35).toLong()

    fun stabilizationModes(support: StabilizationSupport): List<VideoStabilizationMode> {
        if (!support.electronicVideo && !support.preview && !support.optical) {
            return listOf(VideoStabilizationMode.Unsupported)
        }
        return buildList {
            add(VideoStabilizationMode.Off)
            if (support.electronicVideo) add(VideoStabilizationMode.Standard)
            if (support.preview) add(VideoStabilizationMode.Preview)
            if (support.optical) add(VideoStabilizationMode.Optical)
            add(VideoStabilizationMode.Auto)
        }
    }

    fun videoFallbackOrder(
        requested: VideoQualitySetting,
        supported: List<VideoQualitySetting>,
    ): List<VideoQualitySetting> {
        val priority = listOf(
            VideoQualitySetting.UHD,
            VideoQualitySetting.FHD,
            VideoQualitySetting.HD,
            VideoQualitySetting.SD,
        )
        if (requested == VideoQualitySetting.Auto) return priority.filter { it in supported }
        val requestedIndex = priority.indexOf(requested)
        return priority.drop(requestedIndex.coerceAtLeast(0)).filter { it in supported }
    }

    fun ruleOfThirds(width: Float, height: Float): List<FloatPoint> = listOf(
        FloatPoint(width / 3f, 0f), FloatPoint(width / 3f, height),
        FloatPoint(width * 2f / 3f, 0f), FloatPoint(width * 2f / 3f, height),
        FloatPoint(0f, height / 3f), FloatPoint(width, height / 3f),
        FloatPoint(0f, height * 2f / 3f), FloatPoint(width, height * 2f / 3f),
    )

    fun fitGoldenRectangle(width: Float, height: Float): FloatBounds {
        if (width <= 0f || height <= 0f) return FloatBounds(0f, 0f, 0f, 0f)
        val availableWidth = width * (1f - GOLDEN_GUIDE_MARGIN * 2f)
        val availableHeight = height * (1f - GOLDEN_GUIDE_MARGIN * 2f)
        val targetWidth: Float
        val targetHeight: Float
        if (availableWidth / availableHeight >= PHI) {
            targetHeight = availableHeight
            targetWidth = availableHeight * PHI
        } else {
            targetWidth = availableWidth
            targetHeight = availableWidth / PHI
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

    fun fitAspectRatio(width: Float, height: Float, targetAspectRatio: Float): FloatBounds {
        if (width <= 0f || height <= 0f || targetAspectRatio <= 0f) return FloatBounds(0f, 0f, 0f, 0f)
        val targetWidth: Float
        val targetHeight: Float
        if (width / height >= targetAspectRatio) {
            targetHeight = height
            targetWidth = height * targetAspectRatio
        } else {
            targetWidth = width
            targetHeight = width / targetAspectRatio
        }
        val left = (width - targetWidth) / 2f
        val top = (height - targetHeight) / 2f
        return FloatBounds(left, top, left + targetWidth, top + targetHeight)
    }

    fun cropDimensions(
        sourceWidth: Int,
        sourceHeight: Int,
        aspectRatio: PhotoAspectRatio,
        viewportWidth: Int = 0,
        viewportHeight: Int = 0,
    ): Pair<Int, Int> {
        if (sourceWidth <= 0 || sourceHeight <= 0) return 0 to 0
        val sourceRatio = sourceWidth.toDouble() / sourceHeight
        val targetRatio = when (aspectRatio) {
            PhotoAspectRatio.FullSensor -> sourceRatio
            PhotoAspectRatio.Ratio4x3 -> 4.0 / 3.0
            PhotoAspectRatio.Ratio3x2 -> 3.0 / 2.0
            PhotoAspectRatio.Ratio16x9 -> 16.0 / 9.0
            PhotoAspectRatio.Ratio1x1 -> 1.0
            PhotoAspectRatio.FullScreen -> {
                if (viewportWidth > 0 && viewportHeight > 0) {
                    maxOf(viewportWidth, viewportHeight).toDouble() / minOf(viewportWidth, viewportHeight)
                } else sourceRatio
            }
        }
        val width: Int
        val height: Int
        if (targetRatio >= sourceRatio) {
            width = sourceWidth
            height = (sourceWidth / targetRatio).toInt()
        } else {
            width = (sourceHeight * targetRatio).toInt()
            height = sourceHeight
        }
        return width.evenDimension(sourceWidth) to height.evenDimension(sourceHeight)
    }

    fun previewAspectRatio(
        sourceWidth: Int,
        sourceHeight: Int,
        aspectRatio: PhotoAspectRatio,
        landscape: Boolean,
        fullScreenAspectRatio: Float,
    ): Float {
        val sensorRatio = if (sourceWidth > 0 && sourceHeight > 0) {
            maxOf(sourceWidth, sourceHeight).toFloat() / minOf(sourceWidth, sourceHeight)
        } else 4f / 3f
        val landscapeRatio = when (aspectRatio) {
            PhotoAspectRatio.FullSensor -> sensorRatio
            PhotoAspectRatio.Ratio4x3 -> 4f / 3f
            PhotoAspectRatio.Ratio3x2 -> 3f / 2f
            PhotoAspectRatio.Ratio16x9 -> 16f / 9f
            PhotoAspectRatio.Ratio1x1 -> 1f
            PhotoAspectRatio.FullScreen -> fullScreenAspectRatio.coerceAtLeast(1f)
        }
        return if (landscape) landscapeRatio else 1f / landscapeRatio
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

    fun jpegOrientationDegrees(
        sensorOrientationDegrees: Int,
        deviceRotationDegrees: Int,
        frontFacing: Boolean,
    ): Int {
        val sensor = ((sensorOrientationDegrees % 360) + 360) % 360
        val device = ((deviceRotationDegrees % 360) + 360) % 360
        return if (frontFacing) {
            (sensor + device) % 360
        } else {
            (sensor - device + 360) % 360
        }
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

    private fun Int.evenDimension(maximum: Int): Int = coerceIn(2, maximum).let { if (it % 2 == 0) it else it - 1 }
}
