package com.watchjobs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchjobs.data.repository.ProfileRepository
import com.watchjobs.model.ProfileSetupStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Etapas do assistente. As opcionais podem ser puladas; as obrigatórias são conferidas no backend ao concluir.
enum class SetupStep(val label: String, val optional: Boolean) {
    RESUME("Currículo", true),
    SUMMARY("Resumo", false),
    EXPERIENCE("Experiências", false),
    SKILLS("Skills", false),
    PREFERENCES("Preferências", false),
    REVIEW("Revisão", false);

    // A etapa está cumprida segundo o backend (as opcionais sempre contam como cumpridas).
    fun done(status: ProfileSetupStatus?): Boolean {
        val req = status?.requirements ?: return optional
        return when (this) {
            RESUME -> true
            SUMMARY -> req.summary
            EXPERIENCE -> req.experiences >= 1
            SKILLS -> req.skills >= MIN_SKILLS
            PREFERENCES -> req.preferencesConfirmed
            REVIEW -> status?.completed == true
        }
    }

    companion object {
        const val MIN_SKILLS = 3

        fun of(name: String?): SetupStep = entries.firstOrNull { it.name == name } ?: RESUME
    }
}

data class ProfileSetupState(
    val status: ProfileSetupStatus? = null,
    val step: SetupStep = SetupStep.RESUME,
    val loading: Boolean = true,
    val completing: Boolean = false,
    val error: String? = null
)

// Assistente de configuração do perfil (F05): a etapa atual fica salva no backend para continuar depois.
class ProfileSetupViewModel(private val repository: ProfileRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileSetupState())
    val state: StateFlow<ProfileSetupState> = mutableState.asStateFlow()

    init {
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }) {
            val status = repository.setupStatus()
            val saved = SetupStep.of(status.step)
            mutableState.update { it.copy(status = status, step = if (status.step == "DONE") SetupStep.REVIEW else saved, loading = false) }
        }
    }

    fun refresh() {
        viewModelScope.launchRequest(onError = {}) {
            val status = repository.setupStatus()
            mutableState.update { it.copy(status = status) }
        }
    }

    fun goTo(step: SetupStep) {
        mutableState.update { it.copy(step = step, error = null) }
        viewModelScope.launchRequest(onError = {}) {
            val status = repository.saveSetupStep(step.name)
            mutableState.update { it.copy(status = status) }
        }
    }

    fun next() {
        SetupStep.entries.getOrNull(state.value.step.ordinal + 1)?.let(::goTo)
    }

    fun back() {
        SetupStep.entries.getOrNull(state.value.step.ordinal - 1)?.let(::goTo)
    }

    fun complete(onDone: () -> Unit) {
        mutableState.update { it.copy(completing = true, error = null) }
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(completing = false, error = ex.generalMessage()) } }) {
            val status = repository.completeSetup()
            mutableState.update { it.copy(status = status, completing = false) }
            onDone()
        }
    }
}
