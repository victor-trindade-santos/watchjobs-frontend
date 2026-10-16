package com.watchjobs.ui.savedjob

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.watchjobs.model.SavedJob
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.ui.components.ConfirmDialog
import com.watchjobs.ui.components.EmptyState
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.GhostButton
import com.watchjobs.ui.components.KpiStrip
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageHeader
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SearchBar
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.components.SectionHint
import com.watchjobs.ui.components.Tag
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.util.salaryRange
import com.watchjobs.util.toBr
import com.watchjobs.viewmodel.SavedJobListViewModel

private val activeStatuses = setOf(SavedJobStatus.APPLIED, SavedJobStatus.IN_PROCESS, SavedJobStatus.OFFER_RECEIVED)

@Composable
fun SavedJobListScreen(
    viewModel: SavedJobListViewModel,
    onNew: () -> Unit,
    onEdit: (Long) -> Unit,
    onApply: (Long) -> Unit,
    showMessage: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val applied = state.items.count { it.id in state.appliedJobIds }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(Modifier.widthIn(max = 1200.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PageHeader(
                    title = "Vagas salvas",
                    subtitle = "${state.items.size} ${if (state.items.size == 1) "vaga" else "vagas"} · $applied com candidatura"
                ) {
                    PrimaryButton("+ Nova vaga", onNew, modifier = Modifier.testTag("newSavedJob"))
                }
                SectionHint(
                    "Vaga salva é uma oportunidade que chamou sua atenção, mesmo antes de você se candidatar. " +
                        "Quando se candidatar, use \"Candidatar-se\": o processo seletivo é acompanhado na aba Candidaturas."
                )
                KpiStrip(
                    listOf(
                        Triple("Vagas salvas", state.items.size.toString(), null),
                        Triple("Com candidatura", applied.toString(), null),
                        Triple("Em andamento", state.items.count { it.status in activeStatuses }.toString(), "ativas")
                    )
                )
                SearchBar(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Buscar por cargo ou empresa (ex.: \"kotlin\", \"acme\")"
                )
                state.error?.let { ErrorBanner(it) }
                Spacer(Modifier.height(4.dp))
            }
        }
        when {
            state.loading && state.items.isEmpty() -> item { LoadingBox() }
            state.items.isEmpty() -> item {
                EmptyState(
                    title = if (state.query.isBlank()) "Nenhuma vaga salva ainda" else "Nada encontrado",
                    message = if (state.query.isBlank()) "Salve a primeira vaga que chamou sua atenção." else "Nenhuma vaga com \"${state.query}\" no cargo ou na empresa.",
                    modifier = Modifier.widthIn(max = 1200.dp)
                ) {
                    if (state.query.isBlank()) PrimaryButton("+ Nova vaga", onNew)
                }
            }
            else -> items(state.items, key = { it.id ?: 0L }) { job ->
                SavedJobRow(
                    job = job,
                    hasApplication = job.id in state.appliedJobIds,
                    onEdit = { job.id?.let(onEdit) },
                    onDelete = { viewModel.requestDelete(job) },
                    onApply = { job.id?.let(onApply) },
                    modifier = Modifier.widthIn(max = 1200.dp)
                )
            }
        }
    }

    state.pendingDelete?.let { job ->
        ConfirmDialog(
            title = "Excluir vaga?",
            message = "\"${job.positionRole}\" na ${job.organizationName} será excluída" +
                if (job.id in state.appliedJobIds) ", junto com a candidatura." else ".",
            confirmText = "Excluir",
            onConfirm = { viewModel.confirmDelete(showMessage) },
            onDismiss = viewModel::cancelDelete
        )
    }
}

// Linha da lista no estilo da Home: empresa e local em cima, cargo em destaque, etiquetas e ações à direita.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedJobRow(
    job: SavedJob,
    hasApplication: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .background(WatchJobsColors.White)
            .border(0.5.dp, WatchJobsColors.Divider)
            .clickable(onClick = onEdit)
            .padding(20.dp)
            .testTag("savedJobRow")
    ) {
        val compact = maxWidth < 640.dp
        val info: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WatchJobsColors.Ink)) { append(job.organizationName) }
                        val place = listOfNotNull(
                            listOfNotNull(job.locationCity, job.locationState).joinToString(", ").ifEmpty { null },
                            job.workMode?.label
                        ).joinToString(" · ")
                        if (place.isNotEmpty()) append("   |   $place")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = WatchJobsColors.Muted
                )
                Text(job.positionRole, style = MaterialTheme.typography.titleLarge, color = WatchJobsColors.Ink)
                job.notes?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(job.status.label, filled = job.status in activeStatuses)
                    job.experienceLevel?.let { Tag(it.label) }
                    job.jobType?.let { Tag(it.label) }
                    if (hasApplication) Tag("Com candidatura")
                }
            }
        }
        val side: @Composable () -> Unit = {
            Column(horizontalAlignment = if (compact) Alignment.Start else Alignment.End) {
                Text(
                    salaryRange(job.salaryMin, job.salaryMax, job.salaryCurrency) ?: "Salário não informado",
                    style = MaterialTheme.typography.titleSmall,
                    color = WatchJobsColors.Ink
                )
                job.savedAt?.let {
                    Text("Salva em ${it.date.toBr()}", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
                }
            }
        }
        val actions: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!hasApplication && job.status in setOf(SavedJobStatus.SAVED, SavedJobStatus.PLANNING_TO_APPLY)) {
                    GhostButton("Candidatar-se", onApply, color = WatchJobsColors.Red)
                }
                PrimaryButton("Editar", onEdit)
                SecondaryButton("Excluir", onDelete, modifier = Modifier.testTag("deleteSavedJob"))
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
