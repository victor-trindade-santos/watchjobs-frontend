package com.watchjobs.viewmodel

import com.watchjobs.data.repository.NotificationRepository
import com.watchjobs.model.AppNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationCenterState(
    val recent: List<AppNotification> = emptyList(),
    val unread: Long = 0,
    val loading: Boolean = false,
    val error: String? = null
)

// Estado do sininho, compartilhado pela navbar e pela página de avisos: os cinco mais recentes e o contador.
// Marcar como lido atualiza a tela na hora e confirma no backend; se falhar, recarrega do servidor.
class NotificationCenter(
    private val repository: NotificationRepository,
    private val scope: CoroutineScope
) {
    private val mutableState = MutableStateFlow(NotificationCenterState())
    val state: StateFlow<NotificationCenterState> = mutableState.asStateFlow()

    fun refreshCount() {
        scope.launch {
            runCatching { repository.unreadCount() }.onSuccess { count -> mutableState.update { it.copy(unread = count) } }
        }
    }

    fun refresh() {
        mutableState.update { it.copy(loading = true, error = null) }
        scope.launch {
            runCatching { repository.list(0, RECENT) to repository.unreadCount() }
                .onSuccess { (page, count) -> mutableState.update { it.copy(recent = page.items.take(RECENT), unread = count, loading = false) } }
                .onFailure { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }
        }
    }

    fun markRead(id: Long) {
        val target = state.value.recent.firstOrNull { it.id == id }
        if (target != null && !target.unread) return
        mutableState.update { state ->
            state.copy(
                recent = state.recent.map { if (it.id == id) it.copy(readAt = it.readAt ?: "lido") else it },
                unread = (state.unread - 1).coerceAtLeast(0)
            )
        }
        scope.launch { runCatching { repository.markRead(id) }.onFailure { refresh() } }
    }

    fun markAllRead() {
        mutableState.update { state -> state.copy(recent = state.recent.map { it.copy(readAt = it.readAt ?: "lido") }, unread = 0) }
        scope.launch { runCatching { repository.markAllRead() }.onFailure { refresh() } }
    }

    fun clear() = mutableState.update { NotificationCenterState() }

    private companion object {
        const val RECENT = 5
    }
}
