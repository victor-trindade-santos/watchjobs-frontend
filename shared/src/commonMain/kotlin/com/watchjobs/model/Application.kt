package com.watchjobs.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Application(
    val id: Int? = null,
    val savedJobId: Long,
    val positionRole: String = "",
    val organizationName: String = "",
    val stageId: Int,
    val stageName: String = "",
    val status: String,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val interviewAt: LocalDateTime? = null,
    val notes: String? = null,
    val feedback: String? = null
)

@Serializable
data class ApplicationStage(
    val id: Int,
    val name: String
)

@Serializable
data class User(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val birthDate: LocalDate
) {
    val initials: String
        get() = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()
}
