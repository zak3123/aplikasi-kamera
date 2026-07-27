package com.fatih.adaptivecompositioncamera

import com.fatih.adaptivecompositioncamera.capability.CameraConfigurationResolver
import com.fatih.adaptivecompositioncamera.capability.DefaultStabilizationResolver
import com.fatih.adaptivecompositioncamera.capability.LastKnownGoodCameraConfiguration
import com.fatih.adaptivecompositioncamera.capability.ModeConflictResolver
import com.fatih.adaptivecompositioncamera.domain.model.CameraCapability
import com.fatih.adaptivecompositioncamera.domain.model.CameraConfiguration
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.CompositionGuide
import com.fatih.adaptivecompositioncamera.domain.model.ExtensionSupport
import com.fatih.adaptivecompositioncamera.domain.model.HardwareLevel
import com.fatih.adaptivecompositioncamera.domain.model.HighSpeedVideoOption
import com.fatih.adaptivecompositioncamera.domain.model.LensFacing
import com.fatih.adaptivecompositioncamera.domain.model.LensRole
import com.fatih.adaptivecompositioncamera.domain.model.PhotoAspectRatio
import com.fatih.adaptivecompositioncamera.domain.model.PhotoQualityPreset
import com.fatih.adaptivecompositioncamera.domain.model.StabilizationSupport
import com.fatih.adaptivecompositioncamera.domain.model.VideoQualitySetting
import com.fatih.adaptivecompositioncamera.domain.model.VideoStabilizationMode
import com.fatih.adaptivecompositioncamera.media.AndroidMediaRepository
import com.fatih.adaptivecompositioncamera.utility.AdaptiveLayout
import com.fatih.adaptivecompositioncamera.utility.CameraMath
import com.fatih.adaptivecompositioncamera.utility.FloatPoint
import com.fatih.adaptivecompositioncamera.ui.camera.professionalGuideCatalog
import com.fatih.adaptivecompositioncamera.ui.camera.CameraUiTokens
import com.fatih.adaptivecompositioncamera.ui.camera.cameraUiLayoutPolicy
import com.fatih.adaptivecompositioncamera.ui.camera.documentAnalysisResolutionKey
import com.fatih.adaptivecompositioncamera.ui.camera.modeResolutionKey
import com.fatih.adaptivecompositioncamera.composition.perspectiveEdgePoints
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraMathTest {
    @Test
    fun professionalCompositionCatalogContainsEverySupportedGuide() {
        assertEquals(
            setOf(
                CompositionGuide.None,
                CompositionGuide.RuleOfThirds,
                CompositionGuide.LeadingLines,
                CompositionGuide.GoldenRatio,
                CompositionGuide.GoldenSpiral,
                CompositionGuide.VanishingPoint,
                CompositionGuide.FrameInFrame,
                CompositionGuide.Centered,
                CompositionGuide.Symmetry,
                CompositionGuide.Diagonal,
                CompositionGuide.GoldenTriangle,
                CompositionGuide.TextureRepetition,
                CompositionGuide.Foreground,
                CompositionGuide.EyeLine,
                CompositionGuide.HorizonLevel,
            ),
            CompositionGuide.entries.toSet(),
        )
        assertEquals(15, CompositionGuide.entries.size)
        assertEquals(CompositionGuide.entries.toSet(), professionalGuideCatalog.toSet())
        assertEquals(15, professionalGuideCatalog.size)
    }

    @Test
    fun megapixelsAreCalculatedFromActualOutputResolution() {
        assertEquals(48.0, CameraMath.megapixels(8000, 6000), 0.0)
        assertEquals(49.9, CameraMath.megapixels(8160, 6120), 0.0)
        assertEquals(12.0, CameraMath.megapixels(4000, 3000), 0.0)
        assertEquals(8.0, CameraMath.megapixels(3264, 2448), 0.0)
    }

    @Test
    fun aspectRatioUsesFriendlyCameraLabels() {
        assertEquals("4:3", CameraMath.aspectRatioLabel(4000, 3000))
        assertEquals("4:3", CameraMath.aspectRatioLabel(4624, 3472))
        assertEquals("20:9", CameraMath.aspectRatioLabel(4624, 2080))
        assertEquals("16:9", CameraMath.aspectRatioLabel(3840, 2160))
        assertEquals("16:9", CameraMath.aspectRatioLabel(3264, 1836))
        assertEquals("1:1", CameraMath.aspectRatioLabel(3000, 3000))
    }

    @Test
    fun nativeResolutionAndOutputCropRemainSeparate() {
        assertEquals(8000 to 6000, CameraMath.cropDimensions(8000, 6000, PhotoAspectRatio.FullSensor))
        assertEquals(6000 to 6000, CameraMath.cropDimensions(8000, 6000, PhotoAspectRatio.Ratio1x1))
        assertEquals(8000 to 4500, CameraMath.cropDimensions(8000, 6000, PhotoAspectRatio.Ratio16x9))
        assertEquals(36.0, CameraMath.megapixels(6000, 6000), 0.0)
    }

    @Test
    fun previewViewportFitsPhoneAndTabletWithoutStretching() {
        val portrait = CameraMath.fitAspectRatio(1080f, 2100f, 3f / 4f)
        val landscape = CameraMath.fitAspectRatio(1800f, 1080f, 4f / 3f)
        assertEquals(0.75f, portrait.width / portrait.height, 0.001f)
        assertEquals(4f / 3f, landscape.width / landscape.height, 0.001f)
        assertTrue(portrait.left >= 0f && portrait.bottom <= 2100f)
        assertTrue(landscape.left >= 0f && landscape.right <= 1800f)
    }

    @Test
    fun resolutionSortingFilteringAndMaximumDetectionUseRealDimensions() {
        val sorted = CameraMath.sortResolutionDimensions(
            listOf(4000 to 3000, 1920 to 1080, 4000 to 3000, 8000 to 6000, 3264 to 1836),
            "JPEG",
        )
        assertEquals(4, sorted.size)
        assertTrue(sorted.first().maximum)
        assertEquals(8000, sorted.first().width)
        assertEquals(listOf("16:9", "16:9"), CameraMath.filterByAspectRatio(sorted, 16, 9).map { it.aspectRatioLabel })
    }

    @Test
    fun photoQualityPresetsSelectActualExposedOutputs() {
        val sizes = listOf(
            resolution(8000, 6000),
            resolution(4624, 3472),
            resolution(3264, 2448),
            resolution(2560, 1440),
        )
        assertEquals(48.0, CameraMath.selectPhotoQuality(sizes, PhotoQualityPreset.Maximum)?.megapixels ?: 0.0, 0.0)
        assertEquals(16.1, CameraMath.selectPhotoQuality(sizes, PhotoQualityPreset.High)?.megapixels ?: 0.0, 0.0)
        assertEquals(8.0, CameraMath.selectPhotoQuality(sizes, PhotoQualityPreset.Medium)?.megapixels ?: 0.0, 0.0)
        assertEquals(3.7, CameraMath.selectPhotoQuality(sizes, PhotoQualityPreset.StorageSaver)?.megapixels ?: 0.0, 0.0)
        assertTrue(CameraMath.selectPhotoQuality(sizes, PhotoQualityPreset.Custom) == null)
    }

    @Test
    fun stabilizationOptionsMapOnlyReportedCapabilities() {
        assertEquals(
            listOf(VideoStabilizationMode.Unsupported),
            CameraMath.stabilizationModes(StabilizationSupport(false, false, false)),
        )
        assertEquals(
            listOf(
                VideoStabilizationMode.Off,
                VideoStabilizationMode.Standard,
                VideoStabilizationMode.Preview,
                VideoStabilizationMode.Optical,
                VideoStabilizationMode.Auto,
            ),
            CameraMath.stabilizationModes(StabilizationSupport(true, true, true)),
        )
    }

    @Test
    fun videoFallbackOrderNeverInventsUnsupportedQuality() {
        val supported = listOf(VideoQualitySetting.FHD, VideoQualitySetting.HD)
        assertEquals(
            listOf(VideoQualitySetting.FHD, VideoQualitySetting.HD),
            CameraMath.videoFallbackOrder(VideoQualitySetting.UHD, supported),
        )
        assertEquals(
            listOf(VideoQualitySetting.FHD, VideoQualitySetting.HD),
            CameraMath.videoFallbackOrder(VideoQualitySetting.Auto, supported),
        )
    }

    @Test
    fun goldenRectangleAlwaysFitsPreviewAndPreservesPhi() {
        listOf(1080f to 1920f, 1920f to 1080f, 1280f to 800f).forEach { (width, height) ->
            val bounds = CameraMath.fitGoldenRectangle(width, height)
            assertTrue(bounds.left >= 0f && bounds.top >= 0f)
            assertTrue(bounds.right <= width && bounds.bottom <= height)
            assertEquals(CameraMath.PHI, bounds.width / bounds.height, 0.001f)
        }
    }

    @Test
    fun goldenTransformFlipsInsideTheSameBounds() {
        val bounds = CameraMath.fitGoldenRectangle(1080f, 1920f)
        val point = FloatPoint(bounds.left + 12f, bounds.top + 20f)
        val horizontal = CameraMath.transformPoint(point, bounds, horizontalFlip = true, verticalFlip = false)
        val both = CameraMath.transformPoint(point, bounds, horizontalFlip = true, verticalFlip = true)
        assertTrue(bounds.contains(horizontal))
        assertTrue(bounds.contains(both))
        assertEquals(bounds.right - 12f, horizontal.x, 0.001f)
        assertEquals(bounds.bottom - 20f, both.y, 0.001f)
    }

    @Test
    fun goldenSpiralQuarterArcsAreContinuousAndStayInsideGoldenSquares() {
        val fitted = CameraMath.fitGoldenRectangle(1080f, 1920f)
        val arcs = CameraMath.goldenSpiralArcs(1080f, 1920f)
        assertTrue(arcs.size >= 8)
        arcs.forEach { arc ->
            assertTrue(arc.square.left >= fitted.left - 0.01f)
            assertTrue(arc.square.top >= fitted.top - 0.01f)
            assertTrue(arc.square.right <= fitted.right + 0.01f)
            assertTrue(arc.square.bottom <= fitted.bottom + 0.01f)
            assertTrue(containsWithTolerance(arc.square, arcPoint(arc.oval, arc.startAngleDegrees)))
            assertTrue(containsWithTolerance(arc.square, arcPoint(arc.oval, arc.startAngleDegrees + arc.sweepAngleDegrees)))
        }
        arcs.zipWithNext().forEach { (first, second) ->
            val end = arcPoint(first.oval, first.startAngleDegrees + first.sweepAngleDegrees)
            val start = arcPoint(second.oval, second.startAngleDegrees)
            assertEquals(end.x, start.x, 0.02f)
            assertEquals(end.y, start.y, 0.02f)
        }
    }

    @Test
    fun ruleOfThirdsCoordinatesMatchPreviewBounds() {
        val points = CameraMath.ruleOfThirds(900f, 600f)
        assertEquals(FloatPoint(300f, 0f), points[0])
        assertEquals(FloatPoint(600f, 600f), points[3])
        assertEquals(FloatPoint(0f, 200f), points[4])
        assertEquals(FloatPoint(900f, 400f), points[7])
    }

    @Test
    fun fillCenterCropMappingAccountsForPreviewCrop() {
        val center = CameraMath.mapFillCenterPoint(FloatPoint(2000f, 1500f), 4000f, 3000f, 1080f, 1920f)
        assertEquals(540f, center.x, 0.01f)
        assertEquals(960f, center.y, 0.01f)
    }

    @Test
    fun frontCameraMirroringStaysInsidePreview() {
        assertEquals(800f, CameraMath.mirrorX(200f, 1000f, true), 0f)
        assertEquals(200f, CameraMath.mirrorX(200f, 1000f, false), 0f)
    }

    @Test
    fun jpegOrientationAccountsForFacingAndDisplayRotation() {
        assertEquals(90, CameraMath.jpegOrientationDegrees(90, 0, frontFacing = false))
        assertEquals(0, CameraMath.jpegOrientationDegrees(90, 90, frontFacing = false))
        assertEquals(180, CameraMath.jpegOrientationDegrees(90, 90, frontFacing = true))
        assertEquals(0, CameraMath.jpegOrientationDegrees(270, 90, frontFacing = true))
    }

    @Test
    fun horizonRollUsesGravityRatherThanGeometricCenter() {
        assertEquals(0f, CameraMath.horizonRollDegrees(0f, 9.8f), 0.01f)
        assertEquals(90f, CameraMath.horizonRollDegrees(9.8f, 0f), 0.01f)
    }

    @Test
    fun fpsAndHighSpeedVisibilityRequireExposedRanges() {
        assertTrue(CameraMath.isFrameRateValid(60, listOf(24..30, 30..60)))
        assertFalse(CameraMath.isFrameRateValid(120, listOf(24..30, 30..60)))
        assertTrue(CameraMath.isHighSpeedVisible(listOf(HighSpeedVideoOption(1920, 1080, 120, 120))))
        assertFalse(CameraMath.isHighSpeedVisible(listOf(HighSpeedVideoOption(1920, 1080, 30, 60))))
    }

    @Test
    fun modeResolverFallsBackToPhotoAndSafeResolution() {
        val twelve = resolution(4000, 3000, recommended = true)
        val capability = fakeCapability(jpeg = listOf(twelve), video = emptyList())
        val resolved = CameraConfigurationResolver().resolve(capability, CameraMode.Video, resolution(8000, 6000))
        assertEquals(CameraMode.Photo, resolved.mode)
        assertSame(twelve, resolved.resolution)
    }

    @Test
    fun modeResolutionPreferencesAreIsolatedByCameraAndMode() {
        assertEquals("0:Photo:photo", modeResolutionKey("0", CameraMode.Photo))
        assertEquals("0:Pro:pro-photo", modeResolutionKey("0", CameraMode.Pro))
        assertEquals("2:Pro:pro-photo", modeResolutionKey("2", CameraMode.Pro))
        assertEquals("0:Documents:document-final", modeResolutionKey("0", CameraMode.Documents))
        assertEquals("0:Documents:analysis", documentAnalysisResolutionKey("0"))
        assertEquals("0:Video:video", modeResolutionKey("0", CameraMode.Video))
        assertTrue(modeResolutionKey("0", CameraMode.Photo) != modeResolutionKey("0", CameraMode.Pro))
        assertTrue(modeResolutionKey("0", CameraMode.Documents) != documentAnalysisResolutionKey("0"))
    }

    @Test
    fun modeConflictDisablesStabilizationForHighSpeed() {
        val capability = fakeCapability(
            jpeg = listOf(resolution(1920, 1080)),
            highSpeed = listOf(HighSpeedVideoOption(1920, 1080, 120, 120)),
        )
        val requested = CameraConfiguration(LensFacing.Rear, "0", CameraMode.SlowMotion, resolution(1920, 1080), stabilizationEnabled = true)
        assertFalse(ModeConflictResolver().resolve(requested, capability).stabilizationEnabled)
    }

    @Test
    fun documentModeDisablesPhotographyCompositionGuides() {
        val resolver = ModeConflictResolver()
        val result = resolver.resolveModeChange(
            currentMode = CameraMode.Photo,
            requestedMode = CameraMode.Documents,
            currentGuide = CompositionGuide.GoldenSpiral,
            previousPhotoGuide = CompositionGuide.GoldenSpiral,
        )
        assertEquals(CameraMode.Documents, result.mode)
        assertEquals(CompositionGuide.None, result.guide)
        assertTrue(result.closeMoreSelector)
        assertTrue(result.closeProControls)
        assertTrue(result.closeCompositionSelector)
        assertFalse(resolver.isCompositionAllowed(CameraMode.Documents, CompositionGuide.RuleOfThirds))
    }

    @Test
    fun leavingDocumentRestoresPreviousCompatiblePhotoGuide() {
        val result = ModeConflictResolver().resolveModeChange(
            currentMode = CameraMode.Documents,
            requestedMode = CameraMode.Pro,
            currentGuide = CompositionGuide.None,
            previousPhotoGuide = CompositionGuide.GoldenSpiral,
        )
        assertEquals(CameraMode.Pro, result.mode)
        assertEquals(CompositionGuide.GoldenSpiral, result.guide)
        assertTrue(result.stopDocumentAnalysis)
        assertFalse(result.closeProControls)
    }

    @Test
    fun videoRejectsComplexPhotographyGuides() {
        val resolver = ModeConflictResolver()
        assertFalse(resolver.isCompositionAllowed(CameraMode.Video, CompositionGuide.GoldenSpiral))
        assertTrue(resolver.isCompositionAllowed(CameraMode.Video, CompositionGuide.RuleOfThirds))
        val result = resolver.resolveModeChange(
            currentMode = CameraMode.Photo,
            requestedMode = CameraMode.Video,
            currentGuide = CompositionGuide.GoldenSpiral,
            previousPhotoGuide = CompositionGuide.GoldenSpiral,
        )
        assertEquals(CameraMode.Video, result.mode)
        assertEquals(CompositionGuide.None, result.guide)
        assertTrue(result.closeProControls)
    }

    @Test
    fun lastKnownGoodConfigurationIsKeptPerFacing() {
        val store = LastKnownGoodCameraConfiguration()
        val rear = CameraConfiguration(LensFacing.Rear, "back", CameraMode.Photo, null)
        val front = CameraConfiguration(LensFacing.Front, "front", CameraMode.Photo, null)
        store.remember(rear)
        store.remember(front)
        assertEquals("back", store.restore(LensFacing.Rear)?.cameraId)
        assertEquals("front", store.restore(LensFacing.Front)?.cameraId)
    }

    @Test
    fun stabilizationCompatibilityKeepsHighSpeedConservative() {
        val capability = fakeCapability(stabilization = StabilizationSupport(optical = true, electronicVideo = true, preview = true))
        assertTrue(DefaultStabilizationResolver().isSupported(capability, CameraMode.Video))
        assertFalse(DefaultStabilizationResolver().isSupported(capability, CameraMode.SlowMotion))
    }

    @Test
    fun responsiveLayoutClassifiesPhoneAndTabletOrientations() {
        assertEquals(AdaptiveLayout.PhonePortrait, CameraMath.adaptiveLayout(393, 873))
        assertEquals(AdaptiveLayout.PhoneLandscape, CameraMath.adaptiveLayout(873, 393))
        assertEquals(AdaptiveLayout.TabletPortrait, CameraMath.adaptiveLayout(800, 1280))
        assertEquals(AdaptiveLayout.TabletLandscape, CameraMath.adaptiveLayout(1280, 800))
    }

    @Test
    fun stockCameraChromeStaysWithinCompactPhoneGuidance() {
        val landscape = cameraUiLayoutPolicy(AdaptiveLayout.PhoneLandscape)
        assertEquals(132f, landscape.captureRailWidth.value, 0f)
        assertTrue(landscape.captureRailWidth.value <= 800f * 0.22f)
        assertTrue(CameraUiTokens.portraitControlsHeight.value <= 200f)
        assertTrue(CameraUiTokens.shutterOuterSize.value in 72f..84f)
    }

    @Test
    fun mediaNamesUseGalleryFriendlyPrefixes() {
        val repository = AndroidMediaRepository()
        val first = repository.createImageName()
        val second = repository.createImageName()
        assertTrue(first.startsWith("IMG_"))
        assertFalse(first == second)
        assertTrue(repository.createImageName("heic").endsWith(".heic"))
        assertTrue(repository.createVideoName().startsWith("VID_"))
    }

    @Test
    fun maximumResolutionModeUsesOnlyActuallyExposedJpegOutputs() {
        val recommended = resolution(4000, 3000, recommended = true)
        val maximum = resolution(8000, 6000)
        val exposed = fakeCapability(jpeg = listOf(maximum, recommended))
        val binnedOnly = fakeCapability(jpeg = listOf(recommended))
        assertTrue(CameraMode.MaximumResolution in CameraConfigurationResolver().availableModes(exposed))
        assertFalse(CameraMode.MaximumResolution in CameraConfigurationResolver().availableModes(binnedOnly))
        assertEquals(48.0, exposed.displayMaximumResolution?.megapixels ?: 0.0, 0.0)
        assertEquals(12.0, binnedOnly.displayMaximumResolution?.megapixels ?: 0.0, 0.0)
    }

    @Test
    fun maximumResolutionStreamMapIsSelectableForDedicatedCamera2Capture() {
        val recommended = resolution(4032, 3024, recommended = true)
        val maximum = resolution(8000, 6000).copy(maximumSensorMode = true)
        val capability = fakeCapability(jpeg = listOf(recommended), maximumJpeg = listOf(maximum))
        assertEquals(8000, capability.selectablePhotoResolutions.first().width)
        assertTrue(capability.selectablePhotoResolutions.first().maximumSensorMode)
        assertEquals(8000, capability.maximumExposedResolution?.width)
        assertTrue(CameraMode.MaximumResolution in CameraConfigurationResolver().availableModes(capability))
    }

    @Test
    fun maximumSensorSessionRemainsSelectableOnFrontCameraWhenAndroidExposesIt() {
        val recommended = resolution(4000, 3000, recommended = true)
        val maximum = resolution(8000, 6000).copy(maximumSensorMode = true)
        val capability = fakeCapability(jpeg = listOf(recommended), maximumJpeg = listOf(maximum))
            .copy(lensFacing = LensFacing.Front)
        assertTrue(capability.selectablePhotoResolutions.any { it.maximumSensorMode })
        assertEquals(8000, capability.displayMaximumResolution?.width)
    }

    @Test
    fun stabilizationResolverFallsBackWithoutFabricatingSupport() {
        val support = StabilizationSupport(optical = false, electronicVideo = true, preview = false)
        val resolver = DefaultStabilizationResolver()
        val video = resolver.resolve(VideoStabilizationMode.Preview, support, CameraMode.Video)
        val highSpeed = resolver.resolve(VideoStabilizationMode.Standard, support, CameraMode.HighFrameRate)
        assertEquals(VideoStabilizationMode.Off, video.effective)
        assertTrue(video.fallbackReason?.contains("incompatible") == true)
        assertEquals(VideoStabilizationMode.Off, highSpeed.effective)
    }

    @Test
    fun perspectiveGuideUsesEveryPreviewEdgeWithoutLeavingBounds() {
        val points = perspectiveEdgePoints(width = 400f, height = 300f, lineCount = 5)
        assertTrue(points.any { it.x == 0f })
        assertTrue(points.any { it.x == 400f })
        assertTrue(points.any { it.y == 0f })
        assertTrue(points.any { it.y == 300f })
        assertTrue(points.all { it.x in 0f..400f && it.y in 0f..300f })
        assertEquals(points.distinct().size, points.size)
    }

    @Test
    fun androidHighResolutionOutputIsSelectableAndEnablesRealMpMode() {
        val recommended = resolution(4000, 3000, recommended = true)
        val highResolution = resolution(8000, 6000).copy(highResolution = true)
        val capability = fakeCapability(jpeg = listOf(recommended), highResolutionJpeg = listOf(highResolution))
        assertEquals(8000, capability.selectablePhotoResolutions.first().width)
        assertTrue(capability.selectablePhotoResolutions.first().highResolution)
        assertEquals(48.0, capability.displayMaximumResolution?.megapixels ?: 0.0, 0.0)
        assertTrue(CameraMode.MaximumResolution in CameraConfigurationResolver().availableModes(capability))
        assertFalse(recommended.id == highResolution.id)
    }

    @Test
    fun proIsCapabilityDrivenAndDocumentsKeepsARealPhotoPath() {
        val manual = fakeCapability(jpeg = listOf(resolution(4000, 3000, recommended = true)))
        val automaticOnly = manual.copy(supportsManualSensor = false)
        assertTrue(CameraMode.Pro in CameraConfigurationResolver().availableModes(manual))
        assertFalse(CameraMode.Pro in CameraConfigurationResolver().availableModes(automaticOnly))
        assertTrue(CameraMode.Documents in CameraConfigurationResolver().availableModes(manual))
        assertTrue(CameraMode.Documents in CameraConfigurationResolver().availableModes(automaticOnly))
    }

    private fun arcPoint(bounds: com.fatih.adaptivecompositioncamera.utility.FloatBounds, angleDegrees: Float): FloatPoint {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val centerX = (bounds.left + bounds.right) / 2f
        val centerY = (bounds.top + bounds.bottom) / 2f
        return FloatPoint(
            centerX + cos(radians).toFloat() * bounds.width / 2f,
            centerY + sin(radians).toFloat() * bounds.height / 2f,
        )
    }

    private fun containsWithTolerance(
        bounds: com.fatih.adaptivecompositioncamera.utility.FloatBounds,
        point: FloatPoint,
        tolerance: Float = 0.05f,
    ): Boolean = point.x in (bounds.left - tolerance)..(bounds.right + tolerance) &&
        point.y in (bounds.top - tolerance)..(bounds.bottom + tolerance)

    private fun resolution(width: Int, height: Int, recommended: Boolean = false): CameraResolution = CameraResolution(
        width = width,
        height = height,
        format = "JPEG",
        megapixels = CameraMath.megapixels(width, height),
        aspectRatioLabel = CameraMath.aspectRatioLabel(width, height),
        recommended = recommended,
        maximum = false,
    )

    private fun fakeCapability(
        jpeg: List<CameraResolution> = emptyList(),
        highResolutionJpeg: List<CameraResolution> = emptyList(),
        maximumJpeg: List<CameraResolution> = emptyList(),
        video: List<CameraResolution> = listOf(resolution(1920, 1080)),
        highSpeed: List<HighSpeedVideoOption> = emptyList(),
        stabilization: StabilizationSupport = StabilizationSupport(false, false, false),
    ): CameraCapability = CameraCapability(
        cameraId = "0",
        friendlyName = "Fake Rear Camera",
        lensFacing = LensFacing.Rear,
        lensRole = LensRole.Main,
        hardwareLevel = HardwareLevel.Full,
        sensorOrientation = 90,
        activeArray = null,
        pixelArray = null,
        physicalCameraIds = emptyList(),
        focalLengths = listOf(4.5f),
        apertures = listOf(1.8f),
        hasFlash = true,
        hasTorch = true,
        minFocusDistance = 0.1f,
        isoRange = "100..3200",
        exposureTimeRange = null,
        exposureCompensationRange = null,
        jpegResolutions = jpeg,
        highResolutionJpegs = highResolutionJpeg,
        maximumResolutionJpegs = maximumJpeg,
        rawResolutions = emptyList(),
        videoResolutions = video,
        fpsRanges = listOf("30..60"),
        highSpeedVideo = highSpeed,
        stabilization = stabilization,
        extensions = ExtensionSupport(),
        supportsRaw = false,
        supportsManualSensor = true,
        supportsManualPostProcessing = true,
        supportsBurst = true,
        supportsLogicalMultiCamera = false,
        supportsDepth = false,
    )
}
