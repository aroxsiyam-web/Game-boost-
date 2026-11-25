package com.example

import com.example.booster.model.DevicePermissionsState
import com.example.booster.model.GameProfile
import com.example.booster.model.GameProfileId
import com.example.booster.model.OverlayBoostMode
import com.example.booster.model.ReversibleSnapshot
import com.example.booster.system.OverlayRuntimeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoosterLogicTest {

    @Test
    fun `default game profiles include Free Fire PUBG Mobile and Minecraft`() {
        val defaults = GameProfile.defaults()
        assertEquals(3, defaults.size)
        assertEquals(
            listOf(GameProfileId.FREE_FIRE, GameProfileId.PUBG_MOBILE, GameProfileId.MINECRAFT),
            defaults.map { it.id },
        )
        assertTrue(defaults[0].packageCandidates.contains("com.dts.freefireth"))
        assertTrue(defaults[1].packageCandidates.contains("com.tencent.ig"))
        assertTrue(defaults[2].packageCandidates.contains("com.mojang.minecraftpe"))
    }

    @Test
    fun `reversible snapshot accurately counts active changes`() {
        val empty = ReversibleSnapshot()
        assertFalse(empty.isActive)
        assertEquals(0, empty.activeChangesCount)

        val active = ReversibleSnapshot(
            isActive = true,
            activeProfileId = GameProfileId.PUBG_MOBILE,
            activeProfileName = "PUBG Mobile",
            dndChanged = true,
            brightnessChanged = true,
            keepScreenOnChanged = true,
            animScaleChanged = true,
            gameModeChanged = true,
            trimmedPackagesCount = 4,
            freedRamMb = 320L,
        )
        assertEquals(6, active.activeChangesCount)
    }

    @Test
    fun `overlay runtime status and permissions default to disabled safely`() {
        val perms = DevicePermissionsState()
        assertFalse(perms.hasAccessibilityOverlayPermission)

        val runtime = OverlayRuntimeStatus()
        assertFalse(runtime.isServiceConnected)
        assertFalse(runtime.isOverlayVisible)
        assertEquals(0, runtime.measuredFps)
        assertEquals(OverlayBoostMode.BALANCED, runtime.activeBoostMode)
        assertEquals(
            listOf("Power Save", "Balanced", "Advance"),
            OverlayBoostMode.entries.map { it.displayName },
        )
    }
}
