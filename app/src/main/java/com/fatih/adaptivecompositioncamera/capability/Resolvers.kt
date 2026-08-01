package com.fatih.adaptivecompositioncamera.capability

import com.fatih.adaptivecompositioncamera.domain.model.CameraCapability
import com.fatih.adaptivecompositioncamera.domain.model.CameraConfiguration
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.CompositionGuide
import com.fatih.adaptivecompositioncamera.domain.model.ExtensionResolver
import com.fatih.adaptivecompositioncamera.domain.model.ExtensionSupport
import com.fatih.adaptivecompositioncamera.domain.model.HighSpeedCapabilityProvider
import com.fatih.adaptivecompositioncamera.domain.model.HighSpeedVideoOption
import com.fatih.adaptivecompositioncamera.domain.model.LensFacing
import com.fatih.adaptivecompositioncamera.domain.model.ModeCompatibilityResult
import com.fatih.adaptivecompositioncamera.domain.model.StabilizationResolver
import com.fatih.adaptivecompositioncamera.domain.model.StabilizationSupport
import com.fatih.adaptivecompositioncamera.domain.model.VideoStabilizationMode
import com.fatih.adaptivecompositioncamera.domain.model.isStillPhotoMode
import com.fatih.adaptivecompositioncamera.domain.model.isVideoMode

class CameraConfigurationResolver {
    fun availableModes(capability: CameraCapability): List<CameraMode> = buildList {
        add(CameraMode.Photo)
        if (capability.extensions.bokeh) add(CameraMode.Portrait)
        if (capability.videoResolutions.isNotEmpty()) add(CameraMode.Video)
        add(CameraMode.Documents)
        if (capability.supportsManualSensor) add(CameraMode.Pro)
        if (capability.extensions.night) add(CameraMode.Night)
        val recommended = capability.jpegResolutions.firstOrNull { it.recommended }
        val maximum = capability.displayMaximumResolution
        if (maximum != null && recommended != null && maximum.megapixels > recommended.megapixels * 1.2) {
            add(CameraMode.MaximumResolution)
        }
        if (capability.highSpeedVideo.isNotEmpty()) {
            add(CameraMode.SlowMotion)
            add(CameraMode.HighFrameRate)
        }
        if (capability.videoResolutions.isNotEmpty()) add(CameraMode.TimeLapse)
        if (capability.supportsBurst) add(CameraMode.Burst)
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
            CameraMode.Documents -> configuration.copy(
                stabilizationEnabled = capability.stabilization.optical,
                resolution = highestStillResolutionForMode(capability, CameraMode.Documents, configuration.resolution),
            )
            CameraMode.Video -> configuration.copy(
                resolution = configuration.resolution?.takeIf { capability.videoResolutions.any { video ->
                    video.width == it.width && video.height == it.height
                } } ?: configuration.resolution,
            )
            CameraMode.SlowMotion, CameraMode.HighFrameRate -> configuration.copy(
                stabilizationEnabled = false,
                resolution = capability.jpegResolutions.firstOrNull { resolution ->
                    capability.highSpeedVideo.any { it.width == resolution.width && it.height == resolution.height }
                } ?: configuration.resolution,
            )
            CameraMode.Pro -> configuration.copy(
                stabilizationEnabled = configuration.stabilizationEnabled && capability.stabilization.optical,
                resolution = highestStillResolutionForMode(capability, CameraMode.Pro, configuration.resolution),
            )
            CameraMode.MaximumResolution -> configuration.copy(
                stabilizationEnabled = false,
                resolution = capability.maximumSensorResolution
                    ?: capability.highResolutionJpegs.firstOrNull()
                    ?: configuration.resolution,
            )
            else -> configuration
        }
    }

    fun mutuallyExclusiveModes(mode: CameraMode): Set<CameraMode> = when (mode) {
        CameraMode.Documents -> setOf(CameraMode.Pro, CameraMode.Video, CameraMode.MaximumResolution)
        CameraMode.Pro -> setOf(CameraMode.Documents, CameraMode.Video)
        CameraMode.Video -> CameraMode.entries.filter { it != CameraMode.Video && it.isStillPhotoMode }.toSet()
        CameraMode.SlowMotion,
        CameraMode.HighFrameRate,
        CameraMode.TimeLapse,
        -> CameraMode.entries.filter { it.isStillPhotoMode }.toSet()
        else -> emptySet()
    }

    fun requiresStillOnlySession(mode: CameraMode): Boolean = mode in setOf(
        CameraMode.Documents,
        CameraMode.Pro,
        CameraMode.MaximumResolution,
        CameraMode.Burst,
        CameraMode.PanoramaExperimental,
    )

    fun requiresAnalysis(mode: CameraMode): Boolean = mode == CameraMode.Documents

    fun isCompositionAllowed(mode: CameraMode, guide: CompositionGuide): Boolean {
        if (guide == CompositionGuide.None) return true
        return when (mode) {
            CameraMode.Documents -> false
            CameraMode.Video,
            CameraMode.SlowMotion,
            CameraMode.HighFrameRate,
            CameraMode.TimeLapse,
            -> guide in setOf(
                CompositionGuide.RuleOfThirds,
                CompositionGuide.Centered,
                CompositionGuide.HorizonLevel,
            )
            else -> true
        }
    }

    fun resolveModeChange(
        currentMode: CameraMode,
        requestedMode: CameraMode,
        currentGuide: CompositionGuide,
        previousPhotoGuide: CompositionGuide,
    ): ModeCompatibilityResult {
        val guide = when {
            requestedMode == CameraMode.Documents -> CompositionGuide.None
            currentMode == CameraMode.Documents && currentGuide == CompositionGuide.None -> previousPhotoGuide
            isCompositionAllowed(requestedMode, currentGuide) -> currentGuide
            else -> CompositionGuide.None
        }
        return ModeCompatibilityResult(
            mode = requestedMode,
            guide = guide,
            closeMoreSelector = true,
            closeProControls = requestedMode != CameraMode.Pro,
            closeCompositionSelector = requestedMode == CameraMode.Documents,
            stopDocumentAnalysis = currentMode == CameraMode.Documents && requestedMode != CameraMode.Documents,
            stopVideoRecording = currentMode == CameraMode.Video && requestedMode != CameraMode.Video,
            reason = when {
                requestedMode == CameraMode.Documents && currentGuide != CompositionGuide.None ->
                    "Document mode uses its own document frame; photo composition guides were hidden."
                currentMode == CameraMode.Documents && guide != currentGuide ->
                    "Photo composition guide restored."
                !isCompositionAllowed(requestedMode, currentGuide) ->
                    "The selected guide is unavailable in ${requestedMode.name} mode."
                else -> null
            },
        )
    }

    private fun highestStillResolutionForMode(
        capability: CameraCapability,
        mode: CameraMode,
        requested: CameraResolution?,
    ): CameraResolution? {
        val choices = when (mode) {
            CameraMode.Pro -> capability.jpegResolutions.ifEmpty { capability.selectablePhotoResolutions }
            CameraMode.Documents -> capability.jpegResolutions.ifEmpty { capability.selectablePhotoResolutions }
            else -> capability.selectablePhotoResolutions
        }
        return choices.firstOrNull { it.id == requested?.id }
            ?: choices.firstOrNull { it.recommended }
            ?: choices.firstOrNull()
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
            requested == VideoStabilizationMode.Auto -> autoPreference(mode).first { it in supported }
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
            requestPlan = StabilizationRequestPlan.from(mode, effective),
        )
    }

    private fun autoPreference(mode: CameraMode): List<VideoStabilizationMode> = when {
        mode.isVideoMode -> listOf(
            VideoStabilizationMode.Preview,
            VideoStabilizationMode.Standard,
            VideoStabilizationMode.Optical,
            VideoStabilizationMode.Off,
        )
        else -> listOf(
            VideoStabilizationMode.Optical,
            VideoStabilizationMode.Off,
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
    val requestPlan: StabilizationRequestPlan = StabilizationRequestPlan.from(CameraMode.Photo, effective),
)

data class StabilizationRequestPlan(
    val requestOis: Boolean,
    val requestStandardEis: Boolean,
    val requestPreviewStabilization: Boolean,
    val verifyCaptureResult: Boolean,
    val evidenceLabel: String,
) {
    companion object {
        fun from(mode: CameraMode, effective: VideoStabilizationMode): StabilizationRequestPlan {
            val requestOis = effective == VideoStabilizationMode.Optical
            val requestStandard = mode.isVideoMode && effective == VideoStabilizationMode.Standard
            val requestPreview = mode.isVideoMode && effective == VideoStabilizationMode.Preview
            return StabilizationRequestPlan(
                requestOis = requestOis,
                requestStandardEis = requestStandard,
                requestPreviewStabilization = requestPreview,
                verifyCaptureResult = requestOis || requestStandard || requestPreview,
                evidenceLabel = when {
                    requestPreview -> "PRE"
                    requestStandard -> "EIS"
                    requestOis -> "OIS"
                    else -> "OFF"
                },
            )
        }
    }
}

class DefaultExtensionResolver : ExtensionResolver {
    override fun availableExtensions(capability: CameraCapability): ExtensionSupport = capability.extensions
}

class DefaultHighSpeedCapabilityProvider : HighSpeedCapabilityProvider {
    override fun options(capability: CameraCapability): List<HighSpeedVideoOption> = capability.highSpeedVideo
}
