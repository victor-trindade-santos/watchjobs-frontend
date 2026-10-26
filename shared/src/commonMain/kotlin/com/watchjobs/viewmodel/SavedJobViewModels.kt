package com.watchjobs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchjobs.data.dto.SavedJobRequest
import com.watchjobs.data.repository.ApplicationRepository
import com.watchjobs.data.repository.SavedJobRepository
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.JobType
import com.watchjobs.model.SavedJob
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.model.WorkMode
import com.watchjobs.util.parseMoney
import com.watchjobs.util.toInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SavedJobListState(
    val query: String = "",
    val items: List<SavedJob> = emptyList(),
    val appliedJobIds: Set<Long> = emptySet(),
    val loading: Boolean = true,
    val error: String? = null,
    val pendingDelete: SavedJob? = null
)

class SavedJobListViewModel(
    private val savedJobRepository: SavedJobRepository,
    private val applicationRepository: ApplicationRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(SavedJobListState())
    val state: StateFlow<SavedJobListState> = mutableState.asStateFlow()
    private var searchJob: Job? = null

    init {
        load()
    }

    // A busca usa o GET /saved-jobs?q= (consulta JPQL com LIKE no backend), com uma pequena espera entre as teclas.
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
        val items = savedJobRepository.list(state.value.query)
        val applied = applicationRepository.list().map { it.savedJobId }.toSet()
        mutableState.update { it.copy(items = items, appliedJobIds = applied, loading = false, error = null) }
    }

    fun requestDelete(job: SavedJob) = mutableState.update { it.copy(pendingDelete = job) }

    fun cancelDelete() = mutableState.update { it.copy(pendingDelete = null) }

    fun confirmDelete(onDeleted: (String) -> Unit) {
        val job = state.value.pendingDelete ?: return
        mutableState.update { it.copy(pendingDelete = null, loading = true) }
        viewModelScope.launchRequest(onError = ::showError) {
            savedJobRepository.delete(job.id ?: return@launchRequest)
            reload()
            onDeleted("Vaga excluída")
        }
    }

    private fun showError(ex: Throwable) = mutableState.update { it.copy(loading = false, error = ex.generalMessage()) }
}

data class SavedJobForm(
    val positionRole: String = "",
    val organizationName: String = "",
    val jobUrl: String = "",
    val locationCity: String = "",
    val locationState: String = "",
    val locationCountryCode: String = "BR",
    val workMode: WorkMode? = null,
    val experienceLevel: ExperienceLevel? = null,
    val jobType: JobType? = null,
    val salaryMin: String = "",
    val salaryMax: String = "",
    val salaryCurrency: String = "BRL",
    val status: SavedJobStatus? = SavedJobStatus.SAVED,
    val notes: String = ""
)

data class SavedJobFormState(
    val isEdit: Boolean = false,
    val form: SavedJobForm = SavedJobForm(),
    // Vaga como veio da API, para mostrar descrição, skills e demais informações (somente leitura).
    val job: SavedJob? = null,
    val hasApplication: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)

class SavedJobFormViewModel(
    private val savedJobId: Long?,
    private val savedJobRepository: SavedJobRepository,
    private val applicationRepository: ApplicationRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(SavedJobFormState(isEdit = savedJobId != null, loading = savedJobId != null))
    val state: StateFlow<SavedJobFormState> = mutableState.asStateFlow()

    init {
        if (savedJobId != null) {
            viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }) {
                val job = savedJobRepository.get(savedJobId)
                val hasApplication = applicationRepository.list().any { it.savedJobId == savedJobId }
                mutableState.update { it.copy(form = job.toForm(), job = job, hasApplication = hasApplication, loading = false) }
            }
        }
    }

    fun update(field: String, transform: (SavedJobForm) -> SavedJobForm) =
        mutableState.update { it.copy(form = transform(it.form), fieldErrors = it.fieldErrors - field) }

    // Limpar Campos: esvazia todos os campos editáveis e os erros.
    fun clear() = mutableState.update {
        it.copy(
            form = SavedJobForm(locationCountryCode = "", salaryCurrency = "", status = null),
            error = null,
            fieldErrors = emptyMap()
        )
    }

    fun save(onSaved: (String) -> Unit) {
        val form = state.value.form
        val salaryMin = parseMoney(form.salaryMin)
        val salaryMax = parseMoney(form.salaryMax)
        val localErrors = buildMap {
            if (form.positionRole.isBlank()) put("positionRole", "Cargo é obrigatório")
            if (form.organizationName.isBlank()) put("organizationName", "Empresa é obrigatória")
            if (form.status == null) put("status", "Status da vaga é obrigatório")
            if (form.salaryMin.isNotBlank() && salaryMin == null) put("salaryMin", "Valor inválido")
            if (form.salaryMax.isNotBlank() && salaryMax == null) put("salaryMax", "Valor inválido")
        }
        if (localErrors.isNotEmpty()) {
            mutableState.update { it.copy(fieldErrors = localErrors, error = "Confira os campos destacados.") }
            return
        }
        val request = SavedJobRequest(
            positionRole = form.positionRole.trim(),
            organizationName = form.organizationName.trim(),
            jobUrl = form.jobUrl.blankToNull(),
            locationCity = form.locationCity.blankToNull(),
            locationState = form.locationState.blankToNull(),
            locationCountryCode = form.locationCountryCode.blankToNull()?.uppercase(),
            workMode = form.workMode,
            experienceLevel = form.experienceLevel,
            jobType = form.jobType,
            salaryMin = salaryMin,
            salaryMax = salaryMax,
            salaryCurrency = form.salaryCurrency.blankToNull()?.uppercase(),
            status = form.status ?: SavedJobStatus.SAVED,
            notes = form.notes.blankToNull()
        )
        mutableState.update { it.copy(saving = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launchRequest(
            onError = { ex -> mutableState.update { it.copy(saving = false, error = ex.generalMessage(), fieldErrors = ex.fieldErrors()) } }
        ) {
            if (savedJobId == null) savedJobRepository.create(request) else savedJobRepository.update(savedJobId, request)
            mutableState.update { it.copy(saving = false) }
            onSaved("Vaga salva")
        }
    }
}

private fun SavedJob.toForm() = SavedJobForm(
    positionRole = positionRole,
    organizationName = organizationName,
    jobUrl = jobUrl.orEmpty(),
    locationCity = locationCity.orEmpty(),
    locationState = locationState.orEmpty(),
    locationCountryCode = locationCountryCode.orEmpty(),
    workMode = workMode,
    experienceLevel = experienceLevel,
    jobType = jobType,
    salaryMin = salaryMin?.toInput().orEmpty(),
    salaryMax = salaryMax?.toInput().orEmpty(),
    salaryCurrency = salaryCurrency.orEmpty().trim(),
    status = status,
    notes = notes.orEmpty()
)

internal fun String.blankToNull(): String? = trim().ifEmpty { null }
