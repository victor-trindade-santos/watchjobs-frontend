package com.watchjobs.model

import kotlinx.serialization.Serializable

// Feed por nota (F4), busca sob demanda (F5), avisos (F5) e Kanban (F6).

@Serializable
data class Criterion(
    val key: String,
    val weight: Int,
    val applicable: Boolean,
    val value: Double,
    val points: Double,
    val detail: String,
    val inferred: Boolean = false
) {
    val label: String
        get() = when (key) {
            "role" -> "Cargo"
            "requiredSkills" -> "Skills obrigatórias"
            "optionalSkills" -> "Skills desejáveis"
            "seniority" -> "Senioridade"
            "workMode" -> "Modo e local"
            "salary" -> "Salário"
            "contract" -> "Contrato"
            else -> key
        }
}

@Serializable
data class MatchResult(
    val score: Double,
    val criteria: List<Criterion> = emptyList(),
    val missingSkills: List<String> = emptyList(),
    val algoVersion: String = ""
)

@Serializable
data class FeedItem(val job: JobPosting, val score: Double, val highlights: List<String> = emptyList(), val skills: List<String> = emptyList())

@Serializable
data class FacetValue(val value: String, val count: Long = 0)

@Serializable
data class FeedFacets(
    val roleFamilies: List<FacetValue> = emptyList(),
    val countries: List<FacetValue> = emptyList(),
    val states: List<FacetValue> = emptyList(),
    val cities: List<FacetValue> = emptyList(),
    val companies: List<FacetValue> = emptyList()
)

@Serializable
data class JobSkillFacet(val id: Long, val name: String, val count: Long = 0)

@Serializable
data class FeedPage(
    val items: List<FeedItem> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val total: Long = 0,
    val profileVersion: Int = 1
)

@Serializable
data class InteractionRequest(val type: String)

@Serializable
data class DiscoverRequest(val q: String, val location: String? = null, val workMode: WorkMode? = null)

@Serializable
data class DiscoverResult(
    val id: Long,
    val status: String,
    val partial: Boolean = false,
    val backgroundPending: Boolean = false,
    val cacheHit: Boolean = false,
    val syncFound: Int = 0,
    val asyncFound: Int = 0,
    val query: String = "",
    val items: List<FeedItem> = emptyList()
)

@Serializable
data class AppNotification(
    val id: Long,
    val type: String,
    val title: String,
    val body: String? = null,
    val createdAt: String = "",
    val readAt: String? = null,
    val payload: kotlinx.serialization.json.JsonObject = kotlinx.serialization.json.JsonObject(emptyMap())
) {
    val searchRequestId: Long?
        get() = (payload["searchRequestId"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull()
}

@Serializable
data class NotificationPage(val items: List<AppNotification> = emptyList(), val total: Long = 0)

@Serializable
data class UnreadCount(val unread: Long = 0)

@Serializable
data class NotificationPreference(
    val minScore: Double = 70.0,
    val inAppEnabled: Boolean = true,
    val emailEnabled: Boolean = false,
    val emailFrequency: String = "DAILY"
)

enum class BoardColumn(val label: String) {
    SAVED("Salvas"),
    APPLIED("Candidatado"),
    IN_REVIEW("Em análise"),
    INTERVIEW("Entrevista"),
    TECH_TEST("Teste técnico"),
    OFFER("Oferta"),
    CLOSED("Encerradas")
}

@Serializable
data class BoardCard(
    val id: Long,
    val positionRole: String,
    val organizationName: String,
    val locationCity: String? = null,
    val workMode: WorkMode? = null,
    val status: SavedJobStatus,
    val matchScore: Double? = null,
    val applicationId: Int? = null,
    val stageName: String? = null,
    val statusUpdatedAt: String? = null
)

@Serializable
data class BoardColumnData(val column: String, val cards: List<BoardCard> = emptyList())

@Serializable
data class Board(val columns: List<BoardColumnData> = emptyList()) {
    fun cards(column: BoardColumn): List<BoardCard> = columns.firstOrNull { it.column == column.name }?.cards.orEmpty()
}

@Serializable
data class MoveRequest(
    val toColumn: String,
    val outcome: SavedJobStatus? = null,
    val beforeId: Long? = null,
    val afterId: Long? = null
)

@Serializable
data class TimelineEvent(val id: Long, val type: String, val occurredAt: String, val description: String)
