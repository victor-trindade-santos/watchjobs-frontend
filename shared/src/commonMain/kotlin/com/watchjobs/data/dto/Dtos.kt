package com.watchjobs.data.dto

import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.JobType
import com.watchjobs.model.LenientLocalDateTimeSerializer
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.model.User
import com.watchjobs.model.WorkMode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class SavedJobRequest(
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
    val status: SavedJobStatus,
    val notes: String? = null
)

// No backend, a data vai como applicationDate e volta como startDate.
@Serializable
data class ApplicationRequest(
    val savedJobId: Long,
    val stageId: Int,
    val status: String,
    val applicationDate: LocalDate,
    val endDate: LocalDate? = null,
    val interviewAt: LocalDateTime? = null,
    val notes: String? = null,
    val feedback: String? = null
)

@Serializable
data class SigninRequest(
    val email: String,
    val password: String
)

@Serializable
data class SignupRequest(
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val email: String,
    val password: String,
    val confirmPassword: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val tokenType: String = "Bearer",
    @Serializable(with = LenientLocalDateTimeSerializer::class)
    val expiresAt: LocalDateTime,
    val user: User,
    val refreshToken: String = ""
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String,
    val confirmPassword: String
)

@Serializable
data class DeleteAccountRequest(val password: String)

@Serializable
data class DemoForgotResponse(val demoCode: String? = null)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, String?>? = null
)
