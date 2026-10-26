package com.watchjobs.ui.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
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
import com.watchjobs.ui.components.DropdownField
import com.watchjobs.ui.components.EmptyState
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.LabeledTextField
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageHeader
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SearchBar
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.components.SectionHint
import com.watchjobs.ui.components.Tag
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.util.salaryRange
import com.watchjobs.viewmodel.JobSearchViewModel

private val pageWidth = 1200.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobSearchScreen(
    viewModel: JobSearchViewModel,
    showMessage: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val query = state.query
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                        "Para guardar uma vaga por mais tempo, use \"Salvar\": ela vai para Vagas salvas e não expira."
                )
                SearchBar(
                    value = query.text,
                    onValueChange = viewModel::onTextChange,
                    placeholder = "Cargo ou empresa (ex.: \"engineer\", \"kotlin\", \"vtex\")"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 4
                ) {
                    val field = Modifier.widthIn(min = 180.dp).weight(1f)
                    LabeledTextField(
                        label = "Local",
                        value = query.location,
                        onValueChange = viewModel::onLocationChange,
                        placeholder = "Cidade, estado ou país",
                        modifier = field,
                        tag = "jobSearchLocation"
                    )
                    DropdownField(
                        label = "Modo",
                        options = WorkMode.selectable,
                        selected = query.workMode,
                        optionLabel = { it.label },
                        onSelect = viewModel::onWorkModeChange,
                        emptyLabel = "Todos",
                        modifier = field,
                        tag = "jobSearchWorkMode"
                    )
                    DropdownField(
                        label = "Nível",
                        options = ExperienceLevel.selectable,
                        selected = query.level,
                        optionLabel = { it.label },
                        onSelect = viewModel::onLevelChange,
                        emptyLabel = "Todos",
                        modifier = field,
                        tag = "jobSearchLevel"
                    )
                    DropdownField(
                        label = "Contrato",
                        options = JobType.selectable,
                        selected = query.jobType,
                        optionLabel = { it.label },
                        onSelect = viewModel::onJobTypeChange,
                        emptyLabel = "Todos",
                        modifier = field,
                        tag = "jobSearchJobType"
                    )
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
                        onOpen = {
                            runCatching { uriHandler.openUri(job.jobUrl) }
                                .onFailure { showMessage("Não foi possível abrir o link da vaga.") }
                        },
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

// Cartão no estilo das vagas salvas: empresa, local e fonte em cima; cargo em destaque; etiquetas e ações.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobPostingCard(
    job: JobPosting,
    saving: Boolean,
    onOpen: () -> Unit,
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
                SecondaryButton("Ver vaga", onOpen, modifier = Modifier.testTag("openJob"))
                PrimaryButton(
                    if (job.alreadySaved) "Salva" else "Salvar",
                    onSave,
                    enabled = !job.alreadySaved,
                    loading = saving,
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
