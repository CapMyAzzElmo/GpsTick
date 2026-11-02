package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.SpeedPreset
import com.example.model.SpeedUnit
import com.example.service.JoyStickController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GPS JoyStick", appName)
    }

    @Test
    fun `verify speed presets and configuration`() {
        JoyStickController.setSpeedPreset(SpeedPreset.WALK)
        assertEquals(4.5f, JoyStickController.speedConfig.value.speedKmh, 0.01f)

        JoyStickController.setSpeedPreset(SpeedPreset.DRIVE)
        assertEquals(55.0f, JoyStickController.speedConfig.value.speedKmh, 0.01f)

        JoyStickController.setSpeedUnit(SpeedUnit.MPH)
        assertEquals(SpeedUnit.MPH, JoyStickController.speedConfig.value.unit)
    }

    @Test
    fun `verify teleport updates coordinate state`() {
        val targetLat = 40.7812
        val targetLng = -73.9665
        JoyStickController.teleportTo(targetLat, targetLng)

        assertEquals(targetLat, JoyStickController.locationState.value.latitude, 0.0001)
        assertEquals(targetLng, JoyStickController.locationState.value.longitude, 0.0001)
    }
}
