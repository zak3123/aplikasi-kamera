package com.fatih.adaptivecompositioncamera.ui.camera

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fatih.adaptivecompositioncamera.utility.AdaptiveLayout

/** Shared camera chrome dimensions. Preview geometry is calculated separately from these tokens. */
object CameraUiTokens {
    val minimumTouchTarget: Dp = 48.dp
    val topIconSize: Dp = 22.dp
    val topVisualSize: Dp = 38.dp
    val secondaryControlSize: Dp = 48.dp
    val shutterOuterSize: Dp = 78.dp
    val shutterStroke: Dp = 3.dp
    val controlGap: Dp = 6.dp
    val compactPadding: Dp = 6.dp
    val portraitControlsHeight: Dp = 164.dp
    val landscapeCaptureRailWidth: Dp = 98.dp
    val landscapeModeRailWidth: Dp = 66.dp
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
