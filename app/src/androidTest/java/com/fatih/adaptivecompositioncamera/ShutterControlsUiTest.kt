package com.fatih.adaptivecompositioncamera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.ui.camera.PocoStyleShutterControls
import com.fatih.adaptivecompositioncamera.ui.theme.AdaptiveCompositionCameraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Camera-free Compose UI tests for the shutter deck. Requires a device/emulator: `gradlew :app:connectedDebugAndroidTest`. */
@RunWith(AndroidJUnit4::class)
class ShutterControlsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(
        modifier: Modifier = Modifier.fillMaxWidth(),
        landscape: Boolean = false,
        mode: CameraMode = CameraMode.Photo,
        recording: Boolean = false,
        canSwitch: Boolean = true,
        hasMedia: Boolean = false,
        onShutter: () -> Unit = {},
        onZoom: (Float) -> Unit = {},
        onMode: (CameraMode) -> Unit = {},
    ) = compose.setContent {
        AdaptiveCompositionCameraTheme {
            PocoStyleShutterControls(
                activeMode = mode,
                availableModes = listOf(CameraMode.Photo, CameraMode.Video),
                availableCameras = emptyList(),
                activeCameraId = null,
                maxResolution = null,
                zoom = 1f,
                minZoom = 0.6f,
                maxZoom = 5f,
                showZoomSlider = false,
                onToggleZoomSlider = {},
                onZoom = onZoom,
                latestThumbnail = null,
                hasLatestMedia = hasMedia,
                onLatestMedia = {},
                isRecording = recording,
                canSwitch = canSwitch,
                onSwitch = {},
                onCamera = {},
                onMode = onMode,
                onMore = {},
                onShutter = onShutter,
                controlRotationDegrees = 0f,
                modifier = modifier,
                landscape = landscape,
            )
        }
    }

    @Test
    fun shutterIsAButtonThatFiresOnTap() {
        var taps = 0
        show(onShutter = { taps++ })
        compose.onNodeWithContentDescription("Take photo").assertHasClickAction().performClick()
        assertEquals(1, taps)
    }

    @Test
    fun shutterDescribesVideoAndRecordingStates() {
        show(mode = CameraMode.Video)
        compose.onNodeWithContentDescription("Start video recording").assertIsDisplayed()
    }

    @Test
    fun shutterDescribesRecordingState() {
        show(mode = CameraMode.Video, recording = true)
        compose.onNodeWithContentDescription("Stop recording").assertIsDisplayed()
    }

    @Test
    fun shutterAndSecondaryControlsMeetTouchTargetMinimum() {
        show()
        listOf("Take photo", "Switch camera", "Open gallery").forEach {
            compose.onNodeWithContentDescription(it).assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun unavailableControlsAreDisabled() {
        show(canSwitch = false, hasMedia = false)
        compose.onNodeWithContentDescription("Switch camera").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Open gallery").assertIsNotEnabled()
    }

    @Test
    fun zoomPresetInvokesCallback() {
        var zoom = 0f
        show(onZoom = { zoom = it })
        compose.onNodeWithText("2x").performClick()
        assertEquals(2f, zoom, 0.001f)
    }

    @Test
    fun modeLabelSwitchesMode() {
        var picked: CameraMode? = null
        show(onMode = { picked = it })
        compose.onNodeWithText("Video").performClick()
        assertEquals(CameraMode.Video, picked)
    }

    @Test
    fun landscapeRailKeepsEveryControlInsideItsBounds() {
        val railWidth = 100.dp
        val railHeight = 320.dp // shortest supported landscape height
        compose.setContent {
            AdaptiveCompositionCameraTheme {
                Box(Modifier.size(railWidth, railHeight)) {
                    PocoStyleShutterControls(
                        activeMode = CameraMode.Photo,
                        availableModes = listOf(CameraMode.Photo, CameraMode.Video),
                        availableCameras = emptyList(),
                        activeCameraId = null,
                        maxResolution = null,
                        zoom = 1f, minZoom = 1f, maxZoom = 1f,
                        showZoomSlider = false, onToggleZoomSlider = {}, onZoom = {},
                        latestThumbnail = null, hasLatestMedia = true, onLatestMedia = {},
                        isRecording = false, canSwitch = true, onSwitch = {},
                        onCamera = {}, onMode = {}, onMore = {}, onShutter = {},
                        controlRotationDegrees = 0f,
                        modifier = Modifier.fillMaxSize(),
                        landscape = true,
                    )
                }
            }
        }
        listOf("Take photo", "Switch camera", "Open gallery").forEach { description ->
            val node = compose.onNodeWithContentDescription(description)
            node.assertIsDisplayed()
            val b = node.getUnclippedBoundsInRoot()
            assertTrue("$description left=${b.left}", b.left >= 0.dp)
            assertTrue("$description right=${b.right}", b.right <= railWidth)
            assertTrue("$description top=${b.top}", b.top >= 0.dp)
            assertTrue("$description bottom=${b.bottom}", b.bottom <= railHeight)
        }
    }
}
