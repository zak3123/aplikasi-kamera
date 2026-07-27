package com.fatih.adaptivecompositioncamera.capability

import com.fatih.adaptivecompositioncamera.domain.model.CameraCapability
import com.fatih.adaptivecompositioncamera.domain.model.CameraConfiguration
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.ExtensionResolver
import com.fatih.adaptivecompositioncamera.domain.model.ExtensionSupport
import com.fatih.adaptivecompositioncamera.domain.model.HighSpeedCapabilityProvider
import com.fatih.adaptivecompositioncamera.domain.model.HighSpeedVideoOption
import com.fatih.adaptivecompositioncamera.domain.model.LensFacing
import com.fatih.adaptivecompositioncamera.domain.model.StabilizationResolver
import com.fatih.adaptivecompositioncamera.domain.model.StabilizationSupport
import com.fatih.adaptivecompositioncamera.domain.model.VideoStabilizationMode

class CameraConfigurationResolver {
    fun availableModes(capability: CameraCapability): List<CameraMode> = buildList {
        add(CameraMode.Photo)
        if (capability.videoResolutions.isNotEmpty()) add(CameraMode.Video)
        add(CameraMode.Documents)
        if (capability.supportsManualSensor) add(CameraMode.Pro)
        val recommended = capability.jpegResolutions.firstOrNull { it.recommended }
        val maximum = capability.displayMaximumResolution
        if (maximum != null && recommended != null && maximum.megapixels > recommended.megapixels * 1.2) {
            add(CameraMode.MaximumResolution)
        }
    }

    fun safeResolution(capability: CameraCapability, requested: CameraResolution?): CameraResolution? {
        val choices = capability.selectablePhotoResolutions
        return choices.firstOrNull { it.id == requested?.id }
            ?: choices.firstOrNull { it.recommended }
            ?: choices.firstOrNull()
    }

    fun resolve(
        capability: CameraCapability,
        mode: CameraMode,
        resolution: CameraResolution?,
    ): CameraConfiguration {
        val safeMode = mode.takeIf { it in availableModes(capability) } ?: CameraMode.Photo
        return CameraConfiguration(
            lensFacing = capability.lensFacing,
            cameraId = capability.cameraId,
            mode = safeMode,
            resolution = safeResolution(capability, resolution),
        )
    }
}

class ModeConflictResolver {
    fun resolve(configuration: CameraConfiguration, capability: CameraCapability): CameraConfiguration {
        return when (configuration.mode) {
            CameraMode.SlowMotion, CameraMode.HighFrameRate -> configuration.copy(
                stabilizationEnabled = false,
                resolution = capability.jpegResolutions.firstOrNull { resolution ->
                    capability.highSpeedVideo.any { it.width == resolution.width && it.height == resolution.height }
                } ?: configuration.resolution,
            )
            CameraMode.MaximumResolution -> configuration.copy(stabilizationEnabled = false)
            else -> configuration
        }
    }
}

class LastKnownGoodCameraConfiguration {
    private val values = mutableMapOf<LensFacing, CameraConfiguration>()

    fun remember(configuration: CameraConfiguration) {
        values[configuration.lensFacing] = configuration
    }

    fun restore(facing: LensFacing): CameraConfiguration? = values[facing]
}

class DefaultStabilizationResolver : StabilizationResolver {
    override fun isSupported(capability: CameraCapability, mode: CameraMode): Boolean {
        return supportedModes(capability, mode).any { it !in setOf(
            VideoStabilizationMode.Off,
            VideoStabilizationMode.Unsupported,
        ) }
    }

    /**
     * Returns only request modes that can legally make sense for the active app
     * mode. CaptureResult still decides whether the HAL actually activated it for
     * the chosen resolution/FPS.
     */
    fun supportedModes(
        capability: CameraCapability,
        mode: CameraMode,
    ): List<VideoStabilizationMode> {
        if (
            mode == CameraMode.MaximumResolution ||
            mode == CameraMode.HighFrameRate ||
            mode == CameraMode.SlowMotion ||
            mode == CameraMode.Burst
        ) {
            return listOf(VideoStabilizationMode.Off)
        }
        return buildList {
            add(VideoStabilizationMode.Off)
            when (mode) {
                CameraMode.Video, CameraMode.TimeLapse -> {
                    if (capability.stabilization.preview) add(VideoStabilizationMode.Preview)
                    if (capability.stabilization.electronicVideo) add(VideoStabilizationMode.Standard)
                    if (capability.stabilization.optical) add(VideoStabilizationMode.Optical)
                }
                CameraMode.Photo,
                CameraMode.Portrait,
                CameraMode.Pro,
                CameraMode.Documents,
                CameraMode.Night,
                CameraMode.PanoramaExperimental,
                -> if (capability.stabilization.optical) add(VideoStabilizationMode.Optical)
                else -> Unit
            }
            if (size > 1) add(VideoStabilizationMode.Auto)
        }
    }

    fun resolve(
        requested: VideoStabilizationMode,
        capability: CameraCapability,
        mode: CameraMode,
    ): StabilizationDecision = resolve(requested, capability.stabilization, mode)

    fun resolve(
        requested: VideoStabilizationMode,
        support: StabilizationSupport,
        mode: CameraMode,
    ): StabilizationDecision {
        val supported = supportedModes(support, mode)
        val effective = when {
            requested == VideoStabilizationMode.Auto -> listOf(
                VideoStabilizationMode.Preview,
                VideoStabilizationMode.Standard,
                VideoStabilizationMode.Optical,
                VideoStabilizationMode.Off,
            ).first { it in supported }
            requested in supported -> requested
            else -> VideoStabilizationMode.Off
        }
        val reason = when {
            effective == requested -> null
            requested == VideoStabilizationMode.Auto -> "Auto selected ${effective.name} from Android-reported modes."
            else -> "${requested.name} is incompatible with ${mode.name}; stabilization was disabled."
        }
        return StabilizationDecision(
            requested = requested,
            effective = effective,
            supported = supported,
            fallbackReason = reason,
        )
    }

    private fun supportedModes(
        support: StabilizationSupport,
        mode: CameraMode,
    ): List<VideoStabilizationMode> {
        if (
            mode == CameraMode.MaximumResolution ||
            mode == CameraMode.HighFrameRate ||
            mode == CameraMode.SlowMotion ||
            mode == CameraMode.Burst
        ) {
            return listOf(VideoStabilizationMode.Off)
        }
        return buildList {
            add(VideoStabilizationMode.Off)
            when (mode) {
                CameraMode.Video, CameraMode.TimeLapse -> {
                    if (support.preview) add(VideoStabilizationMode.Preview)
                    if (support.electronicVideo) add(VideoStabilizationMode.Standard)
                    if (support.optical) add(VideoStabilizationMode.Optical)
                }
                else -> if (support.optical) add(VideoStabilizationMode.Optical)
            }
            if (size > 1) add(VideoStabilizationMode.Auto)
        }
    }
}

data class StabilizationDecision(
    val requested: VideoStabilizationMode,
    val effective: VideoStabilizationMode,
    val supported: List<VideoStabilizationMode>,
    val fallbackReason: String?,
)

class DefaultExtensionResolver : ExtensionResolver {
    override fun availableExtensions(capability: CameraCapability): ExtensionSupport = capability.extensions
}

class DefaultHighSpeedCapabilityProvider : HighSpeedCapabilityProvider {
    override fun options(capability: CameraCapability): List<HighSpeedVideoOption> = capability.highSpeedVideo
}
