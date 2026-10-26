package com.watchjobs.data.repository

import com.watchjobs.data.dto.ApplicationRequest
import com.watchjobs.data.dto.AuthResponse
import com.watchjobs.data.dto.DeleteAccountRequest
import com.watchjobs.data.dto.DemoForgotResponse
import com.watchjobs.data.dto.ForgotPasswordRequest
import com.watchjobs.data.dto.LogoutRequest
import com.watchjobs.data.dto.ResetPasswordRequest
import com.watchjobs.data.dto.SavedJobRequest
import com.watchjobs.data.dto.SigninRequest
import com.watchjobs.data.dto.SignupRequest
import com.watchjobs.data.local.TokenStorage
import com.watchjobs.data.remote.ApiClient
import com.watchjobs.data.remote.ApiException
import com.watchjobs.data.remote.ApiJson
import kotlinx.serialization.encodeToString
import com.watchjobs.model.Application
import com.watchjobs.model.ApplicationStage
import com.watchjobs.model.SavedJob
import com.watchjobs.model.User

class AuthRepository(
    private val api: ApiClient,
    private val tokenStorage: TokenStorage
) {
    suspend fun signin(request: SigninRequest): User {
        val response: AuthResponse = api.post("/signin", request)
        tokenStorage.save(response)
        return response.user
    }

    suspend fun signup(request: SignupRequest): User = api.post("/signup", request)

    suspend fun me(): User = api.get("/me")

    fun currentUser(): User? = tokenStorage.user()

    fun isLoggedIn(): Boolean = tokenStorage.hasValidSession()

    suspend fun logout() {
        try {
            if (tokenStorage.refreshToken() != null) {
                api.requestWithLatestBody("POST", "/auth/logout") {
                    tokenStorage.refreshToken()?.let { ApiJson.encodeToString(LogoutRequest(it)) }
                }
            }
        } catch (_: ApiException) {
            // A sessão local é limpa mesmo se o servidor já não reconhecer o token.
        } finally {
            tokenStorage.clear()
        }
    }

    suspend fun deleteAccount(password: String) {
        api.request("DELETE", "/me", ApiJson.encodeToString(DeleteAccountRequest(password)))
        tokenStorage.clear()
    }

    suspend fun forgotPassword(email: String): String? {
        val body = api.request("POST", "/password/forgot", ApiJson.encodeToString(ForgotPasswordRequest(email)))
        if (body.isBlank()) return null
        return runCatching { ApiJson.decodeFromString<DemoForgotResponse>(body).demoCode }.getOrNull()
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String, confirmPassword: String) {
        api.request(
            "POST",
            "/password/reset",
            ApiJson.encodeToString(ResetPasswordRequest(email, code, newPassword, confirmPassword))
        )
    }
}

class SavedJobRepository(private val api: ApiClient) {
    suspend fun list(query: String = ""): List<SavedJob> = api.get("/saved-jobs" + queryParam(query))

    suspend fun get(id: Long): SavedJob = api.get("/saved-jobs/$id")

    suspend fun create(request: SavedJobRequest): SavedJob = api.post("/saved-jobs", request)

    suspend fun update(id: Long, request: SavedJobRequest): SavedJob = api.put("/saved-jobs/$id", request)

    suspend fun delete(id: Long) = api.delete("/saved-jobs/$id")
}

class ApplicationRepository(private val api: ApiClient) {
    suspend fun list(query: String = ""): List<Application> = api.get("/applications" + queryParam(query))

    suspend fun get(id: Int): Application = api.get("/applications/$id")

    suspend fun create(request: ApplicationRequest): Application = api.post("/applications", request)

    suspend fun update(id: Int, request: ApplicationRequest): Application = api.put("/applications/$id", request)

    suspend fun delete(id: Int) = api.delete("/applications/$id")

    suspend fun stages(): List<ApplicationStage> = api.get("/application-stages")
}

private fun queryParam(query: String): String =
    if (query.isBlank()) "" else "?q=" + encodeQuery(query.trim())

// Codifica o texto da busca para a URL (UTF-8, sem depender de biblioteca de rede).
internal fun encodeQuery(value: String): String = buildString {
    value.encodeToByteArray().forEach { byte ->
        val c = byte.toInt().toChar()
        if (c.isLetterOrDigit() && byte >= 0 || c in "-_.~") {
            append(c)
        } else {
            append('%')
            append(HEX[(byte.toInt() shr 4) and 0x0F])
            append(HEX[byte.toInt() and 0x0F])
        }
    }
}

private const val HEX = "0123456789ABCDEF"
