package com.watchjobs

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.watchjobs.data.dto.ApplicationRequest
import com.watchjobs.data.dto.SavedJobRequest
import com.watchjobs.data.dto.SigninRequest
import com.watchjobs.data.dto.SignupRequest
import com.watchjobs.data.local.InMemoryKeyValueStore
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.JobType
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.model.WorkMode
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

// Teste integrado: abre o app de verdade (Compose Desktop) contra o backend rodando em localhost:8081.
// Se o backend estiver fora do ar, o teste é ignorado. As capturas de tela ficam em shared/build/screenshots.
@OptIn(ExperimentalTestApi::class)
class AppIntegrationTest {
    private val baseUrl = System.getProperty("watchjobs.baseUrl") ?: "http://localhost:8081"
    private val screenshots = File("build/screenshots").apply { mkdirs() }
    private var shotPrefix = ""

    private fun backendIsUp(): Boolean = runCatching {
        val connection = URI("$baseUrl/me").toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 2_000
        connection.responseCode
    }.isSuccess

    private fun ComposeUiTest.shot(name: String) {
        waitForIdle()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(screenshots, "$shotPrefix$name.png"))
    }

    private fun ComposeUiTest.waitForText(text: String, timeoutMillis: Long = 15_000) = waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun ComposeUiTest.waitForNoText(text: String, timeoutMillis: Long = 15_000) = waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
    }

    private fun ComposeUiTest.waitForTag(tag: String) = waitUntil(timeoutMillis = 15_000) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }

    private fun ComposeUiTest.fill(tag: String, text: String) {
        waitForTag(tag)
        onNodeWithTag(tag).performTextClearance()
        onNodeWithTag(tag).performTextInput(text)
    }

    private fun ComposeUiTest.choose(tag: String, option: String) {
        waitForTag(tag)
        onNodeWithTag(tag).performClick()
        waitForText(option)
        onAllNodesWithText(option, useUnmergedTree = true).onFirst().performClick()
        waitForIdle()
    }

    // Se um passo falhar, guarda a tela daquele momento para facilitar o diagnóstico.
    private fun ComposeUiTest.withFailureShot(name: String, block: ComposeUiTest.() -> Unit) {
        try {
            block()
        } catch (failure: Throwable) {
            runCatching { shot(name) }
            throw failure
        }
    }

    @Test
    fun fluxoCompletoContraOBackend() {
        if (!backendIsUp()) {
            println("Backend fora do ar em $baseUrl: teste integrado ignorado.")
            return
        }
        fullFlow(AppContainer(demoMode = false, store = InMemoryKeyValueStore(), baseUrl = baseUrl))
    }

    // O mesmo fluxo, sem backend: o app usa o backend simulado do modo demonstração.
    @Test
    fun fluxoCompletoEmModoDemonstracao() {
        shotPrefix = "demo-"
        fullFlow(AppContainer(demoMode = true))
    }

    private fun fullFlow(container: AppContainer) {
        val email = "maya${System.currentTimeMillis()}@watchjobs.com"
        runDesktopComposeUiTest(width = 1280, height = 860) {
            withFailureShot("ERRO-fluxo-completo") {
                setContent { App(container) }
                waitForText("Bem-vindo de volta.")
                shot("01-login")

                // Cadastro: primeiro a validação local da data, depois a validação do backend (400 com details).
                onNodeWithTag("goToSignup").performClick()
                waitForText("Comece a acompanhar suas vagas.")
                onNodeWithTag("signupButton").performClick()
                waitForText("Data de nascimento é obrigatória")
                fill("signupBirthDate", "10/05/1995")
                onNodeWithTag("signupButton").performClick()
                waitForText("Nome é obrigatório")
                waitForText("Confira os campos destacados.")
                shot("02-cadastro-erros-do-backend")

                fill("signupFirstName", "Maya")
                fill("signupLastName", "Rendell")
                fill("signupEmail", email)
                fill("signupPassword", "senha1234")
                fill("signupConfirmPassword", "senha1234")
                shot("03-cadastro-preenchido")
                onNodeWithTag("signupButton").performClick()

                // Volta ao login com o e-mail preenchido.
                waitForText("Conta criada")
                onNodeWithTag("loginPassword").performTextInput("errada123")
                onNodeWithTag("loginButton").performClick()
                waitForText("E-mail ou senha inválidos")
                fill("loginPassword", "senha1234")
                onNodeWithTag("loginButton").performClick()
                waitForText("Nenhuma vaga salva ainda")
                shot("04-vagas-vazia")

                // Nova vaga pelo formulário: erros locais, erro do backend e gravação.
                onNodeWithTag("newSavedJob").performClick()
                waitForText("Registre uma vaga")
                onNodeWithTag("saveButton").performScrollTo().performClick()
                waitForText("Cargo é obrigatório")
                fill("jobRole", "Senior Product Designer, Platform")
                fill("jobCompany", "Northlight Systems")
                fill("jobCity", "São Paulo")
                fill("jobState", "SP")
                fill("jobCountry", "B")
                choose("jobWorkMode", "Híbrido")
                choose("jobLevel", "Sênior")
                choose("jobType", "CLT")
                fill("jobSalaryMin", "15.000")
                fill("jobSalaryMax", "22.000")
                fill("jobNotes", "Indicação da Priya. Perguntar sobre design ops.")
                onNodeWithTag("saveButton").performScrollTo().performClick()
                waitForText("País deve ter 2 letras maiúsculas")
                shot("05-vaga-erro-do-backend")
                fill("jobCountry", "BR")
                onNodeWithTag("saveButton").performScrollTo().performClick()
                waitForText("Vaga salva")
                waitForText("Senior Product Designer, Platform")

                // Mais vagas e candidaturas pela API, para encher a lista e o quadro.
                val ids = runBlocking {
                    val repo = container.savedJobRepository
                    val ferrolane = repo.create(SavedJobRequest("Staff Product Designer, Payments", "Ferrolane", locationCity = "Amsterdam", workMode = WorkMode.REMOTE, experienceLevel = ExperienceLevel.LEAD, jobType = JobType.PJ, salaryMin = 105000.0, salaryMax = 125000.0, salaryCurrency = "EUR", status = SavedJobStatus.SAVED))
                    val halden = repo.create(SavedJobRequest("Design Engineer", "Halden Labs", locationCity = "Copenhagen", workMode = WorkMode.ONSITE, experienceLevel = ExperienceLevel.MID, jobType = JobType.CLT, status = SavedJobStatus.SAVED))
                    val trailhead = repo.create(SavedJobRequest("Senior UX Researcher", "Trailhead Grid", workMode = WorkMode.HYBRID, status = SavedJobStatus.SAVED))
                    repo.create(SavedJobRequest("Head of Design", "Ostrom Analytics", locationCity = "Curitiba", locationState = "PR", workMode = WorkMode.REMOTE, salaryMin = 30000.0, status = SavedJobStatus.PLANNING_TO_APPLY))
                    val apps = container.applicationRepository
                    apps.create(ApplicationRequest(ferrolane.id!!, 4, "IN_PROCESS", LocalDate(2026, 10, 10), interviewAt = LocalDateTime(2026, 10, 19, 14, 0), notes = "Portfolio review"))
                    apps.create(ApplicationRequest(halden.id!!, 3, "APPLIED", LocalDate(2026, 10, 12), interviewAt = LocalDateTime(2026, 10, 23, 17, 0)))
                    apps.create(ApplicationRequest(trailhead.id!!, 5, "REJECTED", LocalDate(2026, 9, 20), endDate = LocalDate(2026, 10, 5), feedback = "Vaga preenchida"))
                    listOf(ferrolane.id, halden.id)
                }

                onNodeWithTag("tab_APPLICATIONS").performClick()
                waitForText("Staff Product Designer, Payments")
                onNodeWithTag("tab_SAVED_JOBS").performClick()
                waitForText("Head of Design")
                shot("06-vagas-lista")

                // Busca com LIKE no backend.
                onNodeWithTag("searchField").performTextInput("northlight")
                waitUntil(timeoutMillis = 15_000) { onAllNodesWithTag("savedJobRow").fetchSemanticsNodes().size == 1 }
                shot("07-vagas-busca")
                onNodeWithTag("searchField").performTextClearance()
                waitForText("5 vagas")

                // Nova candidatura pelo formulário.
                onNodeWithTag("tab_APPLICATIONS").performClick()
                waitForText("Staff Product Designer, Payments")
                onNodeWithTag("newApplication").performClick()
                waitForText("Só aparecem as vagas")
                choose("appJob", "Senior Product Designer, Platform — Northlight Systems")
                choose("appStage", "Entrevista RH")
                fill("appInterview", "22/10/2026 09:30")
                fill("appNotes", "Recrutadora: Ana. Levar cases de design system.")
                shot("08-candidatura-form")
                onNodeWithTag("saveButton").performScrollTo().performClick()
                waitForText("Candidatura salva")
                waitForText("Senior Product Designer, Platform")
                shot("09-candidaturas-quadro")

                // Editar: muda o status para Em processo, e a vaga acompanha.
                onNodeWithText("Senior Product Designer, Platform").performClick()
                waitForText("Editar candidatura")
                choose("appStatus", "Em processo")
                onNodeWithTag("saveButton").performScrollTo().performClick()
                waitForNoText("Editar candidatura")
                val northlight = runBlocking { container.savedJobRepository.list("northlight").single() }
                assertEquals(SavedJobStatus.IN_PROCESS, northlight.status)

                // Excluir a candidatura pelo quadro: a vaga volta para Salva.
                // O primeiro cartão do quadro é o da Halden (coluna Candidatadas).
                onAllNodesWithTag("deleteApplication").onFirst().performClick()
                waitForText("Excluir candidatura?")
                onNodeWithTag("confirmButton").performClick()
                waitForText("Candidatura excluída")
                val haldenStatus = runBlocking { container.savedJobRepository.get(ids[1]).status }
                assertEquals(SavedJobStatus.SAVED, haldenStatus)
                shot("10-candidaturas-apos-excluir")

                // Limpar Campos no formulário de vaga.
                onNodeWithTag("tab_SAVED_JOBS").performClick()
                waitForText("Head of Design")
                onNodeWithText("Head of Design").performClick()
                waitForText("Editar vaga")
                waitForTag("jobRole")
                shot("11-vaga-edicao")
                onNodeWithTag("clearButton").performScrollTo().performClick()
                waitForNoText("Ostrom Analytics")
                onNodeWithTag("saveButton").performScrollTo().performClick()
                waitForText("Status da vaga é obrigatório")
                onNodeWithTag("backLink").performClick()

                // Excluir a vaga da Ferrolane, que tem candidatura em processo: a candidatura vai junto (cascata).
                // Ordem da lista (mais nova primeiro): Ostrom, Trailhead, Halden, Ferrolane, Northlight.
                waitForText("Head of Design")
                val applicationsBefore = runBlocking { container.applicationRepository.list().size }
                onAllNodesWithTag("deleteSavedJob")[3].performClick()
                waitForText("junto com a candidatura")
                onNodeWithTag("confirmButton").performClick()
                waitForText("Vaga excluída")
                val (jobsLeft, applicationsLeft) = runBlocking {
                    container.savedJobRepository.list().size to container.applicationRepository.list().size
                }
                assertEquals(4, jobsLeft)
                assertEquals(applicationsBefore - 1, applicationsLeft)
                shot("12-vagas-apos-excluir")

                // Sair.
                onNodeWithTag("logoutButton").performClick()
                waitForText("Bem-vindo de volta.")
            }
        }
    }

    @Test
    fun telasEmLarguraDeCelular() {
        if (!backendIsUp()) {
            println("Backend fora do ar em $baseUrl: teste integrado ignorado.")
            return
        }
        val email = "celular${System.currentTimeMillis()}@watchjobs.com"
        val container = AppContainer(demoMode = false, store = InMemoryKeyValueStore(), baseUrl = baseUrl)
        runDesktopComposeUiTest(width = 412, height = 900) {
            withFailureShot("ERRO-celular") {
                setContent { App(container) }
                waitForText("Bem-vindo de volta.")
                shot("20-celular-login")
                onNodeWithTag("goToSignup").performClick()
                waitForText("Comece a acompanhar suas vagas.")
                shot("21-celular-cadastro")
            }
        }
        runBlocking {
            container.authRepository.signup(SignupRequest("Leo", "Costa", LocalDate(1998, 3, 2), email, "senha1234", "senha1234"))
            container.authRepository.signin(SigninRequest(email, "senha1234"))
            val job = container.savedJobRepository.create(SavedJobRequest("Desenvolvedor Kotlin", "ACME Tecnologia", locationCity = "Recife", locationState = "PE", workMode = WorkMode.REMOTE, experienceLevel = ExperienceLevel.MID, jobType = JobType.CLT, salaryMin = 9000.0, salaryMax = 13000.0, salaryCurrency = "BRL", status = SavedJobStatus.SAVED))
            container.applicationRepository.create(ApplicationRequest(job.id!!, 2, "IN_PROCESS", LocalDate(2026, 10, 14), interviewAt = LocalDateTime(2026, 10, 21, 10, 0)))
        }
        runDesktopComposeUiTest(width = 412, height = 900) {
            withFailureShot("ERRO-celular") {
                setContent { App(container) }
                waitForText("Desenvolvedor Kotlin")
                shot("22-celular-vagas")
                onNodeWithTag("tab_APPLICATIONS").performClick()
                waitForText("Toque em um cartão")
                waitForText("Desenvolvedor Kotlin")
                shot("23-celular-candidaturas")
                onNodeWithText("Desenvolvedor Kotlin").performClick()
                waitForText("Editar candidatura")
                waitForTag("appStatus")
                shot("24-celular-candidatura-form")
            }
        }
    }
}
