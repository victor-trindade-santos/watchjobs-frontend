package com.watchjobs.data.local

import com.watchjobs.data.dto.AuthResponse
import com.watchjobs.model.User
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock

// Armazenamento chave-valor nativo: SharedPreferences (Android), java.util.prefs (Desktop) e localStorage (Web).
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

expect fun createKeyValueStore(): KeyValueStore

class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()
    override fun get(key: String): String? = values[key]
    override fun put(key: String, value: String) {
        values[key] = value
    }
    override fun remove(key: String) {
        values.remove(key)
    }
}

class TokenStorage(
    private val store: KeyValueStore,
    private val json: Json
) {
    fun save(auth: AuthResponse) {
        store.put(KEY_TOKEN, auth.token)
        store.put(KEY_EXPIRES_AT, auth.expiresAt.toString())
        store.put(KEY_USER, json.encodeToString(auth.user))
        if (auth.refreshToken.isNotBlank()) store.put(KEY_REFRESH, auth.refreshToken)
    }

    // O access pode já ter vencido: ele ainda é enviado para o servidor responder 401 e o refresh rodar.
    fun token(): String? = store.get(KEY_TOKEN)

    fun refreshToken(): String? = store.get(KEY_REFRESH)

    fun user(): User? = store.get(KEY_USER)?.let { runCatching { json.decodeFromString<User>(it) }.getOrNull() }

    fun hasValidSession(): Boolean = refreshToken() != null || accessStillValid()

    fun clear() {
        store.remove(KEY_TOKEN)
        store.remove(KEY_REFRESH)
        store.remove(KEY_EXPIRES_AT)
        store.remove(KEY_USER)
    }

    private fun accessStillValid(): Boolean {
        if (store.get(KEY_TOKEN) == null) return false
        val expiresAt = store.get(KEY_EXPIRES_AT)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            ?: return false
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return expiresAt > now
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_REFRESH = "refreshToken"
        const val KEY_EXPIRES_AT = "expiresAt"
        const val KEY_USER = "user"
    }
}
