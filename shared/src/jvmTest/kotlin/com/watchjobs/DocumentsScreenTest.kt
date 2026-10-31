package com.watchjobs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.watchjobs.data.local.InMemoryKeyValueStore
import com.watchjobs.data.local.TokenStorage
import com.watchjobs.data.remote.ApiClient
import com.watchjobs.data.remote.ApiJson
import com.watchjobs.data.remote.HttpResult
import com.watchjobs.data.remote.HttpTransport
import com.watchjobs.data.repository.DocumentRepository
import com.watchjobs.data.repository.SavedJobRepository
import com.watchjobs.ui.documents.DocumentsScreen
import com.watchjobs.ui.documents.decodeBase64
import com.watchjobs.viewmodel.DocumentsViewModel
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

// F10: escolher a vaga, gerar, ver avisos, editar e salvar nova versão.
@OptIn(ExperimentalTestApi::class)
class DocumentsScreenTest {
    private class FakeDocuments : HttpTransport {
        val calls = CopyOnWriteArrayList<String>()
        var version = 0
        var content = ""

        private fun detail(): String {
            val versions = (version downTo 1).joinToString(",") { v ->
                """{"version":$v,"source":"${if (v == 1) "AI" else "EDIT"}","warnings":[],"length":10,"createdAt":"2026-01-20T10:00:00Z"}"""
            }
            val text = ApiJson.encodeToString(kotlinx.serialization.serializer<String>(), content)
            return """{"id":5,"savedJobId":1,"type":"RESUME","title":"Currículo — Backend Kotlin (Globex)","positionRole":"Backend Kotlin",
                "organizationName":"Globex","currentVersion":$version,"content":$text,
                "warnings":${if (version == 1) "[\"Tecnologias citadas que não estão no seu perfil: AWS.\"]" else "[]"},
                "versions":[$versions],"updatedAt":"2026-01-20T10:00:00Z"}"""
        }

        override suspend fun send(method: String, url: String, headers: Map<String, String>, body: String?): HttpResult {
            val path = url.substringAfter("http://test")
            calls += "$method $path"
            return when {
                path == "/documents/status" -> HttpResult(200, """{"enabled":true,"dailyLimit":20,"usedToday":$version}""")
                path.startsWith("/saved-jobs") -> HttpResult(200, """[{"id":1,"positionRole":"Backend Kotlin","organizationName":"Globex","status":"SAVED"}]""")
                method == "GET" && path == "/documents?savedJobId=1" -> HttpResult(
                    200,
                    if (version == 0) "[]" else """[{"id":5,"savedJobId":1,"type":"RESUME","title":"Currículo","positionRole":"Backend Kotlin",
                        "organizationName":"Globex","currentVersion":$version,"updatedAt":"2026-01-20T10:00:00Z"}]"""
                )
                method == "POST" && path == "/documents/generate" -> {
                    version = 1
                    content = "# Ana\n## Resumo profissional\nBackend com Kotlin."
                    HttpResult(200, detail())
                }
                method == "PUT" && path == "/documents/5" -> {
                    version += 1
                    content = ApiJson.decodeFromString(com.watchjobs.model.EditDocumentRequest.serializer(), body.orEmpty()).content
                    HttpResult(200, detail())
                }
                method == "GET" && path == "/documents/5" -> HttpResult(200, detail())
                else -> HttpResult(404, """{"code":"NOT_FOUND","message":"x"}""")
            }
        }
    }

    @Test
    fun generateReviewEditAndSaveVersion() = runDesktopComposeUiTest(width = 1280, height = 1400) {
        val transport = FakeDocuments()
        val api = ApiClient(transport, TokenStorage(InMemoryKeyValueStore(), ApiJson), "http://test")
        val vm = DocumentsViewModel(DocumentRepository(api), SavedJobRepository(api))
        val messages = mutableListOf<String>()
        setContent { DocumentsScreen(vm, onOpenProfile = {}, showMessage = { messages += it }) }
        waitUntil(timeoutMillis = 5_000) { !vm.state.value.loading }
        waitForIdle()

        onNodeWithTag("generate_RESUME").assertIsEnabled().performClick()
        waitUntil(timeoutMillis = 5_000) { vm.state.value.current != null }
        waitForIdle()
        onNodeWithTag("documentWarnings").assertExists()
        onNodeWithTag("saveDocument").assertIsNotEnabled()

        onNodeWithTag("documentEditor").performTextReplacement("# Ana\n## Resumo profissional\nBackend com Kotlin e APIs.")
        waitForIdle()
        onNodeWithTag("saveDocument").assertIsEnabled().performClick()
        waitUntil(timeoutMillis = 5_000) { vm.state.value.current?.currentVersion == 2 }
        waitForIdle()
        onNodeWithTag("version_1").assertExists()
        onNodeWithTag("version_2").assertExists()
        assertTrue(transport.calls.any { it == "POST /documents/generate" })
        assertTrue(transport.calls.any { it == "PUT /documents/5" })
        assertTrue(messages.any { "Versão 2" in it })
    }

    @Test
    fun base64DecodesExportedFiles() {
        assertContentEquals(byteArrayOf(37, 80, 68, 70), decodeBase64("JVBERg=="))
        assertTrue(decodeBase64("%%%") == null)
    }
}
