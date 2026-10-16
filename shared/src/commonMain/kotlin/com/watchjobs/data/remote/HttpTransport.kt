package com.watchjobs.data.remote

class HttpResult(val status: Int, val body: String)

// O código comum só conhece esta interface. Cada plataforma entrega a sua implementação nativa.
interface HttpTransport {
    suspend fun send(method: String, url: String, headers: Map<String, String>, body: String?): HttpResult
}

expect fun createHttpTransport(): HttpTransport

expect fun platformBaseUrl(): String

// Modo demonstração: o app usa o FakeBackendTransport (dados em memória) e não precisa do backend.
expect fun platformDemoMode(): Boolean

object AppConfig {
    val baseUrl: String = platformBaseUrl()
}
