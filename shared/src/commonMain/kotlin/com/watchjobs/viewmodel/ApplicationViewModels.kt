package com.watchjobs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchjobs.data.dto.ApplicationRequest
import com.watchjobs.data.repository.ApplicationRepository
import com.watchjobs.data.repository.SavedJobRepository
import com.watchjobs.model.Application
import com.watchjobs.model.ApplicationStage
import com.watchjobs.model.SavedJob
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.util.parseBrDate
import com.watchjobs.util.parseBrDateTime
import com.watchjobs.util.toBr
import com.watchjobs.util.today
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ApplicationListState(
    val query: String = "",
    val items: List<Application> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val pendingDelete: Application? = null
)

class ApplicationListViewModel(private val applicationRepository: ApplicationRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ApplicationListState())
    val state: StateFlow<ApplicationListState> = mutableState.asStateFlow()
    private var searchJob: Job? = null

    init {
        load()
    }

    // GET /applications?q= faz o LIKE em notas, feedback, cargo e empresa da vaga.
    fun onQueryChange(value: String) {
        mutableState.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launchRequest(onError = ::showError) {
            delay(300)
            reload()
        }
    }

    fun load() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launchRequest(onError = ::showError) { reload() }
    }

    private suspend fun reload() {
        val items = applicationRepository.list(state.value.query)
        mutableState.update { it.copy(items = items, loading = false, error = null) }
    }

    fun requestDelete(application: Application) = mutableState.update { it.copy(pendingDelete = application) }

    fun cancelDelete() = mutableState.update { it.copy(pendingDelete = null) }

    fun confirmDelete(onDeleted: (String) -> Unit) {
        val application = state.value.pendingDelete ?: return
        mutableState.update { it.copy(pendingDelete = null, loading = true) }
        viewModelScope.launchRequest(onError = ::showError) {
            applicationRepository.delete(application.id ?: return@launchRequest)
            reload()
            onDeleted("Candidatura excluída. A vaga voltou para Salva.")
        }
    }

    private fun showError(ex: Throwable) = mutableState.update { it.copy(loading = false, error = ex.generalMessage()) }
}

data class ApplicationForm(
    val savedJob: SavedJob? = null,
    val stage: ApplicationStage? = null,
    val status: SavedJobStatus? = SavedJobStatus.APPLIED,
    val applicationDate: String = "",
    val endDate: String = "",
    val interviewAt: String = "",
    val notes: String = "",
    val feedback: String = ""
)

data class ApplicationFormState(
    val isEdit: Boolean = false,
    val form: ApplicationForm = ApplicationForm(),
    val availableJobs: List<SavedJob> = emptyList(),
    val stages: List<ApplicationStage> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)

class ApplicationFormViewModel(
    private val applicationId: Int?,
    private val preselectedSavedJobId: Long?,
    private val applicationRepository: ApplicationRepository,
    private val savedJobRepository: SavedJobRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        ApplicationFormState(isEdit = applicationId != null, form = ApplicationForm(applicationDate = today().toBr()))
    )
    val state: StateFlow<ApplicationFormState> = mutableState.asStateFlow()

    init {
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }) {
            val stages = applicationRepository.stages()
            val jobs = savedJobRepository.list()
            val applications = applicationRepository.list()
            if (applicationId != null) {
                val application = applicationRepository.get(applicationId)
                val job = jobs.firstOrNull { it.id == application.savedJobId }
                    ?: SavedJob(id = application.savedJobId, positionRole = application.positionRole, organizationName = application.organizationName)
                mutableState.update {
                    it.copy(
                        form = application.toForm(job, stages),
                        availableJobs = listOf(job),
                        stages = stages,
                        loading = false
                    )
                }
            } else {
                // Na criação, só as vagas que ainda não têm candidatura (uma candidatura por vaga).
                val applied = applications.map { it.savedJobId }.toSet()
                val available = jobs.filter { it.id !in applied }
                mutableState.update {
                    it.copy(
                        form = it.form.copy(
                            savedJob = available.firstOrNull { job -> job.id == preselectedSavedJobId },
                            stage = stages.firstOrNull()
                        ),
                        availableJobs = available,
                        stages = stages,
                        loading = false
                    )
                }
            }
        }
    }

    fun update(field: String, transform: (ApplicationForm) -> ApplicationForm) =
        mutableState.update { it.copy(form = transform(it.form), fieldErrors = it.fieldErrors - field) }

    // Limpar Campos: na edição, a vaga continua travada.
    fun clear() = mutableState.update {
        it.copy(
            form = ApplicationForm(savedJob = if (it.isEdit) it.form.savedJob else null, status = null),
            error = null,
            fieldErrors = emptyMap()
        )
    }

    fun save(onSaved: (String) -> Unit) {
        val form = state.value.form
        val applicationDate = parseBrDate(form.applicationDate)
        val endDate = parseBrDate(form.endDate)
        val interviewAt = parseBrDateTime(form.interviewAt)
        val localErrors = buildMap {
            if (form.savedJob == null) put("savedJobId", "Vaga é obrigatória")
            if (form.stage == null) put("stageId", "Etapa da candidatura é obrigatória")
            if (form.status == null) put("status", "Status da candidatura é obrigatório")
            if (applicationDate == null) {
                put("applicationDate", if (form.applicationDate.isBlank()) "Data da candidatura é obrigatória" else "Data inválida (use dd/mm/aaaa)")
            }
            if (form.endDate.isNotBlank() && endDate == null) put("endDate", "Data inválida (use dd/mm/aaaa)")
            if (form.interviewAt.isNotBlank() && interviewAt == null) put("interviewAt", "Use dd/mm/aaaa hh:mm")
        }
        if (localErrors.isNotEmpty() || applicationDate == null) {
            mutableState.update { it.copy(fieldErrors = localErrors, error = "Confira os campos destacados.") }
            return
        }
        val request = ApplicationRequest(
            savedJobId = form.savedJob?.id ?: return,
            stageId = form.stage?.id ?: return,
            status = form.status?.name ?: return,
            applicationDate = applicationDate,
            endDate = endDate,
            interviewAt = interviewAt,
            notes = form.notes.blankToNull(),
            feedback = form.feedback.blankToNull()
        )
        mutableState.update { it.copy(saving = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launchRequest(
            onError = { ex -> mutableState.update { it.copy(saving = false, error = ex.generalMessage(), fieldErrors = ex.fieldErrors()) } }
        ) {
            if (applicationId == null) applicationRepository.create(request) else applicationRepository.update(applicationId, request)
            mutableState.update { it.copy(saving = false) }
            onSaved("Candidatura salva. O status da vaga foi atualizado.")
        }
    }

    fun requestDelete() = mutableState.update { it.copy(showDeleteConfirm = true) }

    fun cancelDelete() = mutableState.update { it.copy(showDeleteConfirm = false) }

    fun confirmDelete(onDeleted: (String) -> Unit) {
        val id = applicationId ?: return
        mutableState.update { it.copy(showDeleteConfirm = false, saving = true) }
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(saving = false, error = ex.generalMessage()) } }) {
            applicationRepository.delete(id)
            onDeleted("Candidatura excluída. A vaga voltou para Salva.")
        }
    }
}

private fun Application.toForm(job: SavedJob, stages: List<ApplicationStage>) = ApplicationForm(
    savedJob = job,
    stage = stages.firstOrNull { it.id == stageId } ?: ApplicationStage(stageId, stageName),
    status = SavedJobStatus.entries.firstOrNull { it.name == status },
    applicationDate = startDate.toBr(),
    endDate = endDate?.toBr().orEmpty(),
    interviewAt = interviewAt?.toBr().orEmpty(),
    notes = notes.orEmpty(),
    feedback = feedback.orEmpty()
)
