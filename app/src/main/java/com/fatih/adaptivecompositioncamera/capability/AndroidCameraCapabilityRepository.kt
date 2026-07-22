package com.fatih.adaptivecompositioncamera.capability

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Range
import android.util.Size
import com.fatih.adaptivecompositioncamera.domain.model.CameraCapability
import com.fatih.adaptivecompositioncamera.domain.model.CameraCapabilityRepository
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.CapabilityReport
import com.fatih.adaptivecompositioncamera.domain.model.ExtensionSupport
import com.fatih.adaptivecompositioncamera.domain.model.HardwareLevel
import com.fatih.adaptivecompositioncamera.domain.model.HighSpeedVideoOption
import com.fatih.adaptivecompositioncamera.domain.model.LensFacing
import com.fatih.adaptivecompositioncamera.domain.model.LensRole
import com.fatih.adaptivecompositioncamera.domain.model.StabilizationSupport
import com.fatih.adaptivecompositioncamera.utility.CameraMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidCameraCapabilityRepository(
    private val context: Context,
) : CameraCapabilityRepository {
    private val manager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val _capabilityReport = MutableStateFlow<CapabilityReport?>(null)
    override val capabilityReport: StateFlow<CapabilityReport?> = _capabilityReport.asStateFlow()

    override suspend fun refresh(): CapabilityReport {
        val cameras = safeCameraIds().mapNotNull { id -> inspectCamera(id) }
        return CapabilityReport(
            generatedAtEpochMillis = System.currentTimeMillis(),
            appPackage = context.packageName,
            cameras = cameras,
        ).also { _capabilityReport.value = it }
    }

    private fun safeCameraIds(): List<String> = try {
        manager.cameraIdList.toList()
    } catch (_: CameraAccessException) {
        emptyList()
    } catch (_: SecurityException) {
        emptyList()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }

    @SuppressLint("InlinedApi", "NewApi")
    private fun inspectCamera(cameraId: String): CameraCapability? {
        val c = try {
            manager.getCameraCharacteristics(cameraId)
        } catch (_: CameraAccessException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        } catch (_: SecurityException) {
            return null
        }

        val facing = c.safe(CameraCharacteristics.LENS_FACING).toLensFacing()
        val capabilities = c.safe(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList().orEmpty().toSet()
        val streamMap = c.safe(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val jpegSizes = streamMap?.getOutputSizes(ImageFormat.JPEG).orEmpty().toList()
        val maximumStreamMap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            c.safe(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
        } else null
        val maximumJpegSizes = maximumStreamMap?.getOutputSizes(ImageFormat.JPEG).orEmpty().toList()
        val heicSizes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            streamMap?.getOutputSizes(ImageFormat.HEIC).orEmpty().toList()
        } else emptyList()
        val rawSizes = streamMap?.getOutputSizes(ImageFormat.RAW_SENSOR).orEmpty().toList()
        val yuvSizes = streamMap?.getOutputSizes(ImageFormat.YUV_420_888).orEmpty().toList()
        val privateSizes = streamMap?.getOutputSizes(MediaRecorder::class.java).orEmpty().toList()
        val focalLengths = c.safe(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList().orEmpty()
        val physicalSize = c.safe(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) c.physicalCameraIds.toList() else emptyList()
        val maxJpeg = CameraMath.sortResolutions(jpegSizes, "JPEG").firstOrNull()

        return CameraCapability(
            cameraId = cameraId,
            friendlyName = friendlyName(cameraId, facing, physicalIds, focalLengths, physicalSize),
            lensFacing = facing,
            lensRole = inferLensRole(facing, focalLengths, physicalSize),
            hardwareLevel = c.safe(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL).toHardwareLevel(),
            sensorOrientation = c.safe(CameraCharacteristics.SENSOR_ORIENTATION),
            activeArray = c.safe(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)?.flattenToString(),
            preCorrectionActiveArray = c.safe(CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE)?.flattenToString(),
            pixelArray = c.safe(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)?.toText(),
            physicalSize = physicalSize?.let { "${it.width} x ${it.height} mm" },
            physicalCameraIds = physicalIds,
            focalLengths = focalLengths,
            apertures = c.safe(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList().orEmpty(),
            hasFlash = c.safe(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true,
            hasTorch = c.safe(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true,
            minFocusDistance = c.safe(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE),
            isoRange = c.safe(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.toText(),
            exposureTimeRange = c.safe(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.toText(),
            exposureCompensationRange = c.safe(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)?.toText(),
            jpegResolutions = CameraMath.sortResolutions(jpegSizes, "JPEG"),
            maximumResolutionJpegs = CameraMath.sortResolutions(maximumJpegSizes, "JPEG").map {
                it.copy(maximumSensorMode = true)
            },
            heicResolutions = CameraMath.sortResolutions(heicSizes, "HEIC"),
            rawResolutions = CameraMath.sortResolutions(rawSizes, "DNG"),
            yuvResolutions = CameraMath.sortResolutions(yuvSizes, "YUV_420_888"),
            videoResolutions = CameraMath.sortResolutions(privateSizes, "MP4"),
            fpsRanges = c.safe(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.toList().orEmpty().map { it.toText() },
            highSpeedVideo = streamMap.highSpeedOptions(),
            stabilization = stabilization(c),
            extensions = ExtensionSupport(),
            supportsRaw = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
            supportsManualSensor = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR),
            supportsManualPostProcessing = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING),
            supportsBurst = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE),
            supportsLogicalMultiCamera = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA),
            supportsDepth = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT),
            supportsBackwardCompatible = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE),
            zoomRatioRange = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                c.safe(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)?.toText()
            } else null,
            supportsUltraHighResolutionSensor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR),
            sensorPixelModes = if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR)
            ) listOf("Normal", "Maximum Resolution") else listOf("Normal"),
            unavailableReasons = buildList {
                if (maxJpeg == null) add("No JPEG output sizes were exposed by Android for this camera.")
                if (!capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)) {
                    add("This camera is not marked as backward-compatible for normal third-party camera capture.")
                }
                val sensorPixels = c.safe(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                val maximumApplicationJpeg = (maximumJpegSizes + jpegSizes)
                    .maxByOrNull { it.width.toLong() * it.height }
                if (sensorPixels != null && maximumApplicationJpeg != null) {
                    val sensorMp = CameraMath.megapixels(sensorPixels.width, sensorPixels.height)
                    val outputMp = CameraMath.megapixels(maximumApplicationJpeg.width, maximumApplicationJpeg.height)
                    if (sensorMp > outputMp + 1.0 && maximumJpegSizes.isEmpty()) {
                        add("This device may use a higher-resolution image sensor, but Android exposes a maximum application capture output of ${maximumApplicationJpeg.width} x ${maximumApplicationJpeg.height}, approximately $outputMp MP.")
                    }
                }
            },
        )
    }

    private fun stabilization(c: CameraCharacteristics): StabilizationSupport {
        val ois = c.safe(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.toList().orEmpty()
            .any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON }
        val video = c.safe(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)?.toList().orEmpty()
        val previewStabilization = Build.VERSION.SDK_INT >= 33 &&
            video.any { it == CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION }
        return StabilizationSupport(
            optical = ois,
            electronicVideo = video.any { it == CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON },
            preview = previewStabilization,
        )
    }

    private fun android.hardware.camera2.params.StreamConfigurationMap?.highSpeedOptions(): List<HighSpeedVideoOption> {
        if (this == null) return emptyList()
        return try {
            val sizes = highSpeedVideoSizes?.toList().orEmpty()
            sizes.flatMap { size ->
                getHighSpeedVideoFpsRangesFor(size)?.toList().orEmpty().map { range ->
                    HighSpeedVideoOption(size.width, size.height, range.lower, range.upper)
                }
            }.distinctBy { listOf(it.width, it.height, it.minFps, it.maxFps) }
                .sortedWith(compareByDescending<HighSpeedVideoOption> { it.maxFps }.thenByDescending { it.width * it.height })
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    private fun friendlyName(
        cameraId: String,
        facing: LensFacing,
        physicalIds: List<String>,
        focalLengths: List<Float>,
        physicalSize: android.util.SizeF?,
    ): String {
        val role = inferLensRole(facing, focalLengths, physicalSize)
        return when (role) {
            LensRole.Main -> "Rear Main Camera"
            LensRole.Ultrawide -> "Rear Ultrawide Camera"
            LensRole.Telephoto -> "Rear Telephoto Camera"
            LensRole.Selfie -> "Front Camera"
            LensRole.External -> "External Camera"
            else -> when (facing) {
                LensFacing.Rear -> "Rear Camera $cameraId"
                LensFacing.Front -> "Front Camera $cameraId"
                LensFacing.External -> "External Camera $cameraId"
                LensFacing.Unknown -> "Camera $cameraId"
            }
        } + if (physicalIds.isNotEmpty()) " (logical)" else ""
    }

    private fun inferLensRole(
        facing: LensFacing,
        focalLengths: List<Float>,
        physicalSize: android.util.SizeF?,
    ): LensRole {
        if (facing == LensFacing.Front) return LensRole.Selfie
        if (facing == LensFacing.External) return LensRole.External
        val firstFocal = focalLengths.firstOrNull() ?: return LensRole.Unknown
        val sensorWidth = physicalSize?.width ?: return LensRole.Unknown
        val approximate35mm = firstFocal * (36f / sensorWidth)
        return when {
            approximate35mm < 20f -> LensRole.Ultrawide
            approximate35mm in 20f..38f -> LensRole.Main
            approximate35mm > 45f -> LensRole.Telephoto
            else -> LensRole.Unknown
        }
    }

    private fun Int?.toLensFacing(): LensFacing = when (this) {
        CameraCharacteristics.LENS_FACING_FRONT -> LensFacing.Front
        CameraCharacteristics.LENS_FACING_BACK -> LensFacing.Rear
        CameraCharacteristics.LENS_FACING_EXTERNAL -> LensFacing.External
        else -> LensFacing.Unknown
    }

    private fun Int?.toHardwareLevel(): HardwareLevel = when (this) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> HardwareLevel.Legacy
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> HardwareLevel.Limited
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> HardwareLevel.Full
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> HardwareLevel.Level3
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> HardwareLevel.External
        else -> HardwareLevel.Unknown
    }

    private fun Size.toText(): String = "${width} x ${height}"
    private fun Range<*>.toText(): String = "${lower}..${upper}"

    private fun <T> CameraCharacteristics.safe(key: CameraCharacteristics.Key<T>): T? {
        return try {
            get(key)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }
}
