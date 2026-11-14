package com.fatih.adaptivecompositioncamera.ui.camera

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fatih.adaptivecompositioncamera.utility.AdaptiveLayout
import kotlin.math.abs

/** Shared camera chrome dimensions. Preview geometry is calculated separately from these tokens. */
data class CameraUiTokenSet(
    val minimumTouchTarget: Dp,
    val topIconSize: Dp,
    val topVisualSize: Dp,
    val topGap: Dp,
    val topEdgeMargin: Dp,
    val topLabelHorizontalPadding: Dp,
    val topLabelVerticalPadding: Dp,
    val lensTouchTarget: Dp,
    val lensVisibleSize: Dp,
    val lensActiveVisibleSize: Dp,
    val lensGap: Dp,
    val modeHorizontalGap: Dp,
    val secondaryTouchTarget: Dp,
    val secondaryControlSize: Dp,
    val shutterTouchTarget: Dp,
    val shutterOuterSize: Dp,
    val shutterStroke: Dp,
    val exposureSliderHeight: Dp,
    val exposureControlWidth: Dp,
    val exposureThumbSize: Dp,
    val focusRingDiameter: Dp,
    val controlGap: Dp,
    val compactPadding: Dp,
    val portraitControlsHeight: Dp,
    val maximumPortraitControlsWidth: Dp,
    val maximumTabletControlsWidth: Dp,
)

object CameraUiTokens {
    val standardPhone = CameraUiTokenSet(
        minimumTouchTarget = 54.dp,
        topIconSize = 23.dp,
        topVisualSize = 42.dp,
        topGap = 10.dp,
        topEdgeMargin = 16.dp,
        topLabelHorizontalPadding = 9.dp,
        topLabelVerticalPadding = 4.dp,
        lensTouchTarget = 58.dp,
        lensVisibleSize = 44.dp,
        lensActiveVisibleSize = 52.dp,
        lensGap = 12.dp,
        modeHorizontalGap = 26.dp,
        secondaryTouchTarget = 64.dp,
        secondaryControlSize = 54.dp,
        shutterTouchTarget = 100.dp,
        shutterOuterSize = 88.dp,
        shutterStroke = 4.dp,
        exposureSliderHeight = 226.dp,
        exposureControlWidth = 68.dp,
        exposureThumbSize = 24.dp,
        focusRingDiameter = 74.dp,
        controlGap = 10.dp,
        compactPadding = 10.dp,
        portraitControlsHeight = 218.dp,
        maximumPortraitControlsWidth = 620.dp,
        maximumTabletControlsWidth = 720.dp,
    )
    val compactPhone = standardPhone.copy(
        minimumTouchTarget = 52.dp,
        topVisualSize = 40.dp,
        topGap = 9.dp,
        lensTouchTarget = 54.dp,
        lensVisibleSize = 40.dp,
        lensActiveVisibleSize = 50.dp,
        secondaryTouchTarget = 62.dp,
        secondaryControlSize = 52.dp,
        shutterTouchTarget = 98.dp,
        shutterOuterSize = 86.dp,
        portraitControlsHeight = 210.dp,
    )
    val largePhone = standardPhone.copy(
        topGap = 12.dp,
        modeHorizontalGap = 30.dp,
        maximumPortraitControlsWidth = 680.dp,
    )
    val tablet = largePhone.copy(
        minimumTouchTarget = 56.dp,
        topVisualSize = 44.dp,
        lensActiveVisibleSize = 54.dp,
        secondaryTouchTarget = 68.dp,
        secondaryControlSize = 56.dp,
        shutterTouchTarget = 104.dp,
        shutterOuterSize = 92.dp,
        maximumPortraitControlsWidth = 720.dp,
        maximumTabletControlsWidth = 820.dp,
    )

    private val active: CameraUiTokenSet = standardPhone

    val minimumTouchTarget: Dp = active.minimumTouchTarget
    val topIconSize: Dp = active.topIconSize
    val topVisualSize: Dp = active.topVisualSize
    val topGap: Dp = active.topGap
    val topEdgeMargin: Dp = active.topEdgeMargin
    val topLabelHorizontalPadding: Dp = active.topLabelHorizontalPadding
    val topLabelVerticalPadding: Dp = active.topLabelVerticalPadding
    val lensTouchTarget: Dp = active.lensTouchTarget
    val lensVisibleSize: Dp = active.lensVisibleSize
    val lensActiveVisibleSize: Dp = active.lensActiveVisibleSize
    val lensGap: Dp = active.lensGap
    val modeHorizontalGap: Dp = active.modeHorizontalGap
    val secondaryTouchTarget: Dp = active.secondaryTouchTarget
    val secondaryControlSize: Dp = active.secondaryControlSize
    val shutterTouchTarget: Dp = active.shutterTouchTarget
    val shutterOuterSize: Dp = active.shutterOuterSize
    val shutterStroke: Dp = active.shutterStroke
    val exposureSliderHeight: Dp = active.exposureSliderHeight
    val exposureControlWidth: Dp = active.exposureControlWidth
    val exposureThumbSize: Dp = active.exposureThumbSize
    val focusRingDiameter: Dp = active.focusRingDiameter
    val controlGap: Dp = active.controlGap
    val compactPadding: Dp = active.compactPadding
    val portraitControlsHeight: Dp = active.portraitControlsHeight
    val maximumPortraitControlsWidth: Dp = active.maximumPortraitControlsWidth
    val maximumTabletControlsWidth: Dp = active.maximumTabletControlsWidth
}

data class CameraUiLayoutPolicy(
    val landscape: Boolean,
    val tablet: Boolean,
    val controlsMaximumWidth: Dp,
    val captureRailWidth: Dp,
)

enum class UiRotation(val sensorCenterDegrees: Int, val controlDegrees: Float) {
    ROTATION_0(0, 0f),
    ROTATION_90(90, -90f),
    ROTATION_180(180, 180f),
    ROTATION_270(270, 90f),
}

fun displayRotationToUiRotation(displayRotation: Int): UiRotation = when (displayRotation) {
    1 -> UiRotation.ROTATION_90
    2 -> UiRotation.ROTATION_180
    3 -> UiRotation.ROTATION_270
    else -> UiRotation.ROTATION_0
}

fun quantizedUiRotation(
    orientationDegrees: Int,
    previous: UiRotation,
    hysteresisDegrees: Int = 12,
): UiRotation {
    if (orientationDegrees !in 0..359) return previous
    val nearest = UiRotation.entries.minBy {
        circularDistanceDegrees(orientationDegrees, it.sensorCenterDegrees)
    }
    if (nearest == previous) return previous
    val enterThreshold = (45 - hysteresisDegrees).coerceIn(20, 45)
    return if (circularDistanceDegrees(orientationDegrees, nearest.sensorCenterDegrees) <= enterThreshold) {
        nearest
    } else {
        previous
    }
}

fun UiRotation.toSurfaceRotation(): Int = when (this) {
    UiRotation.ROTATION_0 -> 0
    UiRotation.ROTATION_90 -> 3
    UiRotation.ROTATION_180 -> 2
    UiRotation.ROTATION_270 -> 1
}

/**
 * Computes the icon/label rotation relative to the current window coordinate space.
 * When Android has already rotated the Activity to match [physicalRotation], returns 0f
 * so controls are never double-rotated.
 */
fun relativeControlRotationDegrees(
    physicalRotation: UiRotation,
    displayRotation: Int,
): Float {
    val physicalClockwise = physicalRotation.sensorCenterDegrees.toFloat()
    val displayCompensationClockwise = when (displayRotation) {
        1 -> 270f
        2 -> 180f
        3 -> 90f
        else -> 0f
    }
    var delta = (displayCompensationClockwise - physicalClockwise) % 360f
    if (delta <= -180f) delta += 360f
    if (delta > 180f) delta -= 360f
    return if (delta == -0f) 0f else delta
}

fun shortestRotationTarget(currentAngle: Float, targetAngle: Float): Float {
    var delta = (targetAngle - currentAngle) % 360f
    if (delta <= -180f) delta += 360f
    if (delta > 180f) delta -= 360f
    return currentAngle + delta
}

fun responsiveTokens(widthDp: Int, heightDp: Int): CameraUiTokenSet {
    val shortSide = minOf(widthDp, heightDp)
    return when {
        shortSide >= 600 -> CameraUiTokens.tablet
        shortSide <= 340 -> CameraUiTokens.compactPhone
        shortSide >= 412 -> CameraUiTokens.largePhone
        else -> CameraUiTokens.standardPhone
    }
}

private fun circularDistanceDegrees(first: Int, second: Int): Int {
    val diff = abs(first - second) % 360
    return minOf(diff, 360 - diff)
}

enum class CameraTopControlSlot {
    Flash,
    QuickSettings,
    Timer,
    AspectRatio,
    Resolution,
    Stabilization,
    Composition,
    Settings,
}

fun topControlCapacity(
    availableWidthDp: Float,
    controlWidthDp: Float = CameraUiTokens.minimumTouchTarget.value,
    gapDp: Float = CameraUiTokens.topGap.value,
    horizontalSafeMarginDp: Float = CameraUiTokens.topEdgeMargin.value,
): Int {
    val usable = (availableWidthDp - horizontalSafeMarginDp * 2f).coerceAtLeast(controlWidthDp)
    return (((usable + gapDp) / (controlWidthDp + gapDp)).toInt()).coerceAtLeast(1)
}

fun visibleTopControlSlots(
    availableWidthDp: Float,
    hasFlash: Boolean,
    captureFormatControlsVisible: Boolean,
    hasStabilization: Boolean,
    compositionVisible: Boolean,
): Set<CameraTopControlSlot> {
    val ordered = buildList {
        if (hasFlash) add(CameraTopControlSlot.Flash)
        add(CameraTopControlSlot.QuickSettings)
        add(CameraTopControlSlot.Timer)
        if (captureFormatControlsVisible) {
            add(CameraTopControlSlot.AspectRatio)
            add(CameraTopControlSlot.Resolution)
        }
        if (hasStabilization) add(CameraTopControlSlot.Stabilization)
        if (compositionVisible) add(CameraTopControlSlot.Composition)
        add(CameraTopControlSlot.Settings)
    }
    val capacity = topControlCapacity(availableWidthDp).coerceAtMost(ordered.size)
    return ordered.take(capacity).toSet()
}

fun cameraUiLayoutPolicy(layout: AdaptiveLayout): CameraUiLayoutPolicy = when (layout) {
    AdaptiveLayout.PhonePortrait -> CameraUiLayoutPolicy(
        landscape = false,
        tablet = false,
        controlsMaximumWidth = CameraUiTokens.maximumPortraitControlsWidth,
        captureRailWidth = 100.dp,
    )
    AdaptiveLayout.TabletPortrait -> CameraUiLayoutPolicy(
        landscape = false,
        tablet = true,
        controlsMaximumWidth = CameraUiTokens.maximumTabletControlsWidth,
        captureRailWidth = 112.dp,
    )
    AdaptiveLayout.PhoneLandscape -> CameraUiLayoutPolicy(
        landscape = true,
        tablet = false,
        controlsMaximumWidth = CameraUiTokens.maximumPortraitControlsWidth,
        captureRailWidth = 100.dp,
    )
    AdaptiveLayout.TabletLandscape -> CameraUiLayoutPolicy(
        landscape = true,
        tablet = true,
        controlsMaximumWidth = CameraUiTokens.maximumTabletControlsWidth,
        captureRailWidth = 112.dp,
    )
}
