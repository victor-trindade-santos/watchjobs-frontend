package com.watchjobs.data.repository

import com.watchjobs.data.remote.ApiClient
import com.watchjobs.data.remote.ApiJson
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.JobDetail
import com.watchjobs.model.JobSearchPage
import com.watchjobs.model.JobType
import com.watchjobs.model.SavedJob
import com.watchjobs.model.WorkMode

data class JobSearchQuery(
    val text: String = "",
    val location: String = "",
    val workMode: WorkMode? = null,
    val level: ExperienceLevel? = null,
    val jobType: JobType? = null
)

// Busca no corpus do backend (não consulta as fontes) e "salvar do corpus".
class JobSearchRepository(private val api: ApiClient) {
    suspend fun search(query: JobSearchQuery, page: Int = 0, size: Int = PAGE_SIZE): JobSearchPage =
        api.get("/jobs/search" + query.toQueryString(page, size))

    suspend fun detail(id: Long): JobDetail = api.get("/jobs/$id")

    // 201 com a vaga salva; 409 quando ela já estava salva.
    suspend fun save(id: Long): SavedJob = ApiJson.decodeFromString(SavedJob.serializer(), api.request("POST", "/jobs/$id/save", null))

    companion object {
        const val PAGE_SIZE = 20
    }
}

internal fun JobSearchQuery.toQueryString(page: Int, size: Int): String {
    val params = buildList {
        text.trim().takeIf { it.isNotEmpty() }?.let { add("q=" + encodeQuery(it)) }
        location.trim().takeIf { it.isNotEmpty() }?.let { add("location=" + encodeQuery(it)) }
        workMode?.takeIf { it != WorkMode.UNKNOWN }?.let { add("workMode=${it.name}") }
        level?.takeIf { it != ExperienceLevel.UNKNOWN }?.let { add("level=${it.name}") }
        jobType?.takeIf { it != JobType.UNKNOWN }?.let { add("jobType=${it.name}") }
        add("page=$page")
        add("size=$size")
    }
    return "?" + params.joinToString("&")
}
