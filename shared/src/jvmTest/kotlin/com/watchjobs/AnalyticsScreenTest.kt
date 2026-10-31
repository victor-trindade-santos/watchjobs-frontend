package com.watchjobs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.watchjobs.data.local.InMemoryKeyValueStore
import com.watchjobs.data.local.TokenStorage
import com.watchjobs.data.remote.ApiClient
import com.watchjobs.data.remote.ApiJson
import com.watchjobs.data.remote.HttpResult
import com.watchjobs.data.remote.HttpTransport
import com.watchjobs.data.repository.AnalyticsRepository
import com.watchjobs.ui.analytics.AnalyticsScreen
import com.watchjobs.viewmodel.AnalyticsViewModel
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertTrue

// F09: a tela mostra os indicadores vindos do backend, troca o período e trata o usuário sem movimentações.
@OptIn(ExperimentalTestApi::class)
class AnalyticsScreenTest {
    private class FakeAnalytics(private val empty: Boolean) : HttpTransport {
        val calls = CopyOnWriteArrayList<String>()
        override suspend fun send(method: String, url: String, headers: Map<String, String>, body: String?): HttpResult {
            val path = url.substringAfter("http://test")
            calls += "$method $path"
            val weeks = path.substringAfter("weeks=").toInt()
            val weekly = (0 until weeks).joinToString(",") { i ->
                val events = if (i == weeks - 1 && !empty) 7 else 0
                """{"start":"2026-01-${(i % 28 + 1).toString().padStart(2, '0')}","saved":${if (events > 0) 3 else 0},"applied":${if (events > 0) 2 else 0},"advanced":0,"closed":0,"events":$events}"""
            }
            val saved = if (empty) 0 else 3
            val json = """
                {"generatedAt":"2026-01-20T10:00:00Z","weeks":$weeks,
                 "totals":{"saved":$saved,"applied":${if (empty) 0 else 2},"active":1,"interviews":1,"offers":0,"accepted":0,"rejected":1,"withdrawn":0},
                 "funnel":[{"stage":"SAVED","label":"Salvas","count":$saved},
                           {"stage":"APPLIED","label":"Candidaturas","count":2,"conversionFromPrevious":0.667},
                           {"stage":"IN_PROCESS","label":"Em processo","count":1,"conversionFromPrevious":0.5}],
                 "byStage":[{"column":"SAVED","label":"Salvas","count":1},{"column":"APPLIED","label":"Candidatado","count":1}],
                 "byRoleFamily":[{"roleFamily":"BACKEND","label":"Back-end","saved":3,"applied":2,"interviews":1,"offers":0}],
                 "weekly":[$weekly],"monthly":[],
                 "history":[{"savedJobId":9,"positionRole":"Dev Kotlin","organizationName":"ACME","type":"MOVED",
                             "description":"Movida de Salva para Candidatura enviada","occurredAt":"2026-01-20T09:00:00Z"}]}
            """.trimIndent()
            return HttpResult(200, json)
        }
    }

    private fun viewModel(transport: FakeAnalytics) =
        AnalyticsViewModel(AnalyticsRepository(ApiClient(transport, TokenStorage(InMemoryKeyValueStore(), ApiJson), "http://test")))

    @Test
    fun showsIndicatorsAndChangesThePeriod() = runDesktopComposeUiTest(width = 1280, height = 1600) {
        val transport = FakeAnalytics(empty = false)
        val opened = mutableListOf<Long>()
        val vm = viewModel(transport)
        setContent { AnalyticsScreen(vm, onOpenBoard = {}, onOpenSavedJob = { opened += it }) }
        waitUntil(timeoutMillis = 5_000) { vm.state.value.data != null }
        waitForIdle()

        onNodeWithTag("analyticsKpis").assertExists()
        onNodeWithTag("funnel_APPLIED").assertExists()
        onNodeWithTag("conversion_APPLIED").assertExists()
        assertTrue(onAllNodesWithText("67%", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        onNodeWithTag("analyticsWeekly").assertExists()
        onNodeWithText("Movida de Salva para Candidatura enviada").performClick()
        assertTrue(opened == listOf(9L))

        onNodeWithTag("period_26").performClick()
        waitUntil(timeoutMillis = 5_000) { vm.state.value.data?.weeks == 26 }
        assertTrue(transport.calls.first().endsWith("/analytics?weeks=12"))
        assertTrue(transport.calls.last().endsWith("/analytics?weeks=26"))
    }

    @Test
    fun emptyUserSeesGuidanceInsteadOfCharts() = runDesktopComposeUiTest(width = 1280, height = 900) {
        val vm = viewModel(FakeAnalytics(empty = true))
        setContent { AnalyticsScreen(vm, onOpenBoard = {}, onOpenSavedJob = {}) }
        waitUntil(timeoutMillis = 5_000) { vm.state.value.data != null }
        waitForIdle()
        onNodeWithTag("analyticsEmpty").assertExists()
        onNodeWithTag("analyticsFunnel").assertDoesNotExist()
    }
}
