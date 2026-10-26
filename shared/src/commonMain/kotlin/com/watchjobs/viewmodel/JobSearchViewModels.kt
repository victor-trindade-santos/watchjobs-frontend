package com.watchjobs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchjobs.data.remote.ApiException
import com.watchjobs.data.repository.JobSearchQuery
import com.watchjobs.data.repository.JobSearchRepository
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.JobDetail
import com.watchjobs.model.JobPosting
import com.watchjobs.model.JobType
import com.watchjobs.model.WorkMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class JobSearchState(
    val query: JobSearchQuery = JobSearchQuery(),
    val items: List<JobPosting> = emptyList(),
    val total: Long = 0,
    val page: Int = 0,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val savingIds: Set<Long> = emptySet()
) {
    val hasMore: Boolean
        get() = items.size < total
}

class JobSearchViewModel(
    private val repository: JobSearchRepository,
    initialQuery: JobSearchQuery = JobSearchQuery(),
    private val onQueryChanged: (JobSearchQuery) -> Unit = {}
) : ViewModel() {
    private val mutableState = MutableStateFlow(JobSearchState(query = initialQuery))
    val state: StateFlow<JobSearchState> = mutableState.asStateFlow()
    private var searchJob: Job? = null

    init {
        search()
    }

    // Texto e local esperam uma pausa na digitação; os filtros de lista buscam na hora.
    fun onTextChange(value: String) = updateQuery(debounce = true) { it.copy(text = value) }

    fun onLocationChange(value: String) = updateQuery(debounce = true) { it.copy(location = value) }

    fun onWorkModeChange(value: WorkMode?) = updateQuery { it.copy(workMode = value) }

    fun onLevelChange(value: ExperienceLevel?) = updateQuery { it.copy(level = value) }

    fun onJobTypeChange(value: JobType?) = updateQuery { it.copy(jobType = value) }

    // Mantém o texto da busca e limpa só os filtros da lateral.
    fun resetFilters() = updateQuery { JobSearchQuery(text = it.text) }

    private fun updateQuery(debounce: Boolean = false, change: (JobSearchQuery) -> JobSearchQuery) {
        mutableState.update { it.copy(query = change(it.query)) }
        onQueryChanged(state.value.query)
        search(debounce)
    }

    fun search(debounce: Boolean = false) {
        searchJob?.cancel()
        mutableState.update { it.copy(loading = true, loadingMore = false, error = null) }
        searchJob = viewModelScope.launchRequest(onError = ::showError) {
            if (debounce) delay(400)
            val page = repository.search(state.value.query, page = 0)
            mutableState.update { it.copy(items = page.items, total = page.total, page = 0, loading = false) }
        }
    }

    fun loadMore() {
        val current = state.value
        if (!current.hasMore || current.loadingMore || current.loading) return
        mutableState.update { it.copy(loadingMore = true) }
        searchJob = viewModelScope.launchRequest(onError = ::showError) {
            val next = repository.search(current.query, page = current.page + 1)
            mutableState.update { state ->
                val known = state.items.map { it.id }.toSet()
                state.copy(
                    items = state.items + next.items.filter { it.id !in known },
                    total = next.total,
                    page = next.page,
                    loadingMore = false
                )
            }
        }
    }

    // Salvar copia a vaga para "Vagas salvas", onde ela fica mesmo depois dos 30 dias do corpus.
    fun save(job: JobPosting, showMessage: (String) -> Unit) {
        if (job.alreadySaved || job.id in state.value.savingIds) return
        mutableState.update { it.copy(savingIds = it.savingIds + job.id) }
        viewModelScope.launchRequest(onError = { ex ->
            val conflict = ex is ApiException && ex.status == 409
            if (conflict) markSaved(job.id)
            mutableState.update { it.copy(savingIds = it.savingIds - job.id) }
            showMessage(if (conflict) "Você já salvou esta vaga" else ex.generalMessage())
        }) {
            repository.save(job.id)
            markSaved(job.id)
            mutableState.update { it.copy(savingIds = it.savingIds - job.id) }
            showMessage("Vaga salva. Ela fica em Vagas salvas mesmo depois de sair da busca.")
        }
    }

    private fun markSaved(id: Long) = mutableState.update { state ->
        state.copy(items = state.items.map { if (it.id == id) it.copy(alreadySaved = true) else it })
    }

    private fun showError(ex: Throwable) =
        mutableState.update { it.copy(loading = false, loadingMore = false, error = ex.generalMessage()) }
}

data class JobDetailState(
    val detail: JobDetail? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val savedJobId: Long? = null,
    val error: String? = null
)

// Detalhes da vaga do corpus, exibidos dentro do WatchJobs (GET /jobs/{id}).
class JobDetailViewModel(private val id: Long, private val repository: JobSearchRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(JobDetailState())
    val state: StateFlow<JobDetailState> = mutableState.asStateFlow()

    init {
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }) {
            val detail = repository.detail(id)
            mutableState.update { it.copy(detail = detail, loading = false) }
        }
    }

    fun save(showMessage: (String) -> Unit) {
        val current = state.value
        if (current.saving || current.detail?.job?.alreadySaved != false) return
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launchRequest(onError = { ex ->
            val conflict = ex is ApiException && ex.status == 409
            mutableState.update { state ->
                state.copy(saving = false, detail = if (conflict) state.detail?.markSaved() else state.detail)
            }
            showMessage(if (conflict) "Você já salvou esta vaga" else ex.generalMessage())
        }) {
            val saved = repository.save(id)
            mutableState.update { it.copy(saving = false, savedJobId = saved.id, detail = it.detail?.markSaved()) }
            showMessage("Vaga salva. Ela está em Vagas salvas e no quadro de Candidaturas.")
        }
    }

    private fun JobDetail.markSaved() = copy(job = job.copy(alreadySaved = true))
}
