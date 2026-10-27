package com.watchjobs.data.remote

class HttpResult(val status: Int, val body: String)

// O código comum só conhece esta interface. Cada plataforma entrega a sua implementação nativa.
interface HttpTransport {
    suspend fun send(method: String, url: String, headers: Map<String, String>, body: String?): HttpResult

    // Corpo binário (envio multipart do ZIP do LinkedIn). Quem não suporta, avisa com uma mensagem clara.
    suspend fun sendBytes(method: String, url: String, headers: Map<String, String>, body: ByteArray): HttpResult =
        throw UnsupportedOperationException("Envio de arquivos não disponível nesta plataforma")
}

expect fun createHttpTransport(): HttpTransport

expect fun platformBaseUrl(): String

// Modo demonstração: o app usa o FakeBackendTransport (dados em memória) e não precisa do backend.
expect fun platformDemoMode(): Boolean

object AppConfig {
    // URL definida no build (-Pwatchjobs.apiUrl=...) vence a padrão de cada plataforma.
    val baseUrl: String = platformBaseUrl()
}

// Monta o corpo multipart/form-data com um único arquivo.
fun multipartBody(boundary: String, field: String, fileName: String, contentType: String, bytes: ByteArray): ByteArray {
    val safeName = fileName.replace("\"", "").replace("\r", "").replace("\n", "")
    val head = "--$boundary\r\nContent-Disposition: form-data; name=\"$field\"; filename=\"$safeName\"\r\n" +
        "Content-Type: $contentType\r\n\r\n"
    val tail = "\r\n--$boundary--\r\n"
    return head.encodeToByteArray() + bytes + tail.encodeToByteArray()
}
