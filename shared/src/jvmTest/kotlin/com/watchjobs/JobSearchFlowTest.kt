package com.watchjobs

import com.watchjobs.data.dto.SigninRequest
import com.watchjobs.data.remote.ApiException
import com.watchjobs.data.remote.FakeBackendTransport
import com.watchjobs.data.repository.JobSearchQuery
import com.watchjobs.data.repository.toQueryString
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.WorkMode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Buscar vagas no modo demonstração: busca, filtros, salvar e o 409 ao salvar de novo.
class JobSearchFlowTest {
    @Test
    fun queryStringKeepsOnlyFilledFilters() {
        assertEquals("?page=0&size=20", JobSearchQuery().toQueryString(0, 20))
        assertEquals(
            "?q=engenheiro%20s%C3%AAnior&workMode=REMOTE&page=1&size=10",
            JobSearchQuery(text = " engenheiro sênior ", workMode = WorkMode.REMOTE, level = ExperienceLevel.UNKNOWN).toQueryString(1, 10)
        )
    }

    @Test
    fun searchSaveAndConflictInDemoMode() = runBlocking {
        val container = AppContainer(demoMode = true)
        container.authRepository.signin(SigninRequest(FakeBackendTransport.DEMO_EMAIL, FakeBackendTransport.DEMO_PASSWORD))
        val jobs = container.jobSearchRepository

        val all = jobs.search(JobSearchQuery())
        assertEquals(4L, all.total)
        assertEquals(setOf(1001L, 1003L), jobs.search(JobSearchQuery(text = "engineer")).items.map { it.id }.toSet())
        assertEquals(listOf(1002L), jobs.search(JobSearchQuery(workMode = WorkMode.REMOTE)).items.map { it.id })
        assertEquals(2, jobs.search(JobSearchQuery(location = "florian")).items.size + jobs.search(JobSearchQuery(location = "rio")).items.size)

        val saved = jobs.save(1001)
        assertEquals("Senior Backend Engineer (Kotlin)", saved.positionRole)
        assertTrue(jobs.search(JobSearchQuery(text = "kotlin")).items.single().alreadySaved)
        assertTrue(container.savedJobRepository.list().any { it.id == saved.id })

        val conflict = assertFailsWith<ApiException> { jobs.save(1001) }
        assertEquals(409, conflict.status)
        assertEquals("Você já salvou esta vaga", conflict.error.message)

        assertEquals(1001L, jobs.detail(1001).job.id)
    }
}
