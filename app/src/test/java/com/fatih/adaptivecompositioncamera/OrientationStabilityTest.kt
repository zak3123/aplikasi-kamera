package com.fatih.adaptivecompositioncamera

import com.fatih.adaptivecompositioncamera.camera.RebindGate
import com.fatih.adaptivecompositioncamera.ui.camera.CameraTopControlSlot
import com.fatih.adaptivecompositioncamera.ui.camera.CameraUiTokens
import com.fatih.adaptivecompositioncamera.ui.camera.UiRotation
import com.fatih.adaptivecompositioncamera.ui.camera.cameraUiLayoutPolicy
import com.fatih.adaptivecompositioncamera.ui.camera.displayRotationToUiRotation
import com.fatih.adaptivecompositioncamera.ui.camera.quantizedUiRotation
import com.fatih.adaptivecompositioncamera.ui.camera.relativeControlRotationDegrees
import com.fatih.adaptivecompositioncamera.ui.camera.responsiveTokens
import com.fatih.adaptivecompositioncamera.ui.camera.toSurfaceRotation
import com.fatih.adaptivecompositioncamera.ui.camera.topControlCapacity
import com.fatih.adaptivecompositioncamera.ui.camera.visibleTopControlSlots
import com.fatih.adaptivecompositioncamera.utility.AdaptiveLayout
import com.fatih.adaptivecompositioncamera.utility.CameraMath
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Orientation / rebind / manifest / layout-budget invariants. Pure JVM; no device needed. */
class OrientationStabilityTest {

    // ---- rotation mapping -------------------------------------------------------------------

    @Test
    fun displayRotationAndSensorQuadrantAgreeForEveryWindowRotation() {
        for (display in 0..3) {
            val physical = displayRotationToUiRotation(display)
            // Auto-rotate ON: window already compensates the device turn -> icons need no extra rotation.
            assertEquals("display=$display", 0f, relativeControlRotationDegrees(physical, display), 0.01f)
            // Capture rotation derived from that quadrant must equal the window rotation.
            assertEquals("display=$display", display, physical.toSurfaceRotation())
        }
    }

    @Test
    fun relativeRotationStaysInHalfOpenRangeAndNeverDoubleRotates() {
        for (physical in UiRotation.entries) for (display in 0..3) {
            val degrees = relativeControlRotationDegrees(physical, display)
            assertTrue("physical=$physical display=$display -> $degrees", degrees > -180f && degrees <= 180f)
            // Result is always a multiple of 90 (no fractional drift).
            assertEquals(0f, degrees % 90f, 0.001f)
        }
    }

    @Test
    fun autoRotateOffKeepsIconsUprightByCounterRotating() {
        // Window stays portrait (display 0) while the phone is turned.
        assertEquals(90f, relativeControlRotationDegrees(UiRotation.ROTATION_270, 0), 0.01f)  // turned counter-clockwise
        assertEquals(-90f, relativeControlRotationDegrees(UiRotation.ROTATION_90, 0), 0.01f)  // turned clockwise
        assertEquals(180f, relativeControlRotationDegrees(UiRotation.ROTATION_180, 0), 0.01f)
    }

    // ---- jitter -----------------------------------------------------------------------------

    @Test
    fun tiltWithinTwentyFiveDegreesNeverChangesQuadrant() {
        for (start in UiRotation.entries) for (tilt in -25..25) {
            val angle = Math.floorMod(start.sensorCenterDegrees + tilt, 360)
            assertEquals("start=$start tilt=$tilt", start, quantizedUiRotation(angle, start))
        }
    }

    @Test
    fun oscillatingAcrossQuadrantBoundaryDoesNotFlap() {
        var current = UiRotation.ROTATION_0
        var transitions = 0
        // Hand wobbling 40..50 degrees around the 45-degree boundary, 200 samples.
        repeat(200) { i ->
            val next = quantizedUiRotation(if (i % 2 == 0) 40 else 50, current)
            if (next != current) transitions++
            current = next
        }
        assertEquals(0, transitions)
    }

    @Test
    fun fullSweepProducesExactlyFourTransitionsEachDirection() {
        fun sweep(angles: IntProgression): Int {
            var current = UiRotation.ROTATION_0
            var transitions = 0
            for (angle in angles) {
                val next = quantizedUiRotation(Math.floorMod(angle, 360), current)
                if (next != current) transitions++
                current = next
            }
            return transitions
        }
        assertEquals(4, sweep(0..360))
        assertEquals(4, sweep(360 downTo 0))
    }

    // ---- RebindGate (pendingRebindAfterRecording / Finalize race) ------------------------------

    @Test
    fun rebindProceedsWhenIdle() {
        val gate = RebindGate()
        assertTrue(gate.request(recordingActive = false))
        assertFalse(gate.pending)
        assertFalse(gate.onRecordingEnded())
    }

    @Test
    fun rebindRequestedDuringRecordingIsDeferredAndReplayedExactlyOnce() {
        val gate = RebindGate()
        assertFalse(gate.request(recordingActive = true))
        assertFalse(gate.request(recordingActive = true))
        assertFalse(gate.request(recordingActive = true))
        assertTrue(gate.pending)
        assertTrue("first Finalize replays", gate.onRecordingEnded())
        assertFalse("second Finalize must not replay", gate.onRecordingEnded())
        assertFalse(gate.pending)
    }

    @Test
    fun freshBindSupersedesDeferredRebind() {
        val gate = RebindGate()
        gate.request(recordingActive = true)
        assertTrue(gate.request(recordingActive = false))
        assertFalse(gate.onRecordingEnded())
    }

    @Test
    fun recordingEndedWithoutDeferredRequestDoesNotRebind() {
        val gate = RebindGate()
        assertFalse(gate.onRecordingEnded())
    }

    @Test
    fun failedStartPathAndFinalizePathShareTheSameGateSemantics() {
        // catch{} (failed start) and Finalize both call onRecordingEnded(); a rebind deferred before either must survive exactly one.
        val gate = RebindGate()
        gate.request(recordingActive = true)
        val replayedOnFailure = gate.onRecordingEnded()
        val replayedOnLateFinalize = gate.onRecordingEnded()
        assertEquals(1, listOf(replayedOnFailure, replayedOnLateFinalize).count { it })
    }

    // ---- Activity must survive configuration changes ----------------------------------------------------

    @Test
    fun mainActivityHandlesEveryRotationRelatedConfigChangeItself() {
        val manifest = listOf(
            Paths.get("app/src/main/AndroidManifest.xml"),
            Paths.get("src/main/AndroidManifest.xml"),
        ).first { Files.exists(it) }
        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(manifest.toFile())
        val ns = "http://schemas.android.com/apk/res/android"
        val activities = document.getElementsByTagName("activity")
        val main = (0 until activities.length).map { activities.item(it) as org.w3c.dom.Element }
            .first { it.getAttributeNS(ns, "name") == ".MainActivity" }
        val handled = main.getAttributeNS(ns, "configChanges").split('|').toSet()
        listOf(
            "orientation", "screenSize", "smallestScreenSize", "screenLayout",
            "keyboardHidden", "uiMode", "density", "fontScale", "layoutDirection", "locale",
        ).forEach { assertTrue("configChanges missing '$it' -> Activity would be recreated and kill recording", it in handled) }
        assertEquals("fullSensor", main.getAttributeNS(ns, "screenOrientation"))
    }

    // ---- responsive layout budget (catches clipping/overlap without a device) -------------------------------

    private val phoneSizesDp = listOf(
        320 to 568, 360 to 640, 360 to 780, 393 to 873, 411 to 891, 412 to 915, 430 to 932, 360 to 800,
    )

    @Test
    fun touchTargetsStayAtLeast48dpInEveryTokenSet() {
        listOf(
            CameraUiTokens.compactPhone, CameraUiTokens.standardPhone,
            CameraUiTokens.largePhone, CameraUiTokens.tablet,
        ).forEach { t ->
            assertTrue(t.minimumTouchTarget.value >= 48f)
            assertTrue(t.lensTouchTarget.value >= 48f)
            assertTrue(t.secondaryTouchTarget.value >= 48f)
            assertTrue(t.shutterTouchTarget.value >= 48f)
        }
    }

    @Test
    fun portraitBottomRowFitsEveryPhoneWidth() {
        phoneSizesDp.forEach { (w, _) ->
            val t = responsiveTokens(w, 800)
            // column padding 16*2, row padding 24*2
            val usable = w - 2 * 16 - 2 * 24
            val needed = 2 * t.secondaryTouchTarget.value + t.shutterTouchTarget.value
            assertTrue("width=$w usable=$usable needed=$needed", usable >= needed)
        }
    }

    @Test
    fun landscapeRailFitsShutterAndThreeControlsOnEveryPhoneAndTablet() {
        val sizes = phoneSizesDp.map { (w, h) -> h to w } + listOf(640 to 320, 915 to 412, 1280 to 800, 1000 to 420)
        sizes.forEach { (w, h) ->
            val layout = CameraMath.adaptiveLayout(w, h)
            assertTrue(layout == AdaptiveLayout.PhoneLandscape || layout == AdaptiveLayout.TabletLandscape)
            val policy = cameraUiLayoutPolicy(layout)
            val t = responsiveTokens(w, h)
            // Rail has no horizontal padding: shutter touch target must fit its width.
            assertTrue("rail ${policy.captureRailWidth} < shutter ${t.shutterTouchTarget} @ ${w}x$h", policy.captureRailWidth.value >= t.shutterTouchTarget.value)
            // switch + shutter + gallery stacked, plus 2*14dp vertical padding.
            val railHeight = 2 * t.secondaryTouchTarget.value + t.shutterTouchTarget.value + 28f
            assertTrue("rail height $railHeight > $h", railHeight <= h)
            // Top controls keep room for at least flash + quick settings + timer left of the rail.
            assertTrue("top capacity @ ${w}x$h", topControlCapacity(w - policy.captureRailWidth.value) >= 3)
        }
    }

    @Test
    fun landscapeVerticalBudgetLeavesRoomForTopBarProPanelAndStrip() {
        val proPanelMinimum = 114f // header 66 + slider row 48
        listOf(320, 360, 412).forEach { h ->
            val t = responsiveTokens(h * 2, h)
            val topBar = t.minimumTouchTarget.value + 12f
            val stack = topBar + proPanelMinimum + t.landscapeStripHeight.value + 4f
            assertTrue("height=$h stack=$stack", stack <= h)
        }
    }

    @Test
    fun narrowestPhoneKeepsCoreTopControlsVisible() {
        val slots = visibleTopControlSlots(
            availableWidthDp = 320f - 16f, // 8dp horizontal padding each side
            hasFlash = true,
            captureFormatControlsVisible = true,
            hasStabilization = true,
            compositionVisible = true,
        )
        assertTrue(CameraTopControlSlot.Flash in slots)
        assertTrue(CameraTopControlSlot.QuickSettings in slots)
        assertTrue(CameraTopControlSlot.Timer in slots)
    }

    @Test
    fun adaptiveLayoutClassificationHandlesAwkwardAspectRatios() {
        assertEquals(AdaptiveLayout.PhoneLandscape, CameraMath.adaptiveLayout(1000, 420)) // 2.4:1
        assertEquals(AdaptiveLayout.PhonePortrait, CameraMath.adaptiveLayout(420, 1000))
        assertEquals(AdaptiveLayout.PhonePortrait, CameraMath.adaptiveLayout(599, 1200))
        assertEquals(AdaptiveLayout.TabletPortrait, CameraMath.adaptiveLayout(600, 960))
        assertEquals(AdaptiveLayout.PhoneLandscape, CameraMath.adaptiveLayout(500, 500)) // square window (split-screen)
    }
}
