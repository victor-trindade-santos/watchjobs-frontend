package com.watchjobs.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class SavedJob(
    val id: Long? = null,
    val positionRole: String,
    val organizationName: String,
    val jobUrl: String? = null,
    val locationCity: String? = null,
    val locationState: String? = null,
    val locationCountryCode: String? = null,
    val workMode: WorkMode? = null,
    val experienceLevel: ExperienceLevel? = null,
    val jobType: JobType? = null,
    val salaryMin: Double? = null,
    val salaryMax: Double? = null,
    val salaryCurrency: String? = null,
    val status: SavedJobStatus = SavedJobStatus.SAVED,
    val notes: String? = null,
    val savedAt: LocalDateTime? = null,
    val statusUpdatedAt: LocalDateTime? = null
)
