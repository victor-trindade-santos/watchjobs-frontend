package com.watchjobs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchjobs.data.repository.AnalyticsRepository
import com.watchjobs.model.Analytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AnalyticsState(
    val weeks: Int = 12,
    val data: Analytics? = null,
    val loading: Boolean = true,
    val error: String? = null
)

// Analytics (F09): recarrega ao abrir a tela, ao trocar o período e no botão "Atualizar".
class AnalyticsViewModel(private val repository: AnalyticsRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun setWeeks(weeks: Int) {
        if (weeks == state.value.weeks) return
        mutableState.update { it.copy(weeks = weeks) }
        load()
    }

    fun load() {
        val weeks = state.value.weeks
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }) {
            val data = repository.analytics(weeks)
            mutableState.update { if (it.weeks == weeks) it.copy(data = data, loading = false) else it }
        }
    }

    companion object {
        val PERIODS = listOf(4, 12, 26, 52)
    }
}
