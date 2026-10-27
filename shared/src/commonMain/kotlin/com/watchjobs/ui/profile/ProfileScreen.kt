@file:OptIn(ExperimentalLayoutApi::class)

package com.watchjobs.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchjobs.model.Availability
import com.watchjobs.model.Education
import com.watchjobs.model.EducationRequest
import com.watchjobs.model.Experience
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.ExperienceRequest
import com.watchjobs.model.ExperienceType
import com.watchjobs.model.LocationInput
import com.watchjobs.model.PersonalRequest
import com.watchjobs.model.Preferences
import com.watchjobs.model.PreferredWorkMode
import com.watchjobs.model.ProficiencyLevel
import com.watchjobs.model.Profile
import com.watchjobs.model.ProfileStats
import com.watchjobs.model.ProfileSummary
import com.watchjobs.model.SkillLevel
import com.watchjobs.model.SkillOption
import com.watchjobs.model.UserLanguage
import com.watchjobs.model.UserLanguageRequest
import com.watchjobs.model.UserSkill
import com.watchjobs.model.UserSkillRequest
import com.watchjobs.ui.components.ConfirmDialog
import com.watchjobs.ui.components.CheckIcon
import com.watchjobs.ui.components.DropdownField
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.GhostButton
import com.watchjobs.ui.components.Hairline
import com.watchjobs.ui.components.LabeledTextField
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageContainer
import com.watchjobs.ui.components.PageHeader
import com.watchjobs.ui.components.Panel
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.components.SectionLabel
import com.watchjobs.ui.components.Tag
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.util.parseBrDate
import com.watchjobs.util.salaryRange
import com.watchjobs.util.toBr
import com.watchjobs.viewmodel.ProfileViewModel
import kotlinx.datetime.LocalDate

private sealed interface ProfileDialog {
    data object Personal : ProfileDialog
    data object Resume : ProfileDialog
    data class ExperienceForm(val item: Experience?) : ProfileDialog
    data class EducationForm(val item: Education?) : ProfileDialog
    data class SkillForm(val item: UserSkill?) : ProfileDialog
    data class LanguageForm(val item: UserLanguage?) : ProfileDialog
    data class Delete(val label: String, val action: ((String?) -> Unit) -> Unit) : ProfileDialog
}

// Perfil no formato de currículo: cabeçalho; experiência e formação na coluna principal;
// dados pessoais, LinkedIn, preferências e estatísticas na lateral (abaixo, em tela estreita).
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onPreferences: () -> Unit,
    onImport: () -> Unit,
    onSearchJobs: () -> Unit,
    showMessage: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var dialog by remember { mutableStateOf<ProfileDialog?>(null) }
    val profile = state.profile
    val done: (String) -> (String?) -> Unit = { message -> { error -> if (error == null) { dialog = null; showMessage(message) } else showMessage(error) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp)) {
        PageContainer(maxWidth = 1200.dp) {
            when {
                state.loading -> LoadingBox()
                profile == null -> ErrorBanner(state.error ?: "Não foi possível carregar o perfil")
                else -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val wide = maxWidth >= 1000.dp
                    val main: @Composable ColumnScope.() -> Unit = {
                        ResumeSection("Experiência profissional", { dialog = ProfileDialog.ExperienceForm(null) }, "add_Experiências") {
                            if (profile.experiences.isEmpty()) EmptyHint("Nada cadastrado ainda. Adicione uma experiência ou importe o seu currículo (ver o card lateral).")
                            ExperienceTimeline(
                                profile.experiences, profile.skills,
                                onEdit = { dialog = ProfileDialog.ExperienceForm(it) },
                                onDelete = { exp -> dialog = ProfileDialog.Delete("a experiência ${exp.jobTitle}") { cb -> viewModel.deleteExperience(exp.id, cb) } }
                            )
                        }
                        ResumeSection("Formação acadêmica", { dialog = ProfileDialog.EducationForm(null) }, "add_Formações") {
                            if (profile.educations.isEmpty()) EmptyHint("Nada cadastrado ainda. Inclua graduação, pós-graduação, MBA e cursos.")
                            profile.educations.forEach { edu ->
                                ItemRow(
                                    title = edu.fieldOfStudy,
                                    subtitle = listOf(
                                        com.watchjobs.model.educationLevelLabel(edu.educationLevel) + " · " + edu.institution,
                                        period(edu.startDate, edu.endDate, edu.current)
                                    ).joinToString("\n"),
                                    source = edu.source,
                                    onEdit = { dialog = ProfileDialog.EducationForm(edu) },
                                    onDelete = { dialog = ProfileDialog.Delete("a formação ${edu.fieldOfStudy}") { cb -> viewModel.deleteEducation(edu.id, cb) } }
                                )
                            }
                        }
                        ResumeSection("Skills", { dialog = ProfileDialog.SkillForm(null) }, "add_Skills") {
                            if (profile.skills.isEmpty()) EmptyHint("Nada cadastrado ainda.")
                            profile.skills.forEach { skill ->
                                val years = skill.yearsExperience?.let { " · $it anos" } ?: profile.summary?.yearsBySkill?.get(skill.skillName)
                                    ?.let { " · $it anos (deduzido)" } ?: ""
                                ItemRow(
                                    title = skill.skillName,
                                    subtitle = (SkillLevel.entries.firstOrNull { it.name == skill.skillLevel }?.label ?: skill.skillLevel) + years,
                                    source = skill.source,
                                    onEdit = { dialog = ProfileDialog.SkillForm(skill) },
                                    onDelete = { dialog = ProfileDialog.Delete("a skill ${skill.skillName}") { cb -> viewModel.deleteSkill(skill.id, cb) } }
                                )
                            }
                        }
                        ResumeSection("Idiomas", { dialog = ProfileDialog.LanguageForm(null) }, "add_Idiomas") {
                            if (profile.languages.isEmpty()) EmptyHint("Nada cadastrado ainda.")
                            profile.languages.forEach { language ->
                                ItemRow(
                                    title = language.languageName,
                                    subtitle = ProficiencyLevel.entries.firstOrNull { it.name == language.proficiencyLevel }?.label ?: language.proficiencyLevel,
                                    source = language.source,
                                    onEdit = { dialog = ProfileDialog.LanguageForm(language) },
                                    onDelete = { dialog = ProfileDialog.Delete("o idioma ${language.languageName}") { cb -> viewModel.deleteLanguage(language.id, cb) } }
                                )
                            }
                        }
                    }
                    val side: @Composable ColumnScope.() -> Unit = {
                        PersonalCard(profile) { dialog = ProfileDialog.Personal }
                        LinkedInCard(state.stats, onImport)
                        PreferencesCard(profile.preferences, onPreferences)
                        StatsCard(state.stats)
                        SummaryCard(profile.summary) { level -> viewModel.correctLevel(level, done("Senioridade atualizada.")) }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                        ProfileHeader(profile, onEdit = { dialog = ProfileDialog.Personal }, onResume = { dialog = ProfileDialog.Resume }, onSearchJobs = onSearchJobs)
                        Hairline()
                        if (wide) {
                            Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(40.dp), content = main)
                                Column(Modifier.width(340.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = side)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(40.dp), content = main)
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = side)
                            }
                        }
                    }
                }
            }
        }
    }

    when (val current = dialog) {
        null -> Unit
        ProfileDialog.Personal -> PersonalDialog(viewModel, profile, done("Dados pessoais salvos.")) { dialog = null }
        ProfileDialog.Resume -> profile?.let { ResumeDialog(it) { dialog = null } }
        is ProfileDialog.ExperienceForm -> ExperienceDialog(viewModel, current.item, done("Experiência salva.")) { dialog = null }
        is ProfileDialog.EducationForm -> EducationDialog(viewModel, current.item, done("Formação salva.")) { dialog = null }
        is ProfileDialog.SkillForm -> SkillDialog(viewModel, current.item, done("Skill salva.")) { dialog = null }
        is ProfileDialog.LanguageForm -> LanguageDialog(viewModel, current.item, done("Idioma salvo.")) { dialog = null }
        is ProfileDialog.Delete -> ConfirmDialog(
            "Excluir",
            "Excluir ${current.label}? O perfil deduzido e as notas serão recalculados.",
            "Excluir",
            onConfirm = { current.action(done("Excluído.")) },
            onDismiss = { dialog = null }
        )
    }
}

// Cartão "Seu perfil profissional": senioridade, área, anos, confiança e os motivos.
@Composable
private fun SummaryCard(summary: ProfileSummary?, onCorrect: (ExperienceLevel?) -> Unit) {
    Panel("Seu perfil profissional") {
        if (summary == null || (summary.totalYears == 0.0 && summary.internshipYears == 0.0 && summary.experienceLevel == null)) {
            Text("Cadastre experiências (ou importe do LinkedIn) para o motor deduzir a sua senioridade e área.",
                style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted)
            return@Panel
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Fact("Senioridade", summary.experienceLevel?.label ?: "—")
            Fact("Área principal", summary.primaryRoleFamily ?: "—")
            Fact("Anos de experiência", "${summary.totalYears}")
        }
        Text(
            "Dedução com ${summary.confidenceLabel}" + if (summary.levelSource == "MANUAL") " · senioridade definida por você" else "",
            style = MaterialTheme.typography.bodySmall,
            color = WatchJobsColors.Muted
        )
        summary.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Ink) }
        DropdownField(
            "Corrigir senioridade",
            ExperienceLevel.selectable,
            if (summary.levelSource == "MANUAL") summary.experienceLevel else null,
            { it.label },
            onCorrect,
            Modifier.widthIn(max = 280.dp),
            emptyLabel = "Usar a dedução automática",
            tag = "correctLevel"
        )
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        SectionLabel(label, color = WatchJobsColors.Muted)
        Text(value, style = MaterialTheme.typography.titleLarge, color = WatchJobsColors.Ink)
    }
}

// Cabeçalho: iniciais, nome, cargo principal, local e objetivo, com os dois atalhos.
@Composable
private fun ProfileHeader(profile: Profile, onEdit: () -> Unit, onResume: () -> Unit, onSearchJobs: () -> Unit) {
    val personal = profile.personal
    val initials = listOf(personal.firstName, personal.lastName).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    val role = profile.experiences.firstOrNull()?.jobTitle
        ?: profile.summary?.primaryRoleFamily?.lowercase()?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }
        ?: "Adicione a sua primeira experiência"
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 700.dp
        val avatar: @Composable () -> Unit = {
            Box(Modifier.size(88.dp).background(WatchJobsColors.Ink).testTag("profileAvatar"), contentAlignment = Alignment.Center) {
                Text(initials, style = MaterialTheme.typography.headlineMedium, color = WatchJobsColors.White)
            }
        }
        val info: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${personal.firstName} ${personal.lastName}", style = MaterialTheme.typography.headlineLarge, color = WatchJobsColors.Ink)
                Text(role, style = MaterialTheme.typography.titleMedium, color = WatchJobsColors.Ink)
                personal.location?.label?.ifEmpty { null }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted)
                }
                val objective = personal.objective?.takeIf { it.isNotBlank() }
                if (objective != null) {
                    Text(objective, style = MaterialTheme.typography.bodyLarge, color = WatchJobsColors.Ink, modifier = Modifier.padding(top = 6.dp))
                } else {
                    GhostButton("Adicionar objetivo profissional", onEdit, Modifier.testTag("addObjective"))
                }
            }
        }
        val buttons: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Visualizar currículo", onResume, Modifier.testTag("viewResume"))
                PrimaryButton("Buscar vagas", onSearchJobs, Modifier.testTag("profileSearchJobs"))
            }
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                avatar()
                info()
                buttons()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.Top) {
                avatar()
                Column(Modifier.weight(1f)) { info() }
                buttons()
            }
        }
    }
}

// Seção do currículo: título em caixa alta, linha fina e o conteúdo.
@Composable
private fun ResumeSection(title: String, onAdd: (() -> Unit)?, addTag: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = WatchJobsColors.Ink, modifier = Modifier.weight(1f))
            if (onAdd != null) GhostButton("+ Adicionar", onAdd, Modifier.testTag(addTag))
        }
        Hairline()
        content()
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted)
}

// Linha do tempo: período à esquerda, marcador, e cargo, empresa, atividades e tecnologias à direita.
@Composable
private fun ExperienceTimeline(
    experiences: List<Experience>,
    skills: List<UserSkill>,
    onEdit: (Experience) -> Unit,
    onDelete: (Experience) -> Unit
) {
    Column {
        experiences.forEachIndexed { index, exp ->
            val technologies = skills.map { it.skillName }.filter { mentions(exp.description, it) }.take(8)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag("experience_${exp.id}")) {
                Text(
                    timelinePeriod(exp.startDate, exp.endDate, exp.current),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (exp.current) WatchJobsColors.Red else WatchJobsColors.Muted,
                    modifier = Modifier.width(112.dp).padding(top = 2.dp)
                )
                Column(Modifier.width(24.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.padding(top = 4.dp).size(9.dp).background(if (exp.current) WatchJobsColors.Red else WatchJobsColors.Ink))
                    if (index < experiences.lastIndex) Box(Modifier.width(1.dp).weight(1f).background(WatchJobsColors.Divider))
                }
                Column(Modifier.weight(1f).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(exp.jobTitle, style = MaterialTheme.typography.titleMedium, color = WatchJobsColors.Ink)
                    Text(exp.organizationName, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted)
                    exp.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    }
                    if (technologies.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            technologies.forEach { NeutralTag(it) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (exp.source == "LINKEDIN_IMPORT") Tag("LinkedIn")
                        if (exp.source == "RESUME_IMPORT") Tag("Currículo")
                        GhostButton("Editar", { onEdit(exp) })
                        GhostButton("Excluir", { onDelete(exp) }, color = WatchJobsColors.Red)
                    }
                }
            }
        }
    }
}

// Etiqueta neutra: o vermelho fica para o match, datas e ações principais.
@Composable
private fun NeutralTag(text: String) {
    Box(Modifier.background(WatchJobsColors.Background).border(1.dp, WatchJobsColors.Divider).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Ink)
    }
}

// A skill aparece no texto como palavra inteira (para "C" ou "R" não casarem com qualquer letra).
private fun mentions(text: String?, word: String): Boolean {
    if (text.isNullOrBlank() || word.isBlank()) return false
    var from = 0
    while (true) {
        val at = text.indexOf(word, from, ignoreCase = true)
        if (at < 0) return false
        val before = text.getOrNull(at - 1)
        val after = text.getOrNull(at + word.length)
        if ((before == null || !before.isLetterOrDigit()) && (after == null || !after.isLetterOrDigit())) return true
        from = at + 1
    }
}

// "2026-03-01" -> "03/2026".
private fun monthYear(iso: String?): String {
    val parts = iso?.take(10)?.split("-") ?: return ""
    return if (parts.size == 3) "${parts[1]}/${parts[0]}" else ""
}

private fun timelinePeriod(start: String, end: String?, current: Boolean): String =
    "${monthYear(start)} – ${if (current || end == null) "hoje" else monthYear(end)}"

@Composable
private fun InfoRow(label: String, value: String, note: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SectionLabel(label, color = WatchJobsColors.Muted)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = WatchJobsColors.Ink)
        note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted) }
    }
}

@Composable
private fun PersonalCard(profile: Profile, onEdit: () -> Unit) {
    val personal = profile.personal
    Panel("Informações pessoais") {
        InfoRow("E-mail", personal.email)
        InfoRow("Localização", personal.location?.label?.ifEmpty { null } ?: "Não informada")
        InfoRow("Disponibilidade", Availability.entries.firstOrNull { it.name == personal.availability }?.label ?: "Não informada")
        InfoRow("Idiomas", profile.languages.joinToString(", ") { "${it.languageName} (${it.proficiencyLevel})" }.ifEmpty { "Não informados" })
        InfoRow(
            "Pretensão salarial",
            salaryRange(profile.preferences?.salaryMin, profile.preferences?.salaryMax, "BRL") ?: "Não informada"
        )
        GhostButton("Editar", onEdit, Modifier.testTag("editPersonal"))
    }
}

// Sem OAuth do LinkedIn disponível para perfis, o card mostra o que foi importado do arquivo de dados.
@Composable
private fun LinkedInCard(stats: ProfileStats?, onImport: () -> Unit) {
    val linkedin = stats?.linkedin
    val imported = linkedin?.imported == true
    Panel("LinkedIn") {
        InfoRow("Status", if (imported) "Dados importados" else "Nada importado")
        InfoRow("Última sincronização", linkedin?.lastImportAt?.let(::brDate)?.ifEmpty { null } ?: "—")
        InfoRow("Experiências importadas", "${linkedin?.importedExperiences ?: 0}")
        InfoRow("Skills identificadas", "${linkedin?.importedSkills ?: 0}")
        SecondaryButton(if (imported) "Importar novamente" else "Importar currículo ou LinkedIn", onImport, Modifier.fillMaxWidth().testTag("openImport"))
    }
}

@Composable
private fun ReadOnlyCheck(label: String, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(16.dp).background(if (checked) WatchJobsColors.Ink else WatchJobsColors.White)
                .border(1.dp, if (checked) WatchJobsColors.Ink else WatchJobsColors.Border),
            contentAlignment = Alignment.Center
        ) {
            if (checked) CheckIcon()
        }
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (checked) WatchJobsColors.Ink else WatchJobsColors.Muted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferencesCard(preferences: Preferences?, onEdit: () -> Unit) {
    Panel("Preferências profissionais") {
        if (preferences == null) {
            EmptyHint("Ainda não definidas. É com elas que o match calcula as notas.")
        } else {
            val anyMode = preferences.preferredWorkMode == "ANY"
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Modo de trabalho", color = WatchJobsColors.Muted)
                listOf(PreferredWorkMode.REMOTE, PreferredWorkMode.HYBRID, PreferredWorkMode.ON_SITE).forEach {
                    ReadOnlyCheck(it.label, anyMode || preferences.preferredWorkMode == it.name)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Nível desejado", color = WatchJobsColors.Muted)
                val wanted = preferences.preferredExperienceLevel
                listOf(ExperienceLevel.JUNIOR, ExperienceLevel.MID, ExperienceLevel.SENIOR).plus(listOfNotNull(wanted)).distinct().forEach {
                    ReadOnlyCheck(it.label, it == wanted)
                }
                if (wanted == null) Text("Usando o nível deduzido do perfil.", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
            }
            InfoRow("Faixa salarial", salaryRange(preferences.salaryMin, preferences.salaryMax, "BRL")?.let { range ->
                if (preferences.salaryMin != null && preferences.salaryMax != null) range.replace("–", " a ") else range
            } ?: "Não definida")
            if (preferences.desiredSkills.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Skills", color = WatchJobsColors.Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        preferences.desiredSkills.forEach { NeutralTag(it.name) }
                    }
                }
            }
        }
        SecondaryButton("Editar preferências", onEdit, Modifier.fillMaxWidth().testTag("openPreferences"))
    }
}

@Composable
private fun StatsCard(stats: ProfileStats?) {
    Panel("Estatísticas do perfil") {
        if (stats == null) {
            EmptyHint("Calculando…")
            return@Panel
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${stats.completeness}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = WatchJobsColors.Red, modifier = Modifier.testTag("profileCompleteness"))
            SectionLabel("Perfil completo", Modifier.padding(bottom = 5.dp), color = WatchJobsColors.Muted)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(WatchJobsColors.Divider)) {
            Box(Modifier.fillMaxWidth((stats.completeness / 100f).coerceIn(0f, 1f)).fillMaxHeight().background(WatchJobsColors.Red))
        }
        stats.missing.take(3).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted) }
        InfoRow("Skills", "${stats.skillCount}")
        InfoRow("Vagas compatíveis", "${stats.compatibleJobs}", "com match de ${stats.compatibleMinScore}% ou mais")
        InfoRow("Precisão do match", "${stats.matchPrecision}%", "parte dos critérios que o seu perfil permite avaliar")
    }
}

// Prévia do currículo, só para leitura.
@Composable
private fun ResumeDialog(profile: Profile, onDismiss: () -> Unit) {
    val personal = profile.personal
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = WatchJobsColors.White,
        title = { Text("${personal.firstName} ${personal.lastName}", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    listOfNotNull(personal.email, personal.location?.label?.ifEmpty { null }).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted
                )
                personal.objective?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                if (profile.experiences.isNotEmpty()) {
                    SectionLabel("Experiência")
                    profile.experiences.forEach { exp ->
                        Column {
                            Text("${exp.jobTitle} · ${exp.organizationName}", style = MaterialTheme.typography.titleSmall)
                            Text(timelinePeriod(exp.startDate, exp.endDate, exp.current), style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
                            exp.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
                if (profile.educations.isNotEmpty()) {
                    SectionLabel("Formação")
                    profile.educations.forEach { edu ->
                        Text("${edu.fieldOfStudy} · ${edu.institution} (${period(edu.startDate, edu.endDate, edu.current)})", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (profile.skills.isNotEmpty()) {
                    SectionLabel("Skills")
                    Text(profile.skills.joinToString(", ") { it.skillName }, style = MaterialTheme.typography.bodyMedium)
                }
                if (profile.languages.isNotEmpty()) {
                    SectionLabel("Idiomas")
                    Text(profile.languages.joinToString(", ") { "${it.languageName} (${it.proficiencyLevel})" }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { PrimaryButton("Fechar", onDismiss, modifier = Modifier.testTag("closeResume")) }
    )
}

@Composable
private fun ItemRow(title: String, subtitle: String, source: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().border(1.dp, WatchJobsColors.Divider).padding(10.dp)) {
        Column(Modifier.weight(1f).clickable(onClick = onEdit), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = WatchJobsColors.Ink)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
            if (source == "LINKEDIN_IMPORT") Tag("LinkedIn", Modifier.padding(top = 4.dp))
            if (source == "RESUME_IMPORT") Tag("Currículo", Modifier.padding(top = 4.dp))
        }
        GhostButton("Editar", onEdit)
        GhostButton("Excluir", onDelete, color = WatchJobsColors.Red)
    }
}

private fun brDate(iso: String?): String = iso?.let { runCatching { LocalDate.parse(it.take(10)).toBr() }.getOrNull() } ?: ""

private fun period(start: String, end: String?, current: Boolean): String =
    "${brDate(start)} a ${if (current || end == null) "hoje" else brDate(end)}"

private fun isoOrNull(text: String): String? = parseBrDate(text.trim())?.toString()

@Composable
private fun FormDialog(
    title: String,
    busy: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = WatchJobsColors.White,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                error?.let { ErrorBanner(it) }
                content()
            }
        },
        confirmButton = { PrimaryButton("Salvar", onConfirm, loading = busy, modifier = Modifier.testTag("dialogSave")) },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) }
    )
}

// Sugestões do catálogo embaixo de um campo de texto (autocompletar).
@Composable
private fun <T> Suggestions(options: List<T>, label: (T) -> String, onPick: (T) -> Unit) {
    if (options.isEmpty()) return
    Column(Modifier.fillMaxWidth().background(WatchJobsColors.Surface).border(1.dp, WatchJobsColors.Divider)) {
        options.take(6).forEach { option ->
            Text(
                label(option),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().clickable { onPick(option) }.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun PersonalDialog(viewModel: ProfileViewModel, profile: com.watchjobs.model.Profile?, onResult: (String?) -> Unit, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val personal = profile?.personal ?: return
    var first by remember { mutableStateOf(personal.firstName) }
    var last by remember { mutableStateOf(personal.lastName) }
    var city by remember { mutableStateOf(personal.location?.city.orEmpty()) }
    var region by remember { mutableStateOf(personal.location?.state.orEmpty()) }
    var country by remember { mutableStateOf(personal.location?.countryCode ?: "BR") }
    var objective by remember { mutableStateOf(personal.objective.orEmpty()) }
    var availability by remember { mutableStateOf(Availability.entries.firstOrNull { it.name == personal.availability }) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog("Dados pessoais", state.busy, error, onConfirm = {
        viewModel.savePersonal(
            PersonalRequest(
                first.trim(), last.trim(),
                location = LocationInput(country.trim().uppercase(), region.trim().ifEmpty { null }, city.trim().ifEmpty { null }),
                objective = objective.trim(),
                availability = availability?.name
            )
        ) { err -> error = err; onResult(err) }
    }, onDismiss = onDismiss) {
        LabeledTextField("Nome", first, { first = it })
        LabeledTextField("Sobrenome", last, { last = it })
        LabeledTextField("Objetivo profissional", objective, { objective = it.take(500) }, singleLine = false, minLines = 3, tag = "personalObjective")
        DropdownField("Disponibilidade", Availability.entries, availability, { it.label }, { availability = it }, emptyLabel = "Não informar", tag = "personalAvailability")
        LabeledTextField("Cidade", city, { city = it })
        LabeledTextField("Estado", region, { region = it }, placeholder = "SP")
        LabeledTextField("País (2 letras)", country, { country = it.take(2) })
    }
}

@Composable
private fun ExperienceDialog(viewModel: ProfileViewModel, item: Experience?, onResult: (String?) -> Unit, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var title by remember { mutableStateOf(item?.jobTitle.orEmpty()) }
    var company by remember { mutableStateOf(item?.organizationName.orEmpty()) }
    var type by remember { mutableStateOf(ExperienceType.entries.firstOrNull { it.name == item?.experienceType } ?: ExperienceType.EMPLOYMENT) }
    var start by remember { mutableStateOf(brDate(item?.startDate)) }
    var end by remember { mutableStateOf(brDate(item?.endDate)) }
    var current by remember { mutableStateOf(item?.current ?: false) }
    var description by remember { mutableStateOf(item?.description.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(if (item == null) "Nova experiência" else "Editar experiência", state.busy, error, onConfirm = {
        val startIso = isoOrNull(start)
        val endIso = if (current) null else isoOrNull(end)
        when {
            title.isBlank() || company.isBlank() -> error = "Informe cargo e empresa"
            startIso == null -> error = "Data de início inválida (dd/mm/aaaa)"
            !current && endIso == null -> error = "Informe a data de término ou marque como atual"
            else -> viewModel.saveExperience(
                item?.id,
                ExperienceRequest(title.trim(), company.trim(), type.name, description.trim().ifEmpty { null }, startIso, endIso, current)
            ) { err -> error = err; onResult(err) }
        }
    }, onDismiss = onDismiss) {
        LabeledTextField("Cargo", title, { title = it }, tag = "expTitle")
        LabeledTextField("Empresa", company, { company = it; viewModel.searchOrganizations(it, "COMPANY") }, tag = "expCompany")
        Suggestions(state.organizationOptions, { it.name }) { company = it.name; viewModel.searchOrganizations("") }
        DropdownField("Tipo", ExperienceType.entries, type, { it.label }, { type = it ?: ExperienceType.EMPLOYMENT }, allowEmpty = false)
        LabeledTextField("Início (dd/mm/aaaa)", start, { start = it }, tag = "expStart")
        DropdownField("Ainda trabalho aqui", listOf(true, false), current, { if (it) "Sim" else "Não" }, { current = it == true }, allowEmpty = false)
        if (!current) LabeledTextField("Término (dd/mm/aaaa)", end, { end = it }, tag = "expEnd")
        LabeledTextField("Descrição (opcional)", description, { description = it }, singleLine = false, minLines = 3)
    }
}

@Composable
private fun EducationDialog(viewModel: ProfileViewModel, item: Education?, onResult: (String?) -> Unit, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var educationId by remember { mutableStateOf(item?.educationId) }
    var institution by remember { mutableStateOf(item?.institution.orEmpty()) }
    var start by remember { mutableStateOf(brDate(item?.startDate)) }
    var end by remember { mutableStateOf(brDate(item?.endDate)) }
    var current by remember { mutableStateOf(item?.current ?: false) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(if (item == null) "Nova formação" else "Editar formação", state.busy, error, onConfirm = {
        val id = educationId
        val startIso = isoOrNull(start)
        val endIso = if (current && end.isBlank()) null else isoOrNull(end)
        when {
            id == null -> error = "Escolha a formação"
            institution.isBlank() -> error = "Informe a instituição"
            startIso == null -> error = "Data de início inválida (dd/mm/aaaa)"
            !current && endIso == null -> error = "Informe a data de término ou marque como cursando"
            else -> viewModel.saveEducation(item?.id, EducationRequest(id, institution.trim(), startIso, endIso, current)) { err -> error = err; onResult(err) }
        }
    }, onDismiss = onDismiss) {
        DropdownField("Formação", state.educations, state.educations.firstOrNull { it.id == educationId }, { it.label },
            { educationId = it?.id }, allowEmpty = false, tag = "eduOption")
        LabeledTextField("Instituição", institution, { institution = it; viewModel.searchOrganizations(it, "EDUCATIONAL_INSTITUTION") })
        Suggestions(state.organizationOptions, { it.name }) { institution = it.name; viewModel.searchOrganizations("") }
        LabeledTextField("Início (dd/mm/aaaa)", start, { start = it })
        DropdownField("Cursando", listOf(true, false), current, { if (it) "Sim" else "Não" }, { current = it == true }, allowEmpty = false)
        LabeledTextField(if (current) "Término previsto (opcional)" else "Término (dd/mm/aaaa)", end, { end = it })
    }
}

@Composable
private fun SkillDialog(viewModel: ProfileViewModel, item: UserSkill?, onResult: (String?) -> Unit, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var picked by remember { mutableStateOf(item?.let { SkillOption(it.skillId, it.skillName) }) }
    var query by remember { mutableStateOf(item?.skillName.orEmpty()) }
    var level by remember { mutableStateOf(SkillLevel.entries.firstOrNull { it.name == item?.skillLevel } ?: SkillLevel.INTERMEDIATE) }
    var years by remember { mutableStateOf(item?.yearsExperience?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(if (item == null) "Nova skill" else "Editar skill", state.busy, error, onConfirm = {
        val skill = picked
        val yearsValue = years.replace(',', '.').trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        when {
            skill == null -> error = "Escolha a skill na lista"
            years.isNotBlank() && yearsValue == null -> error = "Anos inválidos"
            else -> viewModel.saveSkill(item?.id, UserSkillRequest(skill.id, level.name, yearsValue)) { err -> error = err; onResult(err) }
        }
    }, onDismiss = onDismiss) {
        LabeledTextField("Skill", query, { query = it; picked = null; viewModel.searchSkills(it) }, placeholder = "Ex.: node", tag = "skillQuery")
        Suggestions(state.skillOptions, { it.name }) { picked = it; query = it.name; viewModel.searchSkills("") }
        picked?.let { Text("Escolhida: ${it.name}", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted) }
        DropdownField("Nível", SkillLevel.entries, level, { it.label }, { level = it ?: SkillLevel.INTERMEDIATE }, allowEmpty = false)
        LabeledTextField("Anos de experiência (opcional)", years, { years = it })
    }
}

@Composable
private fun LanguageDialog(viewModel: ProfileViewModel, item: UserLanguage?, onResult: (String?) -> Unit, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var languageId by remember { mutableStateOf(item?.languageId) }
    var level by remember { mutableStateOf(ProficiencyLevel.entries.firstOrNull { it.name == item?.proficiencyLevel } ?: ProficiencyLevel.B1) }
    var error by remember { mutableStateOf<String?>(null) }
    FormDialog(if (item == null) "Novo idioma" else "Editar idioma", state.busy, error, onConfirm = {
        val id = languageId
        if (id == null) error = "Escolha o idioma"
        else viewModel.saveLanguage(item?.id, UserLanguageRequest(id, level.name)) { err -> error = err; onResult(err) }
    }, onDismiss = onDismiss) {
        DropdownField("Idioma", state.languages, state.languages.firstOrNull { it.id == languageId }, { it.name }, { languageId = it?.id }, allowEmpty = false)
        DropdownField("Nível", ProficiencyLevel.entries, level, { it.label }, { level = it ?: ProficiencyLevel.B1 }, allowEmpty = false)
    }
}
