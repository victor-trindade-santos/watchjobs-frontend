package com.watchjobs.model

import kotlinx.serialization.Serializable

// Analytics de candidaturas (F09): tudo vem calculado do backend a partir dos eventos reais do Kanban.
@Serializable
data class FunnelStep(val stage: String, val label: String, val count: Int, val conversionFromPrevious: Double? = null)

@Serializable
data class StageCount(val column: String, val label: String, val count: Int)

@Serializable
data class FamilyCount(
    val roleFamily: String? = null,
    val label: String,
    val saved: Int,
    val applied: Int,
    val interviews: Int,
    val offers: Int
)

@Serializable
data class PeriodPoint(val start: String, val saved: Int, val applied: Int, val advanced: Int, val closed: Int, val events: Int)

@Serializable
data class HistoryItem(
    val savedJobId: Long,
    val positionRole: String,
    val organizationName: String,
    val type: String,
    val description: String,
    val occurredAt: String
)

@Serializable
data class AnalyticsTotals(
    val saved: Int = 0,
    val applied: Int = 0,
    val active: Int = 0,
    val interviews: Int = 0,
    val offers: Int = 0,
    val accepted: Int = 0,
    val rejected: Int = 0,
    val withdrawn: Int = 0
)

@Serializable
data class Analytics(
    val generatedAt: String,
    val lastEventAt: String? = null,
    val weeks: Int,
    val totals: AnalyticsTotals,
    val funnel: List<FunnelStep> = emptyList(),
    val byStage: List<StageCount> = emptyList(),
    val byRoleFamily: List<FamilyCount> = emptyList(),
    val weekly: List<PeriodPoint> = emptyList(),
    val monthly: List<PeriodPoint> = emptyList(),
    val history: List<HistoryItem> = emptyList()
) {
    val isEmpty: Boolean get() = totals.saved == 0
}
