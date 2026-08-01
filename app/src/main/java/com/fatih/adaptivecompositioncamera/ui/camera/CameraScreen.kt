package com.fatih.adaptivecompositioncamera.ui.camera

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.Surface as AndroidSurface
import androidx.camera.core.CameraSelector
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PhotoSizeSelectLarge
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fatih.adaptivecompositioncamera.camera.CameraRuntime
import com.fatih.adaptivecompositioncamera.capability.CameraConfigurationResolver
import com.fatih.adaptivecompositioncamera.capability.DefaultStabilizationResolver
import com.fatih.adaptivecompositioncamera.capability.ModeConflictResolver
import com.fatih.adaptivecompositioncamera.capability.StabilizationRequestPlan
import com.fatih.adaptivecompositioncamera.composition.CompositionGuideOverlay
import com.fatih.adaptivecompositioncamera.composition.rememberLevelReading
import com.fatih.adaptivecompositioncamera.domain.model.AppSettings
import com.fatih.adaptivecompositioncamera.domain.model.CameraCapability
import com.fatih.adaptivecompositioncamera.domain.model.CameraDiagnostics
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.CameraUiState
import com.fatih.adaptivecompositioncamera.domain.model.CapabilityReport
import com.fatih.adaptivecompositioncamera.domain.model.CompositionGuide
import com.fatih.adaptivecompositioncamera.domain.model.FlashMode
import com.fatih.adaptivecompositioncamera.domain.model.GuideStyle
import com.fatih.adaptivecompositioncamera.domain.model.ExtensionSupport
import com.fatih.adaptivecompositioncamera.domain.model.LensFacing
import com.fatih.adaptivecompositioncamera.domain.model.LensRole
import com.fatih.adaptivecompositioncamera.domain.model.LevelReading
import com.fatih.adaptivecompositioncamera.domain.model.MediaItem
import com.fatih.adaptivecompositioncamera.domain.model.PhotoAspectRatio
import com.fatih.adaptivecompositioncamera.domain.model.RuntimeCameraInfo
import com.fatih.adaptivecompositioncamera.domain.model.StabilizationSupport
import com.fatih.adaptivecompositioncamera.domain.model.VideoFpsRange
import com.fatih.adaptivecompositioncamera.domain.model.VideoQualitySetting
import com.fatih.adaptivecompositioncamera.domain.model.VideoStabilizationMode
import com.fatih.adaptivecompositioncamera.media.AndroidMediaRepository
import com.fatih.adaptivecompositioncamera.utility.CameraMath
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(markerClass = [androidx.camera.core.ExperimentalLensFacing::class])
@Composable
fun CameraScreen(
    settings: AppSettings,
    report: CapabilityReport?,
    cameraPermissionGranted: Boolean,
    audioPermissionGranted: Boolean,
    legacyStoragePermissionGranted: Boolean,
    requestCameraPermission: () -> Unit,
    requestAudioPermission: () -> Unit,
    requestLegacyStoragePermission: () -> Unit,
    volumeShutterEvent: Int,
    onModeChange: (CameraMode) -> Unit,
    onGuideChange: (CompositionGuide) -> Unit,
    onCameraChange: (String) -> Unit,
    onResolutionChange: (String, String) -> Unit,
    onAspectRatioChange: (PhotoAspectRatio) -> Unit,
    onDiagnosticsChange: (CameraDiagnostics) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!cameraPermissionGranted) {
        PermissionPrompt(modifier, requestCameraPermission)
        return
    }

    val context = LocalContext.current
    val activity = context.findActivity()
    val configuration = LocalConfiguration.current
    val localView = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val runtime = remember { CameraRuntime(context.applicationContext) }
    val mediaRepository = remember { AndroidMediaRepository() }
    val configurationResolver = remember { CameraConfigurationResolver() }
    val modeConflictResolver = remember { ModeConflictResolver() }
    val stabilizationResolver = remember { DefaultStabilizationResolver() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val controlRotationTarget = rememberCameraControlRotationDegrees()
    val controlRotationDegrees by animateFloatAsState(
        targetValue = controlRotationTarget,
        label = "camera-control-rotation",
    )

    val availableCameras = remember(report) {
        report?.cameras.orEmpty().filter {
            it.isOpenable && it.supportsBackwardCompatible && it.jpegResolutions.isNotEmpty()
        }
    }
    val availableFacings = remember(availableCameras) { availableCameras.map { it.lensFacing }.toSet() }
    var activeCameraId by remember { mutableStateOf<String?>(null) }
    val activeCapability = remember(availableCameras, activeCameraId) {
        availableCameras.firstOrNull { it.cameraId == activeCameraId }
    }
    val lensFacing = activeCapability?.lensFacing ?: LensFacing.Rear
    val availableModes = remember(activeCapability) {
        activeCapability?.let(configurationResolver::availableModes).orEmpty().ifEmpty { listOf(CameraMode.Photo) }
    }
    val resolutions = activeCapability?.selectablePhotoResolutions.orEmpty()
    val modeResolutionIds = remember { mutableStateMapOf<String, String>() }
    val activeResolutionId = activeCapability?.cameraId?.let { cameraId ->
        modeResolutionIds[modeResolutionKey(cameraId, settings.mode)]
            ?: settings.selectedResolutionIds[cameraId]
    }
    var selectedResolution by remember(activeCapability?.cameraId, settings.mode, activeResolutionId, resolutions) {
        mutableStateOf(
            resolutions.firstOrNull { it.id == activeResolutionId }
                ?: resolutions.firstOrNull { it.recommended }
                ?: resolutions.firstOrNull(),
        )
    }
    fun selectResolutionForCurrentMode(resolution: CameraResolution, persistAsCameraDefault: Boolean = true) {
        selectedResolution = resolution
        activeCapability?.cameraId?.let { cameraId ->
            modeResolutionIds[modeResolutionKey(cameraId, settings.mode)] = resolution.id
            if (persistAsCameraDefault) onResolutionChange(cameraId, resolution.id)
        }
    }
    val dedicatedHighResolution = selectedResolution?.maximumSensorMode == true ||
        (selectedResolution?.highResolution == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val effectivePhotoAspect = if (dedicatedHighResolution) {
        PhotoAspectRatio.FullSensor
    } else {
        settings.photoAspectRatio
    }
    val isFront = lensFacing == LensFacing.Front
    val mirrorPreview = isFront && settings.mirrorFrontPreview
    val sessionState by runtime.state.collectAsState()
    val stabilizationStatus by runtime.stabilizationStatus.collectAsState()
    val stabilizationEvidence by runtime.stabilizationEvidence.collectAsState()
    val videoFpsOptions = remember(activeCapability) {
        activeCapability?.fpsRangeValues.orEmpty()
            .filter { it.max in setOf(24, 25, 30, 50, 60, 120) }
            .distinctBy { it.max }
            .sortedBy { it.max }
    }
    val videoStabilizationOptions = remember(activeCapability, settings.mode) {
        activeCapability?.let { capability ->
            stabilizationResolver.supportedModes(capability, settings.mode)
                .takeIf { modes -> modes.any { it != VideoStabilizationMode.Off } }
                ?: emptyList()
        } ?: emptyList()
    }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var captureViewportSize by remember { mutableStateOf(IntSize.Zero) }
    var cameraRebindToken by remember { mutableIntStateOf(0) }
    var runtimeInfo by remember { mutableStateOf(RuntimeCameraInfo()) }
    var videoQuality by remember(activeCameraId) { mutableStateOf(VideoQualitySetting.Auto) }
    var videoFpsRange by remember(activeCameraId) {
        mutableStateOf(
            videoFpsOptions.firstOrNull { it.max == 30 }
                ?: videoFpsOptions.lastOrNull(),
        )
    }
    var videoStabilization by remember(activeCameraId) {
        mutableStateOf(
            if (videoStabilizationOptions.any { it != VideoStabilizationMode.Unsupported }) {
                VideoStabilizationMode.Auto
            } else {
                VideoStabilizationMode.Unsupported
            },
        )
    }
    val stabilizationDecision = remember(activeCapability, settings.mode, videoStabilization) {
        activeCapability?.let { capability ->
            stabilizationResolver.resolve(
                requested = videoStabilization,
                capability = capability,
                mode = settings.mode,
            )
        }
    }
    val stabilizationRequestPlan = stabilizationDecision?.requestPlan
    val stabilizationPlanLabel = stabilizationRequestPlan?.uiLabel() ?: videoStabilization.shortLabel()
    val acceptedStabilizationLabel = stabilizationAcceptedShortLabel(
        status = stabilizationStatus,
        evidence = stabilizationEvidence,
        fallback = stabilizationPlanLabel,
    )
    var flashMode by remember { mutableStateOf(FlashMode.Off) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var countdown by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var showZoomSlider by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var exposure by remember { mutableIntStateOf(0) }
    var showExposure by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isRecordingPaused by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var flashVisible by remember { mutableStateOf(false) }
    var captureEffectVisible by remember { mutableStateOf(false) }
    var previousBrightness by remember { mutableStateOf<Float?>(null) }
    var latestMedia by remember { mutableStateOf<MediaItem?>(null) }
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var captureInProgress by remember { mutableStateOf(false) }
    var countdownGeneration by remember { mutableIntStateOf(0) }
    var lastCameraError by remember { mutableStateOf<String?>(null) }
    var lastCaptureError by remember { mutableStateOf<String?>(null) }
    var lastRecordingError by remember { mutableStateOf<String?>(null) }
    var actualSavedResolution by remember { mutableStateOf<String?>(null) }
    var configurationMismatch by remember { mutableStateOf<String?>(null) }
    var proIso by remember(activeCameraId) { mutableIntStateOf(100) }
    var proExposureNanos by remember(activeCameraId) { mutableLongStateOf(10_000_000L) }
    var proFocusDistance by remember(activeCameraId) { mutableFloatStateOf(0f) }
    var proManualExposure by remember(activeCameraId) { mutableStateOf(false) }
    var proManualFocus by remember(activeCameraId) { mutableStateOf(false) }
    var proWhiteBalance by remember(activeCameraId) { mutableIntStateOf(CameraMetadata.CONTROL_AWB_MODE_AUTO) }
    var proControl by remember { mutableStateOf(ProControl.Iso) }
    var proDetailsVisible by remember { mutableStateOf(false) }

    val estimatedOutputDimensions = selectedResolution?.let { source ->
        if (settings.matchPreviewCrop) {
            CameraMath.cropDimensions(
                source.width,
                source.height,
                effectivePhotoAspect,
                previewSize.width,
                previewSize.height,
            )
        } else source.width to source.height
    }
    val estimatedOutputLabel = estimatedOutputDimensions?.let { (width, height) ->
        val megapixels = CameraMath.megapixels(width, height)
        friendlyTopMegapixelLabel(megapixels)
    }

    var showResolutionSheet by remember { mutableStateOf(false) }
    var showAspectSheet by remember { mutableStateOf(false) }
    var showCompositionSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showVideoSettingsSheet by remember { mutableStateOf(false) }
    var showQuickSettings by remember { mutableStateOf(false) }
    val cameraUiState = CameraUiState(
        activeCaptureMode = settings.mode,
        sessionState = sessionState,
        activeCameraId = activeCameraId,
        selectedResolutionLabel = estimatedOutputLabel,
        stabilizationLabel = acceptedStabilizationLabel.takeIf { videoStabilizationOptions.isNotEmpty() },
        quickSettingsExpanded = showQuickSettings,
        moreSelectorVisible = showMoreSheet,
        compositionSelectorVisible = showCompositionSheet,
        proControlVisible = settings.mode == CameraMode.Pro && proDetailsVisible,
        documentModeActive = settings.mode == CameraMode.Documents,
    )
    var guideStyle by remember { mutableStateOf(GuideStyle()) }
    var previousPhotoGuide by remember {
        mutableStateOf(settings.guide.takeIf { it != CompositionGuide.None } ?: CompositionGuide.RuleOfThirds)
    }
    var previousMode by remember { mutableStateOf(settings.mode) }
    var vanishingPoint by remember { mutableStateOf(Offset(0.5f, 0.45f)) }
    var frameBounds by remember { mutableStateOf(Rect(0.18f, 0.20f, 0.82f, 0.80f)) }
    var eyeLineFraction by remember { mutableFloatStateOf(0.36f) }
    val rawLevelReading = rememberLevelReading(settings.guide == CompositionGuide.HorizonLevel)
    var levelCalibration by remember { mutableStateOf(LevelReading()) }
    val levelReading = rawLevelReading.copy(
        rollDegrees = rawLevelReading.rollDegrees - levelCalibration.rollDegrees,
        pitchDegrees = rawLevelReading.pitchDegrees - levelCalibration.pitchDegrees,
    )
    var wasLevel by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { latestMedia = mediaRepository.latestMedia(context) }
    LaunchedEffect(availableCameras, settings.selectedCameraId) {
        val persisted = availableCameras.firstOrNull { it.cameraId == settings.selectedCameraId }
        val current = availableCameras.firstOrNull { it.cameraId == activeCameraId }
        val preferred = availableCameras.sortedWith(
            compareBy<CameraCapability> { if (it.lensFacing == LensFacing.Rear) 0 else 1 }
                .thenBy { if (it.lensRole in listOf(LensRole.Wide, LensRole.Main)) 0 else 1 },
        ).firstOrNull()
        val resolved = persisted ?: current ?: preferred
        if (resolved != null && resolved.cameraId != activeCameraId) activeCameraId = resolved.cameraId
    }
    LaunchedEffect(latestMedia) {
        thumbnail = latestMedia?.let { mediaRepository.loadThumbnail(context, it) }
    }
    LaunchedEffect(settings.mode, availableModes) {
        if (settings.mode !in availableModes) onModeChange(CameraMode.Photo)
        if (settings.mode == CameraMode.MaximumResolution) {
            val maximumChoice = selectedResolution
                ?.takeIf { it.highResolution || it.maximumSensorMode }
                ?: activeCapability?.displayMaximumResolution
            if (maximumChoice == null) {
                onModeChange(CameraMode.Photo)
                onMessage("Ultra HD is unavailable for this camera.")
            } else {
                val it = maximumChoice
                selectResolutionForCurrentMode(it)
                if ((it.maximumSensorMode ||
                        (it.highResolution && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) &&
                    settings.photoAspectRatio != PhotoAspectRatio.FullSensor
                ) {
                    onAspectRatioChange(PhotoAspectRatio.FullSensor)
                    onMessage("High-resolution capture uses the full exposed JPEG output. Crops are disabled for this mode.")
                }
            }
        }
    }
    LaunchedEffect(settings.mode, settings.guide) {
        if (
            settings.mode != CameraMode.Documents &&
            settings.guide != CompositionGuide.None &&
            modeConflictResolver.isCompositionAllowed(settings.mode, settings.guide)
        ) {
            previousPhotoGuide = settings.guide
        }
        if (settings.mode == CameraMode.Documents) {
            showQuickSettings = false
            showCompositionSheet = false
            proDetailsVisible = false
            proManualExposure = false
            proManualFocus = false
            proWhiteBalance = CameraMetadata.CONTROL_AWB_MODE_AUTO
            runtime.resetProControls()
            if (settings.guide != CompositionGuide.None) onGuideChange(CompositionGuide.None)
        } else if (!modeConflictResolver.isCompositionAllowed(settings.mode, settings.guide)) {
            showQuickSettings = false
            showCompositionSheet = false
            onGuideChange(CompositionGuide.None)
        }
        if (
            previousMode == CameraMode.Documents &&
            settings.mode != CameraMode.Documents &&
            settings.guide == CompositionGuide.None &&
            previousPhotoGuide != CompositionGuide.None
        ) {
            onGuideChange(previousPhotoGuide)
        }
        previousMode = settings.mode
    }
    LaunchedEffect(activeCameraId, videoFpsOptions, videoStabilizationOptions) {
        if (videoFpsRange !in videoFpsOptions) {
            videoFpsRange = videoFpsOptions.firstOrNull { it.max == 30 } ?: videoFpsOptions.lastOrNull()
        }
        if (videoStabilizationOptions.isEmpty()) {
            videoStabilization = VideoStabilizationMode.Unsupported
        } else if (videoStabilization !in videoStabilizationOptions) {
            videoStabilization = if (VideoStabilizationMode.Auto in videoStabilizationOptions) {
                VideoStabilizationMode.Auto
            } else {
                videoStabilizationOptions.first()
            }
        }
    }
    LaunchedEffect(
        settings.mode,
        runtimeInfo.supportsManualSensor,
        runtimeInfo.isoMin,
        runtimeInfo.isoMax,
        runtimeInfo.exposureTimeMinNanos,
        runtimeInfo.exposureTimeMaxNanos,
        runtimeInfo.availableWhiteBalanceModes,
        runtimeInfo.targetRotation,
    ) {
        if (settings.mode == CameraMode.Pro && runtimeInfo.supportsManualSensor) {
            if (
                proControl == ProControl.WhiteBalance &&
                runtimeInfo.availableWhiteBalanceModes.size <= 1
            ) {
                proControl = ProControl.Iso
            }
            proIso = proIso.coerceIn(runtimeInfo.isoMin, runtimeInfo.isoMax)
            proExposureNanos = proExposureNanos.coerceIn(
                runtimeInfo.exposureTimeMinNanos,
                minOf(runtimeInfo.exposureTimeMaxNanos, 250_000_000L)
                    .coerceAtLeast(runtimeInfo.exposureTimeMinNanos),
            )
            if (proManualExposure) runtime.setManualExposure(proIso, proExposureNanos)
            if (proWhiteBalance in runtimeInfo.availableWhiteBalanceModes) {
                runtime.setWhiteBalance(proWhiteBalance)
            } else {
                proWhiteBalance = CameraMetadata.CONTROL_AWB_MODE_AUTO
            }
        } else {
            proManualExposure = false
            proManualFocus = false
            runtime.resetProControls()
            proWhiteBalance = CameraMetadata.CONTROL_AWB_MODE_AUTO
        }
    }
    LaunchedEffect(activeCameraId, selectedResolution?.id, effectivePhotoAspect, settings.matchPreviewCrop) {
        actualSavedResolution = null
        configurationMismatch = null
    }
    LaunchedEffect(levelReading.isLevel, settings.guide) {
        val aligned = settings.guide == CompositionGuide.HorizonLevel && levelReading.isLevel
        if (aligned && !wasLevel && settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        wasLevel = aligned
    }
    LaunchedEffect(isRecording) {
        recordingSeconds = 0
        while (isRecording) {
            delay(1_000)
            if (!isRecordingPaused) recordingSeconds++
        }
    }
    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(3_500)
            focusPoint = null
            showExposure = false
        }
    }
    LaunchedEffect(showZoomSlider, zoom) {
        if (showZoomSlider) {
            delay(3_000)
            showZoomSlider = false
        }
    }
    LaunchedEffect(proDetailsVisible, proControl, settings.mode) {
        if (settings.mode == CameraMode.Pro && proDetailsVisible) {
            delay(4_000)
            proDetailsVisible = false
        }
    }

    LaunchedEffect(
        previewView,
        previewSize,
        captureViewportSize,
        lensFacing,
        selectedResolution?.id,
        settings.mode,
        effectivePhotoAspect,
        settings.matchPreviewCrop,
        configuration.orientation,
        cameraRebindToken,
        videoQuality,
        videoFpsRange,
        videoStabilization,
    ) {
        val view = previewView ?: return@LaunchedEffect
        if (previewSize.width <= 0 || previewSize.height <= 0) return@LaunchedEffect
        val viewport = captureViewportSize.takeIf { it.width > 0 && it.height > 0 } ?: previewSize
        val selectorFacing = when (lensFacing) {
            LensFacing.Front -> CameraSelector.LENS_FACING_FRONT
            LensFacing.External -> CameraSelector.LENS_FACING_EXTERNAL
            else -> CameraSelector.LENS_FACING_BACK
        }
        runtime.bind(
            previewView = view,
            lifecycleOwner = lifecycleOwner,
            lensFacing = selectorFacing,
            cameraId = activeCapability?.cameraId,
            resolution = selectedResolution,
            mode = settings.mode,
            viewportWidth = viewport.width,
            viewportHeight = viewport.height,
            targetRotation = localView.display?.rotation ?: AndroidSurface.ROTATION_0,
            matchPreviewCrop = settings.matchPreviewCrop,
            videoQuality = videoQuality,
            videoFpsRange = videoFpsRange,
            videoStabilization = videoStabilization,
            stabilizationSupport = activeCapability?.stabilization
                ?: StabilizationSupport(false, false, false),
            onBound = {
                runtimeInfo = it
                if (!dedicatedHighResolution) {
                    val accepted = resolutions.firstOrNull { resolution ->
                        resolution.width == it.captureWidth && resolution.height == it.captureHeight
                    }
                    if (settings.mode != CameraMode.Video && selectedResolution != null && accepted == null && it.captureWidth > 0) {
                        configurationMismatch = "Requested ${selectedResolution!!.width} x ${selectedResolution!!.height}, but CameraX bound ${it.captureWidth} x ${it.captureHeight}."
                    }
                    accepted?.let { actual ->
                        if (selectedResolution?.id != actual.id) {
                            configurationMismatch = "Requested ${selectedResolution?.width} x ${selectedResolution?.height}, but CameraX bound ${actual.width} x ${actual.height}."
                        }
                        selectResolutionForCurrentMode(actual, persistAsCameraDefault = settings.mode != CameraMode.Pro)
                    }
                }
                zoom = it.minZoom.coerceAtLeast(1f).coerceAtMost(it.maxZoom)
                exposure = 0.coerceIn(it.exposureMin, it.exposureMax)
                flashMode = FlashMode.Off
            },
            onError = {
                lastCameraError = it
                if (it.contains("safe photo configuration was restored", ignoreCase = true)) {
                    onModeChange(CameraMode.Photo)
                }
                onMessage(it)
            },
        )
    }

    LaunchedEffect(
        sessionState,
        activeCapability,
        settings.mode,
        selectedResolution,
        runtimeInfo,
        lastCameraError,
        lastCaptureError,
        lastRecordingError,
        actualSavedResolution,
        configurationMismatch,
        effectivePhotoAspect,
        stabilizationStatus,
    ) {
        onDiagnosticsChange(
            CameraDiagnostics(
                currentCameraId = activeCapability?.cameraId,
                currentCameraName = activeCapability?.friendlyName,
                sessionState = sessionState.javaClass.simpleName,
                mode = settings.mode,
                activeResolution = if (settings.mode == CameraMode.Video && runtimeInfo.videoWidth > 0) {
                    "${runtimeInfo.videoWidth} × ${runtimeInfo.videoHeight} (${videoQualityLabel(runtimeInfo.videoWidth, runtimeInfo.videoHeight)})"
                } else estimatedOutputDimensions?.let { (width, height) ->
                    "$width × $height ($estimatedOutputLabel ${if (effectivePhotoAspect == PhotoAspectRatio.FullSensor) "native" else "crop"})"
                },
                requestedResolution = selectedResolution?.let { "${it.width} × ${it.height} (${it.megapixelLabel})" },
                boundCaptureResolution = runtimeInfo.captureWidth.takeIf { it > 0 }
                    ?.let { "${runtimeInfo.captureWidth} × ${runtimeInfo.captureHeight}" },
                actualSavedResolution = actualSavedResolution,
                selectedAspectRatio = effectivePhotoAspect.label(),
                sensorPixelMode = runtimeInfo.sensorPixelMode,
                configurationMismatch = configurationMismatch,
                previewResolution = runtimeInfo.previewWidth.takeIf { it > 0 }
                    ?.let { width -> "$width × ${runtimeInfo.previewHeight}" },
                currentFps = runtimeInfo.requestedFpsRange?.label
                    ?: activeCapability?.fpsRanges?.joinToString(limit = 3),
                stabilization = if (settings.mode == CameraMode.Video) {
                    "$stabilizationStatus | $stabilizationEvidence"
                } else {
                    stabilizationStatus
                },
                requestedVideoQuality = runtimeInfo.selectedVideoQuality.name,
                supportedVideoQualities = runtimeInfo.supportedVideoQualities.joinToString { it.name },
                requestedFps = runtimeInfo.requestedFpsRange?.label,
                extension = activeCapability?.extensions?.activeLabel() ?: "None",
                lastCameraError = lastCameraError,
                lastCaptureError = lastCaptureError,
                lastRecordingError = lastRecordingError,
            ),
        )
    }

    DisposableEffect(previewView) {
        val view = previewView
        if (view == null) return@DisposableEffect onDispose { }
        val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                zoom = runtime.zoomTo(zoom * detector.scaleFactor)
                showZoomSlider = true
                return true
            }
        })
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true
            override fun onSingleTapUp(event: MotionEvent): Boolean {
                focusPoint = Offset(event.x, event.y)
                showExposure = runtimeInfo.exposureMin != runtimeInfo.exposureMax
                runtime.focusAt(view, event.x, event.y)
                return true
            }
            override fun onDoubleTap(event: MotionEvent): Boolean {
                exposure = runtime.setExposure(0)
                focusPoint = Offset(event.x, event.y)
                showExposure = runtimeInfo.exposureMin != runtimeInfo.exposureMax
                if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                return true
            }
        })
        view.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) view.performClick()
            true
        }
        onDispose { view.setOnTouchListener(null) }
    }

    DisposableEffect(Unit) {
        onDispose {
            previousBrightness?.let { brightness -> activity?.setScreenBrightness(brightness) }
            runtime.release()
        }
    }

    fun capturePhoto() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !legacyStoragePermissionGranted) {
            requestLegacyStoragePermission()
            onMessage("Storage access is required to save photos on Android 9 and earlier.")
            return
        }
        if (countdown > 0) {
            countdownGeneration++
            countdown = 0
            onMessage("Timer cancelled")
            return
        }
        if (captureInProgress || isRecording) return
        val generation = ++countdownGeneration
        scope.launch {
            try {
                if (timerSeconds > 0) {
                    countdown = timerSeconds
                    while (countdown > 0) {
                        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        delay(1_000)
                        if (generation != countdownGeneration) return@launch
                        countdown--
                    }
                }
                captureInProgress = true
                if (isFront && settings.screenFlash && flashMode != FlashMode.Off) {
                    previousBrightness = activity?.window?.attributes?.screenBrightness
                    activity?.setScreenBrightness(1f)
                    flashVisible = true
                    delay(220)
                }
                captureEffectVisible = true
                delay(70)
                captureEffectVisible = false
                runtime.capturePhoto(
                    reverseHorizontal = isFront && settings.saveMirroredSelfie,
                    cameraId = activeCapability?.cameraId,
                    resolution = selectedResolution,
                    targetRotation = localView.display?.rotation ?: AndroidSurface.ROTATION_0,
                    flashMode = flashMode,
                )
                    .onSuccess { uri ->
                        val captured = mediaRepository.mediaItem(context, uri, "image/jpeg")?.copy(
                            cameraId = activeCapability?.cameraId,
                            requestedResolution = selectedResolution?.let { "${it.width} x ${it.height}" },
                            boundResolution = if (dedicatedHighResolution) {
                                selectedResolution?.let { "${it.width} x ${it.height} Camera2 high resolution" }
                            } else {
                                runtimeInfo.captureWidth.takeIf { it > 0 }
                                    ?.let { "${runtimeInfo.captureWidth} x ${runtimeInfo.captureHeight}" }
                            },
                        )
                        latestMedia = captured
                        captured?.takeIf { it.width > 0 && it.height > 0 }?.let { media ->
                            val actualMp = CameraMath.megapixels(media.width, media.height)
                            actualSavedResolution = "${media.width} × ${media.height} ($actualMp MP)"
                            estimatedOutputDimensions?.let { (expectedWidth, expectedHeight) ->
                                val expectedMp = CameraMath.megapixels(expectedWidth, expectedHeight)
                                val actualRatio = maxOf(media.width, media.height).toFloat() / minOf(media.width, media.height)
                                val expectedRatio = maxOf(expectedWidth, expectedHeight).toFloat() / minOf(expectedWidth, expectedHeight)
                                if (actualMp < expectedMp * 0.80 || abs(actualRatio - expectedRatio) > 0.035f) {
                                    configurationMismatch =
                                        "Requested output $expectedWidth x $expectedHeight ($expectedMp MP), but the saved JPEG is ${media.width} x ${media.height} ($actualMp MP)."
                                }
                            }
                        }
                        lastCaptureError = null
                        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMessage(configurationMismatch ?: actualSavedResolution?.let { "Photo saved - $it" } ?: "Photo saved")
                    }
                    .onFailure {
                        lastCaptureError = it.message ?: "Photo capture failed."
                        if (dedicatedHighResolution) {
                            activeCapability?.normalMaximumResolution?.let { fallback ->
                                selectResolutionForCurrentMode(fallback)
                                onModeChange(CameraMode.Photo)
                                configurationMismatch =
                                    "High-resolution capture failed: $lastCaptureError. " +
                                        "Restored ${fallback.width} x ${fallback.height} (${fallback.megapixelLabel})."
                            }
                        }
                        onMessage(configurationMismatch ?: lastCaptureError!!)
                    }
            } finally {
                if (dedicatedHighResolution) cameraRebindToken++
                captureInProgress = false
                captureEffectVisible = false
                flashVisible = false
                previousBrightness?.let { activity?.setScreenBrightness(it) }
                previousBrightness = null
                countdown = 0
            }
        }
    }

    fun toggleVideo() {
        if (isRecording) {
            runtime.stopVideo()
            return
        }
        if (settings.audioEnabled && !audioPermissionGranted) {
            requestAudioPermission()
            onMessage("Allow microphone access, or disable video audio in Settings.")
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !legacyStoragePermissionGranted) {
            requestLegacyStoragePermission()
            onMessage("Storage access is required to save videos on Android 9 and earlier.")
            return
        }
        scope.launch {
            isRecording = true
            isRecordingPaused = false
            runtime.startVideo(settings.audioEnabled)
                .catch {
                    isRecording = false
                    lastRecordingError = it.message ?: "Video recording failed."
                    onMessage(lastRecordingError!!)
                }
                .collect { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        isRecording = false
                        isRecordingPaused = false
                        if (event.hasError()) {
                            lastRecordingError = "Recording failed with error ${event.error}."
                            onMessage(lastRecordingError!!)
                        } else {
                            lastRecordingError = null
                            latestMedia = mediaRepository.mediaItem(context, event.outputResults.outputUri, "video/mp4")
                            onMessage("Video saved")
                        }
                    }
                }
        }
    }

    fun selectMode(mode: CameraMode) {
        val resolved = modeConflictResolver.resolveModeChange(
            currentMode = settings.mode,
            requestedMode = mode,
            currentGuide = settings.guide,
            previousPhotoGuide = previousPhotoGuide,
        )
        showMoreSheet = false
        showQuickSettings = false
        if (resolved.closeCompositionSelector) showCompositionSheet = false
        if (mode == CameraMode.Pro && selectedResolution?.let { it.highResolution || it.maximumSensorMode } == true) {
            onMessage("Pro keeps the current resolution. If this camera rejects it, choose another Pro resolution manually.")
        }
        if (resolved.closeProControls) {
            proManualExposure = false
            proManualFocus = false
            proDetailsVisible = false
            proWhiteBalance = CameraMetadata.CONTROL_AWB_MODE_AUTO
            runtime.resetProControls()
        }
        if (settings.guide != resolved.guide) onGuideChange(resolved.guide)
        resolved.reason?.let(onMessage)
        onModeChange(resolved.mode)
    }

    var lastVolumeShutterEvent by remember { mutableIntStateOf(volumeShutterEvent) }
    LaunchedEffect(volumeShutterEvent) {
        if (volumeShutterEvent != lastVolumeShutterEvent) {
            lastVolumeShutterEvent = volumeShutterEvent
            if (!settings.volumeShutter) return@LaunchedEffect
            if (settings.mode == CameraMode.Video) toggleVideo() else capturePhoto()
        }
    }

    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        val layout = CameraMath.adaptiveLayout(maxWidth.value.toInt(), maxHeight.value.toInt())
        val policy = cameraUiLayoutPolicy(layout)
        val guideTop = if (policy.landscape) 10.dp else 72.dp
        val guideBottom = if (policy.landscape) 10.dp else CameraUiTokens.portraitControlsHeight + 10.dp
        val guideEnd = if (policy.landscape) policy.captureRailWidth + 8.dp else 6.dp
        val guideStart = 6.dp
        val fullLandscapeRatio = maxOf(maxWidth.value, maxHeight.value) /
            minOf(maxWidth.value, maxHeight.value).coerceAtLeast(1f)
        val targetAspect = if (settings.mode == CameraMode.Video) {
            val ratio = if (runtimeInfo.videoWidth > 0 && runtimeInfo.videoHeight > 0) {
                maxOf(runtimeInfo.videoWidth, runtimeInfo.videoHeight).toFloat() /
                    minOf(runtimeInfo.videoWidth, runtimeInfo.videoHeight)
            } else 16f / 9f
            if (policy.landscape) ratio else 1f / ratio
        } else {
            CameraMath.previewAspectRatio(
                selectedResolution?.width ?: 4,
                selectedResolution?.height ?: 3,
                effectivePhotoAspect,
                policy.landscape,
                fullLandscapeRatio,
            )
        }
        val fullScreenFrame =
            settings.mode != CameraMode.Video && effectivePhotoAspect == PhotoAspectRatio.FullScreen
        val availableFrame = com.fatih.adaptivecompositioncamera.utility.FloatBounds(
            guideStart.value,
            guideTop.value,
            maxWidth.value - guideEnd.value,
            maxHeight.value - guideBottom.value,
        )
        val localFrame = CameraMath.fitAspectRatio(
            availableFrame.width,
            availableFrame.height,
            targetAspect,
        )
        val fittedFrame = com.fatih.adaptivecompositioncamera.utility.FloatBounds(
            availableFrame.left + localFrame.left,
            availableFrame.top + localFrame.top,
            availableFrame.left + localFrame.right,
            availableFrame.top + localFrame.bottom,
        )
        val requestedViewport = if (targetAspect >= 1f) {
            IntSize((targetAspect * 1_000f).toInt(), 1_000)
        } else {
            IntSize(1_000, (1_000f / targetAspect).toInt())
        }
        LaunchedEffect(requestedViewport) { captureViewportSize = requestedViewport }
        val frameModifier = Modifier
            .offset(fittedFrame.left.dp, fittedFrame.top.dp)
            .size(fittedFrame.width.dp, fittedFrame.height.dp)
            .clipToBounds()
        val frameRectPixels = Rect(
            with(density) { fittedFrame.left.dp.toPx() },
            with(density) { fittedFrame.top.dp.toPx() },
            with(density) { fittedFrame.right.dp.toPx() },
            with(density) { fittedFrame.bottom.dp.toPx() },
        )

        Box(Modifier.fillMaxSize().onSizeChanged { previewSize = it }) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewContext ->
                    PreviewView(previewContext).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView = this
                    }
                },
                update = { view ->
                    view.scaleX = if (isFront && !settings.mirrorFrontPreview) -1f else 1f
                },
            )

            CaptureFrameOverlay(
                frame = frameRectPixels,
                visible = settings.mode == CameraMode.Documents && !fullScreenFrame,
                modifier = Modifier.fillMaxSize(),
            )

            Box(frameModifier) {
                if (settings.mode == CameraMode.Documents) {
                    DocumentGuideOverlay(Modifier.fillMaxSize())
                } else {
                    CompositionGuideOverlay(
                        guide = settings.guide,
                        mirrored = mirrorPreview,
                        style = guideStyle,
                        levelReading = levelReading,
                        vanishingPoint = vanishingPoint,
                        frameBounds = frameBounds,
                        eyeLineFraction = eyeLineFraction,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (settings.mode != CameraMode.Documents && !guideStyle.overlayLocked) {
                    InteractiveGuideLayer(
                        guide = settings.guide,
                        vanishingPoint = vanishingPoint,
                        frameBounds = frameBounds,
                        eyeLineFraction = eyeLineFraction,
                        onVanishingPoint = { vanishingPoint = it },
                        onFrameBounds = { frameBounds = it },
                        onEyeLine = { eyeLineFraction = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            focusPoint?.let { point ->
                FocusIndicator(point, Modifier.fillMaxSize())
                AnimatedVisibility(visible = showExposure) {
                    PocoExposureControl(
                        focusPoint = point,
                        exposureIndex = exposure,
                        minExposure = runtimeInfo.exposureMin,
                        maxExposure = runtimeInfo.exposureMax,
                        exposureStep = runtimeInfo.exposureStep,
                        onExposure = { next ->
                            val applied = runtime.setExposure(next)
                            if (applied != exposure && settings.haptics) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            exposure = applied
                        },
                        onReset = {
                            exposure = runtime.setExposure(0)
                            if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        if (!isRecording && !captureInProgress) {
            PocoStyleTopControls(
                modifier = (if (policy.landscape) Modifier.align(Alignment.TopStart) else Modifier.align(Alignment.TopCenter))
                    .statusBarsPadding().displayCutoutPadding().padding(horizontal = 8.dp, vertical = 6.dp),
                hasFlash = runtimeInfo.hasFlash || (isFront && settings.screenFlash),
                flashMode = flashMode,
                onFlash = {
                    flashMode = if (settings.mode == CameraMode.Video) {
                        if (flashMode == FlashMode.Torch) FlashMode.Off else FlashMode.Torch
                    } else flashMode.next(runtimeInfo.hasFlash)
                    if (runtimeInfo.hasFlash && !runtime.setFlashMode(flashMode)) onMessage("Flash mode is unavailable for this camera.")
                },
                timerSeconds = timerSeconds,
                onTimer = { timerSeconds = timerSeconds.nextTimer() },
                quickSettingsExpanded = cameraUiState.quickSettingsExpanded,
                onQuickSettings = { showQuickSettings = !showQuickSettings },
                captureFormatControlsVisible = !cameraUiState.documentModeActive,
                aspectRatioLabel = if (cameraUiState.activeCaptureMode == CameraMode.Video) {
                    runtimeInfo.requestedFpsRange?.let { "${it.max} FPS" } ?: "FPS"
                } else effectivePhotoAspect.shortLabel(),
                resolutionLabel = if (cameraUiState.activeCaptureMode == CameraMode.Video) {
                    runtimeInfo.selectedVideoQuality.displayLabel()
                } else if (configurationMismatch != null) {
                    actualSavedResolution?.substringAfter('(')?.substringBefore(')') ?: estimatedOutputLabel
                } else estimatedOutputLabel,
                onAspectRatio = {
                    if (settings.mode == CameraMode.Video) {
                        showVideoSettingsSheet = true
                    } else if (dedicatedHighResolution) {
                        onMessage("High-resolution capture always saves the full Android-exposed JPEG output.")
                    } else if (resolutions.isNotEmpty()) showAspectSheet = true
                },
                onResolution = {
                    if (settings.mode == CameraMode.Video) {
                        showVideoSettingsSheet = true
                    } else if (resolutions.isNotEmpty()) showResolutionSheet = true
                },
                stabilizationLabel = cameraUiState.stabilizationLabel?.takeIf { videoStabilizationOptions.isNotEmpty() },
                onStabilization = { showVideoSettingsSheet = true },
                compositionVisible = !cameraUiState.documentModeActive,
                compositionActive = !cameraUiState.documentModeActive && settings.guide != CompositionGuide.None,
                onComposition = { showCompositionSheet = true },
                onSettings = onOpenSettings,
                controlRotationDegrees = controlRotationDegrees,
            )
        }

        AnimatedVisibility(
            visible = showQuickSettings && !isRecording && !captureInProgress,
            modifier = (if (policy.landscape) Modifier.align(Alignment.TopStart) else Modifier.align(Alignment.TopCenter))
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(top = if (policy.landscape) 58.dp else 62.dp, start = 8.dp, end = 8.dp),
        ) {
            PocoStyleQuickSettings(
                hasAspectRatio = settings.mode != CameraMode.Documents,
                hasResolution = settings.mode != CameraMode.Documents,
                hasComposition = settings.mode != CameraMode.Documents,
                hasStabilization = videoStabilizationOptions.isNotEmpty(),
                aspectRatioLabel = if (settings.mode == CameraMode.Video) "FPS" else effectivePhotoAspect.shortLabel(),
                resolutionLabel = if (settings.mode == CameraMode.Video) {
                    runtimeInfo.selectedVideoQuality.displayLabel()
                } else estimatedOutputLabel,
                stabilizationLabel = cameraUiState.stabilizationLabel,
                onAspectRatio = {
                    showQuickSettings = false
                    if (settings.mode == CameraMode.Video) showVideoSettingsSheet = true else showAspectSheet = true
                },
                onResolution = {
                    showQuickSettings = false
                    if (settings.mode == CameraMode.Video) showVideoSettingsSheet = true else showResolutionSheet = true
                },
                onComposition = {
                    showQuickSettings = false
                    showCompositionSheet = true
                },
                onStabilization = {
                    showQuickSettings = false
                    showVideoSettingsSheet = true
                },
                onSettings = {
                    showQuickSettings = false
                    onOpenSettings()
                },
                controlRotationDegrees = controlRotationDegrees,
            )
        }

        if (isRecording) {
            RecordingTimer(
                seconds = recordingSeconds,
                paused = isRecordingPaused,
                onPauseResume = {
                    isRecordingPaused = if (isRecordingPaused) {
                        if (runtime.resumeVideo()) false else true
                    } else {
                        if (runtime.pauseVideo()) true else false
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 10.dp),
            )
        }

        if (countdown > 0) {
            Text(countdown.toString(), color = Color.White, fontSize = 86.sp, modifier = Modifier.align(Alignment.Center))
        }

        if (settings.mode == CameraMode.Pro && !isRecording && !captureInProgress) {
            PocoStyleProControls(
                runtimeInfo = runtimeInfo,
                activeControl = proControl,
                detailsVisible = proDetailsVisible,
                iso = proIso,
                exposureNanos = proExposureNanos,
                focusDistance = proFocusDistance,
                exposureCompensation = exposure,
                manualExposure = proManualExposure,
                manualFocus = proManualFocus,
                whiteBalanceMode = proWhiteBalance,
                onControl = {
                    proControl = it
                    proDetailsVisible = true
                },
                onIso = {
                    proIso = it
                    proManualExposure = true
                    runtime.setManualExposure(proIso, proExposureNanos)
                },
                onExposure = {
                    proExposureNanos = it
                    proManualExposure = true
                    runtime.setManualExposure(proIso, proExposureNanos)
                },
                onFocus = {
                    proFocusDistance = it
                    proManualFocus = true
                    runtime.setManualFocus(it)
                },
                onExposureCompensation = {
                    proManualExposure = false
                    runtime.resetProControls()
                    exposure = runtime.setExposure(it)
                },
                onWhiteBalance = {
                    proWhiteBalance = it
                    runtime.setWhiteBalance(it)
                },
                onAuto = {
                    proManualExposure = false
                    proManualFocus = false
                    proDetailsVisible = false
                    exposure = runtime.setExposure(0)
                    proWhiteBalance = CameraMetadata.CONTROL_AWB_MODE_AUTO
                    runtime.resetProControls()
                },
                modifier = if (policy.landscape) {
                    Modifier.align(Alignment.BottomCenter)
                        .padding(
                            start = 0.dp,
                            top = 0.dp,
                            end = policy.captureRailWidth + 12.dp,
                            bottom = 10.dp,
                        )
                        .widthIn(max = 620.dp)
                } else {
                    Modifier.align(Alignment.BottomCenter)
                        .padding(
                            start = 12.dp,
                            top = 0.dp,
                            end = 12.dp,
                            bottom = CameraUiTokens.portraitControlsHeight + 8.dp,
                        )
                        .widthIn(max = 620.dp)
                },
            )
        }

        PocoStyleShutterControls(
            modifier = if (policy.landscape) {
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(policy.captureRailWidth)
                    .navigationBarsPadding().displayCutoutPadding()
            } else {
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .widthIn(max = policy.controlsMaximumWidth).navigationBarsPadding()
            },
            landscape = policy.landscape,
            activeMode = cameraUiState.activeCaptureMode,
            availableModes = availableModes,
            availableCameras = availableCameras.filter {
                it.lensFacing == lensFacing || it.lensFacing == LensFacing.External
            },
            activeCameraId = activeCameraId,
            maxResolution = activeCapability?.displayMaximumResolution,
            zoom = zoom,
            minZoom = runtimeInfo.minZoom,
            maxZoom = runtimeInfo.maxZoom,
            showZoomSlider = showZoomSlider,
            onToggleZoomSlider = { showZoomSlider = !showZoomSlider },
            onZoom = { zoom = runtime.zoomTo(it) },
            latestThumbnail = thumbnail,
            hasLatestMedia = latestMedia != null,
            onLatestMedia = { latestMedia?.let(onOpenMedia) },
            isRecording = isRecording,
            canSwitch = LensFacing.Front in availableFacings && LensFacing.Rear in availableFacings &&
                !isRecording && !captureInProgress,
            onSwitch = {
                val target = preferredCamera(availableCameras, if (isFront) LensFacing.Rear else LensFacing.Front)
                if (target != null) {
                    activeCameraId = target.cameraId
                    onCameraChange(target.cameraId)
                }
                zoom = 1f
                focusPoint = null
                showExposure = false
            },
            onCamera = { camera ->
                if (!captureInProgress) {
                    activeCameraId = camera.cameraId
                    onCameraChange(camera.cameraId)
                    zoom = 1f
                    focusPoint = null
                    showExposure = false
                }
            },
            onMode = { if (!isRecording && !captureInProgress) selectMode(it) },
            onMore = { if (!isRecording && !captureInProgress) showMoreSheet = true },
            onShutter = { if (settings.mode == CameraMode.Video) toggleVideo() else capturePhoto() },
            controlRotationDegrees = controlRotationDegrees,
        )

        if (flashVisible) Box(Modifier.fillMaxSize().background(Color.White))
        else if (captureEffectVisible) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.46f)))
    }

    if (showResolutionSheet) {
        ResolutionSheet(
            resolutions = resolutions,
            selected = selectedResolution,
            reportedMaximum = activeCapability?.maximumExposedResolution,
            onSelect = {
                selectResolutionForCurrentMode(it, persistAsCameraDefault = settings.mode != CameraMode.Pro)
                if (it.highResolution || it.maximumSensorMode) {
                    onModeChange(CameraMode.MaximumResolution)
                    if ((it.maximumSensorMode ||
                            (it.highResolution && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) &&
                        settings.photoAspectRatio != PhotoAspectRatio.FullSensor
                    ) {
                        onAspectRatioChange(PhotoAspectRatio.FullSensor)
                        onMessage("High-resolution capture uses full sensor output and briefly restarts the preview.")
                    }
                }
                else if (settings.mode == CameraMode.MaximumResolution) onModeChange(CameraMode.Photo)
            },
            onDismiss = { showResolutionSheet = false },
        )
    }
    if (showAspectSheet) {
        AspectRatioSheet(
            sourceResolution = selectedResolution,
            selected = effectivePhotoAspect,
            viewportWidth = previewSize.width,
            viewportHeight = previewSize.height,
            onSelect = onAspectRatioChange,
            onDismiss = { showAspectSheet = false },
        )
    }
    if (showVideoSettingsSheet) {
        VideoSettingsSheet(
            mode = settings.mode,
            supportedQualities = runtimeInfo.supportedVideoQualities,
            selectedQuality = videoQuality,
            fpsRanges = videoFpsOptions,
            selectedFpsRange = videoFpsRange,
            stabilizationModes = videoStabilizationOptions,
            selectedStabilization = videoStabilization,
            stabilizationStatus = stabilizationStatus,
            stabilizationEvidence = stabilizationEvidence,
            stabilizationPlan = stabilizationRequestPlan,
            effectiveStabilizationLabel = stabilizationPlanLabel,
            acceptedStabilizationLabel = acceptedStabilizationLabel,
            onQuality = { videoQuality = it },
            onFps = { videoFpsRange = it },
            onStabilization = { videoStabilization = it },
            onDismiss = { showVideoSettingsSheet = false },
        )
    }
    if (showCompositionSheet) {
        CompositionSheet(
            selected = settings.guide,
            style = guideStyle,
            onSelect = onGuideChange,
            onStyle = { guideStyle = it },
            onResetInteractive = {
                vanishingPoint = Offset(0.5f, 0.45f)
                frameBounds = Rect(0.18f, 0.20f, 0.82f, 0.80f)
                eyeLineFraction = 0.36f
                levelCalibration = LevelReading()
            },
            onVanishingPreset = { vanishingPoint = it },
            onFramePreset = { frameBounds = it },
            onEyeLinePreset = { eyeLineFraction = it },
            onLevelCalibrate = { levelCalibration = rawLevelReading },
            onDismiss = { showCompositionSheet = false },
        )
    }
    if (showMoreSheet) {
        PocoStyleMoreScreen(
            modes = availableModes,
            activeMode = settings.mode,
            maxResolution = activeCapability?.displayMaximumResolution,
            onSelect = ::selectMode,
            onDismiss = { showMoreSheet = false },
        )
    }
}

@Composable
private fun CaptureFrameOverlay(
    frame: Rect,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    Canvas(modifier) {
        val safeFrame = Rect(
            left = frame.left.coerceIn(0f, size.width),
            top = frame.top.coerceIn(0f, size.height),
            right = frame.right.coerceIn(0f, size.width),
            bottom = frame.bottom.coerceIn(0f, size.height),
        )
        if (safeFrame.width <= 0f || safeFrame.height <= 0f) return@Canvas
        val shade = Color.Black.copy(alpha = 0.10f)
        if (safeFrame.top > 0f) drawRect(shade, size = Size(size.width, safeFrame.top))
        if (safeFrame.bottom < size.height) {
            drawRect(
                shade,
                topLeft = Offset(0f, safeFrame.bottom),
                size = Size(size.width, size.height - safeFrame.bottom),
            )
        }
        if (safeFrame.left > 0f) {
            drawRect(
                shade,
                topLeft = Offset(0f, safeFrame.top),
                size = Size(safeFrame.left, safeFrame.height),
            )
        }
        if (safeFrame.right < size.width) {
            drawRect(
                shade,
                topLeft = Offset(safeFrame.right, safeFrame.top),
                size = Size(size.width - safeFrame.right, safeFrame.height),
            )
        }

        val corner = (minOf(safeFrame.width, safeFrame.height) * 0.055f)
            .coerceIn(14.dp.toPx(), 28.dp.toPx())
        val outlineWidth = 3.2.dp.toPx()
        val lineWidth = 1.25.dp.toPx()
        val corners = listOf(
            Triple(Offset(safeFrame.left, safeFrame.top), Offset(corner, 0f), Offset(0f, corner)),
            Triple(Offset(safeFrame.right, safeFrame.top), Offset(-corner, 0f), Offset(0f, corner)),
            Triple(Offset(safeFrame.left, safeFrame.bottom), Offset(corner, 0f), Offset(0f, -corner)),
            Triple(Offset(safeFrame.right, safeFrame.bottom), Offset(-corner, 0f), Offset(0f, -corner)),
        )
        corners.forEach { (origin, horizontal, vertical) ->
            drawLine(Color.Black.copy(alpha = 0.58f), origin, origin + horizontal, outlineWidth, StrokeCap.Round)
            drawLine(Color.Black.copy(alpha = 0.58f), origin, origin + vertical, outlineWidth, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.78f), origin, origin + horizontal, lineWidth, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.78f), origin, origin + vertical, lineWidth, StrokeCap.Round)
        }
    }
}

internal enum class ProControl { Iso, Shutter, WhiteBalance, Focus, Exposure }

internal fun modeResolutionKey(cameraId: String, mode: CameraMode): String = "$cameraId:${mode.name}:${mode.resolutionScope()}"

internal fun documentAnalysisResolutionKey(cameraId: String): String = "$cameraId:${CameraMode.Documents.name}:analysis"

private fun CameraMode.resolutionScope(): String = when (this) {
    CameraMode.Video,
    CameraMode.SlowMotion,
    CameraMode.HighFrameRate,
    CameraMode.TimeLapse,
    -> "video"
    CameraMode.Documents -> "document-final"
    CameraMode.Pro -> "pro-photo"
    CameraMode.MaximumResolution -> "maximum-photo"
    else -> "photo"
}

internal fun ProControl.shortLabel(): String = when (this) {
    ProControl.Iso -> "ISO"
    ProControl.Shutter -> "S"
    ProControl.WhiteBalance -> "WB"
    ProControl.Focus -> "MF"
    ProControl.Exposure -> "EV"
}

internal fun ProControl.valueLabel(
    iso: Int,
    exposureNanos: Long,
    focusDistance: Float,
    exposureCompensation: Int,
    manualExposure: Boolean,
    manualFocus: Boolean,
    whiteBalanceMode: Int,
): String = when (this) {
    ProControl.Iso -> if (manualExposure) iso.toString() else "A"
    ProControl.Shutter -> if (manualExposure) shutterLabel(exposureNanos) else "A"
    ProControl.WhiteBalance -> whiteBalanceLabel(whiteBalanceMode)
    ProControl.Focus -> if (!manualFocus) "AF" else if (focusDistance < 0.01f) "∞" else "${(1f / focusDistance).coerceAtMost(99f).let { "%.1f".format(it) }} m"
    ProControl.Exposure -> if (exposureCompensation > 0) "+$exposureCompensation" else exposureCompensation.toString()
}

internal fun whiteBalanceLabel(mode: Int): String = when (mode) {
    CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT -> "Incandescent"
    CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT -> "Fluorescent"
    CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT -> "Warm"
    CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT -> "Daylight"
    CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> "Cloudy"
    CameraMetadata.CONTROL_AWB_MODE_TWILIGHT -> "Twilight"
    CameraMetadata.CONTROL_AWB_MODE_SHADE -> "Shade"
    else -> "Auto"
}

internal fun shutterLabel(nanos: Long): String {
    val seconds = nanos / 1_000_000_000.0
    return if (seconds >= 1.0) {
        "${"%.1f".format(seconds)} s"
    } else {
        "1/${(1.0 / seconds.coerceAtLeast(0.000001)).roundToInt()}"
    }
}

@Composable
private fun DocumentGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "Document framing guide" }) {
        val pageHeight = size.height * 0.76f
        val pageWidth = minOf(size.width * 0.82f, pageHeight / 1.4142f)
        val left = (size.width - pageWidth) / 2f
        val top = (size.height - pageHeight) / 2f
        val right = left + pageWidth
        val bottom = top + pageHeight
        val shade = Color.Black.copy(alpha = 0.14f)
        drawRect(shade, size = Size(size.width, top))
        drawRect(shade, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
        drawRect(shade, topLeft = Offset(0f, top), size = Size(left, pageHeight))
        drawRect(shade, topLeft = Offset(right, top), size = Size(size.width - right, pageHeight))
        drawRect(
            Color.White.copy(alpha = 0.46f),
            topLeft = Offset(left, top),
            size = Size(pageWidth, pageHeight),
            style = Stroke(1.dp.toPx()),
        )
        val corner = minOf(pageWidth, pageHeight) * 0.10f
        val stroke = 3.dp.toPx()
        val color = Color.White
        listOf(
            Offset(left, top) to Offset(1f, 1f),
            Offset(right, top) to Offset(-1f, 1f),
            Offset(left, bottom) to Offset(1f, -1f),
            Offset(right, bottom) to Offset(-1f, -1f),
        ).forEach { (origin, direction) ->
            drawLine(color, origin, Offset(origin.x + direction.x * corner, origin.y), stroke, StrokeCap.Square)
            drawLine(color, origin, Offset(origin.x, origin.y + direction.y * corner), stroke, StrokeCap.Square)
        }
    }
}

internal data class ExposureControlGeometry(
    val x: Int,
    val y: Int,
    val placedOnLeft: Boolean,
)

internal fun exposureControlGeometry(
    focusPoint: Offset,
    viewportWidth: Int,
    viewportHeight: Int,
    controlWidthPx: Int,
    controlHeightPx: Int,
    ringRadiusPx: Int,
    gapPx: Int,
): ExposureControlGeometry {
    val safeMargin = 12
    val useLeft = focusPoint.x + ringRadiusPx + gapPx + controlWidthPx > viewportWidth - safeMargin
    val rawX = if (useLeft) {
        focusPoint.x - ringRadiusPx - gapPx - controlWidthPx
    } else {
        focusPoint.x + ringRadiusPx + gapPx
    }
    val rawY = focusPoint.y - controlHeightPx / 2f
    return ExposureControlGeometry(
        x = rawX.roundToInt().coerceIn(safeMargin, (viewportWidth - controlWidthPx - safeMargin).coerceAtLeast(safeMargin)),
        y = rawY.roundToInt().coerceIn(safeMargin, (viewportHeight - controlHeightPx - safeMargin).coerceAtLeast(safeMargin)),
        placedOnLeft = useLeft,
    )
}

internal fun exposureEvLabel(index: Int, step: Float): String {
    val unit = if (step > 0f) step else 1f
    val ev = index * unit
    return when {
        abs(ev) < 0.05f -> "0"
        else -> String.format(Locale.US, "%+.1f", ev)
    }
}

internal fun friendlyTopMegapixelLabel(megapixels: Double): String {
    return if (megapixels >= 10.0) {
        "${megapixels.roundToInt()} MP"
    } else if (abs(megapixels - megapixels.roundToInt()) < 0.05) {
        "${megapixels.roundToInt()} MP"
    } else {
        String.format(Locale.US, "%.1f MP", megapixels)
    }
}

@Composable
private fun FocusIndicator(point: Offset, modifier: Modifier = Modifier) {
    val radius = with(LocalDensity.current) { (CameraUiTokens.focusRingDiameter / 2).toPx() }
    Canvas(modifier) {
        drawCircle(
            Color(0xFFFFD166),
            radius = radius,
            center = point,
            style = Stroke(width = 2.2.dp.toPx()),
        )
        drawLine(Color(0xFFFFD166), Offset(point.x - radius, point.y), Offset(point.x - radius * 0.64f, point.y), 2.2.dp.toPx(), StrokeCap.Square)
        drawLine(Color(0xFFFFD166), Offset(point.x + radius * 0.64f, point.y), Offset(point.x + radius, point.y), 2.2.dp.toPx(), StrokeCap.Square)
        drawLine(Color(0xFFFFD166), Offset(point.x, point.y - radius), Offset(point.x, point.y - radius * 0.64f), 2.2.dp.toPx(), StrokeCap.Square)
        drawLine(Color(0xFFFFD166), Offset(point.x, point.y + radius * 0.64f), Offset(point.x, point.y + radius), 2.2.dp.toPx(), StrokeCap.Square)
    }
}

@Composable
private fun InteractiveGuideLayer(
    guide: CompositionGuide,
    vanishingPoint: Offset,
    frameBounds: Rect,
    eyeLineFraction: Float,
    onVanishingPoint: (Offset) -> Unit,
    onFrameBounds: (Rect) -> Unit,
    onEyeLine: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (guide !in listOf(CompositionGuide.VanishingPoint, CompositionGuide.FrameInFrame, CompositionGuide.EyeLine)) return
    var resizeFrame by remember { mutableStateOf(false) }
    Box(
        modifier.pointerInput(guide, vanishingPoint, frameBounds, eyeLineFraction) {
            detectDragGestures(
                onDragStart = { position ->
                    if (guide == CompositionGuide.FrameInFrame) {
                        val handle = Offset(frameBounds.right * size.width, frameBounds.bottom * size.height)
                        resizeFrame = (position - handle).getDistance() < 72.dp.toPx()
                    }
                },
            ) { change, drag ->
                change.consume()
                when (guide) {
                    CompositionGuide.VanishingPoint -> onVanishingPoint(
                        Offset(
                            (change.position.x / size.width).coerceIn(0.05f, 0.95f),
                            (change.position.y / size.height).coerceIn(0.08f, 0.92f),
                        ),
                    )
                    CompositionGuide.FrameInFrame -> {
                        val dx = drag.x / size.width
                        val dy = drag.y / size.height
                        if (resizeFrame) {
                            onFrameBounds(frameBounds.copy(
                                right = (frameBounds.right + dx).coerceIn(frameBounds.left + 0.18f, 0.96f),
                                bottom = (frameBounds.bottom + dy).coerceIn(frameBounds.top + 0.18f, 0.96f),
                            ))
                        } else {
                            val width = frameBounds.width
                            val height = frameBounds.height
                            val left = (frameBounds.left + dx).coerceIn(0.04f, 0.96f - width)
                            val top = (frameBounds.top + dy).coerceIn(0.06f, 0.94f - height)
                            onFrameBounds(Rect(left, top, left + width, top + height))
                        }
                    }
                    CompositionGuide.EyeLine -> onEyeLine((change.position.y / size.height).coerceIn(0.18f, 0.70f))
                    else -> Unit
                }
            }
        },
    )
}

@Composable
private fun RecordingTimer(
    seconds: Int,
    paused: Boolean,
    onPauseResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, color = Color.Black.copy(alpha = 0.58f), shape = RoundedCornerShape(6.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFF3B30)))
            Spacer(Modifier.width(8.dp))
            Text("%02d:%02d".format(seconds / 60, seconds % 60), color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onPauseResume, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (paused) "Resume recording" else "Pause recording",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun PermissionPrompt(modifier: Modifier, requestCameraPermission: () -> Unit) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(52.dp))
            Text("Camera access is required for preview and capture.", color = MaterialTheme.colorScheme.onBackground)
            Button(onClick = requestCameraPermission) { Text("Allow camera") }
        }
    }
}

private fun preferredCamera(cameras: List<CameraCapability>, facing: LensFacing): CameraCapability? =
    cameras.filter { it.lensFacing == facing }.minByOrNull {
        when (it.lensRole) {
            LensRole.Main, LensRole.Wide, LensRole.Selfie -> 0
            LensRole.Unknown -> 1
            else -> 2
        }
    }

private fun CameraCapability.lensSelectorLabel(cameras: List<CameraCapability>): String {
    val facingCameras = cameras.filter { it.lensFacing == lensFacing }
    val position = facingCameras.indexOfFirst { it.cameraId == cameraId }.coerceAtLeast(0) + 1
    return when (lensRole) {
        LensRole.Main, LensRole.Wide -> "1x"
        LensRole.Ultrawide -> "Ultra"
        LensRole.Telephoto -> "Tele"
        LensRole.Macro -> "Macro"
        LensRole.Selfie -> if (facingCameras.size == 1) "Front" else "Front $position"
        LensRole.External -> if (facingCameras.size == 1) "External" else "External $position"
        LensRole.Unknown -> when (lensFacing) {
            LensFacing.Rear -> "Rear $position"
            LensFacing.Front -> "Front $position"
            LensFacing.External -> "External $position"
            LensFacing.Unknown -> "Camera $position"
        }
    }
}

private fun ExtensionSupport.activeLabel(): String = buildList {
    if (hdr) add("HDR")
    if (night) add("Night")
    if (bokeh) add("Bokeh")
    if (faceRetouch) add("Face retouch")
}.joinToString().ifBlank { "None" }

private fun FlashMode.next(hardwareFlash: Boolean): FlashMode = when {
    !hardwareFlash -> if (this == FlashMode.Off) FlashMode.On else FlashMode.Off
    this == FlashMode.Off -> FlashMode.Auto
    this == FlashMode.Auto -> FlashMode.On
    this == FlashMode.On -> FlashMode.Torch
    else -> FlashMode.Off
}

private fun Int.nextTimer(): Int = when (this) {
    0 -> 2
    2 -> 3
    3 -> 5
    5 -> 10
    else -> 0
}

private fun formatZoom(value: Float): String = if (abs(value - value.toInt()) < 0.04f) "${value.toInt()}x" else "%.1fx".format(value)

private fun videoQualityLabel(width: Int, height: Int): String = when {
    width <= 0 || height <= 0 -> "Video"
    width >= 7_680 || height >= 4_320 -> "8K"
    width >= 3_840 || height >= 2_160 -> "4K"
    width >= 2_560 || height >= 1_440 -> "1440p"
    width >= 1_920 || height >= 1_080 -> "1080p"
    width >= 1_280 || height >= 720 -> "720p"
    else -> "${minOf(width, height)}p"
}

private fun VideoQualitySetting.displayLabel(): String = when (this) {
    VideoQualitySetting.Auto -> "Auto"
    VideoQualitySetting.UHD -> "4K"
    VideoQualitySetting.FHD -> "1080p"
    VideoQualitySetting.HD -> "720p"
    VideoQualitySetting.SD -> "480p"
}

private fun VideoStabilizationMode.shortLabel(): String = when (this) {
    VideoStabilizationMode.Off -> "OFF"
    VideoStabilizationMode.Standard -> "EIS"
    VideoStabilizationMode.Preview -> "PRE"
    VideoStabilizationMode.Optical -> "OIS"
    VideoStabilizationMode.Auto -> "AUTO"
    VideoStabilizationMode.Unsupported -> "N/A"
}

internal fun StabilizationRequestPlan.uiLabel(): String = evidenceLabel

internal fun stabilizationAcceptedShortLabel(
    status: String,
    evidence: String,
    fallback: String,
): String {
    val merged = "$status $evidence"
    return when {
        merged.contains("Preview stabilization active", ignoreCase = true) ||
            merged.contains("resultEis=2", ignoreCase = true) -> "PRE"
        merged.contains("EIS active", ignoreCase = true) ||
            merged.contains("resultEis=1", ignoreCase = true) -> "EIS"
        merged.contains("OIS active", ignoreCase = true) ||
            merged.contains("resultOis=1", ignoreCase = true) -> "OIS"
        merged.contains("Stabilization off", ignoreCase = true) ||
            (merged.contains("resultEis=0", ignoreCase = true) && merged.contains("resultOis=0", ignoreCase = true)) -> "OFF"
        merged.contains("pending", ignoreCase = true) ||
            merged.contains("verifying", ignoreCase = true) -> fallback
        else -> fallback
    }
}

private fun PhotoAspectRatio.shortLabel(): String = when (this) {
    PhotoAspectRatio.FullSensor -> "Full"
    PhotoAspectRatio.Ratio4x3 -> "4:3"
    PhotoAspectRatio.Ratio3x2 -> "3:2"
    PhotoAspectRatio.Ratio16x9 -> "16:9"
    PhotoAspectRatio.Ratio1x1 -> "1:1"
    PhotoAspectRatio.FullScreen -> "Screen"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun rememberCameraControlRotationDegrees(): Float {
    val context = LocalContext.current
    var rotation by remember { mutableFloatStateOf(0f) }
    DisposableEffect(context) {
        val listener = object : OrientationEventListener(context.applicationContext) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val snapped = when {
                    orientation >= 315 || orientation < 45 -> 0f
                    orientation < 135 -> -90f
                    orientation < 225 -> 180f
                    else -> 90f
                }
                if (rotation != snapped) rotation = snapped
            }
        }
        if (listener.canDetectOrientation()) listener.enable()
        onDispose { listener.disable() }
    }
    return rotation
}

private fun Activity.setScreenBrightness(value: Float) {
    val attributes = window.attributes
    attributes.screenBrightness = value
    window.attributes = attributes
}
