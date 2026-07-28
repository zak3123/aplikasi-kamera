package com.fatih.adaptivecompositioncamera.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.FlashMode
import com.fatih.adaptivecompositioncamera.domain.model.VideoStabilizationMode
import com.fatih.adaptivecompositioncamera.media.AndroidMediaRepository
import com.fatih.adaptivecompositioncamera.utility.CameraMath
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/**
 * Dedicated one-surface Camera2 still session for Android-exposed slow/high or
 * maximum-sensor JPEG outputs. Preview is intentionally owned by CameraX and is
 * unbound before this operation begins.
 */
@RequiresApi(Build.VERSION_CODES.S)
internal class MaximumResolutionCamera2Capture(
    private val context: Context,
    private val mediaRepository: AndroidMediaRepository,
) {
    suspend fun capture(
        cameraId: String,
        resolution: CameraResolution,
        targetRotation: Int,
        flashMode: FlashMode,
        stabilization: VideoStabilizationMode = VideoStabilizationMode.Off,
    ): Result<Uri> = runCatching {
        val request = HighResolutionRequest.validate(
            context = context,
            cameraId = cameraId,
            resolution = resolution,
            targetRotation = targetRotation,
            flashMode = flashMode,
            stabilization = stabilization,
        )
        withTimeout(35_000L) {
            suspendCancellableCoroutine { continuation ->
                val operation = CaptureOperation(
                    context = context,
                    mediaRepository = mediaRepository,
                    request = request,
                    onSuccess = { uri ->
                        if (continuation.isActive) continuation.resume(uri)
                    },
                    onFailure = { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    },
                )
                continuation.invokeOnCancellation { operation.cancel() }
                operation.start()
            }
        }
    }

    private data class HighResolutionRequest(
        val cameraId: String,
        val resolution: CameraResolution,
        val targetRotation: Int,
        val flashMode: FlashMode,
        val stabilization: VideoStabilizationMode,
        val sensorOrientation: Int,
        val frontFacing: Boolean,
        val useMaximumSensorMode: Boolean,
        val supportedAeModes: Set<Int>,
        val supportedAfModes: Set<Int>,
        val supportedOisModes: Set<Int>,
        val requestOis: Boolean,
    ) {
        companion object {
            fun validate(
                context: Context,
                cameraId: String,
                resolution: CameraResolution,
                targetRotation: Int,
                flashMode: FlashMode,
                stabilization: VideoStabilizationMode,
            ): HighResolutionRequest {
                require(resolution.maximumSensorMode || resolution.highResolution) {
                    "The requested output is not exposed as a slow/high-resolution Camera2 JPEG."
                }
                require(resolution.width > 0 && resolution.height > 0) {
                    "The requested JPEG dimensions are invalid."
                }
                val manager = context.getSystemService(CameraManager::class.java)
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val capabilities = characteristics[
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                ]?.toSet() ?: emptySet()
                val requestKeys = characteristics.availableCaptureRequestKeys ?: emptyList()
                val maximumMode = resolution.maximumSensorMode
                if (maximumMode) {
                    require(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR in capabilities,
                    ) {
                        "Android no longer reports ultra-high-resolution capability for camera $cameraId."
                    }
                    require(CaptureRequest.SENSOR_PIXEL_MODE in requestKeys) {
                        "Camera $cameraId does not accept SENSOR_PIXEL_MODE requests."
                    }
                }
                val advertised = if (maximumMode) {
                    characteristics[
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION
                    ]?.getOutputSizes(ImageFormat.JPEG).orEmpty().toList()
                } else {
                    characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                        ?.getHighResolutionOutputSizes(ImageFormat.JPEG).orEmpty().toList()
                }
                require(advertised.any { it.matches(resolution) }) {
                    "${resolution.width}x${resolution.height} is absent from the active Camera2 " +
                        if (maximumMode) "maximum-resolution map." else "high-resolution map."
                }
                val supportedOisModes = characteristics[
                    CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
                ]?.toSet() ?: emptySet()
                val requestOis = stabilization in setOf(
                    VideoStabilizationMode.Optical,
                    VideoStabilizationMode.Auto,
                ) && CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON in supportedOisModes
                CameraEvidenceLogger.record(
                    context,
                    "MAX_CAPTURE",
                    "validated camera=$cameraId requested=${resolution.width}x${resolution.height} " +
                        "megapixels=${resolution.megapixels} maximumPixelMode=$maximumMode " +
                        "advertised=${advertised.joinToString { "${it.width}x${it.height}" }} " +
                        "requestedStabilization=${stabilization.name} supportedOis=$supportedOisModes " +
                        "requestOis=$requestOis",
                )
                return HighResolutionRequest(
                    cameraId = cameraId,
                    resolution = resolution,
                    targetRotation = targetRotation,
                    flashMode = flashMode,
                    stabilization = stabilization,
                    sensorOrientation = characteristics[CameraCharacteristics.SENSOR_ORIENTATION] ?: 0,
                    frontFacing = characteristics[CameraCharacteristics.LENS_FACING] ==
                        CameraCharacteristics.LENS_FACING_FRONT,
                    useMaximumSensorMode = maximumMode,
                    supportedAeModes = characteristics[
                        CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES
                    ]?.toSet() ?: emptySet(),
                    supportedAfModes = characteristics[
                        CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
                    ]?.toSet() ?: emptySet(),
                    supportedOisModes = supportedOisModes,
                    requestOis = requestOis,
                )
            }
        }
    }

    private class CaptureOperation(
        private val context: Context,
        private val mediaRepository: AndroidMediaRepository,
        private val request: HighResolutionRequest,
        private val onSuccess: (Uri) -> Unit,
        private val onFailure: (Throwable) -> Unit,
    ) {
        private val completed = AtomicBoolean(false)
        private val captureMetadata = AtomicReference("CaptureResult pending")
        private val thread = HandlerThread("AdaptiveCamera2-MaxJpeg").apply { start() }
        private val handler = Handler(thread.looper)
        private val executor = Executor(handler::post)
        private val manager = context.getSystemService(CameraManager::class.java)
        private val reader = ImageReader.newInstance(
            request.resolution.width,
            request.resolution.height,
            ImageFormat.JPEG,
            1,
        )

        private var cameraDevice: CameraDevice? = null
        private var captureSession: CameraCaptureSession? = null

        @SuppressLint("MissingPermission")
        fun start() {
            reader.setOnImageAvailableListener({ source ->
                val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
                val imageTimestamp = image.timestamp
                val result = runCatching {
                    image.use { captured ->
                        val plane = captured.planes.firstOrNull()
                            ?: error("Camera2 returned a JPEG without an image plane.")
                        val bytes = ByteArray(plane.buffer.remaining())
                        plane.buffer.get(bytes)
                        val dimensions = decodeDimensions(bytes)
                        require(dimensions.matches(request.resolution)) {
                            "Camera2 returned ${dimensions.width}x${dimensions.height}; " +
                                "requested ${request.resolution.width}x${request.resolution.height}."
                        }
                        val uri = saveVerifiedJpeg(bytes, dimensions)
                        CameraEvidenceLogger.record(
                            context,
                            "MAX_CAPTURE",
                            "saved uri=$uri actual=${dimensions.width}x${dimensions.height} " +
                                "bytes=${bytes.size} imageTimestampNs=$imageTimestamp " +
                                "metadata=${captureMetadata.get()}",
                        )
                        uri
                    }
                }
                result.onSuccess(::completeSuccess).onFailure(::completeFailure)
            }, handler)

            CameraEvidenceLogger.record(
                context,
                "MAX_CAPTURE",
                "open camera=${request.cameraId} output=${request.resolution.width}x${request.resolution.height}",
            )
            runCatching {
                manager.openCamera(request.cameraId, executor, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        if (completed.get()) {
                            camera.close()
                            return
                        }
                        cameraDevice = camera
                        configureSession(camera)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        completeFailure(IllegalStateException("Camera disconnected before the JPEG was delivered."))
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        completeFailure(IllegalStateException("Camera2 open failed with error=$error."))
                    }
                })
            }.onFailure(::completeFailure)
        }

        fun cancel() {
            if (completed.compareAndSet(false, true)) {
                CameraEvidenceLogger.record(context, "MAX_CAPTURE", "cancelled camera=${request.cameraId}")
                closeResources()
            }
        }

        private fun configureSession(camera: CameraDevice) {
            runCatching {
                val output = OutputConfiguration(reader.surface)
                if (request.useMaximumSensorMode) {
                    output.addSensorPixelModeUsed(CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
                }
                val configuration = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    listOf(output),
                    executor,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            if (completed.get()) {
                                session.close()
                                return
                            }
                            captureSession = session
                            submitStillRequest(camera, session)
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            session.close()
                            completeFailure(
                                IllegalStateException(
                                    "Camera2 rejected the verified ${request.resolution.width}x" +
                                        "${request.resolution.height} JPEG-only session.",
                                ),
                            )
                        }
                    },
                )
                camera.createCaptureSession(configuration)
            }.onFailure(::completeFailure)
        }

        private fun submitStillRequest(camera: CameraDevice, session: CameraCaptureSession) {
            runCatching {
                val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(reader.surface)
                    set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    set(CaptureRequest.CONTROL_AE_MODE, chooseAeMode())
                    if (CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE in request.supportedAfModes) {
                        set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    }
                    if (request.flashMode == FlashMode.Torch) {
                        set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)
                    }
                    if (request.useMaximumSensorMode) {
                        set(CaptureRequest.SENSOR_PIXEL_MODE, CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
                    }
                    if (request.supportedOisModes.isNotEmpty()) {
                        set(
                            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                            if (request.requestOis) {
                                CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                            } else {
                                CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
                            },
                        )
                    }
                    set(CaptureRequest.JPEG_QUALITY, 95.toByte())
                    set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation())
                }.build()
                CameraEvidenceLogger.record(
                    context,
                    "MAX_CAPTURE",
                    "submit aeMode=${captureRequest[CaptureRequest.CONTROL_AE_MODE]} " +
                        "afMode=${captureRequest[CaptureRequest.CONTROL_AF_MODE]} " +
                        "pixelMode=${captureRequest[CaptureRequest.SENSOR_PIXEL_MODE]} " +
                        "requestOis=${captureRequest[CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE]} " +
                        "jpegOrientation=${captureRequest[CaptureRequest.JPEG_ORIENTATION]}",
                )
                session.capture(captureRequest, captureCallback, handler)
            }.onFailure(::completeFailure)
        }

        private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult,
            ) {
                val metadata = buildString {
                    append("frame=${result.frameNumber}")
                    append(",sensorTimestampNs=${result[CaptureResult.SENSOR_TIMESTAMP]}")
                    append(",pixelMode=${result[CaptureResult.SENSOR_PIXEL_MODE]}")
                    append(",requestedOis=${request[CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE]}")
                    append(",resultOis=${result[CaptureResult.LENS_OPTICAL_STABILIZATION_MODE]}")
                    append(",crop=${result[CaptureResult.SCALER_CROP_REGION]}")
                    append(",aeState=${result[CaptureResult.CONTROL_AE_STATE]}")
                    append(",afState=${result[CaptureResult.CONTROL_AF_STATE]}")
                    append(",flashState=${result[CaptureResult.FLASH_STATE]}")
                    append(",exposureNs=${result[CaptureResult.SENSOR_EXPOSURE_TIME]}")
                    append(",iso=${result[CaptureResult.SENSOR_SENSITIVITY]}")
                }
                captureMetadata.set(metadata)
                CameraEvidenceLogger.record(context, "MAX_CAPTURE_RESULT", metadata)
            }

            override fun onCaptureFailed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                failure: CaptureFailure,
            ) {
                completeFailure(
                    IllegalStateException(
                        "Camera2 still request failed reason=${failure.reason} " +
                            "sequence=${failure.sequenceId} frame=${failure.frameNumber}.",
                    ),
                )
            }

            override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
                completeFailure(IllegalStateException("Camera2 capture sequence $sequenceId was aborted."))
            }
        }

        private fun chooseAeMode(): Int {
            val preferred = when (request.flashMode) {
                FlashMode.Auto -> CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH
                FlashMode.On -> CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                FlashMode.Off, FlashMode.Torch -> CameraMetadata.CONTROL_AE_MODE_ON
            }
            return preferred.takeIf(request.supportedAeModes::contains)
                ?: CameraMetadata.CONTROL_AE_MODE_ON.takeIf(request.supportedAeModes::contains)
                ?: CameraMetadata.CONTROL_AE_MODE_OFF
        }

        private fun jpegOrientation(): Int {
            val deviceDegrees = when (request.targetRotation) {
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }
            return CameraMath.jpegOrientationDegrees(
                request.sensorOrientation,
                deviceDegrees,
                request.frontFacing,
            )
        }

        private fun decodeDimensions(jpeg: ByteArray): Size {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options)
            require(options.outWidth > 0 && options.outHeight > 0) {
                "Camera2 returned bytes that are not a decodable JPEG."
            }
            return Size(options.outWidth, options.outHeight)
        }

        private fun saveVerifiedJpeg(jpeg: ByteArray, dimensions: Size): Uri {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, mediaRepository.createImageName("jpg"))
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/AdaptiveCompositionCamera")
                put(MediaStore.Images.Media.IS_PENDING, 1)
                put(MediaStore.Images.Media.WIDTH, dimensions.width)
                put(MediaStore.Images.Media.HEIGHT, dimensions.height)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore could not allocate the maximum-resolution photo.")
            return try {
                resolver.openOutputStream(uri, "w")?.use { it.write(jpeg) }
                    ?: error("MediaStore could not open the JPEG output stream.")
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null,
                )
                uri
            } catch (error: Throwable) {
                resolver.delete(uri, null, null)
                throw error
            }
        }

        private fun completeSuccess(uri: Uri) {
            if (!completed.compareAndSet(false, true)) return
            closeResources()
            onSuccess(uri)
        }

        private fun completeFailure(error: Throwable) {
            if (!completed.compareAndSet(false, true)) return
            CameraEvidenceLogger.record(
                context,
                "MAX_CAPTURE_ERROR",
                "${error::class.java.simpleName}: ${error.message}",
            )
            closeResources()
            onFailure(error)
        }

        private fun closeResources() {
            reader.setOnImageAvailableListener(null, null)
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            reader.close()
            thread.quitSafely()
        }
    }
}

private fun Size.matches(resolution: CameraResolution): Boolean =
    (width == resolution.width && height == resolution.height) ||
        (width == resolution.height && height == resolution.width)
