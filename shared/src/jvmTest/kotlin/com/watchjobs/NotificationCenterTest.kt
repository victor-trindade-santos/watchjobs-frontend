package com.watchjobs

import com.watchjobs.data.local.InMemoryKeyValueStore
import com.watchjobs.data.local.TokenStorage
import com.watchjobs.data.remote.ApiClient
import com.watchjobs.data.remote.ApiJson
import com.watchjobs.data.remote.HttpResult
import com.watchjobs.data.remote.HttpTransport
import com.watchjobs.data.repository.NotificationRepository
import com.watchjobs.viewmodel.NotificationCenter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Menu do sininho: cinco mais recentes, marcar como lido (um e todos) e contador atualizado sem recarregar.
class NotificationCenterTest {
    private class FakeNotifications : HttpTransport {
        val calls = CopyOnWriteArrayList<String>()
        var readIds = mutableSetOf<Long>()
        override suspend fun send(method: String, url: String, headers: Map<String, String>, body: String?): HttpResult {
            val path = url.substringAfter("http://test")
            calls += "$method $path"
            return when {
                path.startsWith("/notifications?") -> {
                    val items = (1L..5L).joinToString(",") { id ->
                        val read = if (id in readIds) "\"2026-10-30T10:00:00Z\"" else "null"
                        """{"id":$id,"type":"NEW_MATCHES","title":"Aviso $id","createdAt":"2026-10-30T10:00:00Z","readAt":$read}"""
                    }
                    HttpResult(200, """{"items":[$items],"total":12}""")
                }
                path == "/notifications/unread-count" -> HttpResult(200, """{"unread":${5 - readIds.size}}""")
                path.endsWith("/read") -> {
                    readIds += path.split("/")[2].toLong()
                    HttpResult(204, "")
                }
                path == "/notifications/read-all" -> {
                    readIds += (1L..5L)
                    HttpResult(204, "")
                }
                else -> HttpResult(404, """{"code":"NOT_FOUND","message":"x"}""")
            }
        }
    }

    @Test
    fun recentMarkOneAndMarkAll() = runBlocking {
        val transport = FakeNotifications()
        val center = NotificationCenter(NotificationRepository(ApiClient(transport, TokenStorage(InMemoryKeyValueStore(), ApiJson), "http://test")), this)

        center.refresh()
        withTimeout(5_000) { while (center.state.value.recent.isEmpty()) delay(10) }
        assertEquals(5, center.state.value.recent.size)
        assertEquals(5L, center.state.value.unread)
        assertTrue(transport.calls.any { it.startsWith("GET /notifications?page=0&size=5") })

        center.markRead(2)
        assertEquals(4L, center.state.value.unread)
        assertEquals(false, center.state.value.recent.first { it.id == 2L }.unread)
        withTimeout(5_000) { while (2L !in transport.readIds) delay(10) }

        center.markRead(2)
        assertEquals(4L, center.state.value.unread)

        center.markAllRead()
        assertEquals(0L, center.state.value.unread)
        assertTrue(center.state.value.recent.none { it.unread })
        withTimeout(5_000) { while (transport.readIds.size < 5) delay(10) }
    }
}
