package com.watchjobs

import com.watchjobs.data.local.InMemoryKeyValueStore
import com.watchjobs.data.local.TokenStorage
import com.watchjobs.data.remote.ApiClient
import com.watchjobs.data.remote.ApiJson
import com.watchjobs.data.remote.HttpResult
import com.watchjobs.data.remote.HttpTransport
import com.watchjobs.data.repository.FeedRepository
import com.watchjobs.data.repository.JobSearchRepository
import com.watchjobs.model.WorkMode
import com.watchjobs.viewmodel.FeedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Busca manual: mexer nos filtros não consulta o backend; só o botão "Buscar vagas" envia.
class FeedFiltersTest {
    private class RecordingTransport : HttpTransport {
        val feedCalls = CopyOnWriteArrayList<String>()
        override suspend fun send(method: String, url: String, headers: Map<String, String>, body: String?): HttpResult = when {
            "/jobs/feed" in url -> {
                feedCalls += url
                HttpResult(200, """{"items":[],"page":0,"size":20,"total":0}""")
            }
            "/jobs/facets" in url -> HttpResult(200, "{}")
            "/jobs/skills" in url -> HttpResult(200, "[]")
            else -> HttpResult(404, """{"code":"NOT_FOUND","message":"x"}""")
        }
    }

    private suspend fun RecordingTransport.awaitCalls(count: Int) = withTimeout(5_000) {
        while (feedCalls.size < count) delay(20)
    }

    @Test
    fun filtersOnlyApplyOnButtonClick() = runBlocking {
        val transport = RecordingTransport()
        val api = ApiClient(transport, TokenStorage(InMemoryKeyValueStore(), ApiJson), "http://test")
        val viewModel = FeedViewModel(FeedRepository(api), JobSearchRepository(api))
        transport.awaitCalls(1)

        viewModel.updateDraft { it.copy(workModes = setOf(WorkMode.REMOTE)) }
        delay(500)
        assertEquals(1, transport.feedCalls.size)
        assertTrue(viewModel.state.value.hasPendingFilters)
        assertTrue(viewModel.state.value.query.workModes.isEmpty())

        viewModel.applyFilters()
        transport.awaitCalls(2)
        assertTrue("workMode=REMOTE" in transport.feedCalls.last())
        assertFalse(viewModel.state.value.hasPendingFilters)

        viewModel.clearFilters()
        delay(300)
        assertEquals(2, transport.feedCalls.size)
        assertEquals(setOf(WorkMode.REMOTE), viewModel.state.value.query.workModes)

        viewModel.applyFilters()
        transport.awaitCalls(3)
        assertFalse("workMode=" in transport.feedCalls.last())

        // Ordenação continua valendo na hora.
        viewModel.update { it.copy(sort = com.watchjobs.data.repository.JobSort.DATE_DESC) }
        transport.awaitCalls(4)
        assertTrue("sort=DATE" in transport.feedCalls.last())
    }
}
