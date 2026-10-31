package com.watchjobs.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.watchjobs.model.Experience
import com.watchjobs.model.PersonalRequest
import com.watchjobs.model.UserSkill
import com.watchjobs.ui.components.ConfirmDialog
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.GhostButton
import com.watchjobs.ui.components.Hairline
import com.watchjobs.ui.components.LabeledTextField
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageContainer
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.components.SectionHint
import com.watchjobs.ui.components.SectionLabel
import com.watchjobs.ui.components.WatchJobsLogo
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.viewmodel.LinkedInImportViewModel
import com.watchjobs.viewmodel.PreferencesViewModel
import com.watchjobs.viewmodel.ProfileSetupViewModel
import com.watchjobs.viewmodel.ProfileViewModel
import com.watchjobs.viewmodel.SetupStep

private sealed interface SetupDialog {
    data class ExperienceForm(val item: Experience?) : SetupDialog
    data class SkillForm(val item: UserSkill?) : SetupDialog
    data class DeleteExperience(val item: Experience) : SetupDialog
    data class DeleteSkill(val item: UserSkill) : SetupDialog
}

// Configuração inicial do perfil (F05): currículo/LinkedIn, resumo, experiências, skills e preferências.
// Reaproveita a importação e as preferências existentes; o progresso fica salvo no backend.
@Composable
fun ProfileSetupScreen(
    viewModel: ProfileSetupViewModel,
    profileViewModel: ProfileViewModel,
    importViewModel: LinkedInImportViewModel,
    preferencesViewModel: PreferencesViewModel,
    onCompleted: () -> Unit,
    onLogout: () -> Unit,
    showMessage: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val profileState by profileViewModel.state.collectAsState()
    val status = state.status
    var dialog by remember { mutableStateOf<SetupDialog?>(null) }
    val afterChange: (String) -> (String?) -> Unit = { message ->
        { error ->
            if (error == null) {
                dialog = null
                viewModel.refresh()
                showMessage(message)
            } else {
                showMessage(error)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(WatchJobsColors.Background).testTag("profileSetup")) {
        Row(
            Modifier.fillMaxWidth().background(WatchJobsColors.White).height(60.dp).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WatchJobsLogo()
            Spacer(Modifier.weight(1f))
            GhostButton("Sair", onLogout, Modifier.testTag("setupLogout"))
        }
        Hairline()
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("Configure o seu perfil", style = MaterialTheme.typography.headlineLarge, color = WatchJobsColors.Ink)
            Text(
                "Para liberar o app: resumo profissional, 1 experiência, 3 skills e as preferências de vagas. Você pode sair e continuar depois.",
                style = MaterialTheme.typography.bodyMedium,
                color = WatchJobsColors.Muted
            )
            Stepper(state.step, status, viewModel::goTo)
            state.error?.let { ErrorBanner(it, Modifier.padding(top = 8.dp)) }
        }
        Hairline()
        if (state.loading) {
            LoadingBox()
            return@Column
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (state.step) {
                SetupStep.RESUME -> LinkedInImportScreen(
                    viewModel = importViewModel,
                    onBack = viewModel::next,
                    onApplied = { message, _ ->
                        profileViewModel.reload()
                        viewModel.refresh()
                        showMessage(message)
                        viewModel.next()
                    }
                )
                SetupStep.PREFERENCES -> {
                    LaunchedEffect(Unit) { preferencesViewModel.reload() }
                    PreferencesScreen(
                        viewModel = preferencesViewModel,
                        onBack = viewModel::back,
                        onSaved = { message ->
                            viewModel.refresh()
                            showMessage(message)
                            viewModel.next()
                        },
                        showMessage = showMessage
                    )
                }
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                    PageContainer(maxWidth = 900.dp) {
                        when (state.step) {
                            SetupStep.SUMMARY -> SummaryStep(profileState.profile?.personal?.objective.orEmpty(), profileState.busy) { text ->
                                profileViewModel.savePersonal(PersonalRequest(objective = text.trim())) { error ->
                                    if (error == null) {
                                        viewModel.refresh()
                                        viewModel.next()
                                    } else {
                                        showMessage(error)
                                    }
                                }
                            }
                            SetupStep.EXPERIENCE -> ExperienceStep(
                                experiences = profileState.profile?.experiences.orEmpty(),
                                onAdd = { dialog = SetupDialog.ExperienceForm(null) },
                                onEdit = { dialog = SetupDialog.ExperienceForm(it) },
                                onDelete = { dialog = SetupDialog.DeleteExperience(it) },
                                onImport = { viewModel.goTo(SetupStep.RESUME) }
                            )
                            SetupStep.SKILLS -> SkillsStep(
                                skills = profileState.profile?.skills.orEmpty(),
                                onAdd = { dialog = SetupDialog.SkillForm(null) },
                                onEdit = { dialog = SetupDialog.SkillForm(it) },
                                onDelete = { dialog = SetupDialog.DeleteSkill(it) }
                            )
                            SetupStep.REVIEW -> ReviewStep(status, state.completing) { viewModel.complete(onCompleted) }
                            else -> Unit
                        }
                        if (state.step != SetupStep.REVIEW && state.step != SetupStep.SUMMARY) {
                            Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SecondaryButton("Voltar", viewModel::back)
                                PrimaryButton(
                                    "Continuar",
                                    viewModel::next,
                                    enabled = state.step.done(status),
                                    modifier = Modifier.testTag("setupNext")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    when (val current = dialog) {
        null -> Unit
        is SetupDialog.ExperienceForm -> ExperienceDialog(profileViewModel, current.item, afterChange("Experiência salva.")) { dialog = null }
        is SetupDialog.SkillForm -> SkillDialog(profileViewModel, current.item, afterChange("Skill salva.")) { dialog = null }
        is SetupDialog.DeleteExperience -> ConfirmDialog(
            "Excluir", "Excluir a experiência ${current.item.jobTitle}?", "Excluir",
            onConfirm = { profileViewModel.deleteExperience(current.item.id, afterChange("Excluída.")) },
            onDismiss = { dialog = null }
        )
        is SetupDialog.DeleteSkill -> ConfirmDialog(
            "Excluir", "Excluir a skill ${current.item.skillName}?", "Excluir",
            onConfirm = { profileViewModel.deleteSkill(current.item.id, afterChange("Excluída.")) },
            onDismiss = { dialog = null }
        )
    }
}

// Indicador das etapas: concluídas com ✓, a atual sublinhada; dá para pular entre elas.
@Composable
private fun Stepper(current: SetupStep, status: com.watchjobs.model.ProfileSetupStatus?, onSelect: (SetupStep) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SetupStep.entries.forEachIndexed { index, step ->
            val active = step == current
            val done = step != SetupStep.REVIEW && step.done(status)
            Row(
                Modifier.border(1.dp, if (active) WatchJobsColors.Red else WatchJobsColors.Border)
                    .background(if (active) WatchJobsColors.RedSoft else WatchJobsColors.White)
                    .clickable { onSelect(step) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("setupStep_${step.name}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (done) "✓" else "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (done) WatchJobsColors.Red else WatchJobsColors.Muted
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    step.label + if (step.optional) " (opcional)" else "",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = WatchJobsColors.Ink
                )
            }
        }
    }
}

@Composable
private fun SummaryStep(current: String, saving: Boolean, onSave: (String) -> Unit) {
    var text by remember(current) { mutableStateOf(current) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHint("Escreva (ou revise o que veio do currículo) um resumo de quem você é profissionalmente: área, experiência e o que procura.")
        LabeledTextField("Resumo profissional", text, { text = it.take(SUMMARY_MAX) }, singleLine = false, minLines = 6, tag = "setupSummary")
        Text("${text.length}/$SUMMARY_MAX caracteres", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
        PrimaryButton("Salvar e continuar", { onSave(text) }, enabled = text.isNotBlank(), loading = saving, modifier = Modifier.testTag("setupSaveSummary"))
    }
}

@Composable
private fun ExperienceStep(
    experiences: List<Experience>,
    onAdd: () -> Unit,
    onEdit: (Experience) -> Unit,
    onDelete: (Experience) -> Unit,
    onImport: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHint("Cadastre pelo menos uma experiência. O que veio do currículo já aparece aqui: revise e edite se precisar.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("+ Adicionar experiência", onAdd, modifier = Modifier.testTag("setupAddExperience"))
            SecondaryButton("Importar do currículo", onImport)
        }
        if (experiences.isEmpty()) Text("Nenhuma experiência ainda.", style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted)
        experiences.forEach { exp ->
            SetupItem(exp.jobTitle, "${exp.organizationName} · ${exp.startDate.take(7)} – ${if (exp.current) "hoje" else exp.endDate?.take(7).orEmpty()}", { onEdit(exp) }, { onDelete(exp) })
        }
    }
}

@Composable
private fun SkillsStep(skills: List<UserSkill>, onAdd: () -> Unit, onEdit: (UserSkill) -> Unit, onDelete: (UserSkill) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHint("Adicione pelo menos ${SetupStep.MIN_SKILLS} habilidades técnicas. Elas pesam no cálculo do match das vagas.")
        Text("${skills.size} de ${SetupStep.MIN_SKILLS} skills mínimas", style = MaterialTheme.typography.titleSmall, color = if (skills.size >= SetupStep.MIN_SKILLS) WatchJobsColors.Ink else WatchJobsColors.RedDark)
        PrimaryButton("+ Adicionar skill", onAdd, modifier = Modifier.testTag("setupAddSkill"))
        skills.forEach { skill -> SetupItem(skill.skillName, skill.skillLevel, { onEdit(skill) }, { onDelete(skill) }) }
    }
}

@Composable
private fun ReviewStep(status: com.watchjobs.model.ProfileSetupStatus?, completing: Boolean, onComplete: () -> Unit) {
    val req = status?.requirements
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Requisitos mínimos")
        RequirementRow("Resumo profissional", req?.summary == true)
        RequirementRow("Pelo menos 1 experiência (${req?.experiences ?: 0})", (req?.experiences ?: 0) >= 1)
        RequirementRow("Pelo menos ${SetupStep.MIN_SKILLS} skills (${req?.skills ?: 0})", (req?.skills ?: 0) >= SetupStep.MIN_SKILLS)
        RequirementRow("Preferências de vagas salvas", req?.preferencesConfirmed == true)
        if (status?.canComplete != true) {
            Text("Volte às etapas sem ✓ para completar o perfil.", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
        }
        PrimaryButton("Concluir e entrar no app", onComplete, enabled = status?.canComplete == true, loading = completing, modifier = Modifier.testTag("completeSetup"))
    }
}

@Composable
private fun RequirementRow(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(18.dp).background(if (ok) WatchJobsColors.Ink else WatchJobsColors.White).border(1.dp, if (ok) WatchJobsColors.Ink else WatchJobsColors.Red),
            contentAlignment = Alignment.Center
        ) {
            if (ok) Text("✓", style = MaterialTheme.typography.labelSmall, color = WatchJobsColors.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (ok) WatchJobsColors.Ink else WatchJobsColors.RedDark)
    }
}

@Composable
private fun SetupItem(title: String, subtitle: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(WatchJobsColors.White).border(1.dp, WatchJobsColors.Divider).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = WatchJobsColors.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
        }
        GhostButton("Editar", onEdit)
        GhostButton("Excluir", onDelete, color = WatchJobsColors.Red)
    }
}
