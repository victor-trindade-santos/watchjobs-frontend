package com.watchjobs.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.watchjobs.model.AppNotification
import com.watchjobs.ui.components.DropdownField
import com.watchjobs.ui.components.EmptyState
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.GhostButton
import com.watchjobs.ui.components.LabeledTextField
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageContainer
import com.watchjobs.ui.components.PageHeader
import com.watchjobs.ui.components.Panel
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.viewmodel.NotificationsViewModel

// Sininho (F5-07): a lista de avisos e as preferências de notificação.
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onOpenSearch: (Long) -> Unit,
    onOpenFeed: () -> Unit,
    onCountChanged: () -> Unit,
    showMessage: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PageContainer(Modifier.padding(top = 20.dp)) {
                PageHeader("Avisos", "Vagas novas com nota alta e buscas concluídas") {
                    GhostButton("Marcar todos como lidos", { viewModel.markAllRead(onCountChanged) })
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
                    NotificationRow(notification) {
                        viewModel.markRead(notification.id, onCountChanged)
                        val searchId = notification.searchRequestId
                        if (notification.type == "SEARCH_COMPLETED" && searchId != null) onOpenSearch(searchId) else onOpenFeed()
                    }
                }
            }
        }
        item { Box(Modifier.padding(bottom = 24.dp)) }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    val unread = notification.readAt == null
    Row(
        Modifier.fillMaxWidth().background(WatchJobsColors.White).border(1.dp, if (unread) WatchJobsColors.Red else WatchJobsColors.Border)
            .clickable(onClick = onClick).padding(14.dp).testTag("notification_${notification.id}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.padding(top = 6.dp).size(8.dp).background(if (unread) WatchJobsColors.Red else WatchJobsColors.Divider))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(notification.title, style = MaterialTheme.typography.titleSmall, color = WatchJobsColors.Ink)
            notification.body?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted) }
            Text(notification.createdAt.take(16).replace('T', ' '), style = MaterialTheme.typography.labelSmall, color = WatchJobsColors.Muted)
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
        }
        PrimaryButton("Salvar preferências", { viewModel.savePreference(showMessage) }, loading = state.savingPreference)
    }
}
