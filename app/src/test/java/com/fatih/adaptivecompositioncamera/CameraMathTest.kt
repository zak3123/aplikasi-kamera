package com.fatih.adaptivecompositioncamera

import com.fatih.adaptivecompositioncamera.capability.CameraConfigurationResolver
import com.fatih.adaptivecompositioncamera.capability.DefaultStabilizationResolver
import com.fatih.adaptivecompositioncamera.capability.LastKnownGoodCameraConfiguration
import com.fatih.adaptivecompositioncamera.capability.ModeConflictResolver
import com.fatih.adaptivecompositioncamera.domain.model.CameraCapability
import com.fatih.adaptivecompositioncamera.domain.model.CameraConfiguration
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.ExtensionSupport
import com.fatih.adaptivecompositioncamera.domain.model.HardwareLevel
import com.fatih.adaptivecompositioncamera.domain.model.HighSpeedVideoOption
import com.fatih.adaptivecompositioncamera.domain.model.LensFacing
import com.fatih.adaptivecompositioncamera.domain.model.LensRole
import com.fatih.adaptivecompositioncamera.domain.model.StabilizationSupport
import com.fatih.adaptivecompositioncamera.media.AndroidMediaRepository
import com.fatih.adaptivecompositioncamera.utility.AdaptiveLayout
import com.fatih.adaptivecompositioncamera.utility.CameraMath
import com.fatih.adaptivecompositioncamera.utility.FloatPoint
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraMathTest {
    @Test
    fun megapixelsAreCalculatedFromActualOutputResolution() {
        assertEquals(48.0, CameraMath.megapixels(8000, 6000), 0.0)
        assertEquals(49.9, CameraMath.megapixels(8160, 6120), 0.0)
        assertEquals(12.0, CameraMath.megapixels(4000, 3000), 0.0)
        assertEquals(8.0, CameraMath.megapixels(3264, 2448), 0.0)
    }

    @Test
    fun aspectRatioUsesReducedIntegerForm() {
        assertEquals("4:3", CameraMath.aspectRatioLabel(4000, 3000))
        assertEquals("16:9", CameraMath.aspectRatioLabel(3840, 2160))
        assertEquals("1:1", CameraMath.aspectRatioLabel(3000, 3000))
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
    fun modeConflictDisablesStabilizationForHighSpeed() {
        val capability = fakeCapability(
            jpeg = listOf(resolution(1920, 1080)),
            highSpeed = listOf(HighSpeedVideoOption(1920, 1080, 120, 120)),
        )
        val requested = CameraConfiguration(LensFacing.Rear, "0", CameraMode.SlowMotion, resolution(1920, 1080), stabilizationEnabled = true)
        assertFalse(ModeConflictResolver().resolve(requested, capability).stabilizationEnabled)
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
