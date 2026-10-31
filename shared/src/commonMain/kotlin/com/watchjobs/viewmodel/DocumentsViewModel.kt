package com.watchjobs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchjobs.data.repository.DocumentRepository
import com.watchjobs.data.repository.SavedJobRepository
import com.watchjobs.model.DocumentDetail
import com.watchjobs.model.DocumentSummary
import com.watchjobs.model.DocumentType
import com.watchjobs.model.DocumentsStatus
import com.watchjobs.model.ExportFile
import com.watchjobs.model.ExportFormat
import com.watchjobs.model.GenerateDocumentRequest
import com.watchjobs.model.SavedJob
import com.watchjobs.model.VersionContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DocumentsState(
    val loading: Boolean = true,
    val error: String? = null,
    val status: DocumentsStatus? = null,
    val jobs: List<SavedJob> = emptyList(),
    val selectedJobId: Long? = null,
    val documents: List<DocumentSummary> = emptyList(),
    val current: DocumentDetail? = null,
    // Texto no editor; só vira versão nova ao salvar.
    val draft: String = "",
    val preview: VersionContent? = null,
    val instructions: String = "",
    val generating: DocumentType? = null,
    val saving: Boolean = false,
    val exporting: Boolean = false
) {
    val selectedJob: SavedJob? get() = jobs.firstOrNull { it.id == selectedJobId }
    val hasUnsavedChanges: Boolean get() = current != null && draft.trim() != current.content.trim()
}

// Documentos por vaga (F10): escolher a vaga, gerar com IA, editar, salvar versões, restaurar e exportar.
class DocumentsViewModel(
    private val repository: DocumentRepository,
    private val savedJobs: SavedJobRepository,
    initialSavedJobId: Long? = null
) : ViewModel() {
    private val mutableState = MutableStateFlow(DocumentsState(selectedJobId = initialSavedJobId))
    val state: StateFlow<DocumentsState> = mutableState.asStateFlow()

    init {
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }) {
            val status = runCatching { repository.status() }.getOrNull()
            val jobs = savedJobs.list()
            val selected = state.value.selectedJobId ?: jobs.firstOrNull()?.id
            mutableState.update { it.copy(status = status, jobs = jobs, selectedJobId = selected) }
            loadDocuments(openFirst = true)
        }
    }

    fun selectJob(id: Long?) {
        if (id == state.value.selectedJobId) return
        mutableState.update { it.copy(selectedJobId = id, current = null, draft = "", preview = null, documents = emptyList()) }
        viewModelScope.launchRequest(onError = ::showError) { loadDocuments(openFirst = true) }
    }

    fun updateInstructions(text: String) = mutableState.update { it.copy(instructions = text.take(1000)) }

    fun updateDraft(text: String) = mutableState.update { it.copy(draft = text) }

    private suspend fun loadDocuments(openFirst: Boolean) {
        val jobId = state.value.selectedJobId
        val documents = if (jobId == null) emptyList() else repository.list(jobId)
        val keep = state.value.current?.takeIf { current -> documents.any { it.id == current.id } }
        val open = keep ?: if (openFirst) documents.firstOrNull()?.let { repository.detail(it.id) } else null
        mutableState.update {
            it.copy(loading = false, documents = documents, current = open, draft = open?.content ?: "", preview = null, error = null)
        }
    }

    fun open(id: Long) {
        viewModelScope.launchRequest(onError = ::showError) {
            val detail = repository.detail(id)
            mutableState.update { it.copy(current = detail, draft = detail.content, preview = null) }
        }
    }

    fun generate(type: DocumentType, showMessage: (String) -> Unit) {
        val jobId = state.value.selectedJobId ?: return showMessage("Escolha uma vaga salva.")
        if (state.value.generating != null) return
        mutableState.update { it.copy(generating = type, error = null) }
        viewModelScope.launchRequest(onError = { ex ->
            mutableState.update { it.copy(generating = null) }
            showMessage(ex.generalMessage())
        }) {
            val detail = repository.generate(GenerateDocumentRequest(jobId, type.name, state.value.instructions.trim().ifEmpty { null }))
            val documents = repository.list(jobId)
            val status = runCatching { repository.status() }.getOrNull() ?: state.value.status
            mutableState.update { it.copy(generating = null, current = detail, draft = detail.content, documents = documents, preview = null, status = status) }
            showMessage(if (detail.warnings.isEmpty()) "${type.label} gerado. Revise antes de usar." else "${type.label} gerado com pontos para revisar.")
        }
    }

    fun save(showMessage: (String) -> Unit) {
        val current = state.value.current ?: return
        if (!state.value.hasUnsavedChanges || state.value.saving) return
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launchRequest(onError = { ex ->
            mutableState.update { it.copy(saving = false) }
            showMessage(ex.generalMessage())
        }) {
            val detail = repository.edit(current.id, state.value.draft)
            mutableState.update { it.copy(saving = false, current = detail, draft = detail.content) }
            showMessage("Versão ${detail.currentVersion} salva.")
        }
    }

    fun discardChanges() = mutableState.update { it.copy(draft = it.current?.content ?: "") }

    fun preview(version: Int) {
        val current = state.value.current ?: return
        if (version == current.currentVersion) return mutableState.update { it.copy(preview = null) }
        viewModelScope.launchRequest(onError = ::showError) {
            val content = repository.version(current.id, version)
            mutableState.update { it.copy(preview = content) }
        }
    }

    fun closePreview() = mutableState.update { it.copy(preview = null) }

    fun restore(version: Int, showMessage: (String) -> Unit) {
        val current = state.value.current ?: return
        viewModelScope.launchRequest(onError = { ex -> showMessage(ex.generalMessage()) }) {
            val detail = repository.restore(current.id, version)
            mutableState.update { it.copy(current = detail, draft = detail.content, preview = null) }
            showMessage("Versão $version restaurada como versão ${detail.currentVersion}.")
        }
    }

    fun delete(showMessage: (String) -> Unit) {
        val current = state.value.current ?: return
        viewModelScope.launchRequest(onError = { ex -> showMessage(ex.generalMessage()) }) {
            repository.delete(current.id)
            mutableState.update { it.copy(current = null, draft = "") }
            loadDocuments(openFirst = true)
            showMessage("Documento excluído.")
        }
    }

    // Exporta a versão em edição já salva (ou a visualizada); com alterações pendentes, pede para salvar antes.
    fun export(format: ExportFormat, onFile: (ExportFile) -> Unit, showMessage: (String) -> Unit) {
        val current = state.value.current ?: return
        if (state.value.hasUnsavedChanges) return showMessage("Salve as alterações antes de exportar.")
        mutableState.update { it.copy(exporting = true) }
        viewModelScope.launchRequest(onError = { ex ->
            mutableState.update { it.copy(exporting = false) }
            showMessage(ex.generalMessage())
        }) {
            val file = repository.export(current.id, format, state.value.preview?.version)
            mutableState.update { it.copy(exporting = false) }
            onFile(file)
        }
    }

    fun exportBase(format: ExportFormat, onFile: (ExportFile) -> Unit, showMessage: (String) -> Unit) {
        mutableState.update { it.copy(exporting = true) }
        viewModelScope.launchRequest(onError = { ex ->
            mutableState.update { it.copy(exporting = false) }
            showMessage(ex.generalMessage())
        }) {
            val file = repository.exportBase(format)
            mutableState.update { it.copy(exporting = false) }
            onFile(file)
        }
    }

    private fun showError(ex: Throwable) = mutableState.update { it.copy(loading = false, error = ex.generalMessage()) }
}
