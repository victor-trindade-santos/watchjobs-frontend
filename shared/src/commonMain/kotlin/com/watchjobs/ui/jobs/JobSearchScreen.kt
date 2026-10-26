package com.watchjobs.ui.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.JobPosting
import com.watchjobs.model.JobType
import com.watchjobs.model.WorkMode
import com.watchjobs.ui.components.EmptyState
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.Hairline
import com.watchjobs.ui.components.LabeledTextField
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageHeader
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SearchBar
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.components.SectionHint
import com.watchjobs.ui.components.SectionLabel
import com.watchjobs.ui.components.Tag
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.util.salaryRange
import com.watchjobs.viewmodel.JobSearchViewModel

private val pageWidth = 1000.dp

@Composable
fun JobSearchScreen(
    viewModel: JobSearchViewModel,
    onView: (Long) -> Unit,
    showMessage: (String) -> Unit
) {
    // Telas largas: filtros fixos na lateral esquerda. Telas estreitas: filtros no topo da lista.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 900.dp) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(WatchJobsColors.White)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 28.dp)
                        .testTag("jobSearchFilters")
                ) {
                    JobFilters(viewModel)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(WatchJobsColors.Divider))
                JobResults(viewModel, onView, showMessage, filtersInline = false, modifier = Modifier.weight(1f))
            }
        } else {
            JobResults(viewModel, onView, showMessage, filtersInline = true, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun JobResults(
    viewModel: JobSearchViewModel,
    onView: (Long) -> Unit,
    showMessage: (String) -> Unit,
    filtersInline: Boolean,
    modifier: Modifier
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(Modifier.widthIn(max = pageWidth).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PageHeader(
                    title = "Buscar vagas",
                    subtitle = "${state.total} ${if (state.total == 1L) "vaga encontrada" else "vagas encontradas"} nas fontes coletadas"
                )
                SectionHint(
                    "As vagas vêm das fontes coletadas pelo WatchJobs e ficam aqui por 30 dias a partir da descoberta. " +
                        "Use \"Visualizar\" para ler a vaga completa e \"Salvar\" para guardá-la em Vagas salvas, onde ela não expira."
                )
                SearchBar(
                    value = state.query.text,
                    onValueChange = viewModel::onTextChange,
                    placeholder = "Cargo ou empresa (ex.: \"engineer\", \"kotlin\", \"vtex\")"
                )
                if (filtersInline) {
                    Column(
                        Modifier.fillMaxWidth().background(WatchJobsColors.White).border(1.dp, WatchJobsColors.Border).padding(18.dp)
                    ) {
                        JobFilters(viewModel)
                    }
                }
                state.error?.let { ErrorBanner(it) }
                Spacer(Modifier.height(4.dp))
            }
        }
        when {
            state.loading && state.items.isEmpty() -> item { LoadingBox() }
            state.items.isEmpty() -> item {
                EmptyState(
                    title = "Nenhuma vaga encontrada",
                    message = "Tente outro termo ou tire algum filtro. O corpus é atualizado algumas vezes por dia.",
                    modifier = Modifier.widthIn(max = pageWidth)
                )
            }
            else -> {
                items(state.items, key = { it.id }) { job ->
                    JobPostingCard(
                        job = job,
                        saving = job.id in state.savingIds,
                        onView = { onView(job.id) },
                        onSave = { viewModel.save(job, showMessage) },
                        modifier = Modifier.widthIn(max = pageWidth).padding(bottom = 12.dp)
                    )
                }
                if (state.hasMore) {
                    item {
                        SecondaryButton(
                            if (state.loadingMore) "Carregando..." else "Carregar mais",
                            viewModel::loadMore,
                            enabled = !state.loadingMore,
                            modifier = Modifier.padding(top = 8.dp).testTag("jobSearchMore")
                        )
                    }
                }
            }
        }
    }
}

// Filtros da busca, no estilo do painel "FILTERS" do protótipo: local, modo (lista única), nível e contrato (etiquetas).
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobFilters(viewModel: JobSearchViewModel) {
    val state by viewModel.state.collectAsState()
    val query = state.query
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Text("FILTROS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = WatchJobsColors.Ink)
        LabeledTextField(
            label = "Local",
            value = query.location,
            onValueChange = viewModel::onLocationChange,
            placeholder = "Cidade, estado ou país",
            tag = "jobSearchLocation"
        )
        FilterGroup("Modo de trabalho") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RadioOption("Qualquer", query.workMode == null, "workMode_ANY") { viewModel.onWorkModeChange(null) }
                WorkMode.selectable.forEach { mode ->
                    RadioOption(mode.label, query.workMode == mode, "workMode_${mode.name}") { viewModel.onWorkModeChange(mode) }
                }
            }
        }
        FilterGroup("Nível") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ExperienceLevel.selectable.forEach { level ->
                    val selected = query.level == level
                    ChoiceChip(level.label, selected, "level_${level.name}") { viewModel.onLevelChange(if (selected) null else level) }
                }
            }
        }
        FilterGroup("Contrato") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                JobType.selectable.forEach { type ->
                    val selected = query.jobType == type
                    ChoiceChip(type.label, selected, "jobType_${type.name}") { viewModel.onJobTypeChange(if (selected) null else type) }
                }
            }
        }
        Hairline()
        SecondaryButton("Limpar filtros", viewModel::resetFilters, Modifier.fillMaxWidth().testTag("resetFilters"))
    }
}

@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(title, color = WatchJobsColors.Muted)
        content()
    }
}

@Composable
private fun RadioOption(text: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(14.dp).border(1.dp, if (selected) WatchJobsColors.Red else WatchJobsColors.Border).background(WatchJobsColors.White),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(8.dp).background(WatchJobsColors.Red))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink)
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (selected) WatchJobsColors.Ink else WatchJobsColors.White)
            .border(1.dp, if (selected) WatchJobsColors.Ink else WatchJobsColors.Border)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = if (selected) WatchJobsColors.White else WatchJobsColors.Ink)
    }
}

// Cartão no estilo das vagas salvas: empresa, local e fonte em cima; cargo em destaque; etiquetas e ações.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobPostingCard(
    job: JobPosting,
    saving: Boolean,
    onView: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .background(WatchJobsColors.White)
            .border(0.5.dp, WatchJobsColors.Divider)
            .padding(20.dp)
            .testTag("jobPostingCard")
    ) {
        val compact = maxWidth < 640.dp
        val info: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WatchJobsColors.Ink)) { append(job.organizationName) }
                        val place = listOfNotNull(job.placeLabel.ifEmpty { null }, job.workMode?.label).joinToString(" · ")
                        if (place.isNotEmpty()) append("   |   $place")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = WatchJobsColors.Muted
                )
                Text(job.title, style = MaterialTheme.typography.titleLarge, color = WatchJobsColors.Ink)
                job.descriptionSnippet?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(job.expiresLabel, filled = job.expiresInDays <= 3)
                    job.experienceLevel?.takeIf { it != ExperienceLevel.UNKNOWN }?.let { Tag(it.label) }
                    job.jobType?.takeIf { it != JobType.UNKNOWN }?.let { Tag(it.label) }
                    if (job.alreadySaved) Tag("Salva")
                }
                Text("via ${job.sourceName}", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
            }
        }
        val side: @Composable () -> Unit = {
            Text(
                salaryRange(job.salaryMin, job.salaryMax, job.salaryCurrency) ?: "Salário não informado",
                style = MaterialTheme.typography.titleSmall,
                color = WatchJobsColors.Ink
            )
        }
        val actions: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton("Visualizar", onView, modifier = Modifier.testTag("viewJob"))
                SecondaryButton(
                    when {
                        job.alreadySaved -> "Salva"
                        saving -> "Salvando..."
                        else -> "Salvar"
                    },
                    onSave,
                    enabled = !job.alreadySaved && !saving,
                    modifier = Modifier.testTag("saveJob")
                )
            }
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                info()
                side()
                actions()
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { info() }
                Spacer(Modifier.width(20.dp))
                side()
                Spacer(Modifier.width(20.dp))
                actions()
            }
        }
    }
}
