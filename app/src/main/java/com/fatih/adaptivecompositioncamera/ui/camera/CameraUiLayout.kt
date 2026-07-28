package com.fatih.adaptivecompositioncamera.ui.camera

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fatih.adaptivecompositioncamera.utility.AdaptiveLayout

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
    val landscapeCaptureRailWidth: Dp,
    val landscapeModeRailWidth: Dp,
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
        landscapeCaptureRailWidth = 104.dp,
        landscapeModeRailWidth = 64.dp,
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
    val landscapeCaptureRailWidth: Dp = active.landscapeCaptureRailWidth
    val landscapeModeRailWidth: Dp = active.landscapeModeRailWidth
    val maximumPortraitControlsWidth: Dp = active.maximumPortraitControlsWidth
    val maximumTabletControlsWidth: Dp = active.maximumTabletControlsWidth
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
