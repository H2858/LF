package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LivingPhonePreferences
import com.example.model.ModuleType
import com.example.model.SoundCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Living Phone", appName)
    }

    @Test
    fun `verify all 8 modules are defined and default enabled`() {
        val modules = ModuleType.values()
        assertEquals(8, modules.size)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = LivingPhonePreferences(context)
        for (module in modules) {
            assertTrue(preferences.isModuleEnabled(module))
        }
    }

    @Test
    fun `verify sound categories mapping`() {
        assertNotNull(SoundCategory.BATTERY_100.folderName)
        assertNotNull(SoundCategory.BATTERY_15.folderName)
        assertNotNull(SoundCategory.POCKET_DARK.folderName)
        assertEquals("POCKET/dark", SoundCategory.POCKET_DARK.folderName)
        assertEquals("POCKET/light", SoundCategory.POCKET_LIGHT.folderName)
        assertEquals("SLEEP/late_night", SoundCategory.SLEEP_LATE_NIGHT.folderName)
        assertEquals("SLEEP/sleep_reminder", SoundCategory.SLEEP_REMINDER.folderName)
        assertEquals("CHARGING/Connect", SoundCategory.CHARGING_CONNECT.folderName)
        assertEquals("CHARGING/Disconnect", SoundCategory.CHARGING_DISCONNECT.folderName)
    }
}
