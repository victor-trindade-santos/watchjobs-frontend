package com.watchjobs.ui.application

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchjobs.model.Application
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.ui.components.ConfirmDialog
import com.watchjobs.ui.components.EmptyState
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.Hairline
import com.watchjobs.ui.components.KpiStrip
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageContainer
import com.watchjobs.ui.components.PageHeader
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SearchBar
import com.watchjobs.ui.components.SectionHint
import com.watchjobs.ui.components.SectionLabel
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.util.toShortWeekday
import com.watchjobs.viewmodel.ApplicationListViewModel

// As colunas do quadro, como no My Applications. Os status em andamento ficam com o sublinhado vermelho.
private class BoardColumn(val title: String, val statuses: Set<SavedJobStatus>, val highlighted: Boolean)

private val boardColumns = listOf(
    BoardColumn("Candidatadas", setOf(SavedJobStatus.APPLIED), false),
    BoardColumn("Em processo", setOf(SavedJobStatus.IN_PROCESS), true),
    BoardColumn("Propostas", setOf(SavedJobStatus.OFFER_RECEIVED, SavedJobStatus.ACCEPTED), true),
    BoardColumn("Encerradas", setOf(SavedJobStatus.REJECTED, SavedJobStatus.WITHDRAWN), false)
)

@Composable
fun ApplicationListScreen(
    viewModel: ApplicationListViewModel,
    onNew: () -> Unit,
    onEdit: (Int) -> Unit,
    showMessage: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val inProcess = state.items.count { it.status == SavedJobStatus.IN_PROCESS.name }
    val withInterview = state.items.count { it.interviewAt != null }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp)) {
        PageContainer {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                PageHeader(
                    title = "Candidaturas",
                    subtitle = "Toque em um cartão para editar. $inProcess em processo · $withInterview com entrevista marcada"
                ) {
                    PrimaryButton("+ Nova candidatura", onNew, modifier = Modifier.testTag("newApplication"))
                }
                SectionHint(
                    "Candidatura é o processo seletivo de uma vaga salva: etapa, status, entrevistas e feedback. " +
                        "Cada vaga tem no máximo uma candidatura, e o status escolhido aqui também atualiza a vaga."
                )
                KpiStrip(
                    listOf(
                        Triple("Candidaturas", state.items.size.toString(), null),
                        Triple("Em processo", inProcess.toString(), null),
                        Triple("Entrevistas", withInterview.toString(), "marcadas"),
                        Triple("Propostas", state.items.count { it.status in setOf("OFFER_RECEIVED", "ACCEPTED") }.toString(), null)
                    )
                )
                SearchBar(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Buscar em notas, feedback, cargo ou empresa"
                )
                state.error?.let { ErrorBanner(it) }
                when {
                    state.loading && state.items.isEmpty() -> LoadingBox()
                    state.items.isEmpty() -> EmptyState(
                        title = if (state.query.isBlank()) "Nenhuma candidatura ainda" else "Nada encontrado",
                        message = if (state.query.isBlank()) "Quando se candidatar a uma vaga salva, registre aqui." else "Nenhuma candidatura com \"${state.query}\"."
                    ) {
                        if (state.query.isBlank()) PrimaryButton("+ Nova candidatura", onNew)
                    }
                    else -> Board(state.items, onEdit, viewModel::requestDelete)
                }
            }
        }
    }

    state.pendingDelete?.let { application ->
        ConfirmDialog(
            title = "Excluir candidatura?",
            message = "A candidatura para \"${application.positionRole}\" será excluída e a vaga voltará para Salva.",
            confirmText = "Excluir",
            onConfirm = { viewModel.confirmDelete(showMessage) },
            onDismiss = viewModel::cancelDelete
        )
    }
}

@Composable
private fun Board(items: List<Application>, onEdit: (Int) -> Unit, onDelete: (Application) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 600.dp) {
            // No celular, as colunas viram seções empilhadas.
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                boardColumns.forEach { column ->
                    BoardColumnContent(column, items, onEdit, onDelete, Modifier.fillMaxWidth())
                }
            }
            return@BoxWithConstraints
        }
        val fits = maxWidth >= 900.dp
        val rowModifier = if (fits) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState())
        Row(rowModifier.height(IntrinsicSize.Min).border(1.dp, WatchJobsColors.Border).background(WatchJobsColors.Background)) {
            boardColumns.forEachIndexed { index, column ->
                if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(WatchJobsColors.Border))
                BoardColumnContent(
                    column, items, onEdit, onDelete,
                    (if (fits) Modifier.weight(1f) else Modifier.width(270.dp)).padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun BoardColumnContent(
    column: BoardColumn,
    items: List<Application>,
    onEdit: (Int) -> Unit,
    onDelete: (Application) -> Unit,
    modifier: Modifier
) {
    val cards = items.filter { item -> column.statuses.any { it.name == item.status } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(column.title, Modifier.weight(1f))
                Text(cards.size.toString(), style = MaterialTheme.typography.labelMedium, color = WatchJobsColors.Muted)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(2.dp).background(if (column.highlighted) WatchJobsColors.Red else WatchJobsColors.Border))
        }
        cards.forEach { application ->
            ApplicationCard(application, { application.id?.let(onEdit) }, { onDelete(application) })
        }
        if (cards.isEmpty()) {
            Text("Nenhuma", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
        }
    }
}

// Cartão no estilo do My Applications: empresa, cargo, linha fina, etapa e a próxima entrevista em vermelho.
@Composable
private fun ApplicationCard(application: Application, onClick: () -> Unit, onDelete: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(WatchJobsColors.Surface)
            .border(1.dp, WatchJobsColors.Border)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)
            .testTag("applicationCard"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                application.organizationName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = WatchJobsColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).testTag("deleteApplication")) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = WatchJobsColors.Muted, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            application.positionRole,
            style = MaterialTheme.typography.titleMedium,
            color = WatchJobsColors.Ink,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            SavedJobStatus.labelOf(application.status),
            style = MaterialTheme.typography.bodySmall,
            color = WatchJobsColors.Muted
        )
        Hairline(Modifier.padding(end = 8.dp, top = 2.dp))
        Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                application.stageName,
                style = MaterialTheme.typography.bodySmall,
                color = WatchJobsColors.Muted,
                modifier = Modifier.weight(1f)
            )
            Text(
                application.interviewAt?.toShortWeekday() ?: "—",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (application.interviewAt != null) WatchJobsColors.Red else WatchJobsColors.Muted
            )
        }
    }
}
