package com.fatih.adaptivecompositioncamera

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI-07 / CAM-ACT: the Activity must not be recreated by rotation. Requires a device/emulator. */
@RunWith(AndroidJUnit4::class)
class OrientationActivityTest {
    @get:Rule
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    @Test
    fun activityInstanceSurvivesTwentyRotationFlips() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var original: Activity? = null
            scenario.onActivity { original = it }
            val sequence = listOf(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            )
            repeat(20) { index ->
                scenario.onActivity { it.requestedOrientation = sequence[index % sequence.size] }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                Thread.sleep(500) // let the window finish rotating
                scenario.onActivity { current ->
                    assertSame("Activity was recreated on rotation #$index", original, current)
                }
            }
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR }
        }
    }
}
