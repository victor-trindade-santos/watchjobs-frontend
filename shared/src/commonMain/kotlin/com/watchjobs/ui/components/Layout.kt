package com.watchjobs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.watchjobs.ui.onboarding.tourTarget
import com.watchjobs.ui.theme.ThemeMode
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import com.watchjobs.ui.theme.WatchJobsColors

enum class MainTab(val label: String) {
    JOB_SEARCH("Buscar vagas"),
    SAVED_JOBS("Vagas salvas"),
    APPLICATIONS("Candidaturas"),
    ANALYTICS("Analytics"),
    PROFILE("Perfil")
}

// Opções do menu do usuário (avatar + seta na navbar).
enum class UserMenuAction(val label: String, val icon: ImageVector) {
    PROFILE("Perfil & Experiência", Icons.Default.Person),
    PREFERENCES("Preferências", Icons.Default.Settings),
    LINKEDIN_IMPORT("Importar LinkedIn", Icons.Default.Add),
    DOCUMENTS("Currículos e cartas", Icons.Default.Edit),
    TOUR("Rever o tour guiado", Icons.Default.Info),
    DELETE_ACCOUNT("Excluir conta", Icons.Default.Delete),
    LOGOUT("Sair", Icons.AutoMirrored.Filled.ExitToApp);

    companion object {
        val standard = listOf(PROFILE, PREFERENCES, LINKEDIN_IMPORT, DOCUMENTS, TOUR, LOGOUT)
    }
}

// Barra de topo das telas internas, no estilo dos protótipos: logo, abas com sublinhado vermelho e o usuário.
@Composable
fun MainScaffold(
    initials: String,
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onUserMenu: (UserMenuAction) -> Unit,
    userMenu: List<UserMenuAction> = UserMenuAction.standard,
    tabs: List<MainTab> = MainTab.entries,
    unreadCount: Long? = null,
    notifications: (@Composable (close: () -> Unit) -> Unit)? = null,
    onNotifications: () -> Unit = {},
    onToggleTheme: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(WatchJobsColors.Background)) {
        BoxWithConstraints(Modifier.fillMaxWidth().background(WatchJobsColors.White)) {
            val compact = maxWidth < 720.dp
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = if (compact) 16.dp else 28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WatchJobsLogo()
                    if (!compact) {
                        Spacer(Modifier.width(36.dp))
                        Tabs(tabs, currentTab, onTabSelected, Modifier.fillMaxHeight())
                    }
                    Spacer(Modifier.weight(1f))
                    if (onToggleTheme != null) {
                        ThemeToggle(onToggleTheme)
                        Spacer(Modifier.width(10.dp))
                    }
                    if (unreadCount != null) {
                        NotificationBell(unreadCount, onNotifications, notifications)
                        Spacer(Modifier.width(10.dp))
                    }
                    UserMenu(initials, userMenu, onUserMenu)
                }
                if (compact) {
                    Tabs(
                        tabs,
                        currentTab,
                        onTabSelected,
                        Modifier.height(44.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                    )
                }
                Hairline()
            }
        }
        content()
    }
}

// Alterna claro/escuro. Mostra o tema para o qual vai mudar: lua no claro, sol no escuro.
@Composable
private fun ThemeToggle(onToggle: () -> Unit) {
    val dark = WatchJobsColors.mode == ThemeMode.DARK
    val label = if (dark) "Usar tema claro" else "Usar tema escuro"
    Box(
        Modifier.size(36.dp)
            .clickable(onClick = onToggle)
            .testTag("themeToggle")
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        val ink = WatchJobsColors.Ink
        val cutout = WatchJobsColors.White
        Canvas(Modifier.size(20.dp)) {
            val c = center
            val r = size.minDimension
            if (dark) {
                drawCircle(ink, radius = r * 0.2f, center = c)
                repeat(8) { i ->
                    val angle = i * (kotlin.math.PI / 4)
                    val dx = kotlin.math.cos(angle).toFloat()
                    val dy = kotlin.math.sin(angle).toFloat()
                    drawLine(
                        ink,
                        Offset(c.x + dx * r * 0.32f, c.y + dy * r * 0.32f),
                        Offset(c.x + dx * r * 0.46f, c.y + dy * r * 0.46f),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            } else {
                drawCircle(ink, radius = r * 0.38f, center = c)
                drawCircle(cutout, radius = r * 0.32f, center = Offset(c.x + r * 0.2f, c.y - r * 0.14f))
            }
        }
    }
}

// Sininho com o número de avisos não lidos. Com "panel", abre o menu suspenso dos avisos recentes.
@Composable
private fun NotificationBell(unread: Long, onClick: () -> Unit, panel: (@Composable (close: () -> Unit) -> Unit)?) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier.size(36.dp)
                .tourTarget("notificationBell")
                .border(1.dp, if (open) WatchJobsColors.Red else Color.Transparent)
                .clickable {
                    if (panel == null) {
                        onClick()
                    } else {
                        open = !open
                        if (open) onClick()
                    }
                }
                .testTag("notificationBell"),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Notifications, contentDescription = "Avisos", tint = WatchJobsColors.Ink)
            if (unread > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).background(WatchJobsColors.Red).padding(horizontal = 4.dp).testTag("unreadBadge"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (unread > 99) "99+" else unread.toString(), style = MaterialTheme.typography.labelSmall, color = WatchJobsColors.OnAccent)
                }
            }
        }
        if (panel != null) {
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                shape = RectangleShape,
                containerColor = WatchJobsColors.White,
                border = BorderStroke(1.dp, WatchJobsColors.Border),
                modifier = Modifier.width(380.dp)
            ) {
                panel { open = false }
            }
        }
    }
}

// Avatar com as iniciais e a seta: abre o menu com perfil, preferências, importação e sair.
@Composable
private fun UserMenu(initials: String, actions: List<UserMenuAction>, onAction: (UserMenuAction) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.tourTarget("userMenu").clickable { open = true }.testTag("userMenu").semantics { contentDescription = "Menu do usuário" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(32.dp).background(WatchJobsColors.Ink), contentAlignment = Alignment.Center) {
                Text(initials, style = MaterialTheme.typography.labelLarge, color = WatchJobsColors.White)
            }
            Box(Modifier.size(width = 24.dp, height = 32.dp).border(1.dp, WatchJobsColors.Border), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = WatchJobsColors.Ink, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RectangleShape,
            containerColor = WatchJobsColors.White,
            border = BorderStroke(1.dp, WatchJobsColors.Border)
        ) {
            actions.forEach { action ->
                if (action == UserMenuAction.LOGOUT && actions.size > 1) Hairline()
                DropdownMenuItem(
                    text = {
                        Text(
                            action.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (action == UserMenuAction.LOGOUT || action == UserMenuAction.DELETE_ACCOUNT) WatchJobsColors.RedDark else WatchJobsColors.Ink
                        )
                    },
                    leadingIcon = { Icon(action.icon, contentDescription = null, tint = WatchJobsColors.Muted, modifier = Modifier.size(18.dp)) },
                    onClick = { open = false; onAction(action) },
                    modifier = Modifier.testTag(if (action == UserMenuAction.LOGOUT) "logoutButton" else "userMenu_${action.name}")
                )
            }
        }
    }
}

@Composable
private fun Tabs(tabs: List<MainTab>, current: MainTab, onSelected: (MainTab) -> Unit, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        tabs.forEach { tab ->
            val active = tab == current
            Column(
                Modifier.fillMaxHeight().width(IntrinsicSize.Max).tourTarget("tab_${tab.name}").clickable { onSelected(tab) }.testTag("tab_${tab.name}"),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    tab.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (active) WatchJobsColors.Ink else WatchJobsColors.Muted
                )
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().height(2.dp).background(if (active) WatchJobsColors.Red else Color.Transparent))
            }
        }
    }
}

// Centraliza o conteúdo com largura máxima, para não esticar demais em telas largas.
@Composable
fun PageContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 1200.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = maxWidth).fillMaxWidth(), content = content)
    }
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < 640.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HeaderTexts(title, subtitle)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), content = actions)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { HeaderTexts(title, subtitle) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), content = actions)
            }
        }
    }
}

@Composable
private fun HeaderTexts(title: String, subtitle: String?) {
    Text(title, style = MaterialTheme.typography.headlineLarge, color = WatchJobsColors.Ink)
    if (subtitle != null) {
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted)
    }
}

// Faixa de indicadores no topo, como "ACTIVE APPLICATIONS | INTERVIEW RATE" da Home.
// No celular, vira uma grade de duas colunas para os rótulos não quebrarem no meio.
@Composable
fun KpiStrip(items: List<Triple<String, String, String?>>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val perRow = if (maxWidth < 560.dp) 2 else items.size
        Column(Modifier.fillMaxWidth().background(WatchJobsColors.White).border(1.dp, WatchJobsColors.Divider)) {
            items.chunked(perRow).forEachIndexed { rowIndex, rowItems ->
                if (rowIndex > 0) Hairline()
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    rowItems.forEachIndexed { index, item ->
                        if (index > 0) VerticalDivider(color = WatchJobsColors.Divider)
                        KpiCell(item, Modifier.weight(1f))
                    }
                    repeat(perRow - rowItems.size) {
                        VerticalDivider(color = WatchJobsColors.Divider)
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCell(item: Triple<String, String, String?>, modifier: Modifier) {
    val (label, value, note) = item
    Column(modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        SectionLabel(label, color = WatchJobsColors.Muted)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = WatchJobsColors.Ink)
            if (note != null) {
                Spacer(Modifier.width(8.dp))
                Text(note, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Red, modifier = Modifier.padding(bottom = 5.dp))
            }
        }
    }
}

// Explicação curta no topo da tela, com o traço vermelho à esquerda.
@Composable
fun SectionHint(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(WatchJobsColors.Red))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = WatchJobsColors.Muted,
            modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)
        )
    }
}

// Barra de busca com o botão LIMPAR, como na Home dos protótipos.
@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        modifier = modifier.fillMaxWidth().testTag("searchField"),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WatchJobsColors.Muted) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                GhostButton("LIMPAR", { onValueChange("") })
            }
        },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted) },
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RectangleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = WatchJobsColors.Ink,
            unfocusedBorderColor = WatchJobsColors.Ink,
            focusedContainerColor = WatchJobsColors.White,
            unfocusedContainerColor = WatchJobsColors.White,
            cursorColor = WatchJobsColors.Red
        )
    )
}

// "‹ VAGAS SALVAS / Nova vaga", como o "ALL ROLES / Halden Labs" do Job Details.
@Composable
fun Breadcrumb(parent: String, current: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "‹  ${parent.uppercase()}",
            style = MaterialTheme.typography.labelMedium,
            color = WatchJobsColors.Ink,
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 6.dp).testTag("backLink")
        )
        Text("  /  $current", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
    }
}

// Painel com borda e título em caixa alta (as seções do Job Details e do Profile).
@Composable
fun Panel(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(WatchJobsColors.White)
            .border(1.dp, WatchJobsColors.Border)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionLabel(title)
        content()
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier
            .fillMaxWidth()
            .border(1.dp, WatchJobsColors.Border)
            .background(WatchJobsColors.White)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LogoMark(height = 40.dp)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted)
        action()
    }
}
