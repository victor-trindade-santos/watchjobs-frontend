package com.watchjobs.ui.application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.ui.components.Breadcrumb
import com.watchjobs.ui.components.ConfirmDialog
import com.watchjobs.ui.components.DropdownField
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.GhostButton
import com.watchjobs.ui.components.LabeledTextField
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageContainer
import com.watchjobs.ui.components.Panel
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.viewmodel.ApplicationFormViewModel

@Composable
fun ApplicationFormScreen(
    viewModel: ApplicationFormViewModel,
    onSaved: (String) -> Unit,
    onCancel: () -> Unit,
    onNewSavedJob: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val form = state.form
    val errors = state.fieldErrors
    val title = if (state.isEdit) "Editar candidatura" else "Nova candidatura"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)) {
        PageContainer {
            Breadcrumb("Candidaturas", title, onCancel)
            Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
            Text(
                "O status escolhido aqui também é gravado na vaga.",
                style = MaterialTheme.typography.bodyMedium,
                color = WatchJobsColors.Muted
            )
            if (state.loading) {
                LoadingBox()
                return@PageContainer
            }
            Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                state.error?.let { ErrorBanner(it) }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val wide = maxWidth >= 860.dp
                    val main: @Composable (Modifier) -> Unit = { modifier ->
                        Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Panel("Vaga") {
                                DropdownField(
                                    label = "Vaga *",
                                    options = state.availableJobs,
                                    selected = form.savedJob,
                                    optionLabel = { "${it.positionRole} — ${it.organizationName}" },
                                    onSelect = { v -> viewModel.update("savedJobId") { it.copy(savedJob = v) } },
                                    error = errors["savedJobId"],
                                    allowEmpty = false,
                                    enabled = !state.isEdit,
                                    tag = "appJob"
                                )
                                when {
                                    state.isEdit -> Text("A vaga não muda depois que a candidatura é criada.", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
                                    state.availableJobs.isEmpty() -> {
                                        Text("Todas as suas vagas já têm candidatura. Salve uma vaga nova primeiro.", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
                                        SecondaryButton("+ Nova vaga", onNewSavedJob)
                                    }
                                    else -> Text("Só aparecem as vagas que ainda não têm candidatura.", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
                                }
                            }
                            Panel("Processo") {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    DropdownField("Etapa *", state.stages, form.stage, { it.name }, { v -> viewModel.update("stageId") { it.copy(stage = v) } }, Modifier.weight(1f), error = errors["stageId"], allowEmpty = false, tag = "appStage")
                                    DropdownField("Status *", SavedJobStatus.applicationStatuses, form.status, { it.label }, { v -> viewModel.update("status") { it.copy(status = v) } }, Modifier.weight(1f), error = errors["status"], allowEmpty = false, tag = "appStatus")
                                }
                            }
                            Panel("Datas") {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    LabeledTextField("Início *", form.applicationDate, { v -> viewModel.update("applicationDate") { it.copy(applicationDate = v) } }, Modifier.weight(1f), error = errors["applicationDate"], placeholder = "dd/mm/aaaa", keyboardType = KeyboardType.Number, tag = "appDate")
                                    LabeledTextField("Término", form.endDate, { v -> viewModel.update("endDate") { it.copy(endDate = v) } }, Modifier.weight(1f), error = errors["endDate"], placeholder = "dd/mm/aaaa", keyboardType = KeyboardType.Number, tag = "appEndDate")
                                }
                                LabeledTextField("Próxima entrevista", form.interviewAt, { v -> viewModel.update("interviewAt") { it.copy(interviewAt = v) } }, error = errors["interviewAt"], placeholder = "dd/mm/aaaa hh:mm", tag = "appInterview")
                            }
                            Panel("Anotações") {
                                LabeledTextField("Notas", form.notes, { v -> viewModel.update("notes") { it.copy(notes = v) } }, error = errors["notes"], singleLine = false, minLines = 3, tag = "appNotes")
                                LabeledTextField("Feedback", form.feedback, { v -> viewModel.update("feedback") { it.copy(feedback = v) } }, error = errors["feedback"], singleLine = false, minLines = 3, tag = "appFeedback")
                            }
                        }
                    }
                    val side: @Composable (Modifier) -> Unit = { modifier ->
                        Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Panel("Resumo") {
                                Text(form.savedJob?.positionRole ?: "Escolha uma vaga", style = MaterialTheme.typography.titleLarge)
                                Text(form.savedJob?.organizationName ?: "", style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted)
                                Text(
                                    listOfNotNull(form.stage?.name, form.status?.label).joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WatchJobsColors.Red
                                )
                            }
                            PrimaryButton("Gravar", { viewModel.save(onSaved) }, Modifier.fillMaxWidth().testTag("saveButton"), loading = state.saving)
                            SecondaryButton("Limpar Campos", viewModel::clear, Modifier.fillMaxWidth().testTag("clearButton"))
                            if (state.isEdit) {
                                SecondaryButton("Excluir candidatura", viewModel::requestDelete, Modifier.fillMaxWidth().testTag("deleteButton"))
                            }
                            GhostButton("Cancelar", onCancel, Modifier.fillMaxWidth())
                        }
                    }
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            main(Modifier.weight(2f))
                            side(Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            main(Modifier.fillMaxWidth())
                            side(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }

    if (state.showDeleteConfirm) {
        ConfirmDialog(
            title = "Excluir candidatura?",
            message = "A candidatura será excluída e a vaga voltará para Salva.",
            confirmText = "Excluir",
            onConfirm = { viewModel.confirmDelete(onSaved) },
            onDismiss = viewModel::cancelDelete
        )
    }
}
