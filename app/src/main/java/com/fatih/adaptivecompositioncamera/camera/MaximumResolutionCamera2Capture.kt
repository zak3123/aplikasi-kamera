package com.fatih.adaptivecompositioncamera.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.Surface
import androidx.annotation.RequiresApi
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.FlashMode
import com.fatih.adaptivecompositioncamera.media.AndroidMediaRepository
import com.fatih.adaptivecompositioncamera.utility.CameraMath
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/**
 * One-shot Camera2 path for Android-exposed high-resolution JPEG sizes.
 * CameraX remains responsible for preview and ordinary capture.
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
    ): Result<Uri> = runCatching {
        require(resolution.maximumSensorMode || resolution.highResolution) {
            "The requested output is not an Android high-resolution JPEG."
        }
        require(resolution.width > 0 && resolution.height > 0) { "The requested output dimensions are invalid." }
        withTimeout(20_000L) {
            suspendCancellableCoroutine { continuation ->
                val operation = CaptureOperation(
                    context = context,
                    mediaRepository = mediaRepository,
                    cameraId = cameraId,
                    resolution = resolution,
                    targetRotation = targetRotation,
                    flashMode = flashMode,
                    onSuccess = { uri -> if (continuation.isActive) continuation.resume(uri) },
                    onFailure = { error -> if (continuation.isActive) continuation.resumeWithException(error) },
                )
                continuation.invokeOnCancellation { operation.cancel() }
                operation.start()
            }
        }
    }

    private class CaptureOperation(
        private val context: Context,
        private val mediaRepository: AndroidMediaRepository,
        private val cameraId: String,
        private val resolution: CameraResolution,
        private val targetRotation: Int,
        private val flashMode: FlashMode,
        private val onSuccess: (Uri) -> Unit,
        private val onFailure: (Throwable) -> Unit,
    ) {
        private val completed = AtomicBoolean(false)
        private val thread = HandlerThread("AdaptiveCamera-MaxResolution").apply { start() }
        private val handler = Handler(thread.looper)
        private val executor = Executor { command -> handler.post(command) }
        private val manager = context.getSystemService(CameraManager::class.java)
        private val reader = ImageReader.newInstance(
            resolution.width,
            resolution.height,
            ImageFormat.JPEG,
            2,
        )

        private var device: CameraDevice? = null
        private var session: CameraCaptureSession? = null

        @SuppressLint("MissingPermission")
        fun start() {
            runCatching { validateHighResolutionOutput() }.onFailure {
                completeFailure(it)
                return
            }
            reader.setOnImageAvailableListener({ imageReader ->
                val image = imageReader.acquireNextImage() ?: return@setOnImageAvailableListener
                val result = runCatching {
                    val buffer = image.planes.firstOrNull()?.buffer
                        ?: throw IllegalStateException("High-resolution capture returned no JPEG plane.")
                    val jpeg = ByteArray(buffer.remaining()).also(buffer::get)
                    saveJpeg(jpeg)
                }
                image.close()
                result.onSuccess(::completeSuccess).onFailure(::completeFailure)
            }, handler)

            runCatching {
                manager.openCamera(cameraId, executor, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        if (completed.get()) {
                            camera.close()
                            return
                        }
                        device = camera
                        createSession(camera)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        completeFailure(IllegalStateException("The camera disconnected during high-resolution capture."))
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        completeFailure(IllegalStateException("Camera2 high-resolution open failed with error $error."))
                    }
                })
            }.onFailure(::completeFailure)
        }

        private fun validateHighResolutionOutput() {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val sizes = if (resolution.maximumSensorMode) {
                val capabilities =
                    characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES] ?: intArrayOf()
                require(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR in capabilities,
                ) {
                    "This camera does not expose Android's ultra-high-resolution sensor capability."
                }
                characteristics[
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION
                ]?.getOutputSizes(ImageFormat.JPEG).orEmpty()
            } else {
                characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                    ?.getHighResolutionOutputSizes(ImageFormat.JPEG).orEmpty()
            }
            require(sizes.any { it.width == resolution.width && it.height == resolution.height }) {
                "${resolution.width} x ${resolution.height} is no longer exposed as a high-resolution JPEG."
            }
        }

        fun cancel() {
            if (completed.compareAndSet(false, true)) closeResources()
        }

        private fun createSession(camera: CameraDevice) {
            runCatching {
                val output = OutputConfiguration(reader.surface).apply {
                    if (resolution.maximumSensorMode) {
                        addSensorPixelModeUsed(CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
                    }
                }
                val configuration = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    listOf(output),
                    executor,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(captureSession: CameraCaptureSession) {
                            if (completed.get()) {
                                captureSession.close()
                                return
                            }
                            session = captureSession
                            issueCapture(camera, captureSession)
                        }

                        override fun onConfigureFailed(captureSession: CameraCaptureSession) {
                            captureSession.close()
                            completeFailure(
                                IllegalStateException(
                                    "Android rejected the ${resolution.width} x ${resolution.height} high-resolution stream.",
                                ),
                            )
                        }
                    },
                )
                camera.createCaptureSession(configuration)
            }.onFailure(::completeFailure)
        }

        private fun issueCapture(camera: CameraDevice, captureSession: CameraCaptureSession) {
            runCatching {
                val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(reader.surface)
                    if (resolution.maximumSensorMode) {
                        set(CaptureRequest.SENSOR_PIXEL_MODE, CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
                    }
                    set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    set(
                        CaptureRequest.CONTROL_AE_MODE,
                        when (flashMode) {
                            FlashMode.Auto -> CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH
                            FlashMode.On, FlashMode.Torch -> CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                            FlashMode.Off -> CameraMetadata.CONTROL_AE_MODE_ON
                        },
                    )
                    set(CaptureRequest.JPEG_QUALITY, 95.toByte())
                    set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation())
                }.build()
                captureSession.capture(
                    request,
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult,
                        ) = Unit

                        override fun onCaptureFailed(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            failure: CaptureFailure,
                        ) {
                            completeFailure(
                                IllegalStateException(
                                    "High-resolution capture failed with reason ${failure.reason}.",
                                ),
                            )
                        }
                    },
                    handler,
                )
            }.onFailure(::completeFailure)
        }

        private fun jpegOrientation(): Int {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val sensorDegrees = characteristics[CameraCharacteristics.SENSOR_ORIENTATION] ?: 0
            val deviceDegrees = when (targetRotation) {
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }
            val frontFacing =
                characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_FRONT
            return CameraMath.jpegOrientationDegrees(sensorDegrees, deviceDegrees, frontFacing)
        }

        private fun saveJpeg(jpeg: ByteArray): Uri {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, mediaRepository.createImageName("jpg"))
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/AdaptiveCompositionCamera")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("MediaStore could not create the maximum-resolution photo.")
            return try {
                resolver.openOutputStream(uri, "w")?.use { output -> output.write(jpeg) }
                    ?: throw IllegalStateException("MediaStore could not open the photo output stream.")
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
            closeResources()
            onFailure(error)
        }

        private fun closeResources() {
            reader.setOnImageAvailableListener(null, null)
            session?.close()
            session = null
            device?.close()
            device = null
            reader.close()
            thread.quitSafely()
        }
    }
}
