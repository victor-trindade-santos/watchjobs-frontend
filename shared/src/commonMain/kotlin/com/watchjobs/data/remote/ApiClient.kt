package com.watchjobs.data.remote

import com.watchjobs.data.dto.ApiError
import com.watchjobs.data.dto.AuthResponse
import com.watchjobs.data.dto.RefreshRequest
import com.watchjobs.data.local.TokenStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

class ApiException(val status: Int, val error: ApiError) : Exception(error.message)

class ApiClient(
    private val transport: HttpTransport,
    private val tokenStorage: TokenStorage,
    private val baseUrl: String
) {
    @PublishedApi
    internal val json = ApiJson

    private val sessionExpiredFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val refreshMutex = Mutex()

    // Emite quando uma rota protegida responde 401: o token é apagado e o app volta para o login.
    val sessionExpired: SharedFlow<Unit> = sessionExpiredFlow

    suspend inline fun <reified T> get(path: String): T =
        json.decodeFromString(request("GET", path, null))

    suspend inline fun <reified B, reified T> post(path: String, body: B): T =
        json.decodeFromString(request("POST", path, json.encodeToString(body)))

    suspend inline fun <reified B, reified T> put(path: String, body: B): T =
        json.decodeFromString(request("PUT", path, json.encodeToString(body)))

    suspend fun delete(path: String, body: String? = null) {
        request("DELETE", path, body)
    }

    suspend fun request(method: String, path: String, body: String?): String =
        requestWithLatestBody(method, path) { body }

    // O corpo é montado de novo na repetição, porque o refresh pode ter trocado o que ele contém.
    suspend fun requestWithLatestBody(method: String, path: String, buildBody: () -> String?): String {
        val sentToken = tokenStorage.token()
        val response = send(method, path, buildBody(), sentToken)
        if (response.status in 200..299) return response.body
        if (response.status == 401 && canRefresh(path)) {
            val refreshed = refreshMutex.withLock {
                val latest = tokenStorage.token()
                if (sentToken != null && latest != null && latest != sentToken) {
                    true
                } else {
                    refreshSession()
                }
            }
            if (refreshed) {
                val retry = send(method, path, buildBody(), tokenStorage.token())
                if (retry.status in 200..299) return retry.body
                fail(retry, clearSession = retry.status == 401)
            }
        }
        fail(response, clearSession = response.status == 401 && sentToken != null && !isPublic(path))
    }

    private suspend fun refreshSession(): Boolean {
        val refresh = tokenStorage.refreshToken() ?: return false
        val response = send("POST", "/auth/refresh", json.encodeToString(RefreshRequest(refresh)), null)
        if (response.status !in 200..299) {
            tokenStorage.clear()
            sessionExpiredFlow.tryEmit(Unit)
            return false
        }
        tokenStorage.save(json.decodeFromString<AuthResponse>(response.body))
        return true
    }

    private fun canRefresh(path: String): Boolean = !isPublic(path) && tokenStorage.refreshToken() != null

    private fun isPublic(path: String): Boolean =
        path == "/signin" || path == "/signup" || path == "/auth/refresh" || path.startsWith("/password/")

    private suspend fun send(method: String, path: String, body: String?, token: String?): HttpResult {
        val headers = buildMap {
            put("Accept", "application/json")
            if (body != null) put("Content-Type", "application/json")
            if (token != null) put("Authorization", "Bearer $token")
        }
        return try {
            transport.send(method, baseUrl + path, headers, body)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            throw ApiException(0, ApiError("NETWORK_ERROR", "Não foi possível conectar ao servidor ($baseUrl)."))
        }
    }

    private fun fail(response: HttpResult, clearSession: Boolean): Nothing {
        val error = runCatching { json.decodeFromString<ApiError>(response.body) }
            .getOrElse { ApiError("HTTP_${response.status}", "Erro inesperado do servidor (${response.status}).") }
        if (clearSession) {
            tokenStorage.clear()
            sessionExpiredFlow.tryEmit(Unit)
        }
        throw ApiException(response.status, error)
    }
}
