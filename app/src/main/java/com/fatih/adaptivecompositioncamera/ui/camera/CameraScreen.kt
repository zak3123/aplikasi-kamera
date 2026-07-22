package com.fatih.adaptivecompositioncamera.ui.camera

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.core.CameraSelector
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fatih.adaptivecompositioncamera.camera.CameraRuntime
import com.fatih.adaptivecompositioncamera.capability.CameraConfigurationResolver
import com.fatih.adaptivecompositioncamera.composition.CompositionGuideOverlay
import com.fatih.adaptivecompositioncamera.composition.rememberLevelReading
import com.fatih.adaptivecompositioncamera.domain.model.AppSettings
import com.fatih.adaptivecompositioncamera.domain.model.CameraCapability
import com.fatih.adaptivecompositioncamera.domain.model.CameraDiagnostics
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.CapabilityReport
import com.fatih.adaptivecompositioncamera.domain.model.CompositionGuide
import com.fatih.adaptivecompositioncamera.domain.model.FlashMode
import com.fatih.adaptivecompositioncamera.domain.model.GuideStyle
import com.fatih.adaptivecompositioncamera.domain.model.ExtensionSupport
import com.fatih.adaptivecompositioncamera.domain.model.LensFacing
import com.fatih.adaptivecompositioncamera.domain.model.LensRole
import com.fatih.adaptivecompositioncamera.domain.model.LevelReading
import com.fatih.adaptivecompositioncamera.domain.model.MediaItem
import com.fatih.adaptivecompositioncamera.domain.model.RuntimeCameraInfo
import com.fatih.adaptivecompositioncamera.media.AndroidMediaRepository
import com.fatih.adaptivecompositioncamera.utility.CameraMath
import kotlin.math.abs
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val runtime = remember { CameraRuntime(context.applicationContext) }
    val mediaRepository = remember { AndroidMediaRepository() }
    val configurationResolver = remember { CameraConfigurationResolver() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val availableCameras = remember(report) {
        report?.cameras.orEmpty().filter { it.supportsBackwardCompatible && it.jpegResolutions.isNotEmpty() }
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
    var selectedResolution by remember(activeCapability, settings.selectedResolutionIds) {
        mutableStateOf(
            resolutions.firstOrNull { it.id == activeCapability?.cameraId?.let(settings.selectedResolutionIds::get) }
                ?: resolutions.firstOrNull { it.recommended }
                ?: resolutions.firstOrNull(),
        )
    }
    val isFront = lensFacing == LensFacing.Front
    val mirrorPreview = isFront && settings.mirrorFrontPreview
    val sessionState by runtime.state.collectAsState()

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var runtimeInfo by remember { mutableStateOf(RuntimeCameraInfo()) }
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

    var showResolutionSheet by remember { mutableStateOf(false) }
    var showAspectSheet by remember { mutableStateOf(false) }
    var showCompositionSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var guideStyle by remember { mutableStateOf(GuideStyle()) }
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
                .thenBy { if (it.lensRole == com.fatih.adaptivecompositioncamera.domain.model.LensRole.Main) 0 else 1 },
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
            resolutions.firstOrNull()?.let {
                selectedResolution = it
                activeCapability?.cameraId?.let { cameraId -> onResolutionChange(cameraId, it.id) }
            }
        }
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
            delay(1_500)
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

    LaunchedEffect(previewView, lensFacing, selectedResolution?.id, settings.mode) {
        val view = previewView ?: return@LaunchedEffect
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
            onBound = {
                runtimeInfo = it
                resolutions.firstOrNull { resolution ->
                    resolution.width == it.captureWidth && resolution.height == it.captureHeight
                }?.let { actual ->
                    selectedResolution = actual
                    activeCapability?.cameraId?.let { cameraId -> onResolutionChange(cameraId, actual.id) }
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
    ) {
        onDiagnosticsChange(
            CameraDiagnostics(
                currentCameraId = activeCapability?.cameraId,
                currentCameraName = activeCapability?.friendlyName,
                sessionState = sessionState.javaClass.simpleName,
                mode = settings.mode,
                activeResolution = if (settings.mode == CameraMode.Video && runtimeInfo.videoWidth > 0) {
                    "${runtimeInfo.videoWidth} × ${runtimeInfo.videoHeight} (${videoQualityLabel(runtimeInfo.videoWidth, runtimeInfo.videoHeight)})"
                } else selectedResolution?.let { "${it.width} × ${it.height} (${it.megapixelLabel})" },
                previewResolution = runtimeInfo.previewWidth.takeIf { it > 0 }
                    ?.let { width -> "$width × ${runtimeInfo.previewHeight}" },
                currentFps = activeCapability?.fpsRanges?.joinToString(limit = 3),
                stabilization = "Camera-managed",
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
                runtime.capturePhoto(reverseHorizontal = isFront && settings.saveMirroredSelfie)
                    .onSuccess { uri ->
                        latestMedia = mediaRepository.mediaItem(context, uri, "image/jpeg")
                        lastCaptureError = null
                        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMessage("Photo saved")
                    }
                    .onFailure {
                        lastCaptureError = it.message ?: "Photo capture failed."
                        onMessage(lastCaptureError!!)
                    }
            } finally {
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

    var lastVolumeShutterEvent by remember { mutableIntStateOf(volumeShutterEvent) }
    LaunchedEffect(volumeShutterEvent) {
        if (volumeShutterEvent != lastVolumeShutterEvent) {
            lastVolumeShutterEvent = volumeShutterEvent
            if (!settings.volumeShutter) return@LaunchedEffect
            if (settings.mode == CameraMode.Video) toggleVideo() else capturePhoto()
        }
    }

    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        val sideRail = maxWidth >= 900.dp && maxWidth > maxHeight
        val guideModifier = Modifier.fillMaxSize().padding(
            top = 82.dp,
            bottom = if (sideRail) 24.dp else (maxHeight * 0.24f).coerceIn(170.dp, 240.dp),
            end = if (sideRail) (maxWidth * 0.30f).coerceIn(300.dp, 380.dp) else 0.dp,
        )
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

        CompositionGuideOverlay(
            guide = settings.guide,
            mirrored = mirrorPreview,
            style = guideStyle,
            levelReading = levelReading,
            vanishingPoint = vanishingPoint,
            frameBounds = frameBounds,
            eyeLineFraction = eyeLineFraction,
            modifier = guideModifier,
        )

        InteractiveGuideLayer(
            guide = settings.guide,
            vanishingPoint = vanishingPoint,
            frameBounds = frameBounds,
            eyeLineFraction = eyeLineFraction,
            onVanishingPoint = { vanishingPoint = it },
            onFrameBounds = { frameBounds = it },
            onEyeLine = { eyeLineFraction = it },
            modifier = guideModifier,
        )

        focusPoint?.let { point ->
            FocusIndicator(point, Modifier.fillMaxSize())
        }

        AnimatedVisibility(
            visible = showExposure,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
        ) {
            Surface(color = Color.Black.copy(alpha = 0.52f), shape = RoundedCornerShape(8.dp)) {
                Slider(
                    value = exposure.toFloat(),
                    onValueChange = { exposure = runtime.setExposure(it.toInt()) },
                    valueRange = runtimeInfo.exposureMin.toFloat()..runtimeInfo.exposureMax.toFloat(),
                    modifier = Modifier.width(150.dp).rotate(-90f),
                )
            }
        }

        if (!isRecording) {
            CameraTopBar(
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().displayCutoutPadding().padding(horizontal = 8.dp, vertical = 6.dp),
                hasFlash = runtimeInfo.hasFlash || (isFront && settings.screenFlash),
                flashMode = flashMode,
                onFlash = {
                    flashMode = flashMode.next(runtimeInfo.hasFlash)
                    if (runtimeInfo.hasFlash && !runtime.setFlashMode(flashMode)) onMessage("Flash mode is unavailable for this camera.")
                },
                timerSeconds = timerSeconds,
                onTimer = { timerSeconds = timerSeconds.nextTimer() },
                aspectRatioLabel = if (settings.mode == CameraMode.Video && runtimeInfo.videoWidth > 0) {
                    CameraMath.aspectRatioLabel(runtimeInfo.videoWidth, runtimeInfo.videoHeight)
                } else selectedResolution?.aspectRatioLabel,
                resolutionLabel = if (settings.mode == CameraMode.Video) {
                    videoQualityLabel(runtimeInfo.videoWidth, runtimeInfo.videoHeight)
                } else selectedResolution?.megapixelLabel,
                onAspectRatio = {
                    if (settings.mode == CameraMode.Video) {
                        onMessage("Video aspect ratio follows the active CameraX video stream.")
                    } else if (resolutions.isNotEmpty()) showAspectSheet = true
                },
                onResolution = {
                    if (settings.mode == CameraMode.Video) {
                        onMessage("Video quality is selected from the CameraX stream accepted by this camera.")
                    } else if (resolutions.isNotEmpty()) showResolutionSheet = true
                },
                onComposition = { showCompositionSheet = true },
                onSettings = onOpenSettings,
                onMore = { showMoreSheet = true },
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
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 74.dp),
            )
        }

        if (countdown > 0) {
            Text(
                countdown.toString(),
                color = Color.White,
                fontSize = 86.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        CameraBottomControls(
            modifier = Modifier.align(if (sideRail) Alignment.CenterEnd else Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .widthIn(max = if (sideRail) 360.dp else 620.dp),
            activeMode = settings.mode,
            availableModes = availableModes,
            availableCameras = availableCameras.filter {
                it.lensFacing == lensFacing || it.lensFacing == LensFacing.External
            },
            activeCameraId = activeCameraId,
            maxResolution = activeCapability?.jpegResolutions?.firstOrNull(),
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
            canSwitch = LensFacing.Front in availableFacings && LensFacing.Rear in availableFacings && !isRecording,
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
                activeCameraId = camera.cameraId
                onCameraChange(camera.cameraId)
                zoom = 1f
                focusPoint = null
                showExposure = false
            },
            onMode = { if (!isRecording) onModeChange(it) },
            onMore = { if (!isRecording) showMoreSheet = true },
            onShutter = { if (settings.mode == CameraMode.Video) toggleVideo() else capturePhoto() },
        )

        if (flashVisible) Box(Modifier.fillMaxSize().background(Color.White))
        else if (captureEffectVisible) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.46f)))
    }

    if (showResolutionSheet) {
        ResolutionSheet(
            resolutions = resolutions,
            selected = selectedResolution,
            onSelect = {
                selectedResolution = it
                activeCapability?.cameraId?.let { cameraId -> onResolutionChange(cameraId, it.id) }
            },
            onDismiss = { showResolutionSheet = false },
        )
    }
    if (showAspectSheet) {
        AspectRatioSheet(
            resolutions = resolutions,
            selected = selectedResolution,
            onSelect = {
                selectedResolution = it
                activeCapability?.cameraId?.let { cameraId -> onResolutionChange(cameraId, it.id) }
            },
            onDismiss = { showAspectSheet = false },
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
        MoreModesSheet(
            modes = availableModes,
            activeMode = settings.mode,
            maxResolution = activeCapability?.jpegResolutions?.firstOrNull(),
            onSelect = onModeChange,
            onDismiss = { showMoreSheet = false },
        )
    }
}

@Composable
private fun CameraTopBar(
    hasFlash: Boolean,
    flashMode: FlashMode,
    onFlash: () -> Unit,
    timerSeconds: Int,
    onTimer: () -> Unit,
    aspectRatioLabel: String?,
    resolutionLabel: String?,
    onAspectRatio: () -> Unit,
    onResolution: () -> Unit,
    onComposition: () -> Unit,
    onSettings: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, color = Color.Black.copy(alpha = 0.46f), shape = RoundedCornerShape(8.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (hasFlash) {
                TopControl(
                    icon = when (flashMode) {
                        FlashMode.Off -> Icons.Rounded.FlashOff
                        FlashMode.Auto -> Icons.Rounded.FlashAuto
                        FlashMode.On, FlashMode.Torch -> Icons.Rounded.FlashOn
                    },
                    description = "Flash ${flashMode.name}",
                    label = flashMode.takeUnless { it == FlashMode.Off }?.name,
                    onClick = onFlash,
                )
            }
            TopControl(Icons.Rounded.Timer, "Self timer", if (timerSeconds == 0) null else "${timerSeconds}s", onTimer)
            TopControl(Icons.Rounded.AspectRatio, "Aspect ratio", aspectRatioLabel, onAspectRatio)
            TopControl(Icons.Rounded.PhotoSizeSelectLarge, "Capture resolution", resolutionLabel, onResolution)
            TopControl(Icons.Rounded.GridOn, "Composition guides", null, onComposition)
            TopControl(Icons.Rounded.Settings, "Settings", null, onSettings)
            TopControl(Icons.Rounded.MoreVert, "More camera controls", null, onMore)
        }
    }
}

@Composable
private fun TopControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    label: String?,
    onClick: () -> Unit,
) {
    Column(Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = description, tint = Color.White)
        }
        if (label != null) {
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun CameraBottomControls(
    activeMode: CameraMode,
    availableModes: List<CameraMode>,
    availableCameras: List<CameraCapability>,
    activeCameraId: String?,
    maxResolution: CameraResolution?,
    zoom: Float,
    minZoom: Float,
    maxZoom: Float,
    showZoomSlider: Boolean,
    onToggleZoomSlider: () -> Unit,
    onZoom: (Float) -> Unit,
    latestThumbnail: Bitmap?,
    hasLatestMedia: Boolean,
    onLatestMedia: () -> Unit,
    isRecording: Boolean,
    canSwitch: Boolean,
    onSwitch: () -> Unit,
    onCamera: (CameraCapability) -> Unit,
    onMode: (CameraMode) -> Unit,
    onMore: () -> Unit,
    onShutter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, color = Color.Black.copy(alpha = 0.58f), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (availableCameras.size > 1) {
                LensSelector(availableCameras, activeCameraId, onCamera)
            }
            if (maxZoom > minZoom + 0.05f) {
                QuickZoomRow(zoom, minZoom, maxZoom, onZoom, onToggleZoomSlider)
                AnimatedVisibility(showZoomSlider) {
                    Slider(value = zoom, onValueChange = onZoom, valueRange = minZoom..maxZoom, modifier = Modifier.fillMaxWidth().height(32.dp))
                }
            }
            ModeCarousel(activeMode, availableModes, maxResolution, onMode, onMore)
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LatestMediaButton(latestThumbnail, hasLatestMedia, onLatestMedia)
                ShutterButton(videoMode = activeMode == CameraMode.Video, recording = isRecording, onClick = onShutter)
                Surface(
                    onClick = onSwitch,
                    enabled = canSwitch,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = if (canSwitch) 0.16f else 0.07f),
                    modifier = Modifier.size(58.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Cameraswitch, "Switch camera", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickZoomRow(
    zoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoom: (Float) -> Unit,
    onToggleSlider: () -> Unit,
) {
    val values = buildList {
        if (minZoom < 0.95f) add(minZoom)
        add(1f.coerceIn(minZoom, maxZoom))
        if (abs(zoom - 1f) >= 0.08f && abs(zoom - minZoom) >= 0.08f) add(zoom)
    }.distinctBy { (it * 10).toInt() }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        values.forEach { value ->
            val selected = abs(zoom - value) < 0.08f
            Surface(
                onClick = { if (selected) onToggleSlider() else onZoom(value) },
                shape = CircleShape,
                color = if (selected) Color.White else Color.Black.copy(alpha = 0.4f),
                modifier = Modifier.size(if (selected) 42.dp else 36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(formatZoom(value), color = if (selected) Color.Black else Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun LensSelector(
    cameras: List<CameraCapability>,
    activeCameraId: String?,
    onCamera: (CameraCapability) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cameras.forEach { camera ->
            val selected = camera.cameraId == activeCameraId
            Surface(
                onClick = { onCamera(camera) },
                shape = CircleShape,
                color = if (selected) Color.White else Color.Black.copy(alpha = 0.42f),
                modifier = Modifier.padding(horizontal = 4.dp).height(48.dp),
            ) {
                Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text(
                        camera.lensSelectorLabel(cameras),
                        color = if (selected) Color.Black else Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCarousel(
    activeMode: CameraMode,
    availableModes: List<CameraMode>,
    maxResolution: CameraResolution?,
    onMode: (CameraMode) -> Unit,
    onMore: () -> Unit,
) {
    val mainModes = listOf(CameraMode.Portrait, CameraMode.Photo, CameraMode.Video).filter { it in availableModes }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        mainModes.forEach { mode ->
            val selected = mode == activeMode
            Text(
                text = mode.label(maxResolution),
                color = if (selected) Color(0xFFFFD166) else Color.White.copy(alpha = 0.76f),
                style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.clickable { onMode(mode) }.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Text(
            "More",
            color = if (activeMode !in mainModes) Color(0xFFFFD166) else Color.White.copy(alpha = 0.76f),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.clickable(onClick = onMore).padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun LatestMediaButton(bitmap: Bitmap?, hasMedia: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = hasMedia,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.14f),
        modifier = Modifier.size(58.dp).border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape),
    ) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), "Latest captured media", Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Collections, "Open gallery", tint = Color.White) }
        }
    }
}

@Composable
private fun ShutterButton(videoMode: Boolean, recording: Boolean, onClick: () -> Unit) {
    Canvas(
        Modifier.size(86.dp).clickable(onClick = onClick)
            .semantics { contentDescription = if (recording) "Stop recording" else if (videoMode) "Start video recording" else "Take photo" },
    ) {
        drawCircle(Color.White, radius = size.minDimension / 2f, style = Stroke(width = 3.dp.toPx()))
        when {
            recording -> drawRoundRect(
                Color(0xFFFF3B30),
                topLeft = Offset(size.width * 0.34f, size.height * 0.34f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.32f, size.height * 0.32f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
            )
            videoMode -> drawCircle(Color(0xFFFF3B30), radius = size.minDimension * 0.36f)
            else -> drawCircle(Color.White, radius = size.minDimension * 0.38f)
        }
    }
}

@Composable
private fun FocusIndicator(point: Offset, modifier: Modifier = Modifier) {
    val radius = with(LocalDensity.current) { 30.dp.toPx() }
    Canvas(modifier) {
        drawCircle(
            Color(0xFFFFD166),
            radius = radius,
            center = point,
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawLine(Color(0xFFFFD166), Offset(point.x - radius, point.y), Offset(point.x - radius * 0.65f, point.y), 1.5.dp.toPx(), StrokeCap.Square)
        drawLine(Color(0xFFFFD166), Offset(point.x + radius * 0.65f, point.y), Offset(point.x + radius, point.y), 1.5.dp.toPx(), StrokeCap.Square)
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
            LensRole.Main, LensRole.Selfie -> 0
            LensRole.Unknown -> 1
            else -> 2
        }
    }

private fun CameraCapability.lensSelectorLabel(cameras: List<CameraCapability>): String {
    val facingCameras = cameras.filter { it.lensFacing == lensFacing }
    val position = facingCameras.indexOfFirst { it.cameraId == cameraId }.coerceAtLeast(0) + 1
    return when (lensRole) {
        LensRole.Main -> "Main"
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Activity.setScreenBrightness(value: Float) {
    val attributes = window.attributes
    attributes.screenBrightness = value
    window.attributes = attributes
}
