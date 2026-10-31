package com.watchjobs.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchjobs.model.AppNotification
import com.watchjobs.ui.components.DropdownField
import com.watchjobs.ui.components.EmptyState
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.GhostButton
import com.watchjobs.ui.components.Hairline
import com.watchjobs.ui.components.LabeledTextField
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageContainer
import com.watchjobs.ui.components.PageHeader
import com.watchjobs.ui.components.Panel
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.util.elapsedLabel
import com.watchjobs.viewmodel.NotificationCenterState
import com.watchjobs.viewmodel.NotificationsViewModel

// Sininho (F5-07): a lista de avisos e as preferências de notificação.
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onOpen: (AppNotification) -> Unit,
    onCountChanged: () -> Unit,
    showMessage: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PageContainer(Modifier.padding(top = 20.dp)) {
                PageHeader("Avisos", "Vagas novas, buscas concluídas, entrevistas e lembretes das suas candidaturas") {
                    GhostButton("Marcar todos como lidos", { viewModel.markAllRead(onCountChanged) }, Modifier.testTag("markAllReadPage"))
                }
            }
        }
        item { PageContainer { PreferencePanel(viewModel, showMessage) } }
        when {
            state.loading -> item { LoadingBox() }
            state.error != null -> item { PageContainer { ErrorBanner(state.error ?: "") } }
            state.items.isEmpty() -> item {
                PageContainer { EmptyState("Nenhum aviso ainda", "Quando aparecer uma vaga com nota acima da sua nota mínima, ela chega aqui.") }
            }
            else -> items(state.items, key = { it.id }) { notification ->
                PageContainer {
                    Box(Modifier.border(1.dp, WatchJobsColors.Border)) {
                        NotificationItem(notification) {
                            viewModel.markRead(notification.id, onCountChanged)
                            onOpen(notification)
                        }
                    }
                }
            }
        }
        item { Box(Modifier.padding(bottom = 24.dp)) }
    }
}

// Menu suspenso do sininho: os cinco avisos mais recentes, "marcar todas como lidas" e "Ver mais".
@Composable
fun NotificationMenu(
    state: NotificationCenterState,
    onOpen: (AppNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    onSeeMore: () -> Unit
) {
    Column(Modifier.fillMaxWidth().testTag("notificationMenu")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("NOTIFICAÇÕES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = WatchJobsColors.Ink, modifier = Modifier.weight(1f))
            Text(
                "MARCAR TODAS COMO LIDAS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (state.unread > 0) WatchJobsColors.Red else WatchJobsColors.Border,
                modifier = Modifier.clickable(enabled = state.unread > 0, onClick = onMarkAllRead).padding(4.dp).testTag("markAllRead")
            )
        }
        Hairline()
        when {
            state.loading && state.recent.isEmpty() -> LoadingBox()
            state.error != null && state.recent.isEmpty() -> Text(state.error, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.RedDark, modifier = Modifier.padding(16.dp))
            state.recent.isEmpty() -> Text("Nenhum aviso por enquanto.", style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted, modifier = Modifier.padding(16.dp))
            else -> state.recent.forEach { notification ->
                NotificationItem(notification) { onOpen(notification) }
                Hairline()
            }
        }
        Box(Modifier.fillMaxWidth().clickable(onClick = onSeeMore).padding(vertical = 12.dp).testTag("notificationsSeeMore"), contentAlignment = Alignment.Center) {
            Text("Ver mais", style = MaterialTheme.typography.labelLarge, color = WatchJobsColors.Ink)
        }
    }
}

// Item: barra colorida à esquerda (vermelha se não lido), categoria, tempo decorrido e o resumo.
@Composable
fun NotificationItem(notification: AppNotification, onClick: () -> Unit) {
    val unread = notification.unread
    val accent = if (unread) WatchJobsColors.Red else WatchJobsColors.Border
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .background(if (unread) WatchJobsColors.White else WatchJobsColors.Background)
            .clickable(onClick = onClick)
            .semantics { stateDescription = if (unread) "Não lido" else "Lido" }
            .testTag("notification_${notification.id}")
    ) {
        Box(Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp).width(3.dp).fillMaxHeight().background(accent))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (unread) WatchJobsColors.Red else WatchJobsColors.Muted,
                    modifier = Modifier.weight(1f)
                )
                Text(elapsedLabel(notification.createdAt), style = MaterialTheme.typography.labelSmall, color = WatchJobsColors.Muted)
            }
            Text(
                notification.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                color = if (unread) WatchJobsColors.Ink else WatchJobsColors.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            notification.body?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PreferencePanel(viewModel: NotificationsViewModel, showMessage: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    val preference = state.preference ?: return
    var minScoreText by remember(preference.minScore) { mutableStateOf(preference.minScore.toInt().toString()) }
    Panel("Preferências de aviso") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledTextField("Nota mínima (0 a 100)", minScoreText, { value ->
                minScoreText = value.filter { it.isDigit() }.take(3)
                minScoreText.toDoubleOrNull()?.let { score -> viewModel.updatePreference { it.copy(minScore = score) } }
            }, Modifier.widthIn(max = 200.dp), tag = "minScore")
            DropdownField("Avisos no app", listOf(true, false), preference.inAppEnabled, { if (it) "Ligados" else "Desligados" },
                { value -> viewModel.updatePreference { it.copy(inAppEnabled = value ?: true) } }, Modifier.widthIn(max = 200.dp), allowEmpty = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DropdownField("E-mail", listOf(true, false), preference.emailEnabled, { if (it) "Ligado" else "Desligado" },
                { value -> viewModel.updatePreference { it.copy(emailEnabled = value ?: false) } }, Modifier.widthIn(max = 200.dp), allowEmpty = false)
            DropdownField("Frequência", listOf("IMMEDIATE", "DAILY"), preference.emailFrequency,
                { if (it == "IMMEDIATE") "Imediato" else "Resumo diário (8 h)" },
                { value -> viewModel.updatePreference { it.copy(emailFrequency = value ?: "DAILY") } },
                Modifier.widthIn(max = 240.dp), allowEmpty = false, enabled = preference.emailEnabled)
            DropdownField("Push no celular", listOf(true, false), preference.pushEnabled, { if (it) "Ligado" else "Desligado" },
                { value -> viewModel.updatePreference { it.copy(pushEnabled = value ?: true) } }, Modifier.widthIn(max = 200.dp), allowEmpty = false, tag = "pushEnabled")
        }
        PrimaryButton("Salvar preferências", { viewModel.savePreference(showMessage) }, loading = state.savingPreference)
    }
}
