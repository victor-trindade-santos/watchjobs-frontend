package com.watchjobs.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

actual fun createHttpTransport(): HttpTransport = JavaHttpClientTransport()

actual fun platformBaseUrl(): String =
    System.getProperty("watchjobs.baseUrl") ?: com.watchjobs.BuildEnv.API_URL.ifEmpty { "http://localhost:8081" }

// Ligado por: .\gradlew.bat :desktopApp:run -Pdemo
actual fun platformDemoMode(): Boolean = System.getProperty("watchjobs.demo") == "true"

// Desktop: java.net.http.HttpClient, que já vem no Java 11 ou mais novo.
class JavaHttpClientTransport : HttpTransport {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    override suspend fun sendBytes(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: ByteArray
    ): HttpResult = withContext(Dispatchers.IO) {
        val builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60))
        headers.forEach { (name, value) -> builder.header(name, value) }
        builder.method(method, HttpRequest.BodyPublishers.ofByteArray(body))
        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        HttpResult(response.statusCode(), response.body() ?: "")
    }

    override suspend fun send(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): HttpResult = withContext(Dispatchers.IO) {
        val builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
        headers.forEach { (name, value) -> builder.header(name, value) }
        val publisher = if (body != null) {
            HttpRequest.BodyPublishers.ofString(body)
        } else {
            HttpRequest.BodyPublishers.noBody()
        }
        builder.method(method, publisher)

        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        HttpResult(response.statusCode(), response.body() ?: "")
    }
}
