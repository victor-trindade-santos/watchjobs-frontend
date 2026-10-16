package com.watchjobs.data.remote

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response

actual fun createHttpTransport(): HttpTransport = FetchTransport()

// Padrão: http://localhost:8081. Outro backend pode ser indicado na URL: http://localhost:8080/?api=http://localhost:8082
actual fun platformBaseUrl(): String = queryParam("api")?.ifBlank { null } ?: "http://localhost:8081"

// Ligado pela URL: http://localhost:8080/?demo
actual fun platformDemoMode(): Boolean = queryParam("demo").let { it != null && it != "false" }

private fun queryParam(name: String): String? =
    window.location.search.removePrefix("?").split("&")
        .firstOrNull { it == name || it.startsWith("$name=") }
        ?.substringAfter("=", "")

// Web: window.fetch() do navegador.
@OptIn(ExperimentalWasmJsInterop::class)
class FetchTransport : HttpTransport {
    override suspend fun send(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): HttpResult {
        val requestHeaders = Headers()
        headers.forEach { (name, value) -> requestHeaders.append(name, value) }

        val init = requestInit(method, requestHeaders, body?.toJsString())
        val response = window.fetch(url, init).await<Response>()
        val text = response.text().await<JsString>().toString()
        return HttpResult(response.status.toInt(), text)
    }
}

// O RequestInit() do kotlinx-browser preenche os campos não usados com null (o navegador recusa "cache: null").
// Por isso o objeto é montado direto em JavaScript, só com os três campos usados.
@OptIn(ExperimentalWasmJsInterop::class)
private fun requestInit(method: String, headers: Headers, body: JsString?): RequestInit =
    js("({ method: method, headers: headers, body: body })")
