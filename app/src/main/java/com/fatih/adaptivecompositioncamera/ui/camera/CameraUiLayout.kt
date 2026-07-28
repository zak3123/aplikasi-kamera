package com.fatih.adaptivecompositioncamera.ui.camera

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fatih.adaptivecompositioncamera.utility.AdaptiveLayout

/** Shared camera chrome dimensions. Preview geometry is calculated separately from these tokens. */
object CameraUiTokens {
    val minimumTouchTarget: Dp = 56.dp
    val topIconSize: Dp = 24.dp
    val topVisualSize: Dp = 44.dp
    val topGap: Dp = 8.dp
    val topEdgeMargin: Dp = 16.dp
    val topLabelHorizontalPadding: Dp = 10.dp
    val topLabelVerticalPadding: Dp = 4.dp
    val lensTouchTarget: Dp = 64.dp
    val lensVisibleSize: Dp = 56.dp
    val lensActiveVisibleSize: Dp = 62.dp
    val lensGap: Dp = 12.dp
    val modeHorizontalGap: Dp = 28.dp
    val secondaryTouchTarget: Dp = 66.dp
    val secondaryControlSize: Dp = 58.dp
    val shutterTouchTarget: Dp = 104.dp
    val shutterOuterSize: Dp = 92.dp
    val shutterStroke: Dp = 4.5.dp
    val exposureSliderHeight: Dp = 218.dp
    val exposureControlWidth: Dp = 72.dp
    val exposureThumbSize: Dp = 26.dp
    val focusRingDiameter: Dp = 76.dp
    val controlGap: Dp = 10.dp
    val compactPadding: Dp = 10.dp
    val portraitControlsHeight: Dp = 238.dp
    val landscapeCaptureRailWidth: Dp = 118.dp
    val landscapeModeRailWidth: Dp = 72.dp
    val maximumPortraitControlsWidth: Dp = 620.dp
    val maximumTabletControlsWidth: Dp = 720.dp
}

data class CameraUiLayoutPolicy(
    val landscape: Boolean,
    val tablet: Boolean,
    val captureRailWidth: Dp,
    val controlsMaximumWidth: Dp,
)

fun cameraUiLayoutPolicy(layout: AdaptiveLayout): CameraUiLayoutPolicy = when (layout) {
    AdaptiveLayout.PhonePortrait -> CameraUiLayoutPolicy(
        landscape = false,
        tablet = false,
        captureRailWidth = 0.dp,
        controlsMaximumWidth = CameraUiTokens.maximumPortraitControlsWidth,
    )
    AdaptiveLayout.TabletPortrait -> CameraUiLayoutPolicy(
        landscape = false,
        tablet = true,
        captureRailWidth = 0.dp,
        controlsMaximumWidth = CameraUiTokens.maximumTabletControlsWidth,
    )
    AdaptiveLayout.PhoneLandscape -> CameraUiLayoutPolicy(
        landscape = true,
        tablet = false,
        captureRailWidth = CameraUiTokens.landscapeCaptureRailWidth + CameraUiTokens.landscapeModeRailWidth,
        controlsMaximumWidth = CameraUiTokens.landscapeCaptureRailWidth + CameraUiTokens.landscapeModeRailWidth,
    )
    AdaptiveLayout.TabletLandscape -> CameraUiLayoutPolicy(
        landscape = true,
        tablet = true,
        captureRailWidth = CameraUiTokens.landscapeCaptureRailWidth + CameraUiTokens.landscapeModeRailWidth,
        controlsMaximumWidth = CameraUiTokens.landscapeCaptureRailWidth + CameraUiTokens.landscapeModeRailWidth,
    )
}
