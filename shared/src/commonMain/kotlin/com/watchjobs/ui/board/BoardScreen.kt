package com.watchjobs.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.watchjobs.model.BoardCard
import com.watchjobs.model.BoardColumn
import com.watchjobs.model.SavedJobStatus
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.GhostButton
import com.watchjobs.ui.components.Hairline
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.MoveIcon
import com.watchjobs.ui.components.PageHeader
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.components.SectionLabel
import com.watchjobs.ui.jobs.formatScore
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.viewmodel.BoardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class PendingMove(val cardId: Long, val column: BoardColumn, val beforeId: Long?, val afterId: Long?)

// Estado do arrasto. As posições estão em coordenadas da raiz da janela, para não depender da rolagem.
private class DragState {
    var card by mutableStateOf<BoardCard?>(null)
    var grab by mutableStateOf(Offset.Zero)
    var pointer by mutableStateOf(Offset.Zero)
    var hover by mutableStateOf<BoardColumn?>(null)
    var cardWidth by mutableStateOf(0)
}

private val CLOSED_OUTCOMES = listOf(SavedJobStatus.ACCEPTED, SavedJobStatus.REJECTED, SavedJobStatus.WITHDRAWN)

// Candidaturas em Kanban: arrastar com o mouse (Desktop e Web) e o ícone de mover em todas as plataformas.
// No toque (celular) não há arrasto: a rolagem fica livre e o ícone abre o menu de destinos.
@Composable
fun BoardScreen(
    viewModel: BoardViewModel,
    onOpenSavedJob: (Long) -> Unit,
    onNewSavedJob: () -> Unit,
    onOpenList: () -> Unit,
    showMessage: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var pendingOutcome by remember { mutableStateOf<PendingMove?>(null) }
    val drag = remember { DragState() }
    val columnCoords = remember { mutableMapOf<BoardColumn, LayoutCoordinates>() }
    val cardCoords = remember { mutableMapOf<Long, LayoutCoordinates>() }
    var boardCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var overlayCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val boardScroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    fun requestMove(move: PendingMove, current: SavedJobStatus) {
        if (move.column == BoardColumn.CLOSED && current !in CLOSED_OUTCOMES) {
            pendingOutcome = move
        } else {
            viewModel.move(move.cardId, move.column, null, move.beforeId, move.afterId, showMessage)
        }
    }

    fun columnAt(point: Offset): BoardColumn? = columnCoords.entries.firstOrNull { (_, coords) ->
        coords.isAttached && Rect(coords.positionInRoot(), coords.size.toSize()).contains(point)
    }?.key

    // Vizinhos de cima e de baixo do ponto onde o card foi solto, na coluna de destino.
    fun drop(card: BoardCard, target: BoardColumn) {
        val siblings = state.board.cards(target).filter { it.id != card.id }
        val centers = siblings.map { sibling ->
            cardCoords[sibling.id]?.takeIf { it.isAttached }?.let { it.positionInRoot().y + it.size.height / 2f } ?: Float.MAX_VALUE
        }
        val afterIndex = centers.indexOfFirst { it > drag.pointer.y }
        val after = siblings.getOrNull(afterIndex)
        val before = if (afterIndex == -1) siblings.lastOrNull() else siblings.getOrNull(afterIndex - 1)
        requestMove(PendingMove(card.id, target, before?.id, after?.id), card.status)
    }

    // Perto da borda do quadro, rola na horizontal para alcançar as colunas que não cabem na tela.
    LaunchedEffect(drag.card != null) {
        while (drag.card != null) {
            val board = boardCoords?.takeIf { it.isAttached }
            if (board != null) {
                val left = board.positionInRoot().x
                val right = left + board.size.width
                when {
                    drag.pointer.x < left + EDGE_SCROLL_ZONE -> boardScroll.scrollBy(-EDGE_SCROLL_STEP)
                    drag.pointer.x > right - EDGE_SCROLL_ZONE -> boardScroll.scrollBy(EDGE_SCROLL_STEP)
                }
                drag.hover = columnAt(drag.pointer)
            }
            delay(16)
        }
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned { overlayCoords = it }) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
            PageHeader("Candidaturas", "Arraste os cards entre as colunas ou use o ícone de mover de cada card.") {
                SecondaryButton("Nova vaga", onNewSavedJob)
                GhostButton("Ver em lista", onOpenList, Modifier.testTag("applicationsList"))
                GhostButton("Atualizar", viewModel::load)
                GhostButton("‹", { scope.launch { boardScroll.animateScrollBy(-COLUMN_SCROLL) } }, Modifier.testTag("boardLeft"))
                GhostButton("›", { scope.launch { boardScroll.animateScrollBy(COLUMN_SCROLL) } }, Modifier.testTag("boardRight"))
            }
            state.error?.let { ErrorBanner(it, Modifier.padding(top = 12.dp)) }
            if (state.loading) {
                LoadingBox()
                return@Column
            }
            Row(
                Modifier.fillMaxSize().padding(top = 16.dp).onGloballyPositioned { boardCoords = it }.horizontalScroll(boardScroll),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BoardColumn.entries.forEach { column ->
                    val cards = state.board.cards(column)
                    val highlighted = drag.card != null && drag.hover == column
                    Column(
                        Modifier.width(COLUMN_WIDTH).fillMaxHeight()
                            .background(if (highlighted) WatchJobsColors.RedSoft else WatchJobsColors.Surface)
                            .border(1.dp, if (highlighted) WatchJobsColors.Red else WatchJobsColors.Divider)
                            .onGloballyPositioned { columnCoords[column] = it }
                            .testTag("column_${column.name}")
                    ) {
                        Box(Modifier.fillMaxWidth().height(3.dp).background(WatchJobsColors.Red))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel(column.label, Modifier.weight(1f))
                            Text("${cards.size}", style = MaterialTheme.typography.labelLarge, color = WatchJobsColors.Muted)
                        }
                        Hairline()
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cards.forEach { card ->
                                key(card.id) {
                                    val current by rememberUpdatedState(card)
                                    val dragging = drag.card?.id == card.id
                                    Box(
                                        Modifier
                                            .alpha(if (dragging) 0.35f else 1f)
                                            .onGloballyPositioned { cardCoords[card.id] = it }
                                            .mouseDrag(
                                                coords = { cardCoords[card.id] },
                                                onStart = { grab, width ->
                                                    drag.card = current
                                                    drag.grab = grab
                                                    drag.cardWidth = width
                                                },
                                                onMove = { root ->
                                                    drag.pointer = root
                                                    drag.hover = columnAt(root)
                                                },
                                                onEnd = {
                                                    val target = drag.hover
                                                    if (target != null) drop(current, target)
                                                    drag.card = null
                                                    drag.hover = null
                                                },
                                                onCancel = {
                                                    drag.card = null
                                                    drag.hover = null
                                                }
                                            )
                                    ) {
                                        KanbanCard(
                                            card = card,
                                            currentColumn = column,
                                            onOpen = { onOpenSavedJob(card.id) },
                                            onMove = { target ->
                                                requestMove(PendingMove(card.id, target, state.board.cards(target).lastOrNull()?.id, null), card.status)
                                            },
                                            onTimeline = { viewModel.openTimeline(card.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // O card que acompanha o ponteiro fica acima de tudo, sem ser cortado pelas colunas.
        val ghost = drag.card
        val overlay = overlayCoords
        if (ghost != null && overlay != null && overlay.isAttached) {
            val origin = overlay.positionInRoot()
            Box(
                Modifier
                    .offset { IntOffset((drag.pointer.x - drag.grab.x - origin.x).roundToInt(), (drag.pointer.y - drag.grab.y - origin.y).roundToInt()) }
                    .width(with(LocalDensity.current) { drag.cardWidth.toDp() })
                    .alpha(0.95f)
                    .testTag("dragGhost")
            ) {
                KanbanCard(ghost, BoardColumn.SAVED, onOpen = {}, onMove = {}, onTimeline = {}, showActions = false)
            }
        }
    }

    pendingOutcome?.let { move ->
        AlertDialog(
            onDismissRequest = { pendingOutcome = null },
            shape = RectangleShape,
            containerColor = WatchJobsColors.White,
            title = { Text("Como terminou?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Para mover para Encerradas, informe o resultado do processo.") },
            confirmButton = {
                Column {
                    listOf(SavedJobStatus.ACCEPTED, SavedJobStatus.REJECTED, SavedJobStatus.WITHDRAWN).forEach { outcome ->
                        GhostButton(outcome.label, {
                            pendingOutcome = null
                            viewModel.move(move.cardId, move.column, outcome, move.beforeId, move.afterId, showMessage)
                        }, Modifier.testTag("outcome_${outcome.name}"))
                    }
                }
            },
            dismissButton = { GhostButton("Cancelar", { pendingOutcome = null }) }
        )
    }

    if (state.timelineFor != null) {
        AlertDialog(
            onDismissRequest = viewModel::closeTimeline,
            shape = RectangleShape,
            containerColor = WatchJobsColors.White,
            title = { Text("Histórico", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.timeline.isEmpty()) Text("Carregando…", color = WatchJobsColors.Muted)
                    state.timeline.forEach { event ->
                        Column {
                            Text(event.description, style = MaterialTheme.typography.bodyMedium)
                            Text(event.occurredAt.take(16).replace('T', ' '), style = MaterialTheme.typography.labelSmall, color = WatchJobsColors.Muted)
                        }
                    }
                }
            },
            confirmButton = { GhostButton("Fechar", viewModel::closeTimeline) }
        )
    }
}

// Card da candidatura: empresa, cargo, match, status e a última atualização, mais abrir, histórico e mover.
@Composable
private fun KanbanCard(
    card: BoardCard,
    currentColumn: BoardColumn,
    onOpen: () -> Unit,
    onMove: (BoardColumn) -> Unit,
    onTimeline: () -> Unit,
    showActions: Boolean = true
) {
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().background(WatchJobsColors.White).border(1.dp, WatchJobsColors.Border).padding(12.dp).testTag("card_${card.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                card.organizationName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = WatchJobsColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            card.matchScore?.let {
                Text("${formatScore(it)}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = WatchJobsColors.Red)
            }
        }
        Text(card.positionRole, style = MaterialTheme.typography.titleSmall, color = WatchJobsColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            listOfNotNull(card.status.label, card.stageName).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = WatchJobsColors.Muted
        )
        updatedOn(card)?.let { date ->
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = WatchJobsColors.Muted)) { append("Atualizado em ") }
                    withStyle(SpanStyle(color = WatchJobsColors.Red, fontWeight = FontWeight.Bold)) { append(date) }
                },
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (showActions) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GhostButton("Abrir vaga", onOpen, Modifier.testTag("openCard_${card.id}"))
                GhostButton("Histórico", onTimeline, Modifier.testTag("timeline_${card.id}"))
                Spacer(Modifier.weight(1f))
                Box {
                    Box(
                        Modifier.size(36.dp).clickable { menu = true }.testTag("moveMenu_${card.id}")
                            .semantics { contentDescription = "Mover para outra coluna" },
                        contentAlignment = Alignment.Center
                    ) {
                        MoveIcon()
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = WatchJobsColors.White) {
                        BoardColumn.entries.filter { it != currentColumn }.forEach { column ->
                            DropdownMenuItem(
                                text = { Text(column.label) },
                                onClick = { menu = false; onMove(column) },
                                modifier = Modifier.testTag("moveTo_${column.name}")
                            )
                        }
                    }
                }
            }
        }
    }
}

// "2026-10-20T13:00:00Z" -> "20/10/2026".
private fun updatedOn(card: BoardCard): String? {
    val parts = (card.statusUpdatedAt ?: return null).take(10).split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else null
}

// Arrasto só com o mouse: no toque a rolagem continua livre e o ícone de mover faz o trabalho.
private fun Modifier.mouseDrag(
    coords: () -> LayoutCoordinates?,
    onStart: (grab: Offset, widthPx: Int) -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (down.type != PointerType.Mouse) return@awaitEachGesture
        val layout = coords() ?: return@awaitEachGesture
        val moved = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() } ?: return@awaitEachGesture
        onStart(down.position, layout.size.width)
        onMove(layout.localToRoot(moved.position))
        val finished = drag(moved.id) { change ->
            change.consume()
            coords()?.let { onMove(it.localToRoot(change.position)) }
        }
        if (finished) onEnd() else onCancel()
    }
}

private val COLUMN_WIDTH = 240.dp
private const val EDGE_SCROLL_ZONE = 70f
private const val EDGE_SCROLL_STEP = 22f
private const val COLUMN_SCROLL = 480f
