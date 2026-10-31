package com.watchjobs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.watchjobs.data.local.InMemoryKeyValueStore
import com.watchjobs.data.local.ThemePreference
import com.watchjobs.ui.components.MainScaffold
import com.watchjobs.ui.components.MainTab
import com.watchjobs.ui.theme.DarkPalette
import com.watchjobs.ui.theme.LightPalette
import com.watchjobs.ui.theme.ThemeMode
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.ui.theme.WatchJobsTheme
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

// F11: o ícone da navbar alterna o tema de todo o app e a escolha fica salva entre acessos.
@OptIn(ExperimentalTestApi::class)
class ThemeTest {
    @AfterTest
    fun resetTheme() = WatchJobsColors.apply(ThemeMode.LIGHT)

    @Test
    fun preferenceIsPersistedAndAppliedOnStartup() {
        val store = InMemoryKeyValueStore()
        ThemePreference(store).set(ThemeMode.DARK)
        WatchJobsColors.apply(ThemeMode.LIGHT)

        ThemePreference(store).applySaved()
        assertEquals(ThemeMode.DARK, WatchJobsColors.mode)
        assertEquals(DarkPalette.ink, WatchJobsColors.Ink)
        assertEquals(ThemeMode.LIGHT, ThemePreference(store).toggle())
        assertEquals(LightPalette.white, WatchJobsColors.White)
        assertEquals("LIGHT", store.get("theme"))
    }

    @Test
    fun accentTextStaysReadableInBothThemes() {
        WatchJobsColors.apply(ThemeMode.DARK)
        assertEquals(LightPalette.white, WatchJobsColors.OnAccent)
        assertNotEquals(WatchJobsColors.Ink, WatchJobsColors.White)
        assertNotEquals(WatchJobsColors.Ink, WatchJobsColors.Background)
    }

    @Test
    fun navbarToggleSwitchesTheWholeTheme() = runDesktopComposeUiTest(width = 1280, height = 600) {
        val preference = ThemePreference(InMemoryKeyValueStore())
        var surface = LightPalette.white
        setContent {
            WatchJobsTheme {
                surface = MaterialTheme.colorScheme.surfaceContainer
                MainScaffold(
                    initials = "AS",
                    currentTab = MainTab.JOB_SEARCH,
                    onTabSelected = {},
                    onUserMenu = {},
                    onToggleTheme = { preference.toggle() }
                ) {}
            }
        }
        onNodeWithContentDescription("Usar tema escuro").assertExists()
        onNodeWithTag("themeToggle").performClick()
        waitForIdle()
        assertEquals(ThemeMode.DARK, WatchJobsColors.mode)
        assertEquals(DarkPalette.white, surface)
        onNodeWithContentDescription("Usar tema claro").assertExists()
        onNodeWithTag("themeToggle").performClick()
        waitForIdle()
        assertEquals(LightPalette.white, surface)
    }
}
