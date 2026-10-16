package com.watchjobs.data.remote

import com.watchjobs.data.dto.ApiError
import com.watchjobs.data.local.TokenStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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

    // Emite quando uma rota protegida responde 401: o token é apagado e o app volta para o login.
    val sessionExpired: SharedFlow<Unit> = sessionExpiredFlow

    suspend inline fun <reified T> get(path: String): T =
        json.decodeFromString(request("GET", path, null))

    suspend inline fun <reified B, reified T> post(path: String, body: B): T =
        json.decodeFromString(request("POST", path, json.encodeToString(body)))

    suspend inline fun <reified B, reified T> put(path: String, body: B): T =
        json.decodeFromString(request("PUT", path, json.encodeToString(body)))

    suspend fun delete(path: String) {
        request("DELETE", path, null)
    }

    suspend fun request(method: String, path: String, body: String?): String {
        val token = tokenStorage.token()
        val headers = buildMap {
            put("Accept", "application/json")
            if (body != null) put("Content-Type", "application/json")
            if (token != null) put("Authorization", "Bearer $token")
        }

        val response = try {
            transport.send(method, baseUrl + path, headers, body)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            throw ApiException(0, ApiError("NETWORK_ERROR", "Não foi possível conectar ao servidor ($baseUrl)."))
        }

        if (response.status in 200..299) return response.body

        val error = runCatching { json.decodeFromString<ApiError>(response.body) }
            .getOrElse { ApiError("HTTP_${response.status}", "Erro inesperado do servidor (${response.status}).") }

        if (response.status == 401 && token != null) {
            tokenStorage.clear()
            sessionExpiredFlow.tryEmit(Unit)
        }
        throw ApiException(response.status, error)
    }
}
