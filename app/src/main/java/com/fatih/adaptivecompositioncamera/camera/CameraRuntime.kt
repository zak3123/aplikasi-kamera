package com.fatih.adaptivecompositioncamera.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import android.util.Rational
import android.util.Range
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.fatih.adaptivecompositioncamera.capability.DefaultStabilizationResolver
import com.fatih.adaptivecompositioncamera.capability.StabilizationDecision
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.CameraSessionState
import com.fatih.adaptivecompositioncamera.domain.model.FlashMode
import com.fatih.adaptivecompositioncamera.domain.model.RuntimeCameraInfo
import com.fatih.adaptivecompositioncamera.domain.model.StabilizationSupport
import com.fatih.adaptivecompositioncamera.domain.model.VideoFpsRange
import com.fatih.adaptivecompositioncamera.domain.model.VideoQualitySetting
import com.fatih.adaptivecompositioncamera.domain.model.VideoStabilizationMode
import com.fatih.adaptivecompositioncamera.media.AndroidMediaRepository
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.delay

class CameraRuntime(
    private val context: Context,
) {
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val captureExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "AdaptiveCamera-Capture").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    private val mediaRepository = AndroidMediaRepository()
    private val bindGeneration = AtomicInteger(0)
    private val _state = MutableStateFlow<CameraSessionState>(CameraSessionState.Discovering)
    val state: StateFlow<CameraSessionState> = _state.asStateFlow()
    private val _stabilizationStatus = MutableStateFlow("Off")
    val stabilizationStatus: StateFlow<String> = _stabilizationStatus.asStateFlow()
    private val _stabilizationEvidence = MutableStateFlow("No CaptureResult received")
    val stabilizationEvidence: StateFlow<String> = _stabilizationEvidence.asStateFlow()
    private val stabilizationResolver = DefaultStabilizationResolver()

    private var provider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var camera: Camera? = null
    private var sensorIsoRange: android.util.Range<Int>? = null
    private var sensorExposureRange: android.util.Range<Long>? = null
    private var sensorMinimumFocusDistance = 0f
    private var manualIso: Int? = null
    private var manualExposureNanos: Long? = null
    private var manualFocusDistance: Float? = null
    private var manualWhiteBalanceMode = CameraMetadata.CONTROL_AWB_MODE_AUTO
    private var availableWhiteBalanceModes: List<Int> = emptyList()
    private var supportedVideoQualities: List<VideoQualitySetting> = emptyList()
    private var selectedVideoQuality = VideoQualitySetting.Auto
    private var requestedFpsRange: VideoFpsRange? = null
    private var requestedStabilization = VideoStabilizationMode.Off
    private var activeCameraId: String? = null
    private var activeRequestedResolution: CameraResolution? = null
    private var activeMode: CameraMode = CameraMode.Photo

    @androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
    fun bind(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        lensFacing: Int,
        cameraId: String?,
        resolution: CameraResolution?,
        mode: CameraMode,
        viewportWidth: Int,
        viewportHeight: Int,
        targetRotation: Int,
        matchPreviewCrop: Boolean,
        videoQuality: VideoQualitySetting = VideoQualitySetting.Auto,
        videoFpsRange: VideoFpsRange? = null,
        videoStabilization: VideoStabilizationMode = VideoStabilizationMode.Off,
        stabilizationSupport: StabilizationSupport = StabilizationSupport(false, false, false),
        onBound: (RuntimeCameraInfo) -> Unit,
        onError: (String) -> Unit,
    ) {
        val generation = bindGeneration.incrementAndGet()
        _state.value = CameraSessionState.Binding
        activeCameraId = cameraId
        activeRequestedResolution = resolution
        activeMode = mode
        CameraEvidenceLogger.record(
            context,
            "CAMERAX_BIND",
            "generation=$generation camera=$cameraId mode=$mode requested=" +
                resolution?.let { "${it.width}x${it.height}/${it.id}" }.orEmpty() +
                " viewport=${viewportWidth}x$viewportHeight rotation=$targetRotation " +
                "matchPreviewCrop=$matchPreviewCrop videoQuality=${videoQuality.name} " +
                "fps=${videoFpsRange?.label ?: "camera-managed"} stabilization=${videoStabilization.name}",
        )
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            if (generation != bindGeneration.get()) return@addListener
            val cameraProvider = runCatching { providerFuture.get() }.getOrElse {
                fail(it.message ?: "Unable to initialize camera provider.", onError)
                return@addListener
            }
            provider = cameraProvider
            val selectorBuilder = CameraSelector.Builder().requireLensFacing(lensFacing)
            if (cameraId != null) {
                selectorBuilder.addCameraFilter { cameraInfos ->
                    cameraInfos.filter { info ->
                        runCatching { Camera2CameraInfo.from(info).cameraId == cameraId }.getOrDefault(false)
                    }
                }
            }
            val selector = selectorBuilder.build()
            val selectedCameraInfo = cameraProvider.availableCameraInfos.firstOrNull { info ->
                cameraId == null || runCatching { Camera2CameraInfo.from(info).cameraId == cameraId }.getOrDefault(false)
            }
            supportedVideoQualities = selectedCameraInfo?.let(QualitySelector::getSupportedQualities)
                .orEmpty().mapNotNull(::fromCameraXQuality)
            selectedVideoQuality = resolveVideoQuality(videoQuality, supportedVideoQualities)
            requestedFpsRange = videoFpsRange
            val stabilizationDecision = stabilizationResolver.resolve(
                requested = videoStabilization,
                support = stabilizationSupport,
                mode = mode,
            )
            requestedStabilization = stabilizationDecision.effective
            stabilizationDecision.fallbackReason?.let { reason ->
                CameraEvidenceLogger.record(context, "STABILIZATION", reason)
            }
            if (
                mode == CameraMode.Video && videoQuality != VideoQualitySetting.Auto &&
                videoQuality !in supportedVideoQualities
            ) {
                onError(
                    "${videoQuality.label()} is unavailable on this lens. " +
                        "${selectedVideoQuality.label()} was selected; no recording has started.",
                )
            }
            val previewBuilder = Preview.Builder().setTargetRotation(targetRotation)
            if (mode == CameraMode.Video) {
                configureVideoRequest(
                    previewBuilder = previewBuilder,
                    cameraId = cameraId,
                    quality = selectedVideoQuality,
                    stabilization = requestedStabilization,
                    fpsRange = videoFpsRange,
                    mode = mode,
                    requestedResolution = resolution,
                )
            }
            val highResolutionMode = mode == CameraMode.MaximumResolution && resolution?.highResolution == true
            val maximumSensorMode = mode == CameraMode.MaximumResolution && resolution?.maximumSensorMode == true
            val captureBuilder = ImageCapture.Builder()
                .setTargetRotation(targetRotation)
                .setCaptureMode(
                if (mode == CameraMode.MaximumResolution) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            )
            if (resolution != null && !resolution.maximumSensorMode &&
                (!resolution.highResolution || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            ) {
                val resolutionSelector = ResolutionSelector.Builder()
                    .setAllowedResolutionMode(
                        if (resolution.highResolution) {
                            ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
                        } else {
                            ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION
                        },
                    )
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(resolution.width, resolution.height),
                            if (mode == CameraMode.MaximumResolution) ResolutionStrategy.FALLBACK_RULE_NONE
                            else ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    ).build()
                captureBuilder.setResolutionSelector(resolutionSelector)
            }
            if (mode != CameraMode.Video) {
                configureStillRequest(
                    previewBuilder = previewBuilder,
                    captureBuilder = captureBuilder,
                    cameraId = cameraId,
                    mode = mode,
                    resolution = resolution,
                    stabilizationDecision = stabilizationDecision,
                )
            }
            val preview = previewBuilder.build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            previewUseCase = preview
            val requestedCapture = captureBuilder.build()
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(selectedVideoQuality.toCameraXQuality()))
                .build()
            val requestedVideo = VideoCapture.Builder(recorder).setTargetRotation(targetRotation).build()

            try {
                cameraProvider.unbindAll()
                if (mode == CameraMode.Video) {
                    camera = bindUseCases(
                        cameraProvider, lifecycleOwner, selector, preview, requestedVideo,
                        viewportWidth, viewportHeight, targetRotation, matchPreviewCrop,
                    )
                    imageCapture = null
                    videoCapture = requestedVideo
                } else {
                    camera = bindUseCases(
                        cameraProvider, lifecycleOwner, selector, preview, requestedCapture,
                        viewportWidth, viewportHeight, targetRotation, matchPreviewCrop,
                    )
                    imageCapture = requestedCapture
                    videoCapture = null
                }
                completeBind(
                    targetRotation = targetRotation,
                    sensorPixelMode = when {
                        maximumSensorMode -> "Maximum Resolution (Camera2 still capture)"
                        highResolutionMode -> "High Resolution"
                        else -> "Normal"
                    },
                    onBound = onBound,
                )
            } catch (requestedFailure: RuntimeException) {
                if (mode == CameraMode.Video) {
                    fail(
                        "Video configuration ${selectedVideoQuality.label()} " +
                            "${videoFpsRange?.label.orEmpty()} with ${requestedStabilization.label()} was rejected. " +
                            "Choose a lower quality or disable stabilization.",
                        onError,
                    )
                } else bindSafePhotoFallback(
                    cameraProvider = cameraProvider,
                    lifecycleOwner = lifecycleOwner,
                    selector = selector,
                    preview = preview,
                    requestedFailure = requestedFailure,
                    viewportWidth = viewportWidth,
                    viewportHeight = viewportHeight,
                    targetRotation = targetRotation,
                    matchPreviewCrop = matchPreviewCrop,
                    onBound = onBound,
                    onError = onError,
                )
            }
        }, mainExecutor)
    }

    private fun bindSafePhotoFallback(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector,
        preview: Preview,
        requestedFailure: RuntimeException,
        viewportWidth: Int,
        viewportHeight: Int,
        targetRotation: Int,
        matchPreviewCrop: Boolean,
        onBound: (RuntimeCameraInfo) -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            cameraProvider.unbindAll()
            val safeCapture = ImageCapture.Builder()
                .setTargetRotation(targetRotation)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            camera = bindUseCases(
                cameraProvider, lifecycleOwner, selector, preview, safeCapture,
                viewportWidth, viewportHeight, targetRotation, matchPreviewCrop,
            )
            imageCapture = safeCapture
            videoCapture = null
            completeBind(targetRotation, "Normal", onBound)
            onError("The requested camera configuration was not accepted. A safe photo configuration was restored. ${requestedFailure.message.orEmpty()}")
        } catch (fallbackFailure: RuntimeException) {
            fail(fallbackFailure.message ?: "Unable to bind a usable camera configuration.", onError)
        }
    }

    private fun bindUseCases(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector,
        preview: Preview,
        captureUseCase: UseCase,
        viewportWidth: Int,
        viewportHeight: Int,
        targetRotation: Int,
        matchPreviewCrop: Boolean,
    ): Camera {
        val group = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(captureUseCase)
            .apply {
                if (matchPreviewCrop && viewportWidth > 0 && viewportHeight > 0) {
                    setViewPort(
                        ViewPort.Builder(Rational(viewportWidth, viewportHeight), targetRotation)
                            .setScaleType(ViewPort.FILL_CENTER)
                            .build(),
                    )
                }
            }.build()
        return cameraProvider.bindToLifecycle(lifecycleOwner, selector, group)
    }

    @androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
    private fun configureVideoRequest(
        previewBuilder: Preview.Builder,
        cameraId: String?,
        quality: VideoQualitySetting,
        stabilization: VideoStabilizationMode,
        fpsRange: VideoFpsRange?,
        mode: CameraMode,
        requestedResolution: CameraResolution?,
    ) {
        val monitor = StabilizationResultMonitor(
            context = context,
            cameraId = cameraId,
            quality = quality,
            fpsRange = fpsRange,
            mode = mode,
            requestedResolution = requestedResolution,
            requested = stabilization,
            onStatus = {
                _stabilizationStatus.value = it.summary
                _stabilizationEvidence.value = it.evidence
            },
        )
        _stabilizationStatus.value = "Requested ${stabilization.label()}; awaiting CaptureResult"
        _stabilizationEvidence.value = "CaptureResult pending"
        CameraEvidenceLogger.record(
            context,
            "STABILIZATION_REQUEST",
            "camera=$cameraId quality=${quality.label()} fps=${fpsRange?.label ?: "camera-managed"} " +
                "effective=${stabilization.name}",
        )
        Camera2Interop.Extender(previewBuilder).apply {
            fpsRange?.let {
                setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(it.min, it.max))
            }
            when (stabilization) {
                VideoStabilizationMode.Standard -> {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON,
                    )
                }
                VideoStabilizationMode.Preview -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        setCaptureRequestOption(
                            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                            CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION,
                        )
                    }
                }
                VideoStabilizationMode.Optical -> {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                    )
                    setCaptureRequestOption(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON,
                    )
                }
                else -> {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                    )
                    setCaptureRequestOption(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF,
                    )
                }
            }
            setSessionCaptureCallback(monitor)
        }
    }

    @androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
    private fun configureStillRequest(
        previewBuilder: Preview.Builder,
        captureBuilder: ImageCapture.Builder,
        cameraId: String?,
        mode: CameraMode,
        resolution: CameraResolution?,
        stabilizationDecision: StabilizationDecision,
    ) {
        val stabilization = stabilizationDecision.effective
        val plan = stabilizationDecision.requestPlan
        val monitor = StabilizationResultMonitor(
            context = context,
            cameraId = cameraId,
            quality = VideoQualitySetting.Auto,
            fpsRange = null,
            mode = mode,
            requestedResolution = resolution,
            requested = stabilization,
            onStatus = {
                _stabilizationStatus.value = it.summary
                _stabilizationEvidence.value = it.evidence
            },
        )
        _stabilizationStatus.value = if (plan.verifyCaptureResult) {
            "Requested ${plan.evidenceLabel}; awaiting CaptureResult"
        } else {
            "Stabilization off"
        }
        _stabilizationEvidence.value = if (plan.verifyCaptureResult) "CaptureResult pending" else "No still stabilization requested"
        CameraEvidenceLogger.record(
            context,
            "STABILIZATION_REQUEST",
            "camera=$cameraId mode=$mode requestedResolution=${resolution?.width}x${resolution?.height} " +
                "effective=${stabilization.name} requestOis=${plan.requestOis} " +
                "requestStandardEis=${plan.requestStandardEis} requestPreview=${plan.requestPreviewStabilization}",
        )
        fun applyStillOptions(extender: Camera2Interop.Extender<*>) {
            extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF,
            )
            extender.setCaptureRequestOption(
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                if (plan.requestOis) {
                    CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                } else {
                    CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
                },
            )
        }
        applyStillOptions(Camera2Interop.Extender(previewBuilder))
        applyStillOptions(Camera2Interop.Extender(captureBuilder))
        Camera2Interop.Extender(previewBuilder).setSessionCaptureCallback(monitor)
    }

    private data class StabilizationFrameEvidence(
        val summary: String,
        val evidence: String,
    )

    private class StabilizationResultMonitor(
        private val context: Context,
        private val cameraId: String?,
        private val quality: VideoQualitySetting,
        private val fpsRange: VideoFpsRange?,
        private val mode: CameraMode,
        private val requestedResolution: CameraResolution?,
        private val requested: VideoStabilizationMode,
        private val onStatus: (StabilizationFrameEvidence) -> Unit,
    ) : CameraCaptureSession.CaptureCallback() {
        private var frameCount = 0
        private var inactiveFrames = 0
        private var lastSignature: String? = null

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            captureRequest: CaptureRequest,
            result: TotalCaptureResult,
        ) {
            frameCount += 1
            val requestedEis = captureRequest[CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE]
            val requestedOis = captureRequest[CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE]
            val resultEis = result[CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE]
            val resultOis = result[CaptureResult.LENS_OPTICAL_STABILIZATION_MODE]
            val previewActive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                resultEis == CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
            val eisActive = resultEis == CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
            val oisActive = resultOis == CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
            val requestedActiveMode = requested in setOf(
                VideoStabilizationMode.Standard,
                VideoStabilizationMode.Preview,
                VideoStabilizationMode.Optical,
            )
            inactiveFrames = if (requestedActiveMode && !previewActive && !eisActive && !oisActive) {
                inactiveFrames + 1
            } else {
                0
            }
            val summary = when {
                previewActive -> "Preview stabilization active"
                eisActive && oisActive -> "EIS + OIS active"
                eisActive -> "EIS active"
                oisActive -> "OIS active"
                requestedActiveMode && inactiveFrames < 12 ->
                    "Stabilization requested; verifying CaptureResult"
                requestedActiveMode ->
                    "Stabilization inactive at ${quality.labelStatic()}/${fpsRange?.label ?: "camera FPS"}"
                else -> "Stabilization off"
            }
            val crop = result[CaptureResult.SCALER_CROP_REGION]
            val evidence = buildString {
                append("camera=$cameraId frame=${result.frameNumber} mode=$mode quality=${quality.labelStatic()} ")
                append("resolution=${requestedResolution?.width}x${requestedResolution?.height} ")
                append("fpsRequested=${fpsRange?.label ?: "camera-managed"} ")
                append("requestEis=$requestedEis requestOis=$requestedOis ")
                append("resultEis=$resultEis resultOis=$resultOis ")
                append("aeFps=${result[CaptureResult.CONTROL_AE_TARGET_FPS_RANGE]} ")
                append("crop=$crop exposureNs=${result[CaptureResult.SENSOR_EXPOSURE_TIME]} ")
                append("frameDurationNs=${result[CaptureResult.SENSOR_FRAME_DURATION]}")
            }
            onStatus(StabilizationFrameEvidence(summary, evidence))
            val signature = "$summary|$resultEis|$resultOis|$crop"
            if (frameCount == 1 || signature != lastSignature || frameCount % 120 == 0) {
                CameraEvidenceLogger.record(context, "STABILIZATION_RESULT", evidence)
                lastSignature = signature
            }
        }

        override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
            CameraEvidenceLogger.record(
                context,
                "STABILIZATION_ERROR",
                "camera=$cameraId captureSequenceAborted=$sequenceId",
            )
        }
    }

    private fun resolveVideoQuality(
        requested: VideoQualitySetting,
        supported: List<VideoQualitySetting>,
    ): VideoQualitySetting {
        val ordered = listOf(
            VideoQualitySetting.UHD,
            VideoQualitySetting.FHD,
            VideoQualitySetting.HD,
            VideoQualitySetting.SD,
        )
        return when {
            requested == VideoQualitySetting.Auto -> ordered.firstOrNull(supported::contains)
            requested in supported -> requested
            else -> ordered.firstOrNull(supported::contains)
        } ?: VideoQualitySetting.FHD
    }

    private fun fromCameraXQuality(quality: Quality): VideoQualitySetting? = when (quality) {
        Quality.UHD -> VideoQualitySetting.UHD
        Quality.FHD -> VideoQualitySetting.FHD
        Quality.HD -> VideoQualitySetting.HD
        Quality.SD -> VideoQualitySetting.SD
        else -> null
    }

    private fun VideoQualitySetting.toCameraXQuality(): Quality = when (this) {
        VideoQualitySetting.UHD -> Quality.UHD
        VideoQualitySetting.FHD, VideoQualitySetting.Auto -> Quality.FHD
        VideoQualitySetting.HD -> Quality.HD
        VideoQualitySetting.SD -> Quality.SD
    }

    private fun VideoQualitySetting.label(): String = when (this) {
        VideoQualitySetting.Auto -> "Auto"
        VideoQualitySetting.UHD -> "4K"
        VideoQualitySetting.FHD -> "1080p"
        VideoQualitySetting.HD -> "720p"
        VideoQualitySetting.SD -> "480p"
    }

    private fun VideoStabilizationMode.label(): String = when (this) {
        VideoStabilizationMode.Off -> "stabilization off"
        VideoStabilizationMode.Standard -> "EIS"
        VideoStabilizationMode.Preview -> "preview stabilization"
        VideoStabilizationMode.Optical -> "OIS"
        VideoStabilizationMode.Auto -> "automatic stabilization"
        VideoStabilizationMode.Unsupported -> "unsupported stabilization"
    }

    @androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
    private fun completeBind(
        targetRotation: Int,
        sensorPixelMode: String,
        onBound: (RuntimeCameraInfo) -> Unit,
    ) {
        val info = camera?.cameraInfo
        val zoomState = info?.zoomState?.value
        val exposure = info?.exposureState?.exposureCompensationRange
        val captureResolution = imageCapture?.resolutionInfo?.resolution
        val videoResolution = videoCapture?.resolutionInfo?.resolution
        val previewResolution = previewUseCase?.resolutionInfo?.resolution
        val camera2Info = info?.let { runCatching { Camera2CameraInfo.from(it) }.getOrNull() }
        val capabilities = camera2Info?.getCameraCharacteristic(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,
        ) ?: intArrayOf()
        sensorIsoRange = camera2Info?.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        sensorExposureRange = camera2Info?.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        sensorMinimumFocusDistance = camera2Info
            ?.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        availableWhiteBalanceModes = camera2Info
            ?.getCameraCharacteristic(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
            ?.toList() ?: emptyList()
        manualIso = null
        manualExposureNanos = null
        manualFocusDistance = null
        manualWhiteBalanceMode = CameraMetadata.CONTROL_AWB_MODE_AUTO
        _state.value = readyStateFor(activeMode)
        CameraEvidenceLogger.record(
            context,
            "CAMERAX_BOUND",
            "camera=$activeCameraId requested=" +
                activeRequestedResolution?.let { "${it.width}x${it.height}" }.orEmpty() +
                " capture=${captureResolution?.width}x${captureResolution?.height} " +
                "video=${videoResolution?.width}x${videoResolution?.height} " +
                "preview=${previewResolution?.width}x${previewResolution?.height} " +
                "sensorPixelMode=$sensorPixelMode quality=${selectedVideoQuality.label()} " +
                "fps=${requestedFpsRange?.label ?: "camera-managed"} " +
                "stabilization=${requestedStabilization.name}",
        )
        onBound(
            RuntimeCameraInfo(
                minZoom = zoomState?.minZoomRatio ?: 1f,
                maxZoom = zoomState?.maxZoomRatio ?: 1f,
                exposureMin = exposure?.lower ?: 0,
                exposureMax = exposure?.upper ?: 0,
                exposureStep = info?.exposureState?.exposureCompensationStep?.toFloat() ?: 0f,
                hasFlash = info?.hasFlashUnit() == true,
                captureWidth = captureResolution?.width ?: 0,
                captureHeight = captureResolution?.height ?: 0,
                videoWidth = videoResolution?.width ?: 0,
                videoHeight = videoResolution?.height ?: 0,
                previewWidth = previewResolution?.width ?: 0,
                previewHeight = previewResolution?.height ?: 0,
                targetRotation = targetRotation,
                sensorPixelMode = sensorPixelMode,
                supportsManualSensor = capabilities.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
                ) && sensorIsoRange != null && sensorExposureRange != null,
                isoMin = sensorIsoRange?.lower ?: 0,
                isoMax = sensorIsoRange?.upper ?: 0,
                exposureTimeMinNanos = sensorExposureRange?.lower ?: 0L,
                exposureTimeMaxNanos = sensorExposureRange?.upper ?: 0L,
                minFocusDistance = sensorMinimumFocusDistance,
                availableWhiteBalanceModes = availableWhiteBalanceModes,
                supportedVideoQualities = supportedVideoQualities,
                selectedVideoQuality = selectedVideoQuality,
                requestedFpsRange = requestedFpsRange,
                requestedStabilization = requestedStabilization,
            ),
        )
    }

    private fun fail(message: String, onError: (String) -> Unit) {
        _state.value = CameraSessionState.Error(message)
        onError(message)
    }

    suspend fun capturePhoto(
        reverseHorizontal: Boolean = false,
        cameraId: String? = null,
        resolution: CameraResolution? = null,
        targetRotation: Int = 0,
        flashMode: FlashMode = FlashMode.Off,
    ): Result<Uri> {
        val useCamera2HighResolution = resolution?.maximumSensorMode == true ||
            (resolution?.highResolution == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        if (useCamera2HighResolution) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return Result.failure(
                    UnsupportedOperationException("Dedicated high-resolution capture requires Android 12 or newer."),
                )
            }
            val resolvedCameraId = cameraId ?: return Result.failure(
                IllegalStateException("Android did not provide an ID for the high-resolution camera."),
            )
            _state.value = CameraSessionState.Capturing
            CameraEvidenceLogger.record(
                context,
                "MAX_CAPTURE",
                "handoff CameraX->Camera2 camera=$resolvedCameraId " +
                    "requested=${resolution.width}x${resolution.height}",
            )
            provider?.unbindAll()
            imageCapture = null
            videoCapture = null
            previewUseCase = null
            camera = null
            // CameraX closes its CameraDevice asynchronously after unbindAll().
            // A short hand-off window avoids an immediate CAMERA_IN_USE rejection.
            delay(250)
            val result = MaximumResolutionCamera2Capture(context, mediaRepository).capture(
                cameraId = resolvedCameraId,
                resolution = resolution,
                targetRotation = targetRotation,
                flashMode = flashMode,
                stabilization = requestedStabilization,
            )
            _state.value = result.fold(
                onSuccess = { CameraSessionState.Reconfiguring },
                onFailure = { CameraSessionState.Error(it.message ?: "High-resolution capture failed.") },
            )
            result.onFailure {
                CameraEvidenceLogger.record(
                    context,
                    "MAX_CAPTURE_ERROR",
                    "camera=$resolvedCameraId requested=${resolution.width}x${resolution.height} error=${it.message}",
                )
            }
            return result
        }
        return captureCameraXPhoto(reverseHorizontal)
    }

    private suspend fun captureCameraXPhoto(reverseHorizontal: Boolean): Result<Uri> = suspendCoroutine { continuation ->
        if (_state.value == CameraSessionState.Capturing) {
            continuation.resume(Result.failure(IllegalStateException("A photo capture is already in progress.")))
            return@suspendCoroutine
        }
        val capture = imageCapture
        if (capture == null) {
            continuation.resume(Result.failure(IllegalStateException("Camera is not ready for photo capture.")))
            return@suspendCoroutine
        }
        _state.value = CameraSessionState.Capturing
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, mediaRepository.createImageName("jpg"))
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/AdaptiveCompositionCamera")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "AdaptiveCompositionCamera")
                directory.mkdirs()
                put(MediaStore.Images.Media.DATA, File(directory, getAsString(MediaStore.MediaColumns.DISPLAY_NAME)).absolutePath)
            }
        }
        val metadata = ImageCapture.Metadata().apply { isReversedHorizontal = reverseHorizontal }
        val options = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        ).setMetadata(metadata).build()
        capture.takePicture(options, captureExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val uri = outputFileResults.savedUri
                if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val ready = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                    context.contentResolver.update(uri, ready, null, null)
                }
                _state.value = readyStateFor(activeMode)
                if (uri != null) {
                    val actual = readJpegDimensions(uri)
                    CameraEvidenceLogger.record(
                        context,
                        "CAMERAX_CAPTURE",
                        "camera=$activeCameraId requested=" +
                            activeRequestedResolution?.let { "${it.width}x${it.height}" }.orEmpty() +
                            " bound=${capture.resolutionInfo?.resolution?.let { "${it.width}x${it.height}" }} " +
                            "actual=${actual?.let { "${it.width}x${it.height}" } ?: "unreadable"} uri=$uri",
                    )
                }
                continuation.resume(
                    uri?.let(Result.Companion::success)
                        ?: Result.failure(IllegalStateException("Photo was saved but Android did not return its URI.")),
                )
            }

            override fun onError(exception: ImageCaptureException) {
                _state.value = CameraSessionState.Error(exception.message ?: "Photo capture failed.")
                continuation.resume(Result.failure(exception))
            }
        })
    }

    private fun readJpegDimensions(uri: Uri): Size? = runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor, null, options)
        }
        options.takeIf { it.outWidth > 0 && it.outHeight > 0 }
            ?.let { Size(it.outWidth, it.outHeight) }
    }.getOrNull()

    @SuppressLint("MissingPermission")
    fun startVideo(audioEnabled: Boolean): Flow<VideoRecordEvent> = callbackFlow {
        if (recording != null) {
            close(IllegalStateException("A video recording is already in progress."))
            return@callbackFlow
        }
        val capture = videoCapture
        if (capture == null) {
            close(IllegalStateException("Video capture is unavailable for the active camera configuration."))
            return@callbackFlow
        }
        _state.value = CameraSessionState.StartingRecording
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, mediaRepository.createVideoName("mp4"))
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/AdaptiveCompositionCamera")
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "AdaptiveCompositionCamera")
                directory.mkdirs()
                put(MediaStore.Video.Media.DATA, File(directory, getAsString(MediaStore.MediaColumns.DISPLAY_NAME)).absolutePath)
            }
        }
        val options = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).setContentValues(values).build()
        var pending = capture.output.prepareRecording(context, options)
        val canUseAudio = audioEnabled &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (canUseAudio) pending = pending.withAudioEnabled()
        recording = pending.start(mainExecutor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> _state.value = CameraSessionState.Recording(System.currentTimeMillis())
                is VideoRecordEvent.Finalize -> {
                    _state.value = if (event.hasError()) {
                        CameraSessionState.Error("Video recording failed with error ${event.error}.")
                    } else readyStateFor(activeMode)
                }
                else -> Unit
            }
            trySend(event)
            if (event is VideoRecordEvent.Finalize) close()
        }
        awaitClose {
            recording?.close()
            recording = null
        }
    }

    fun stopVideo() {
        _state.value = CameraSessionState.StoppingRecording
        recording?.stop()
    }

    fun pauseVideo(): Boolean = recording?.let {
        it.pause()
        true
    } ?: false

    fun resumeVideo(): Boolean = recording?.let {
        it.resume()
        true
    } ?: false

    fun setFlashMode(mode: FlashMode): Boolean {
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
        if (!hasFlash && mode != FlashMode.Off) return false
        if (mode == FlashMode.Torch) {
            camera?.cameraControl?.enableTorch(true)
            imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
        } else {
            camera?.cameraControl?.enableTorch(false)
            val capture = imageCapture ?: return mode == FlashMode.Off
            capture.flashMode = when (mode) {
                FlashMode.Off -> ImageCapture.FLASH_MODE_OFF
                FlashMode.Auto -> ImageCapture.FLASH_MODE_AUTO
                FlashMode.On -> ImageCapture.FLASH_MODE_ON
                FlashMode.Torch -> ImageCapture.FLASH_MODE_OFF
            }
        }
        return true
    }

    fun zoomTo(ratio: Float): Float {
        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return 1f
        val safe = ratio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
        camera?.cameraControl?.setZoomRatio(safe)
        return safe
    }

    fun focusAt(previewView: PreviewView, x: Float, y: Float) {
        val control = camera?.cameraControl ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
        ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        _state.value = CameraSessionState.Focusing
        val future = control.startFocusAndMetering(action)
        future.addListener({
            if (_state.value == CameraSessionState.Focusing) _state.value = readyStateFor(activeMode)
        }, mainExecutor)
    }

    fun setExposure(index: Int): Int {
        val range = camera?.cameraInfo?.exposureState?.exposureCompensationRange ?: return 0
        val safe = index.coerceIn(range.lower, range.upper)
        camera?.cameraControl?.setExposureCompensationIndex(safe)
        return safe
    }

    @androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
    fun setManualExposure(iso: Int, exposureNanos: Long): Pair<Int, Long>? {
        val isoRange = sensorIsoRange ?: return null
        val exposureRange = sensorExposureRange ?: return null
        manualIso = iso.coerceIn(isoRange.lower, isoRange.upper)
        manualExposureNanos = exposureNanos.coerceIn(
            exposureRange.lower,
            minOf(exposureRange.upper, 250_000_000L).coerceAtLeast(exposureRange.lower),
        )
        return if (applyProControls()) manualIso!! to manualExposureNanos!! else null
    }

    @androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
    fun setManualFocus(distance: Float?): Float? {
        manualFocusDistance = distance?.coerceIn(0f, sensorMinimumFocusDistance)
        return if (applyProControls()) manualFocusDistance else null
    }

    @androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
    fun setWhiteBalance(mode: Int): Boolean {
        if (mode !in availableWhiteBalanceModes) return false
        manualWhiteBalanceMode = mode
        return applyProControls()
    }

    @androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
    fun resetProControls(): Boolean {
        val control = camera?.cameraControl ?: return false
        manualIso = null
        manualExposureNanos = null
        manualFocusDistance = null
        manualWhiteBalanceMode = CameraMetadata.CONTROL_AWB_MODE_AUTO
        Camera2CameraControl.from(control).clearCaptureRequestOptions()
        return true
    }

    @androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
    private fun applyProControls(): Boolean {
        val control = camera?.cameraControl ?: return false
        val options = CaptureRequestOptions.Builder().apply {
            val iso = manualIso
            val exposureNanos = manualExposureNanos
            if (iso != null && exposureNanos != null) {
                setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
                setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureNanos)
                setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, exposureNanos.coerceAtLeast(33_333_333L))
            }
            manualFocusDistance?.let {
                setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, it)
            }
            setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, manualWhiteBalanceMode)
        }.build()
        Camera2CameraControl.from(control).setCaptureRequestOptions(options)
        return true
    }

    private fun readyStateFor(mode: CameraMode): CameraSessionState = when (mode) {
        CameraMode.Pro -> CameraSessionState.ProReady
        CameraMode.Documents -> CameraSessionState.DocumentReady
        CameraMode.Video -> CameraSessionState.VideoReady
        CameraMode.MaximumResolution -> CameraSessionState.HighResolutionReady
        CameraMode.SlowMotion,
        CameraMode.HighFrameRate,
        -> CameraSessionState.SlowMotionReady
        CameraMode.TimeLapse -> CameraSessionState.TimeLapseReady
        else -> CameraSessionState.PhotoReady
    }

    fun release() {
        bindGeneration.incrementAndGet()
        recording?.close()
        recording = null
        provider?.unbindAll()
        imageCapture = null
        videoCapture = null
        previewUseCase = null
        camera = null
        sensorIsoRange = null
        sensorExposureRange = null
        sensorMinimumFocusDistance = 0f
        manualIso = null
        manualExposureNanos = null
        manualFocusDistance = null
        manualWhiteBalanceMode = CameraMetadata.CONTROL_AWB_MODE_AUTO
        availableWhiteBalanceModes = emptyList()
        supportedVideoQualities = emptyList()
        selectedVideoQuality = VideoQualitySetting.Auto
        requestedFpsRange = null
        requestedStabilization = VideoStabilizationMode.Off
        _stabilizationStatus.value = "Off"
        _stabilizationEvidence.value = "No CaptureResult received"
        _state.value = CameraSessionState.Released
        captureExecutor.shutdown()
    }
}

private fun VideoQualitySetting.labelStatic(): String = when (this) {
    VideoQualitySetting.Auto -> "Auto"
    VideoQualitySetting.UHD -> "4K"
    VideoQualitySetting.FHD -> "1080p"
    VideoQualitySetting.HD -> "720p"
    VideoQualitySetting.SD -> "480p"
}
