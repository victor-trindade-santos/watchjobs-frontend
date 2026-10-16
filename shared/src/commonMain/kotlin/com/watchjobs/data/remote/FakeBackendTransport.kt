package com.watchjobs.data.remote

import com.watchjobs.data.dto.ApiError
import com.watchjobs.data.dto.ApplicationRequest
import com.watchjobs.data.dto.AuthResponse
import com.watchjobs.data.dto.SavedJobRequest
import com.watchjobs.data.dto.SigninRequest
import com.watchjobs.data.dto.SignupRequest
import com.watchjobs.model.Application
import com.watchjobs.model.ApplicationStage
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.JobType
import com.watchjobs.model.SavedJob
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.model.User
import com.watchjobs.model.WorkMode
import com.watchjobs.util.today
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerializationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

// Backend simulado em memória, para rodar o app sem o Spring (modo demonstração).
// Atende as mesmas rotas do backend, com as mesmas validações, regras de status e mensagens de erro.
// Os dados somem ao fechar o app.
class FakeBackendTransport(seed: Boolean = true) : HttpTransport {
    private class StoredUser(val user: User, val password: String)
    private class StoredJob(val userId: Int, var job: SavedJob)
    private class StoredApplication(
        val id: Int,
        val userId: Int,
        val savedJobId: Long,
        var stageId: Int,
        var status: String,
        var startDate: LocalDate,
        var endDate: LocalDate?,
        var interviewAt: LocalDateTime?,
        var notes: String?,
        var feedback: String?
    )

    private class Fail(val result: HttpResult) : Exception()

    private val json = ApiJson
    private val mutex = Mutex()
    private val users = mutableListOf<StoredUser>()
    private val jobs = mutableListOf<StoredJob>()
    private val applications = mutableListOf<StoredApplication>()
    private val stages = listOf("Triagem", "Entrevista RH", "Teste técnico", "Entrevista técnica", "Entrevista final", "Proposta")
        .mapIndexed { index, name -> ApplicationStage(index + 1, name) }
    private var nextUserId = 1
    private var nextJobId = 1L
    private var nextApplicationId = 1

    init {
        if (seed) seedDemoData()
    }

    override suspend fun send(method: String, url: String, headers: Map<String, String>, body: String?): HttpResult =
        mutex.withLock {
            try {
                route(method, url, headers, body)
            } catch (fail: Fail) {
                fail.result
            } catch (ex: SerializationException) {
                error(400, "MALFORMED_REQUEST", "Corpo da requisição inválido")
            } catch (ex: IllegalArgumentException) {
                error(400, "MALFORMED_REQUEST", "Corpo da requisição inválido")
            }
        }

    private fun route(method: String, url: String, headers: Map<String, String>, body: String?): HttpResult {
        val pathAndQuery = "/" + url.substringAfter("://").substringAfter("/", "")
        val path = pathAndQuery.substringBefore("?").trimEnd('/')
        val query = pathAndQuery.substringAfter("?", "").split("&")
            .firstOrNull { it.startsWith("q=") }?.removePrefix("q=")?.let(::decodeQuery)
        val segments = path.split("/").filter { it.isNotEmpty() }

        when {
            method == "POST" && path == "/signup" -> return signup(decode(body))
            method == "POST" && path == "/signin" -> return signin(decode(body))
        }

        val user = authenticatedUser(headers)
            ?: return error(401, "UNAUTHORIZED", "Não autenticado")

        return when (segments.firstOrNull()) {
            "me" -> if (method == "GET" && segments.size == 1) ok(user) else notFound()
            "application-stages" -> if (method == "GET") ok(stages.filter { query.isNullOrBlank() || it.name.contains(query.trim(), true) }) else notFound()
            "saved-jobs" -> notFound()
            "applications" -> notFound()
            else -> notFound()
        }
    }

    // ---------- autenticação ----------

    private fun signup(request: SignupRequest): HttpResult {
        val errors = Checks().apply {
            check("firstName", request.firstName.isNotBlank(), "Nome é obrigatório")
            check("firstName", request.firstName.length <= 60, "Nome deve ter no máximo 60 caracteres")
            check("lastName", request.lastName.isNotBlank(), "Sobrenome é obrigatório")
            check("lastName", request.lastName.length <= 80, "Sobrenome deve ter no máximo 80 caracteres")
            check("birthDate", request.birthDate < today(), "Data de nascimento deve ser uma data passada")
            check("email", request.email.isNotBlank(), "E-mail é obrigatório")
            check("email", EMAIL.matches(request.email.trim()), "E-mail inválido")
            check("email", request.email.length <= 254, "E-mail deve ter no máximo 254 caracteres")
            check("password", request.password.isNotBlank(), "Senha é obrigatória")
            check("password", request.password.length in 8..255, "Senha deve ter entre 8 e 255 caracteres")
            check("password", PASSWORD.matches(request.password), "Senha deve conter pelo menos uma letra e um número")
            check("confirmPassword", request.confirmPassword.isNotBlank(), "Confirmação de senha é obrigatória")
        }
        errors.throwIfAny()

        val email = request.email.trim().lowercase()
        if (users.any { it.user.email == email }) throw Fail(error(409, "CONFLICT", "Email já cadastrado"))
        if (request.password != request.confirmPassword) {
            throw Fail(error(400, "BUSINESS_RULE", "A senha e a confirmação de senha não coincidem"))
        }
        val user = User(nextUserId++, request.firstName.trim(), request.lastName.trim(), email, request.birthDate)
        users += StoredUser(user, request.password)
        return ok(user, 201)
    }

    private fun signin(request: SigninRequest): HttpResult {
        Checks().apply {
            check("email", request.email.isNotBlank(), "E-mail é obrigatório")
            check("email", EMAIL.matches(request.email.trim()), "E-mail inválido")
            check("password", request.password.isNotBlank(), "Senha é obrigatória")
        }.throwIfAny()

        val stored = users.firstOrNull { it.user.email == request.email.trim().lowercase() && it.password == request.password }
            ?: return error(401, "INVALID_CREDENTIALS", "E-mail ou senha inválidos")
        val expiresAt = Clock.System.now().plus(120.minutes).toLocalDateTime(TimeZone.currentSystemDefault())
        return ok(AuthResponse("demo-token-${stored.user.id}", "Bearer", expiresAt, stored.user))
    }

    private fun authenticatedUser(headers: Map<String, String>): User? {
        val userId = headers["Authorization"]?.removePrefix("Bearer demo-token-")?.toIntOrNull() ?: return null
        return users.firstOrNull { it.user.id == userId }?.user
    }

    // ---------- utilitários ----------

    private class Checks {
        val errors = linkedMapOf<String, String?>()

        fun check(field: String, valid: Boolean, message: String) {
            if (!valid && field !in errors) errors[field] = message
        }
    }

    private fun Checks.throwIfAny() {
        if (errors.isNotEmpty()) throw Fail(error(400, "VALIDATION_ERROR", "Dados inválidos", errors))
    }

    private inline fun <reified T> decode(body: String?): T = json.decodeFromString(body ?: "")

    private inline fun <reified T> ok(value: T, status: Int = 200) = HttpResult(status, json.encodeToString(value))

    private fun error(status: Int, code: String, message: String, details: Map<String, String?>? = null) =
        HttpResult(status, json.encodeToString(ApiError(code, message, details)))

    private fun notFound() = error(404, "NOT_FOUND", "Rota não encontrada")

    private fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    private fun decodeQuery(value: String): String {
        val bytes = mutableListOf<Byte>()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '%' && i + 2 <= value.lastIndex) {
                bytes += value.substring(i + 1, i + 3).toInt(16).toByte()
                i += 3
            } else {
                bytes += (if (c == '+') ' ' else c).code.toByte()
                i++
            }
        }
        return bytes.toByteArray().decodeToString()
    }

    // Usuário de demonstração com algumas vagas e candidaturas, para o app não começar vazio.
    private fun seedDemoData() {
        val user = User(nextUserId++, "Maya", "Rendell", DEMO_EMAIL, LocalDate(1995, 5, 10))
        users += StoredUser(user, DEMO_PASSWORD)
    }

    companion object {
        const val DEMO_EMAIL = "demo@watchjobs.com"
        const val DEMO_PASSWORD = "demo1234"
        private val EMAIL = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")
        private val PASSWORD = Regex("""^(?=.*[A-Za-z])(?=.*\d).+$""")
    }
}
