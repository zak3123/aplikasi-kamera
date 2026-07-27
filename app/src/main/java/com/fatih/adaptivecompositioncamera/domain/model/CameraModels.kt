package com.fatih.adaptivecompositioncamera.domain.model

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
enum class LensFacing { Front, Rear, External, Unknown }

@Serializable
enum class LensRole { Main, Wide, Ultrawide, Telephoto, Macro, Selfie, External, Unknown }

@Serializable
enum class HardwareLevel { Legacy, Limited, Full, Level3, External, Unknown }

@Serializable
enum class CameraMode {
    Portrait,
    Photo,
    Video,
    Pro,
    Documents,
    Night,
    MaximumResolution,
    SlowMotion,
    HighFrameRate,
    TimeLapse,
    Burst,
    PanoramaExperimental,
}

val CameraMode.isStillPhotoMode: Boolean
    get() = this in setOf(
        CameraMode.Photo,
        CameraMode.Portrait,
        CameraMode.Pro,
        CameraMode.Documents,
        CameraMode.Night,
        CameraMode.MaximumResolution,
        CameraMode.Burst,
        CameraMode.PanoramaExperimental,
    )

val CameraMode.isVideoMode: Boolean
    get() = this in setOf(
        CameraMode.Video,
        CameraMode.SlowMotion,
        CameraMode.HighFrameRate,
        CameraMode.TimeLapse,
    )

@Serializable
enum class PhotoAspectRatio {
    FullSensor,
    Ratio4x3,
    Ratio3x2,
    Ratio16x9,
    Ratio1x1,
    FullScreen,
}

@Serializable
enum class CompositionGuide {
    None,
    RuleOfThirds,
    LeadingLines,
    VanishingPoint,
    GoldenRatio,
    GoldenSpiral,
    FrameInFrame,
    Centered,
    Symmetry,
    Diagonal,
    GoldenTriangle,
    TextureRepetition,
    Foreground,
    EyeLine,
    HorizonLevel,
}

enum class FlashMode { Off, Auto, On, Torch }

enum class SpiralOrientation { TopLeft, TopRight, BottomLeft, BottomRight }

enum class GuideLineStyle { Solid, Dashed }

enum class ForegroundZone { Bottom, Left, Right }

enum class PhotoQualityPreset { Maximum, High, Medium, StorageSaver, Custom }

enum class VideoQualitySetting { Auto, UHD, FHD, HD, SD }

enum class VideoStabilizationMode { Off, Standard, Preview, Optical, Auto, Unsupported }

@Serializable
data class VideoFpsRange(val min: Int, val max: Int) {
    val label: String get() = if (min == max) "$max FPS" else "$min–$max FPS"
}

data class GuideStyle(
    val colorArgb: Long = 0xFFFFFFFF,
    val opacity: Float = 0.82f,
    val thicknessDp: Float = 1.4f,
    val lineStyle: GuideLineStyle = GuideLineStyle.Solid,
    val outline: Boolean = true,
    val intersections: Boolean = true,
    val spiralOrientation: SpiralOrientation = SpiralOrientation.TopRight,
    val spiralClockwise: Boolean = true,
    val spiralHorizontalFlip: Boolean = false,
    val spiralVerticalFlip: Boolean = false,
    val vanishingLineCount: Int = 5,
    val frameCornerDp: Float = 12f,
    val frameDimOutside: Boolean = true,
    val textureGridSize: Int = 6,
    val textureDiagonals: Boolean = true,
    val foregroundZone: ForegroundZone = ForegroundZone.Bottom,
    val foregroundFraction: Float = 0.32f,
    val faceSafeArea: Boolean = true,
    val centeredTarget: Boolean = true,
    val overlayRotationDegrees: Int = 0,
    val overlayMirrorHorizontal: Boolean = false,
    val overlayLocked: Boolean = false,
)

@Serializable
data class CameraResolution(
    val width: Int,
    val height: Int,
    val format: String,
    val megapixels: Double,
    val aspectRatioLabel: String,
    val recommended: Boolean = false,
    val maximum: Boolean = false,
    val highResolution: Boolean = false,
    val maximumSensorMode: Boolean = false,
) {
    val id: String get() = "$width:$height:$format:$highResolution:$maximumSensorMode"
    val megapixelLabel: String get() = if (megapixels % 1.0 == 0.0) "${megapixels.toInt()} MP" else "$megapixels MP"
}

@Serializable
data class HighSpeedVideoOption(
    val width: Int,
    val height: Int,
    val minFps: Int,
    val maxFps: Int,
    val playbackFps: Int = 30,
) {
    val slowMotionFactor: Double get() = maxFps.toDouble() / playbackFps.toDouble()
}

@Serializable
data class StabilizationSupport(
    val optical: Boolean,
    val electronicVideo: Boolean,
    val preview: Boolean,
)

@Serializable
data class ExtensionSupport(
    val auto: Boolean = false,
    val hdr: Boolean = false,
    val night: Boolean = false,
    val bokeh: Boolean = false,
    val faceRetouch: Boolean = false,
)

@Serializable
data class CameraCapability(
    val cameraId: String,
    val friendlyName: String,
    val lensFacing: LensFacing,
    val lensRole: LensRole,
    val hardwareLevel: HardwareLevel,
    val sensorOrientation: Int?,
    val activeArray: String?,
    val preCorrectionActiveArray: String? = null,
    val pixelArray: String?,
    val physicalSize: String? = null,
    val physicalCameraIds: List<String>,
    val focalLengths: List<Float>,
    val apertures: List<Float>,
    val hasFlash: Boolean,
    val hasTorch: Boolean,
    val minFocusDistance: Float?,
    val isoRange: String?,
    val exposureTimeRange: String?,
    val exposureCompensationRange: String?,
    val jpegResolutions: List<CameraResolution>,
    val highResolutionJpegs: List<CameraResolution> = emptyList(),
    val maximumResolutionJpegs: List<CameraResolution> = emptyList(),
    val heicResolutions: List<CameraResolution> = emptyList(),
    val rawResolutions: List<CameraResolution>,
    val yuvResolutions: List<CameraResolution> = emptyList(),
    val videoResolutions: List<CameraResolution>,
    val fpsRanges: List<String>,
    val highSpeedVideo: List<HighSpeedVideoOption>,
    val stabilization: StabilizationSupport,
    val extensions: ExtensionSupport,
    val supportsRaw: Boolean,
    val supportsManualSensor: Boolean,
    val supportsManualPostProcessing: Boolean,
    val supportsBurst: Boolean,
    val supportsLogicalMultiCamera: Boolean,
    val supportsDepth: Boolean,
    val supportsBackwardCompatible: Boolean = true,
    val zoomRatioRange: String? = null,
    val supportsUltraHighResolutionSensor: Boolean = false,
    val sensorPixelModes: List<String> = emptyList(),
    val unavailableReasons: List<String> = emptyList(),
    val isOpenable: Boolean = true,
    val isLogical: Boolean = false,
    val isPhysicalOnly: Boolean = false,
    val parentLogicalCameraIds: List<String> = emptyList(),
    val maximumPixelArray: String? = null,
    val autofocusModes: List<String> = emptyList(),
    val autoExposureModes: List<String> = emptyList(),
    val autoWhiteBalanceModes: List<String> = emptyList(),
    val videoStabilizationModes: List<String> = emptyList(),
    val opticalStabilizationModes: List<String> = emptyList(),
    val fpsRangeValues: List<VideoFpsRange> = emptyList(),
    val exposureCompensationStep: String? = null,
    val maximumDigitalZoom: Float? = null,
    val availableCaptureRequestKeys: List<String> = emptyList(),
) {
    val selectablePhotoResolutions: List<CameraResolution>
        get() = (
            maximumResolutionJpegs +
                highResolutionJpegs +
                jpegResolutions
            )
            .distinctBy { "${it.width}:${it.height}:${it.format}" }
            .sortedByDescending { it.width.toLong() * it.height }

    val displayMaximumResolution: CameraResolution?
        get() = selectablePhotoResolutions.maxByOrNull { it.width.toLong() * it.height }

    val maximumExposedResolution: CameraResolution?
        get() = (maximumResolutionJpegs + highResolutionJpegs + jpegResolutions)
            .maxByOrNull { it.width.toLong() * it.height }

    val normalMaximumResolution: CameraResolution?
        get() = jpegResolutions.maxByOrNull { it.width.toLong() * it.height }

    val maximumSensorResolution: CameraResolution?
        get() = maximumResolutionJpegs.maxByOrNull { it.width.toLong() * it.height }
}

@Serializable
data class CapabilityReport(
    val generatedAtEpochMillis: Long,
    val appPackage: String,
    val cameras: List<CameraCapability>,
    val manufacturer: String = android.os.Build.MANUFACTURER,
    val model: String = android.os.Build.MODEL,
    val androidVersion: String = "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
    val warning: String = "Reported support is based on Android-exposed camera APIs and can differ from the manufacturer stock camera application.",
)

data class AppSettings(
    val selectedCameraId: String? = null,
    val selectedResolutionIds: Map<String, String> = emptyMap(),
    val mode: CameraMode = CameraMode.Photo,
    val guide: CompositionGuide = CompositionGuide.RuleOfThirds,
    val photoAspectRatio: PhotoAspectRatio = PhotoAspectRatio.FullSensor,
    val matchPreviewCrop: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val mirrorFrontPreview: Boolean = true,
    val saveMirroredSelfie: Boolean = false,
    val screenFlash: Boolean = true,
    val includeGuideInExport: Boolean = false,
    val haptics: Boolean = true,
    val audioEnabled: Boolean = true,
    val volumeShutter: Boolean = true,
)

data class CameraDiagnostics(
    val currentCameraId: String? = null,
    val currentCameraName: String? = null,
    val sessionState: String = "Discovering",
    val mode: CameraMode = CameraMode.Photo,
    val activeResolution: String? = null,
    val requestedResolution: String? = null,
    val boundCaptureResolution: String? = null,
    val actualSavedResolution: String? = null,
    val selectedAspectRatio: String? = null,
    val sensorPixelMode: String = "Normal",
    val configurationMismatch: String? = null,
    val previewResolution: String? = null,
    val currentFps: String? = null,
    val stabilization: String = "Off",
    val requestedVideoQuality: String? = null,
    val supportedVideoQualities: String? = null,
    val requestedFps: String? = null,
    val extension: String = "None",
    val lastCameraError: String? = null,
    val lastCaptureError: String? = null,
    val lastRecordingError: String? = null,
)

sealed interface CameraSessionState {
    data object Uninitialized : CameraSessionState
    data object PermissionRequired : CameraSessionState
    data object Discovering : CameraSessionState
    data object Binding : CameraSessionState
    data object Ready : CameraSessionState
    data object PhotoReady : CameraSessionState
    data object ProReady : CameraSessionState
    data object DocumentReady : CameraSessionState
    data object VideoReady : CameraSessionState
    data object HighResolutionReady : CameraSessionState
    data object SlowMotionReady : CameraSessionState
    data object TimeLapseReady : CameraSessionState
    data object Focusing : CameraSessionState
    data object Capturing : CameraSessionState
    data object StartingRecording : CameraSessionState
    data class Recording(val startedAtMillis: Long) : CameraSessionState
    data object StoppingRecording : CameraSessionState
    data object SwitchingCamera : CameraSessionState
    data object Reconfiguring : CameraSessionState
    data class Error(val message: String) : CameraSessionState
    data object Released : CameraSessionState
}

data class CameraConfiguration(
    val lensFacing: LensFacing,
    val cameraId: String?,
    val mode: CameraMode,
    val resolution: CameraResolution?,
    val flashMode: FlashMode = FlashMode.Off,
    val stabilizationEnabled: Boolean = false,
)

data class ModeCompatibilityResult(
    val mode: CameraMode,
    val guide: CompositionGuide,
    val closeMoreSelector: Boolean = true,
    val closeProControls: Boolean = false,
    val closeCompositionSelector: Boolean = false,
    val stopDocumentAnalysis: Boolean = false,
    val stopVideoRecording: Boolean = false,
    val reason: String? = null,
)

data class RuntimeCameraInfo(
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val exposureMin: Int = 0,
    val exposureMax: Int = 0,
    val hasFlash: Boolean = false,
    val captureWidth: Int = 0,
    val captureHeight: Int = 0,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val previewWidth: Int = 0,
    val previewHeight: Int = 0,
    val targetRotation: Int = 0,
    val sensorPixelMode: String = "Normal",
    val supportsManualSensor: Boolean = false,
    val isoMin: Int = 0,
    val isoMax: Int = 0,
    val exposureTimeMinNanos: Long = 0L,
    val exposureTimeMaxNanos: Long = 0L,
    val minFocusDistance: Float = 0f,
    val availableWhiteBalanceModes: List<Int> = emptyList(),
    val supportedVideoQualities: List<VideoQualitySetting> = emptyList(),
    val selectedVideoQuality: VideoQualitySetting = VideoQualitySetting.Auto,
    val requestedFpsRange: VideoFpsRange? = null,
    val requestedStabilization: VideoStabilizationMode = VideoStabilizationMode.Off,
)

data class MediaItem(
    val uri: Uri,
    val mimeType: String,
    val displayName: String,
    val dateTakenMillis: Long,
    val width: Int = 0,
    val height: Int = 0,
    val durationMillis: Long = 0,
    val sizeBytes: Long = 0,
    val rotationDegrees: Int = 0,
    val cameraId: String? = null,
    val requestedResolution: String? = null,
    val boundResolution: String? = null,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}

data class LevelReading(
    val rollDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
) {
    val isLevel: Boolean get() = kotlin.math.abs(rollDegrees) < 1f && kotlin.math.abs(pitchDegrees) < 2f
}
