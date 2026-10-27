package com.watchjobs.model

import kotlinx.serialization.Serializable

// Vaga do corpus (coletada das fontes). Fica 30 dias; para guardar, o usuário salva.
// As datas vêm como texto ISO-8601 com fuso; a tela usa expiresInDays, calculado pelo backend.
@Serializable
data class JobPosting(
    val id: Long,
    val title: String,
    val organizationName: String,
    val locationCity: String? = null,
    val locationState: String? = null,
    val locationCountryCode: String? = null,
    val rawLocation: String? = null,
    val workMode: WorkMode? = null,
    val experienceLevel: ExperienceLevel? = null,
    val jobType: JobType? = null,
    val roleFamily: String? = null,
    val salaryMin: Double? = null,
    val salaryMax: Double? = null,
    val salaryCurrency: String? = null,
    val jobUrl: String,
    val sourceName: String,
    val requiresAttribution: Boolean = false,
    val descriptionSnippet: String? = null,
    val postedAt: String? = null,
    val discoveredAt: String? = null,
    val expiresAt: String? = null,
    val expiresInDays: Int = 0,
    val alreadySaved: Boolean = false
) {
    val placeLabel: String
        get() = listOfNotNull(locationCity, locationState).joinToString(", ").ifEmpty { rawLocation ?: "" }

    val expiresLabel: String
        get() = when (expiresInDays) {
            0 -> "Expira hoje"
            1 -> "Expira em 1 dia"
            else -> "Expira em $expiresInDays dias"
        }
}

@Serializable
data class JobSearchPage(
    val items: List<JobPosting> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val total: Long = 0
)

@Serializable
data class JobSkill(val name: String, val required: Boolean = true)

@Serializable
data class JobDetail(
    val job: JobPosting,
    val description: String? = null,
    val skills: List<JobSkill> = emptyList(),
    val corpusStatus: String = "ACTIVE",
    val match: MatchResult? = null
)
