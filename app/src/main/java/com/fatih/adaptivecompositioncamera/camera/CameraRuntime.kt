package com.fatih.adaptivecompositioncamera.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import android.util.Rational
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
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
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
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.CameraSessionState
import com.fatih.adaptivecompositioncamera.domain.model.FlashMode
import com.fatih.adaptivecompositioncamera.domain.model.RuntimeCameraInfo
import com.fatih.adaptivecompositioncamera.media.AndroidMediaRepository
import java.util.concurrent.TimeUnit
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
    private val mediaRepository = AndroidMediaRepository()
    private val bindGeneration = AtomicInteger(0)
    private val _state = MutableStateFlow<CameraSessionState>(CameraSessionState.Discovering)
    val state: StateFlow<CameraSessionState> = _state.asStateFlow()

    private var provider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var camera: Camera? = null

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
        onBound: (RuntimeCameraInfo) -> Unit,
        onError: (String) -> Unit,
    ) {
        val generation = bindGeneration.incrementAndGet()
        _state.value = CameraSessionState.Binding
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
            val preview = Preview.Builder()
                .setTargetRotation(targetRotation)
                .build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            previewUseCase = preview
            val highResolutionMode = mode == CameraMode.MaximumResolution && resolution?.highResolution == true
            val maximumSensorMode = mode == CameraMode.MaximumResolution && resolution?.maximumSensorMode == true
            val captureBuilder = ImageCapture.Builder()
                .setTargetRotation(targetRotation)
                .setCaptureMode(
                if (mode == CameraMode.MaximumResolution) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            )
            if (resolution != null && !resolution.maximumSensorMode) {
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
            val requestedCapture = captureBuilder.build()
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(Quality.FHD, FallbackStrategy.higherQualityOrLowerThan(Quality.FHD)),
                ).build()
            val requestedVideo = VideoCapture.Builder(recorder)
                .setTargetRotation(targetRotation)
                .build()

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
                bindSafePhotoFallback(
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
        _state.value = CameraSessionState.Ready
        onBound(
            RuntimeCameraInfo(
                minZoom = zoomState?.minZoomRatio ?: 1f,
                maxZoom = zoomState?.maxZoomRatio ?: 1f,
                exposureMin = exposure?.lower ?: 0,
                exposureMax = exposure?.upper ?: 0,
                hasFlash = info?.hasFlashUnit() == true,
                captureWidth = captureResolution?.width ?: 0,
                captureHeight = captureResolution?.height ?: 0,
                videoWidth = videoResolution?.width ?: 0,
                videoHeight = videoResolution?.height ?: 0,
                previewWidth = previewResolution?.width ?: 0,
                previewHeight = previewResolution?.height ?: 0,
                targetRotation = targetRotation,
                sensorPixelMode = sensorPixelMode,
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
        if (resolution?.maximumSensorMode == true) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return Result.failure(
                    UnsupportedOperationException("Maximum-resolution sensor mode requires Android 12 or newer."),
                )
            }
            val resolvedCameraId = cameraId ?: return Result.failure(
                IllegalStateException("Android did not provide an ID for the maximum-resolution camera."),
            )
            _state.value = CameraSessionState.Capturing
            provider?.unbindAll()
            imageCapture = null
            videoCapture = null
            previewUseCase = null
            camera = null
            delay(180)
            val result = MaximumResolutionCamera2Capture(context, mediaRepository).capture(
                cameraId = resolvedCameraId,
                resolution = resolution,
                targetRotation = targetRotation,
                flashMode = flashMode,
            )
            _state.value = result.fold(
                onSuccess = { CameraSessionState.Reconfiguring },
                onFailure = { CameraSessionState.Error(it.message ?: "Maximum-resolution capture failed.") },
            )
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
        capture.takePicture(options, mainExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val uri = outputFileResults.savedUri
                if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val ready = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                    context.contentResolver.update(uri, ready, null, null)
                }
                _state.value = CameraSessionState.Ready
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
                    } else CameraSessionState.Ready
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
        future.addListener({ if (_state.value == CameraSessionState.Focusing) _state.value = CameraSessionState.Ready }, mainExecutor)
    }

    fun setExposure(index: Int): Int {
        val range = camera?.cameraInfo?.exposureState?.exposureCompensationRange ?: return 0
        val safe = index.coerceIn(range.lower, range.upper)
        camera?.cameraControl?.setExposureCompensationIndex(safe)
        return safe
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
        _state.value = CameraSessionState.Released
    }
}
