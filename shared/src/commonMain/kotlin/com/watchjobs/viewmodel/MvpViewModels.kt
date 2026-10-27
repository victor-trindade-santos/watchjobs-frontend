package com.watchjobs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchjobs.data.remote.ApiException
import com.watchjobs.data.repository.BoardRepository
import com.watchjobs.data.repository.FeedQuery
import com.watchjobs.data.repository.FeedRepository
import com.watchjobs.data.repository.JobSearchRepository
import com.watchjobs.data.repository.NotificationRepository
import com.watchjobs.data.repository.ProfileRepository
import com.watchjobs.model.AppNotification
import com.watchjobs.model.Board
import com.watchjobs.model.BoardColumn
import com.watchjobs.model.DiscoverRequest
import com.watchjobs.model.DiscoverResult
import com.watchjobs.model.FeedFacets
import com.watchjobs.model.FeedItem
import com.watchjobs.model.JobSkillFacet
import com.watchjobs.model.MoveRequest
import com.watchjobs.model.NotificationPreference
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.model.TimelineEvent
import com.watchjobs.model.WorkMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FeedState(
    val query: FeedQuery = FeedQuery(),
    val items: List<FeedItem> = emptyList(),
    val total: Long = 0,
    val page: Int = 0,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val savingIds: Set<Long> = emptySet(),
    val facets: FeedFacets = FeedFacets(),
    val skillSuggestions: List<JobSkillFacet> = emptyList(),
    val skillsHasMore: Boolean = false,
    val skillsLoading: Boolean = false
) {
    val hasMore: Boolean
        get() = items.size < total
}

// Página inicial e "Buscar vagas" numa tela só: vagas ordenadas pela nota, com todos os filtros opcionais.
class FeedViewModel(
    private val feed: FeedRepository,
    private val jobs: JobSearchRepository,
    initialQuery: FeedQuery = FeedQuery(),
    private val onQueryChanged: (FeedQuery) -> Unit = {}
) : ViewModel() {
    private val mutableState = MutableStateFlow(FeedState(query = initialQuery))
    val state: StateFlow<FeedState> = mutableState.asStateFlow()
    private var loadJob: Job? = null
    private var skillJob: Job? = null
    private var skillTerm = ""
    private var skillPage = 0

    init {
        load()
        viewModelScope.launchRequest(onError = {}) {
            val facets = feed.facets()
            mutableState.update { it.copy(facets = facets) }
        }
        searchSkills("")
    }

    // Texto e salário esperam uma pausa na digitação; os demais filtros valem na hora.
    fun update(debounce: Boolean = false, change: (FeedQuery) -> FeedQuery) {
        mutableState.update { it.copy(query = change(it.query)) }
        onQueryChanged(state.value.query)
        load(debounce)
    }

    fun clearFilters() = update { FeedQuery(text = it.text, sort = it.sort) }

    fun load(debounce: Boolean = false) {
        loadJob?.cancel()
        val query = state.value.query
        if (query.salaryMin != null && query.salaryMax != null && query.salaryMin > query.salaryMax) {
            mutableState.update { it.copy(loading = false, loadingMore = false, error = "O salário mínimo não pode ser maior que o máximo.") }
            return
        }
        mutableState.update { it.copy(loading = true, loadingMore = false, error = null) }
        loadJob = viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }) {
            if (debounce) delay(350)
            val page = feed.feed(state.value.query)
            mutableState.update { it.copy(items = page.items, total = page.total, page = 0, loading = false) }
        }
    }

    fun loadMore() {
        val current = state.value
        if (!current.hasMore || current.loadingMore || current.loading) return
        mutableState.update { it.copy(loadingMore = true) }
        loadJob = viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(loadingMore = false, error = ex.generalMessage()) } }) {
            val next = feed.feed(current.query, current.page + 1)
            mutableState.update { state ->
                val known = state.items.map { it.job.id }.toSet()
                state.copy(items = state.items + next.items.filter { it.job.id !in known }, total = next.total, page = next.page, loadingMore = false)
            }
        }
    }

    // Autocompletar das skills: só as que existem em vagas ativas, paginado conforme o usuário rola.
    fun searchSkills(term: String) {
        skillJob?.cancel()
        skillTerm = term.trim()
        skillPage = 0
        mutableState.update { it.copy(skillsLoading = true) }
        skillJob = viewModelScope.launchRequest(onError = { mutableState.update { it.copy(skillsLoading = false) } }) {
            if (skillTerm.isNotEmpty()) delay(250)
            val options = feed.skills(skillTerm, 0, SKILL_PAGE)
            mutableState.update { it.copy(skillSuggestions = options, skillsHasMore = options.size == SKILL_PAGE, skillsLoading = false) }
        }
    }

    fun loadMoreSkills() {
        if (!state.value.skillsHasMore || state.value.skillsLoading) return
        mutableState.update { it.copy(skillsLoading = true) }
        skillJob = viewModelScope.launchRequest(onError = { mutableState.update { it.copy(skillsLoading = false) } }) {
            val options = feed.skills(skillTerm, ++skillPage, SKILL_PAGE)
            mutableState.update {
                it.copy(skillSuggestions = it.skillSuggestions + options, skillsHasMore = options.size == SKILL_PAGE, skillsLoading = false)
            }
        }
    }

    fun addSkill(skill: JobSkillFacet) = update { query ->
        if (query.skills.any { it.id == skill.id }) query else query.copy(skills = query.skills + skill)
    }

    fun removeSkill(id: Long) = update { query -> query.copy(skills = query.skills.filterNot { it.id == id }) }

    // Dispensar ou ocultar tira a vaga da lista na hora.
    fun interact(id: Long, type: String, showMessage: (String) -> Unit) {
        viewModelScope.launchRequest(onError = { showMessage(it.generalMessage()) }) {
            feed.interact(id, type)
            mutableState.update { state -> state.copy(items = state.items.filterNot { it.job.id == id }, total = (state.total - 1).coerceAtLeast(0)) }
            showMessage(if (type == "HIDDEN") "Vaga ocultada." else "Vaga dispensada.")
        }
    }

    fun save(item: FeedItem, showMessage: (String) -> Unit) {
        val id = item.job.id
        if (item.job.alreadySaved || id in state.value.savingIds) return
        mutableState.update { it.copy(savingIds = it.savingIds + id) }
        viewModelScope.launchRequest(onError = { ex ->
            val conflict = ex is ApiException && ex.status == 409
            if (conflict) markSaved(id)
            mutableState.update { it.copy(savingIds = it.savingIds - id) }
            showMessage(if (conflict) "Você já salvou esta vaga" else ex.generalMessage())
        }) {
            jobs.save(id)
            markSaved(id)
            mutableState.update { it.copy(savingIds = it.savingIds - id) }
            showMessage("Vaga salva com a nota de agora.")
        }
    }

    private fun markSaved(id: Long) = mutableState.update { state ->
        state.copy(items = state.items.map { if (it.job.id == id) it.copy(job = it.job.copy(alreadySaved = true)) else it })
    }

    private companion object {
        const val SKILL_PAGE = 20
    }
}

data class DiscoverState(
    val q: String = "",
    val location: String = "",
    val workMode: WorkMode? = null,
    val running: Boolean = false,
    val result: DiscoverResult? = null,
    val error: String? = null
)

// "Buscar mais vagas" (F5-07): abre pré-preenchido com o perfil; mostra o resultado na hora e o aviso
// de que outras fontes ainda estão sendo consultadas. Com um id, abre o resultado de um pedido anterior.
class DiscoverViewModel(
    private val feed: FeedRepository,
    private val profile: ProfileRepository,
    existingId: Long?
) : ViewModel() {
    private val mutableState = MutableStateFlow(DiscoverState())
    val state: StateFlow<DiscoverState> = mutableState.asStateFlow()

    init {
        if (existingId != null) {
            refresh(existingId)
        } else {
            viewModelScope.launchRequest(onError = {}) {
                val preferences = profile.preferences()
                mutableState.update { state ->
                    state.copy(
                        q = preferences?.preferredRoles?.firstOrNull().orEmpty(),
                        location = preferences?.location?.city.orEmpty(),
                        workMode = WorkMode.entries.firstOrNull { it.name == preferences?.preferredWorkMode }
                    )
                }
            }
        }
    }

    fun update(change: (DiscoverState) -> DiscoverState) = mutableState.update(change)

    fun discover() {
        val current = state.value
        if (current.q.isBlank() || current.running) return
        mutableState.update { it.copy(running = true, error = null) }
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(running = false, error = ex.generalMessage()) } }) {
            val result = feed.discover(DiscoverRequest(current.q.trim(), current.location.trim().ifEmpty { null }, current.workMode))
            mutableState.update { it.copy(running = false, result = result) }
        }
    }

    fun refresh(id: Long? = null) {
        val target = id ?: state.value.result?.id ?: return
        mutableState.update { it.copy(running = true, error = null) }
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(running = false, error = ex.generalMessage()) } }) {
            val result = feed.discoverStatus(target)
            mutableState.update { it.copy(running = false, result = result, q = result.query) }
        }
    }
}

data class NotificationsState(
    val items: List<AppNotification> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val preference: NotificationPreference? = null,
    val savingPreference: Boolean = false
)

// Lista de avisos e preferências de notificação (F5-07).
class NotificationsViewModel(private val repository: NotificationRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = mutableState.asStateFlow()

    init {
        load()
        viewModelScope.launchRequest(onError = {}) {
            val preference = repository.preference()
            mutableState.update { it.copy(preference = preference) }
        }
    }

    fun load() {
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }) {
            val page = repository.list()
            mutableState.update { it.copy(items = page.items, loading = false, error = null) }
        }
    }

    fun markRead(id: Long, onDone: () -> Unit = {}) {
        viewModelScope.launchRequest(onError = {}) {
            repository.markRead(id)
            load()
            onDone()
        }
    }

    fun markAllRead(onDone: () -> Unit) {
        viewModelScope.launchRequest(onError = {}) {
            repository.markAllRead()
            load()
            onDone()
        }
    }

    fun updatePreference(change: (NotificationPreference) -> NotificationPreference) =
        mutableState.update { state -> state.copy(preference = state.preference?.let(change)) }

    fun savePreference(showMessage: (String) -> Unit) {
        val preference = state.value.preference ?: return
        if (preference.minScore !in 0.0..100.0) return showMessage("A nota mínima vai de 0 a 100.")
        mutableState.update { it.copy(savingPreference = true) }
        viewModelScope.launchRequest(onError = { ex ->
            mutableState.update { it.copy(savingPreference = false) }
            showMessage(ex.generalMessage())
        }) {
            val saved = repository.savePreference(preference)
            mutableState.update { it.copy(preference = saved, savingPreference = false) }
            showMessage("Preferências de aviso salvas.")
        }
    }
}

data class BoardState(
    val board: Board = Board(),
    val loading: Boolean = true,
    val error: String? = null,
    val moving: Boolean = false,
    val timelineFor: Long? = null,
    val timeline: List<TimelineEvent> = emptyList()
)

// Kanban (F6-05/06): mover pelo menu ou arrastando; a ordem fica no backend (board_rank).
class BoardViewModel(private val repository: BoardRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(BoardState())
    val state: StateFlow<BoardState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }) {
            val board = repository.board()
            mutableState.update { it.copy(board = board, loading = false, error = null) }
        }
    }

    // beforeId = card que fica logo acima; afterId = card que fica logo abaixo.
    fun move(
        cardId: Long,
        column: BoardColumn,
        outcome: SavedJobStatus? = null,
        beforeId: Long? = null,
        afterId: Long? = null,
        showMessage: (String) -> Unit
    ) {
        if (state.value.moving) return
        mutableState.update { it.copy(moving = true) }
        viewModelScope.launchRequest(onError = { ex ->
            mutableState.update { it.copy(moving = false) }
            showMessage(ex.generalMessage())
        }) {
            repository.move(cardId, MoveRequest(column.name, outcome, beforeId, afterId))
            val board = repository.board()
            mutableState.update { it.copy(board = board, moving = false) }
        }
    }

    fun openTimeline(id: Long) {
        mutableState.update { it.copy(timelineFor = id, timeline = emptyList()) }
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(error = ex.generalMessage()) } }) {
            val events = repository.timeline(id)
            mutableState.update { it.copy(timeline = events) }
        }
    }

    fun closeTimeline() = mutableState.update { it.copy(timelineFor = null, timeline = emptyList()) }
}
