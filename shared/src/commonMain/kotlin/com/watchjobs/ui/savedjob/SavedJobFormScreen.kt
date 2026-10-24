package com.watchjobs.ui.savedjob

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
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.JobType
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.model.WorkMode
import com.watchjobs.ui.components.Breadcrumb
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
import com.watchjobs.viewmodel.SavedJobFormViewModel

@Composable
fun SavedJobFormScreen(
    viewModel: SavedJobFormViewModel,
    onSaved: (String) -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val form = state.form
    val errors = state.fieldErrors
    val title = if (state.isEdit) "Editar vaga" else "Nova vaga"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)) {
        PageContainer {
            Breadcrumb("Vagas salvas", title, onCancel)
            Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
            Text(
                if (state.isEdit) "Atualize os dados e grave." else "Registre uma vaga que chamou sua atenção.",
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
                            Panel("A vaga") {
                                LabeledTextField("Cargo *", form.positionRole, { v -> viewModel.update("positionRole") { it.copy(positionRole = v) } }, error = errors["positionRole"], placeholder = "Ex.: Desenvolvedor Kotlin", tag = "jobRole")
                                LabeledTextField("Empresa *", form.organizationName, { v -> viewModel.update("organizationName") { it.copy(organizationName = v) } }, error = errors["organizationName"], tag = "jobCompany")
                                LabeledTextField("URL da vaga", form.jobUrl, { v -> viewModel.update("jobUrl") { it.copy(jobUrl = v) } }, error = errors["jobUrl"], placeholder = "https://", keyboardType = KeyboardType.Uri, tag = "jobUrl")
                            }
                            Panel("Local") {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    LabeledTextField("Cidade", form.locationCity, { v -> viewModel.update("locationCity") { it.copy(locationCity = v) } }, Modifier.weight(2f), error = errors["locationCity"], tag = "jobCity")
                                    LabeledTextField("Estado", form.locationState, { v -> viewModel.update("locationState") { it.copy(locationState = v) } }, Modifier.weight(1f), error = errors["locationState"], tag = "jobState")
                                    LabeledTextField("País", form.locationCountryCode, { v -> viewModel.update("locationCountryCode") { it.copy(locationCountryCode = v.uppercase().take(2)) } }, Modifier.weight(1f), error = errors["locationCountryCode"], placeholder = "BR", tag = "jobCountry")
                                }
                            }
                            Panel("Detalhes") {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    DropdownField("Modo de trabalho", WorkMode.selectable, form.workMode, { it.label }, { v -> viewModel.update("workMode") { it.copy(workMode = v) } }, Modifier.weight(1f), tag = "jobWorkMode")
                                    DropdownField("Nível", ExperienceLevel.selectable, form.experienceLevel, { it.label }, { v -> viewModel.update("experienceLevel") { it.copy(experienceLevel = v) } }, Modifier.weight(1f), tag = "jobLevel")
                                    DropdownField("Contrato", JobType.selectable, form.jobType, { it.label }, { v -> viewModel.update("jobType") { it.copy(jobType = v) } }, Modifier.weight(1f), tag = "jobType")
                                }
                            }
                            Panel("Salário") {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    LabeledTextField("Mínimo", form.salaryMin, { v -> viewModel.update("salaryMin") { it.copy(salaryMin = v) } }, Modifier.weight(2f), error = errors["salaryMin"], placeholder = "8.000,00", keyboardType = KeyboardType.Decimal, tag = "jobSalaryMin")
                                    LabeledTextField("Máximo", form.salaryMax, { v -> viewModel.update("salaryMax") { it.copy(salaryMax = v) } }, Modifier.weight(2f), error = errors["salaryMax"], placeholder = "12.000,00", keyboardType = KeyboardType.Decimal, tag = "jobSalaryMax")
                                    LabeledTextField("Moeda", form.salaryCurrency, { v -> viewModel.update("salaryCurrency") { it.copy(salaryCurrency = v.uppercase().take(3)) } }, Modifier.weight(1f), error = errors["salaryCurrency"], placeholder = "BRL", tag = "jobCurrency")
                                }
                            }
                            Panel("Notas") {
                                LabeledTextField("Anotações", form.notes, { v -> viewModel.update("notes") { it.copy(notes = v) } }, error = errors["notes"], singleLine = false, minLines = 4, placeholder = "Quem indicou, benefícios, próximos passos…", tag = "jobNotes")
                            }
                        }
                    }
                    val side: @Composable (Modifier) -> Unit = { modifier ->
                        Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Panel("Status") {
                                DropdownField(
                                    label = "Status da vaga *",
                                    options = SavedJobStatus.entries,
                                    selected = form.status,
                                    optionLabel = { it.label },
                                    onSelect = { v -> viewModel.update("status") { it.copy(status = v) } },
                                    error = errors["status"],
                                    allowEmpty = false,
                                    tag = "jobStatus"
                                )
                                if (state.hasApplication) {
                                    Text(
                                        "Esta vaga tem candidatura: o status é copiado para ela. Para voltar a Salva, exclua a candidatura.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WatchJobsColors.Muted
                                    )
                                }
                            }
                            PrimaryButton("Gravar", { viewModel.save(onSaved) }, Modifier.fillMaxWidth().testTag("saveButton"), loading = state.saving)
                            SecondaryButton("Limpar Campos", viewModel::clear, Modifier.fillMaxWidth().testTag("clearButton"))
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
}
