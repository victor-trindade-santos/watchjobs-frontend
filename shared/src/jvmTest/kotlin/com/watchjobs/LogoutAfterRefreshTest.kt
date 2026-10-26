package com.watchjobs

import com.watchjobs.data.dto.AuthResponse
import com.watchjobs.data.dto.LogoutRequest
import com.watchjobs.data.local.InMemoryKeyValueStore
import com.watchjobs.data.local.TokenStorage
import com.watchjobs.data.remote.ApiClient
import com.watchjobs.data.remote.ApiJson
import com.watchjobs.data.remote.HttpResult
import com.watchjobs.data.remote.HttpTransport
import com.watchjobs.data.repository.AuthRepository
import com.watchjobs.model.User
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LogoutAfterRefreshTest {
    private val user = User(1, "Ana", "Lima", "ana@watchjobs.com", LocalDate(1995, 1, 1))

    private fun auth(token: String, refresh: String) =
        AuthResponse(token = token, expiresAt = LocalDateTime(2030, 1, 1, 0, 0), user = user, refreshToken = refresh)

    @Test
    fun logoutWithExpiredAccessRevokesTheRotatedRefreshToken() = runBlocking {
        val revoked = mutableListOf<String>()
        val transport = object : HttpTransport {
            override suspend fun send(method: String, url: String, headers: Map<String, String>, body: String?): HttpResult =
                when {
                    url.endsWith("/auth/refresh") ->
                        HttpResult(200, ApiJson.encodeToString(AuthResponse.serializer(), auth("access-new", "refresh-new")))
                    headers["Authorization"] != "Bearer access-new" ->
                        HttpResult(401, """{"code":"UNAUTHORIZED","message":"Não autenticado"}""")
                    else -> {
                        val refresh = ApiJson.decodeFromString(LogoutRequest.serializer(), requireNotNull(body)).refreshToken
                        if (refresh != "refresh-new") {
                            HttpResult(401, """{"code":"INVALID_CREDENTIALS","message":"Refresh token inválido ou expirado"}""")
                        } else {
                            revoked += refresh
                            HttpResult(204, "")
                        }
                    }
                }
        }
        val storage = TokenStorage(InMemoryKeyValueStore(), ApiJson)
        storage.save(auth("access-old", "refresh-old"))
        val repository = AuthRepository(ApiClient(transport, storage, "http://test"), storage)

        repository.logout()

        assertEquals(listOf("refresh-new"), revoked)
        assertNull(storage.refreshToken())
    }
}
