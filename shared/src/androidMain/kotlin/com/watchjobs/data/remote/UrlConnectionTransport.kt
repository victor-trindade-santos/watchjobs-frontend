package com.watchjobs.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

actual fun createHttpTransport(): HttpTransport = UrlConnectionTransport()

// 10.0.2.2 é o "localhost do PC" visto de dentro do emulador.
// No celular de verdade, troque pelo IP do PC na mesma rede Wi-Fi (ou gere o APK com -Pwatchjobs.apiUrl=...).
actual fun platformBaseUrl(): String = com.watchjobs.BuildEnv.API_URL.ifEmpty { "http://10.0.2.2:8081" }

// No Android o app sempre usa o backend; o modo demonstração é para Desktop e Web.
actual fun platformDemoMode(): Boolean = false

// Android: HttpURLConnection.
class UrlConnectionTransport : HttpTransport {
    override suspend fun sendBytes(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: ByteArray
    ): HttpResult = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 60_000
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            HttpResult(status, stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "")
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun send(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): HttpResult = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.encodeToByteArray()) }
            }

            val status = connection.responseCode
            // Para status 400 ou mais, o corpo vem no errorStream.
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            HttpResult(status, text)
        } finally {
            connection.disconnect()
        }
    }
}
