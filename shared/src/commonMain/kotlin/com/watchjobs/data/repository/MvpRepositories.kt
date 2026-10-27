package com.watchjobs.data.repository

import com.watchjobs.data.remote.ApiClient
import com.watchjobs.data.remote.ApiJson
import com.watchjobs.model.ApplyImportRequest
import com.watchjobs.model.Board
import com.watchjobs.model.BoardCard
import com.watchjobs.model.DiscoverRequest
import com.watchjobs.model.DiscoverResult
import com.watchjobs.model.Education
import com.watchjobs.model.EducationOption
import com.watchjobs.model.EducationRequest
import com.watchjobs.model.Experience
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.ExperienceRequest
import com.watchjobs.model.FeedFacets
import com.watchjobs.model.FeedPage
import com.watchjobs.model.InteractionRequest
import com.watchjobs.model.JobSkillFacet
import com.watchjobs.model.JobType
import com.watchjobs.model.LanguageOption
import com.watchjobs.model.LevelCorrection
import com.watchjobs.model.MoveRequest
import com.watchjobs.model.NotificationPage
import com.watchjobs.model.NotificationPreference
import com.watchjobs.model.OrganizationOption
import com.watchjobs.model.Personal
import com.watchjobs.model.PersonalRequest
import com.watchjobs.model.Preferences
import com.watchjobs.model.PreferencesRequest
import com.watchjobs.model.PreferencesSuggestion
import com.watchjobs.model.Profile
import com.watchjobs.model.ProfileImport
import com.watchjobs.model.ProfileStats
import com.watchjobs.model.ProfileSummary
import com.watchjobs.model.RoleFamilyOption
import com.watchjobs.model.SkillOption
import com.watchjobs.model.TimelineEvent
import com.watchjobs.model.UnreadCount
import com.watchjobs.model.UserLanguage
import com.watchjobs.model.UserLanguageRequest
import com.watchjobs.model.UserSkill
import com.watchjobs.model.UserSkillRequest
import com.watchjobs.model.WorkMode
import kotlinx.serialization.encodeToString

// Perfil, preferências, perfil deduzido e importação do LinkedIn (F3).
class ProfileRepository(private val api: ApiClient) {
    suspend fun profile(): Profile = api.get("/profile")

    suspend fun stats(): ProfileStats = api.get("/profile/stats")

    // POST: alias de PATCH aceito pelo backend (o Android não envia PATCH).
    suspend fun updatePersonal(request: PersonalRequest): Personal = api.post("/profile/personal", request)

    suspend fun createExperience(request: ExperienceRequest): Experience = api.post("/profile/experiences", request)
    suspend fun updateExperience(id: Int, request: ExperienceRequest): Experience = api.put("/profile/experiences/$id", request)
    suspend fun deleteExperience(id: Int) = api.delete("/profile/experiences/$id")

    suspend fun createEducation(request: EducationRequest): Education = api.post("/profile/educations", request)
    suspend fun updateEducation(id: Int, request: EducationRequest): Education = api.put("/profile/educations/$id", request)
    suspend fun deleteEducation(id: Int) = api.delete("/profile/educations/$id")

    suspend fun createSkill(request: UserSkillRequest): UserSkill = api.post("/profile/skills", request)
    suspend fun updateSkill(id: Int, request: UserSkillRequest): UserSkill = api.put("/profile/skills/$id", request)
    suspend fun deleteSkill(id: Int) = api.delete("/profile/skills/$id")

    suspend fun createLanguage(request: UserLanguageRequest): UserLanguage = api.post("/profile/languages", request)
    suspend fun updateLanguage(id: Int, request: UserLanguageRequest): UserLanguage = api.put("/profile/languages/$id", request)
    suspend fun deleteLanguage(id: Int) = api.delete("/profile/languages/$id")

    // 204 quando o usuário ainda não tem preferências.
    suspend fun preferences(): Preferences? {
        val body = api.request("GET", "/profile/preferences", null)
        return if (body.isBlank()) null else ApiJson.decodeFromString(Preferences.serializer(), body)
    }

    suspend fun savePreferences(request: PreferencesRequest): Preferences = api.put("/profile/preferences", request)

    suspend fun suggestion(): PreferencesSuggestion = api.get("/profile/preferences/suggestion")

    suspend fun summary(): ProfileSummary = api.get("/profile/summary")

    suspend fun recompute(): ProfileSummary = ApiJson.decodeFromString(ProfileSummary.serializer(), api.request("POST", "/profile/summary/recompute", null))

    suspend fun correctLevel(level: ExperienceLevel?): ProfileSummary = api.put("/profile/summary/level", LevelCorrection(level))

    suspend fun uploadLinkedIn(fileName: String, bytes: ByteArray): ProfileImport =
        ApiJson.decodeFromString(ProfileImport.serializer(), api.upload("/profile/import/linkedin", "file", fileName, "application/zip", bytes))

    // PDF, DOCX ou TXT; o backend reconhece o formato pelo conteúdo.
    suspend fun uploadResume(fileName: String, bytes: ByteArray): ProfileImport {
        val mime = when (fileName.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "text/plain"
        }
        return ApiJson.decodeFromString(ProfileImport.serializer(), api.upload("/profile/import/resume", "file", fileName, mime, bytes))
    }

    suspend fun applyImport(id: Long, refs: List<String>): ProfileImport = api.post("/profile/import/$id/apply", ApplyImportRequest(refs))

    suspend fun discardImport(id: Long) = api.delete("/profile/import/$id")
}

class CatalogRepository(private val api: ApiClient) {
    suspend fun skills(q: String): List<SkillOption> = api.get("/catalog/skills?q=" + encodeQuery(q))
    suspend fun organizations(q: String, type: String? = null): List<OrganizationOption> =
        api.get("/catalog/organizations?q=" + encodeQuery(q) + (type?.let { "&type=$it" } ?: ""))
    suspend fun languages(): List<LanguageOption> = api.get("/catalog/languages")
    suspend fun educations(): List<EducationOption> = api.get("/catalog/educations")
    suspend fun roleFamilies(): List<RoleFamilyOption> = api.get("/catalog/role-families")
}

enum class JobSort(val label: String, val param: String) {
    SCORE_DESC("Maior match → menor match", "SCORE"),
    SCORE_ASC("Menor match → maior match", "SCORE_ASC"),
    DATE_DESC("Mais recentes", "DATE"),
    DATE_ASC("Mais antigas", "DATE_ASC"),
    SALARY_DESC("Maior salário", "SALARY_DESC"),
    SALARY_ASC("Menor salário", "SALARY_ASC")
}

enum class SkillMatchMode(val param: String) { ANY("ANY"), ALL("ALL") }

// Todos os filtros são opcionais: conjunto vazio, texto vazio e limite nulo não filtram.
data class FeedQuery(
    val text: String = "",
    val workModes: Set<WorkMode> = emptySet(),
    val levels: Set<ExperienceLevel> = emptySet(),
    val jobTypes: Set<JobType> = emptySet(),
    val salaryMin: Long? = null,
    val salaryMax: Long? = null,
    val skills: List<JobSkillFacet> = emptyList(),
    val skillMatch: SkillMatchMode = SkillMatchMode.ANY,
    val roleFamilies: Set<String> = emptySet(),
    val countries: Set<String> = emptySet(),
    val states: Set<String> = emptySet(),
    val cities: Set<String> = emptySet(),
    val companies: Set<String> = emptySet(),
    val sort: JobSort = JobSort.SCORE_DESC
) {
    // Quantos grupos de filtro estão em uso (o texto da busca não conta).
    val activeFilterCount: Int
        get() = listOf(
            workModes.isNotEmpty(), levels.isNotEmpty(), jobTypes.isNotEmpty(), salaryMin != null || salaryMax != null,
            skills.isNotEmpty(), roleFamilies.isNotEmpty(), countries.isNotEmpty(), states.isNotEmpty(),
            cities.isNotEmpty(), companies.isNotEmpty()
        ).count { it }
}

// Feed por nota (F4) e busca sob demanda (F5).
class FeedRepository(private val api: ApiClient) {
    suspend fun feed(query: FeedQuery, page: Int = 0, size: Int = 20): FeedPage {
        val params = buildList {
            query.text.trim().takeIf { it.isNotEmpty() }?.let { add("q=" + encodeQuery(it)) }
            query.workModes.filter { it != WorkMode.UNKNOWN }.forEach { add("workMode=${it.name}") }
            query.levels.filter { it != ExperienceLevel.UNKNOWN }.forEach { add("level=${it.name}") }
            query.jobTypes.filter { it != JobType.UNKNOWN }.forEach { add("jobType=${it.name}") }
            query.salaryMin?.let { add("salaryMin=$it") }
            query.salaryMax?.let { add("salaryMax=$it") }
            query.skills.forEach { add("skill=${it.id}") }
            if (query.skills.size > 1) add("skillMatch=${query.skillMatch.param}")
            query.roleFamilies.forEach { add("roleFamily=" + encodeQuery(it)) }
            query.countries.forEach { add("country=" + encodeQuery(it)) }
            query.states.forEach { add("state=" + encodeQuery(it)) }
            query.cities.forEach { add("city=" + encodeQuery(it)) }
            query.companies.forEach { add("company=" + encodeQuery(it)) }
            add("sort=${query.sort.param}")
            add("page=$page")
            add("size=$size")
        }
        return api.get("/jobs/feed?" + params.joinToString("&"))
    }

    suspend fun facets(): FeedFacets = api.get("/jobs/facets")

    suspend fun skills(q: String, page: Int = 0, size: Int = 20): List<JobSkillFacet> =
        api.get("/jobs/skills?q=" + encodeQuery(q) + "&page=$page&size=$size")

    suspend fun interact(id: Long, type: String) {
        api.request("POST", "/jobs/$id/interactions", ApiJson.encodeToString(InteractionRequest(type)))
    }

    suspend fun discover(request: DiscoverRequest): DiscoverResult = api.post("/jobs/discover", request)

    suspend fun discoverStatus(id: Long): DiscoverResult = api.get("/jobs/discover/$id")
}

class NotificationRepository(private val api: ApiClient) {
    suspend fun list(page: Int = 0): NotificationPage = api.get("/notifications?page=$page&size=30")
    suspend fun unreadCount(): Long = api.get<UnreadCount>("/notifications/unread-count").unread
    suspend fun markRead(id: Long) {
        api.request("POST", "/notifications/$id/read", null)
    }
    suspend fun markAllRead() {
        api.request("POST", "/notifications/read-all", null)
    }
    suspend fun preference(): NotificationPreference = api.get("/notifications/preferences")
    suspend fun savePreference(preference: NotificationPreference): NotificationPreference =
        api.put("/notifications/preferences", preference)
}

// Kanban (F6). Mover usa POST, alias de PATCH aceito pelo backend.
class BoardRepository(private val api: ApiClient) {
    suspend fun board(): Board = api.get("/saved-jobs/board")
    suspend fun move(id: Long, request: MoveRequest): BoardCard = api.post("/saved-jobs/$id/move", request)
    suspend fun timeline(id: Long): List<TimelineEvent> = api.get("/saved-jobs/$id/timeline")
}
