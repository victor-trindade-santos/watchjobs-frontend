@file:OptIn(ExperimentalLayoutApi::class)

package com.watchjobs.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.JobType
import com.watchjobs.model.PreferredWorkMode
import com.watchjobs.model.importStatusLabel
import com.watchjobs.ui.components.Breadcrumb
import com.watchjobs.ui.components.DropdownField
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.GhostButton
import com.watchjobs.ui.components.Hairline
import com.watchjobs.ui.components.LabeledTextField
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageContainer
import com.watchjobs.ui.components.Panel
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.components.SectionHint
import com.watchjobs.ui.components.Tag
import com.watchjobs.ui.components.rememberFilePicker
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.viewmodel.LinkedInImportViewModel
import com.watchjobs.viewmodel.PreferencesViewModel

// Preferências (F3-11): o que o usuário quer encontrar. "Usar sugestões" preenche com o perfil deduzido.
@Composable
fun PreferencesScreen(viewModel: PreferencesViewModel, onBack: () -> Unit, onSaved: (String) -> Unit, showMessage: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    val form = state.form
    var skillQuery by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)) {
        PageContainer(maxWidth = 900.dp) {
            Breadcrumb("Perfil", "Preferências", onBack)
            Hairline(Modifier.padding(top = 8.dp, bottom = 16.dp))
            if (state.loading) {
                LoadingBox()
                return@PageContainer
            }
            SectionHint(
                if (state.confirmed) "O feed e os avisos usam estas preferências. Mudar qualquer campo recalcula as notas."
                else "Ao salvar pela primeira vez, a busca automática é ligada: você recebe avisos de vagas com nota alta."
            )
            if (state.autoFilled) {
                SectionHint(
                    "Preenchemos estas preferências com o seu currículo. Revise, escolha o modo de trabalho e o salário, ajuste o que quiser e salve.",
                    Modifier.padding(top = 12.dp).testTag("autoFilledHint")
                )
            }
            if (state.suggestionAvailable) {
                SecondaryButton("Usar sugestões do perfil", { viewModel.useSuggestion(showMessage) }, Modifier.padding(top = 12.dp).testTag("useSuggestion"))
            }
            state.error?.let { ErrorBanner(it, Modifier.padding(top = 12.dp)) }
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LabeledTextField("Cargos desejados (separados por vírgula)", form.roles, { v -> viewModel.update { it.copy(roles = v) } },
                    error = state.fieldErrors["preferredRoles"], placeholder = "Ex.: Desenvolvedor Backend, Engenheiro de Software", tag = "prefRoles")
                LabeledTextField("Palavras-chave (opcional)", form.keywords, { v -> viewModel.update { it.copy(keywords = v) } }, placeholder = "Ex.: fintech, Kotlin")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField("Modo de trabalho", PreferredWorkMode.entries, form.workMode, { it.label },
                        { v -> viewModel.update { it.copy(workMode = v ?: PreferredWorkMode.ANY) } }, Modifier.widthIn(min = 200.dp, max = 260.dp), allowEmpty = false, tag = "prefMode")
                    DropdownField("Nível", ExperienceLevel.selectable, form.level, { it.label },
                        { v -> viewModel.update { it.copy(level = v) } }, Modifier.widthIn(min = 200.dp, max = 260.dp), emptyLabel = "Usar o deduzido")
                    DropdownField("Contrato", JobType.selectable, form.jobType, { it.label },
                        { v -> viewModel.update { it.copy(jobType = v) } }, Modifier.widthIn(min = 200.dp, max = 260.dp), emptyLabel = "Qualquer")
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledTextField("Salário mínimo (R$/mês)", form.salaryMin, { v -> viewModel.update { it.copy(salaryMin = v) } },
                        Modifier.widthIn(min = 180.dp, max = 240.dp), error = state.fieldErrors["salaryMin"])
                    LabeledTextField("Salário máximo (opcional)", form.salaryMax, { v -> viewModel.update { it.copy(salaryMax = v) } },
                        Modifier.widthIn(min = 180.dp, max = 240.dp), error = state.fieldErrors["salaryMax"])
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledTextField("Cidade", form.city, { v -> viewModel.update { it.copy(city = v) } }, Modifier.widthIn(min = 180.dp, max = 240.dp))
                    LabeledTextField("Estado", form.state, { v -> viewModel.update { it.copy(state = v) } }, Modifier.widthIn(min = 100.dp, max = 140.dp))
                    LabeledTextField("País", form.country, { v -> viewModel.update { it.copy(country = v.take(2)) } }, Modifier.widthIn(min = 80.dp, max = 120.dp))
                }
                Panel("Skills que você quer ver nas vagas") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        form.desiredSkills.forEach { skill -> Tag("${skill.name}  ×", Modifier.clickable { viewModel.removeSkill(skill.id) }) }
                    }
                    LabeledTextField("Adicionar skill", skillQuery, { skillQuery = it; viewModel.searchSkills(it) }, placeholder = "Ex.: kotlin")
                    if (state.skillOptions.isNotEmpty() && skillQuery.isNotBlank()) {
                        Column(Modifier.fillMaxWidth().background(WatchJobsColors.Surface).border(1.dp, WatchJobsColors.Divider)) {
                            state.skillOptions.take(6).forEach { option ->
                                Text(option.name, Modifier.fillMaxWidth().clickable {
                                    viewModel.addSkill(option)
                                    skillQuery = ""
                                    viewModel.searchSkills("")
                                }.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
                DropdownField("Busca automática e avisos", listOf(true, false), form.autoSearch, { if (it) "Ligada" else "Desligada" },
                    { v -> viewModel.update { it.copy(autoSearch = v ?: true) } }, Modifier.widthIn(max = 280.dp), allowEmpty = false)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Salvar preferências", { viewModel.save(onSaved) }, loading = state.saving, modifier = Modifier.testTag("savePreferences"))
                    SecondaryButton("Cancelar", onBack)
                }
            }
        }
    }
}

// Importar dados (F3-12): enviar o currículo (PDF, DOCX, TXT) ou o ZIP do LinkedIn, conferir a prévia e marcar o que aplicar.
@Composable
fun LinkedInImportScreen(
    viewModel: LinkedInImportViewModel,
    onBack: () -> Unit,
    onApplied: (message: String, reviewPreferences: Boolean) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val pickResume = rememberFilePicker(listOf("pdf", "docx", "txt")) { file -> file?.let(viewModel::uploadResume) }
    val pickZip = rememberFilePicker(listOf("zip")) { file -> file?.let(viewModel::uploadLinkedIn) }
    val current = state.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)) {
        PageContainer(maxWidth = 900.dp) {
            Breadcrumb("Perfil", "Importar dados", onBack)
            Hairline(Modifier.padding(top = 8.dp, bottom = 16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Panel("Enviar o seu currículo") {
                    Text(
                        "Envie um currículo em PDF, DOCX ou TXT (até 10 MB), ou o PDF do seu perfil do LinkedIn. " +
                            "Vamos preencher experiências, formação, skills e idiomas e sugerir as suas preferências. " +
                            "Você confere tudo antes de aplicar e pode ajustar depois.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    PrimaryButton("Escolher currículo", pickResume, loading = state.uploading, modifier = Modifier.testTag("pickResume"))
                    Text("O arquivo é lido na hora e não é guardado.", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
                }
                Panel("Como baixar o seu currículo do LinkedIn (PDF)") {
                    listOf(
                        "Abra o LinkedIn no computador e entre no seu perfil (clique em Você e depois em Ver perfil).",
                        "Logo abaixo da sua foto e do cargo, clique em Mais (em algumas versões o botão se chama Recursos).",
                        "Escolha Salvar como PDF (em inglês, Save to PDF). O arquivo é baixado na hora.",
                        "Volte aqui e envie o PDF em \"Escolher currículo\"."
                    ).forEachIndexed { i, text -> Text("${i + 1}. $text", style = MaterialTheme.typography.bodyMedium) }
                    Text(
                        "No celular o caminho é parecido (perfil, Mais, Salvar como PDF), mas a opção nem sempre aparece no aplicativo: se não aparecer, use o computador. " +
                            "Se faltar algo no resultado, confira se Experiência, Formação e Competências estão preenchidas no seu perfil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WatchJobsColors.Muted
                    )
                }
                Panel("Tem o arquivo de dados completo do LinkedIn (ZIP)?") {
                    listOf(
                        "No LinkedIn, abra Configurações e privacidade › Privacidade de dados › Obter uma cópia dos seus dados.",
                        "Escolha \"Quero algo em particular\" e marque Cargos, Formação acadêmica, Competências, Idiomas e Perfil.",
                        "Quando o e-mail do LinkedIn chegar (de alguns minutos até um dia), baixe o ZIP e envie aqui."
                    ).forEachIndexed { i, text -> Text("${i + 1}. $text", style = MaterialTheme.typography.bodyMedium) }
                    SecondaryButton("Escolher o arquivo ZIP", pickZip, modifier = Modifier.testTag("pickZip"))
                }
            }
            state.error?.let { ErrorBanner(it, Modifier.padding(top = 12.dp)) }
            if (current != null) {
                val preview = current.preview
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (current.status == "APPLIED") {
                        SectionHint("Importação aplicada: ${current.itemsApplied} itens entraram no perfil.")
                    } else {
                        SectionHint(
                            if (current.importSource == "RESUME")
                                "Prévia do currículo. A leitura é aproximada: desmarque o que estiver errado e ajuste o resto depois, no perfil."
                            else "Prévia do arquivo do LinkedIn. Desmarque o que não quiser importar."
                        )
                    }
                    preview.profile?.headline?.let {
                        Panel("Objetivo profissional lido") {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                            Text("Entra no perfil só se o seu objetivo ainda estiver vazio.", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
                        }
                    }
                    PreviewGroup("Experiências", preview.experiences.map { Triple(it.ref, "${it.jobTitle} · ${it.organizationName}", it.status) }, state.selected, viewModel::toggle)
                    PreviewGroup("Formações", preview.educations.map { Triple(it.ref, listOfNotNull(it.degree, it.institution).joinToString(" · "), it.status) }, state.selected, viewModel::toggle)
                    PreviewGroup("Skills", preview.skills.map { Triple(it.ref, it.name, it.status) }, state.selected, viewModel::toggle)
                    PreviewGroup("Idiomas", preview.languages.map { Triple(it.ref, "${it.name} (${it.proficiencyLevel})", it.status) }, state.selected, viewModel::toggle)
                    if (current.status == "PREVIEW") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryButton("Aplicar ${state.selected.size} itens", { viewModel.apply(onApplied) }, loading = state.applying,
                                enabled = state.selected.isNotEmpty(), modifier = Modifier.testTag("applyImport"))
                            GhostButton("Descartar prévia", { viewModel.discard(onBack) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewGroup(title: String, items: List<Triple<String, String, String>>, selected: Set<String>, onToggle: (String) -> Unit) {
    if (items.isEmpty()) return
    Panel(title) {
        items.forEach { (ref, label, status) ->
            val selectable = status == "NEW"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = ref in selected,
                    onCheckedChange = { if (selectable) onToggle(ref) },
                    enabled = selectable,
                    colors = CheckboxDefaults.colors(checkedColor = WatchJobsColors.Red)
                )
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(importStatusLabel(status), style = MaterialTheme.typography.bodySmall,
                        color = if (status == "CONFLICT" || status == "UNRECOGNIZED") WatchJobsColors.RedDark else WatchJobsColors.Muted)
                }
            }
        }
    }
}
