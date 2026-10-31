package com.watchjobs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.watchjobs.ui.onboarding.ProvideTour
import com.watchjobs.ui.onboarding.TourOverlay
import com.watchjobs.ui.onboarding.TourRegistry
import com.watchjobs.ui.onboarding.TourStep
import com.watchjobs.ui.onboarding.tourTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Tour guiado: destaca elementos reais, avança, volta, mostra o progresso e avisa como terminou.
@OptIn(ExperimentalTestApi::class)
class TourOverlayTest {
    @Test
    fun navigatesStepsAndReportsTheResult() = runDesktopComposeUiTest(width = 1000, height = 700) {
        val registry = TourRegistry()
        val results = mutableListOf<Boolean>()
        val steps = listOf(TourStep("first", "Primeiro", "Texto 1"), TourStep("second", "Segundo", "Texto 2"))
        setContent {
            ProvideTour(registry) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.offset(40.dp, 40.dp).size(120.dp, 40.dp).tourTarget("first"))
                    Box(Modifier.offset(400.dp, 300.dp).size(80.dp, 40.dp).tourTarget("second"))
                    TourOverlay(registry, steps) { results += it }
                }
            }
        }
        waitForIdle()
        assertTrue("first" in registry.bounds && "second" in registry.bounds)
        onNodeWithTag("tourProgress").assertTextEquals("1 de 2")
        onNodeWithTag("tourNext").performClick()
        onNodeWithTag("tourProgress").assertTextEquals("2 de 2")
        onNodeWithTag("tourPrevious").performClick()
        onNodeWithTag("tourProgress").assertTextEquals("1 de 2")
        onNodeWithTag("tourSkip").performClick()
        onNodeWithTag("tourNext").performClick()
        onNodeWithTag("tourFinish").performClick()
        assertEquals(listOf(false, true), results)
    }
}
