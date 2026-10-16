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
            "saved-jobs" -> savedJobs(method, segments.getOrNull(1), user.id, query, body)
            "applications" -> applicationsRoute(method, segments.getOrNull(1), user.id, query, body)
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

    // ---------- vagas salvas ----------

    private fun savedJobs(method: String, idText: String?, userId: Int, query: String?, body: String?): HttpResult {
        if (idText == null) {
            return when (method) {
                "GET" -> ok(
                    jobs.filter { it.userId == userId }
                        .map { it.job }
                        .filter { query.isNullOrBlank() || it.positionRole.contains(query.trim(), true) || it.organizationName.contains(query.trim(), true) }
                        .sortedByDescending { it.id }
                )
                "POST" -> createJob(userId, decode(body))
                else -> notFound()
            }
        }
        val id = idText.toLongOrNull() ?: return error(400, "INVALID_PARAMETER", "Parâmetro inválido: id")
        val stored = jobs.firstOrNull { it.userId == userId && it.job.id == id }
            ?: return error(404, "NOT_FOUND", "Vaga não encontrada")
        return when (method) {
            "GET" -> ok(stored.job)
            "PUT" -> updateJob(stored, decode(body))
            "DELETE" -> {
                // A candidatura vai junto, como o ON DELETE CASCADE da V2.
                jobs.remove(stored)
                applications.removeAll { it.savedJobId == id }
                HttpResult(204, "")
            }
            else -> notFound()
        }
    }

    private fun validateJob(request: SavedJobRequest) {
        Checks().apply {
            check("positionRole", request.positionRole.isNotBlank(), "Cargo é obrigatório")
            check("positionRole", request.positionRole.length <= 255, "Cargo deve ter no máximo 255 caracteres")
            check("organizationName", request.organizationName.isNotBlank(), "Empresa é obrigatória")
            check("organizationName", request.organizationName.length <= 150, "Empresa deve ter no máximo 150 caracteres")
            check("jobUrl", (request.jobUrl?.length ?: 0) <= 2048, "URL da vaga deve ter no máximo 2048 caracteres")
            check("locationCity", (request.locationCity?.length ?: 0) <= 100, "Cidade deve ter no máximo 100 caracteres")
            check("locationState", (request.locationState?.length ?: 0) <= 100, "Estado deve ter no máximo 100 caracteres")
            check("locationCountryCode", request.locationCountryCode?.let { Regex("^[A-Z]{2}$").matches(it) } ?: true, "País deve ter 2 letras maiúsculas")
            check("salaryMin", (request.salaryMin ?: 0.0) >= 0, "Salário mínimo não pode ser negativo")
            check("salaryMax", (request.salaryMax ?: 0.0) >= 0, "Salário máximo não pode ser negativo")
            check("salaryCurrency", request.salaryCurrency?.let { Regex("^[A-Z]{3}$").matches(it) } ?: true, "Moeda deve ter 3 letras maiúsculas")
            check("notes", (request.notes?.length ?: 0) <= 4000, "Notas devem ter no máximo 4000 caracteres")
        }.throwIfAny()
        val min = request.salaryMin
        val max = request.salaryMax
        if (min != null && max != null && min > max) {
            throw Fail(error(400, "BUSINESS_RULE", "Salário mínimo não pode ser maior que o salário máximo"))
        }
    }

    private fun createJob(userId: Int, request: SavedJobRequest): HttpResult {
        validateJob(request)
        val now = now()
        val job = request.toJob(nextJobId++, now, now)
        jobs += StoredJob(userId, job)
        return ok(job, 201)
    }

    private fun updateJob(stored: StoredJob, request: SavedJobRequest): HttpResult {
        validateJob(request)
        val current = stored.job
        val application = applications.firstOrNull { it.savedJobId == current.id }
        if (application != null && request.status in setOf(SavedJobStatus.SAVED, SavedJobStatus.PLANNING_TO_APPLY)) {
            throw Fail(error(409, "CONFLICT", "Exclua a candidatura para voltar a vaga para Salva ou Planejando se candidatar"))
        }
        val statusChanged = current.status != request.status
        stored.job = request.toJob(
            id = current.id ?: 0,
            savedAt = current.savedAt ?: now(),
            statusUpdatedAt = if (statusChanged) now() else current.statusUpdatedAt ?: now()
        )
        if (statusChanged) application?.status = request.status.name
        return ok(stored.job)
    }

    private fun SavedJobRequest.toJob(id: Long, savedAt: LocalDateTime, statusUpdatedAt: LocalDateTime) = SavedJob(
        id = id,
        positionRole = positionRole.trim(),
        organizationName = organizationName.trim(),
        jobUrl = jobUrl,
        locationCity = locationCity,
        locationState = locationState,
        locationCountryCode = locationCountryCode,
        workMode = workMode,
        experienceLevel = experienceLevel,
        jobType = jobType,
        salaryMin = salaryMin,
        salaryMax = salaryMax,
        salaryCurrency = salaryCurrency,
        status = status,
        notes = notes,
        savedAt = savedAt,
        statusUpdatedAt = statusUpdatedAt
    )

    // ---------- candidaturas ----------

    private fun applicationsRoute(method: String, idText: String?, userId: Int, query: String?, body: String?): HttpResult {
        if (idText == null) {
            return when (method) {
                "GET" -> ok(
                    applications.filter { it.userId == userId }
                        .map { it.toResponse() }
                        .filter {
                            val q = query?.trim()
                            q.isNullOrEmpty() || listOf(it.notes, it.feedback, it.positionRole, it.organizationName)
                                .any { field -> field?.contains(q, true) == true }
                        }
                        .sortedWith(compareByDescending<Application> { it.startDate }.thenByDescending { it.id })
                )
                "POST" -> createApplication(userId, decode(body))
                else -> notFound()
            }
        }
        val id = idText.toIntOrNull() ?: return error(400, "INVALID_PARAMETER", "Parâmetro inválido: id")
        val stored = applications.firstOrNull { it.userId == userId && it.id == id }
            ?: return error(404, "NOT_FOUND", "Candidatura não encontrada")
        return when (method) {
            "GET" -> ok(stored.toResponse())
            "PUT" -> updateApplication(stored, decode(body))
            "DELETE" -> {
                // A vaga volta para SAVED quando a candidatura é excluída.
                applications.remove(stored)
                jobs.firstOrNull { it.job.id == stored.savedJobId }?.let {
                    it.job = it.job.copy(status = SavedJobStatus.SAVED, statusUpdatedAt = now())
                }
                HttpResult(204, "")
            }
            else -> notFound()
        }
    }

    private fun validateApplication(request: ApplicationRequest) {
        Checks().apply {
            check("savedJobId", request.savedJobId > 0, "Vaga inválida")
            check("stageId", request.stageId > 0, "Etapa da candidatura inválida")
            check("status", request.status.isNotBlank(), "Status da candidatura é obrigatório")
            check("status", request.status in SavedJobStatus.applicationStatuses.map { it.name }, "Status da candidatura inválido")
            check("applicationDate", request.applicationDate <= today(), "Data da candidatura não pode ser futura")
            check("notes", (request.notes?.length ?: 0) <= 4000, "Notas devem ter no máximo 4000 caracteres")
            check("feedback", (request.feedback?.length ?: 0) <= 4000, "Feedback deve ter no máximo 4000 caracteres")
        }.throwIfAny()
        val endDate = request.endDate
        if (endDate != null && endDate < request.applicationDate) {
            throw Fail(error(400, "BUSINESS_RULE", "Data de término não pode ser anterior à data da candidatura"))
        }
    }

    private fun createApplication(userId: Int, request: ApplicationRequest): HttpResult {
        validateApplication(request)
        val job = jobs.firstOrNull { it.userId == userId && it.job.id == request.savedJobId }
            ?: throw Fail(error(404, "NOT_FOUND", "Vaga não encontrada"))
        if (applications.any { it.savedJobId == request.savedJobId }) {
            throw Fail(error(409, "CONFLICT", "Esta vaga já possui uma candidatura"))
        }
        findStage(request.stageId)
        syncJobStatus(job, request.status)
        val stored = StoredApplication(
            nextApplicationId++, userId, request.savedJobId, request.stageId, request.status,
            request.applicationDate, request.endDate, request.interviewAt, request.notes, request.feedback
        )
        applications += stored
        return ok(stored.toResponse(), 201)
    }

    private fun updateApplication(stored: StoredApplication, request: ApplicationRequest): HttpResult {
        validateApplication(request)
        if (stored.savedJobId != request.savedJobId) {
            throw Fail(error(400, "BUSINESS_RULE", "Não é possível trocar a vaga de uma candidatura"))
        }
        findStage(request.stageId)
        stored.stageId = request.stageId
        stored.status = request.status
        stored.startDate = request.applicationDate
        stored.endDate = request.endDate
        stored.interviewAt = request.interviewAt
        stored.notes = request.notes
        stored.feedback = request.feedback
        jobs.firstOrNull { it.job.id == stored.savedJobId }?.let { syncJobStatus(it, request.status) }
        return ok(stored.toResponse())
    }

    private fun findStage(id: Int): ApplicationStage =
        stages.firstOrNull { it.id == id } ?: throw Fail(error(404, "NOT_FOUND", "Etapa da candidatura não encontrada"))

    // A vaga é a fonte da verdade do status; a candidatura recebe uma cópia.
    private fun syncJobStatus(stored: StoredJob, status: String) {
        val newStatus = SavedJobStatus.valueOf(status)
        if (stored.job.status != newStatus) {
            stored.job = stored.job.copy(status = newStatus, statusUpdatedAt = now())
        }
    }

    private fun StoredApplication.toResponse(): Application {
        val job = jobs.first { it.job.id == savedJobId }.job
        val stage = stages.first { it.id == stageId }
        return Application(
            id = id,
            savedJobId = savedJobId,
            positionRole = job.positionRole,
            organizationName = job.organizationName,
            stageId = stageId,
            stageName = stage.name,
            status = status,
            startDate = startDate,
            endDate = endDate,
            interviewAt = interviewAt,
            notes = notes,
            feedback = feedback
        )
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
        val today = today()
        fun job(role: String, company: String, city: String?, state: String?, mode: WorkMode, level: ExperienceLevel?, type: JobType?, min: Double?, max: Double?, currency: String?, status: SavedJobStatus): Long {
            val request = SavedJobRequest(role, company, null, city, state, if (state != null) "BR" else null, mode, level, type, min, max, currency, status, null)
            val id = nextJobId++
            jobs += StoredJob(user.id, request.toJob(id, now(), now()))
            return id
        }
        fun application(jobId: Long, stage: Int, status: SavedJobStatus, daysAgo: Int, interviewInDays: Int?, hour: Int, notes: String?, feedback: String? = null) {
            val interview = interviewInDays?.let { LocalDateTime(today.plus(it, DateTimeUnit.DAY), kotlinx.datetime.LocalTime(hour, if (hour == 9) 30 else 0)) }
            applications += StoredApplication(
                nextApplicationId++, user.id, jobId, stage, status.name, today.minus(daysAgo, DateTimeUnit.DAY),
                if (status == SavedJobStatus.REJECTED) today.minus(2, DateTimeUnit.DAY) else null, interview, notes, feedback
            )
        }
        val northlight = job("Senior Product Designer, Platform", "Northlight Systems", "São Paulo", "SP", WorkMode.HYBRID, ExperienceLevel.SENIOR, JobType.CLT, 15000.0, 22000.0, "BRL", SavedJobStatus.IN_PROCESS)
        val ferrolane = job("Staff Product Designer, Payments", "Ferrolane", "Amsterdam", null, WorkMode.REMOTE, ExperienceLevel.LEAD, JobType.PJ, 105000.0, 125000.0, "EUR", SavedJobStatus.OFFER_RECEIVED)
        val halden = job("Design Engineer", "Halden Labs", "Copenhagen", null, WorkMode.ONSITE, ExperienceLevel.MID, JobType.CLT, null, null, null, SavedJobStatus.APPLIED)
        val trailhead = job("Senior UX Researcher", "Trailhead Grid", null, null, WorkMode.HYBRID, ExperienceLevel.SENIOR, null, null, null, null, SavedJobStatus.REJECTED)
        job("Head of Design", "Ostrom Analytics", "Curitiba", "PR", WorkMode.REMOTE, ExperienceLevel.LEAD, null, 30000.0, null, "BRL", SavedJobStatus.PLANNING_TO_APPLY)
        job("Product Manager, Marketplace", "Meridian Freight", "Recife", "PE", WorkMode.REMOTE, ExperienceLevel.MID, JobType.CLT, 12000.0, 16000.0, "BRL", SavedJobStatus.SAVED)
        application(northlight, 2, SavedJobStatus.IN_PROCESS, 6, 3, 9, "Recrutadora: Ana. Levar cases de design system.")
        application(ferrolane, 6, SavedJobStatus.OFFER_RECEIVED, 20, 2, 14, "Portfolio review aprovado.")
        application(halden, 3, SavedJobStatus.APPLIED, 4, 5, 17, "Take-home enviado.")
        application(trailhead, 5, SavedJobStatus.REJECTED, 30, null, 0, null, "Vaga preenchida internamente.")
    }

    companion object {
        const val DEMO_EMAIL = "demo@watchjobs.com"
        const val DEMO_PASSWORD = "demo1234"
        private val EMAIL = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")
        private val PASSWORD = Regex("""^(?=.*[A-Za-z])(?=.*\d).+$""")
    }
}
