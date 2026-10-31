package com.watchjobs.ui.onboarding

import androidx.compose.animation.core.animateRectAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.watchjobs.ui.components.GhostButton
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.theme.WatchJobsColors
import kotlin.math.roundToInt

// Etapa do tour: o elemento real que fica em destaque (targetId) e a explicação curta.
data class TourStep(val targetId: String, val title: String, val text: String)

// Elementos da tela que o tour pode destacar, com a posição em coordenadas da janela.
class TourRegistry {
    val bounds = mutableStateMapOf<String, Rect>()

    fun update(id: String, rect: Rect) {
        if (bounds[id] != rect) bounds[id] = rect
    }

    fun remove(id: String) {
        bounds.remove(id)
    }
}

val LocalTour = staticCompositionLocalOf<TourRegistry?> { null }

@Composable
fun ProvideTour(registry: TourRegistry, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalTour provides registry, content = content)

// Marca um elemento real da interface como alvo do tour.
fun Modifier.tourTarget(id: String): Modifier = composed {
    val registry = LocalTour.current
    DisposableEffect(registry, id) { onDispose { registry?.remove(id) } }
    if (registry == null) this else onGloballyPositioned { coords ->
        if (coords.isAttached) registry.update(id, Rect(coords.positionInRoot(), coords.size.toSize()))
    }
}

// Etapas do tour, adaptadas ao que existe no app (o "dashboard" é a página de vagas por match).
val defaultTourSteps = listOf(
    TourStep("tab_JOB_SEARCH", "Início: vagas por match", "Esta é a página inicial: as vagas aparecem ordenadas pela nota de compatibilidade com o seu perfil."),
    TourStep("jobSearchFilters", "Buscar vagas e filtros", "Abra os grupos de filtros, escolha o que quiser e clique em \"Buscar vagas\" para aplicar."),
    TourStep("tab_APPLICATIONS", "Candidaturas", "Acompanhe cada processo seletivo no quadro: simplificado ou detalhado, com o histórico de cada vaga."),
    TourStep("tab_ANALYTICS", "Analytics", "Funil, evolução semanal, áreas e conversão entre etapas, calculados a partir das suas movimentações reais."),
    TourStep("userMenu", "Perfil & Experiência", "No menu do avatar ficam o perfil profissional, as preferências, a importação do LinkedIn e o tour."),
    TourStep("notificationBell", "Notificações", "Vagas novas, entrevistas, prazos e lembretes chegam aqui. Clique para ver os mais recentes."),
    TourStep("userMenu", "Preferências", "Ajuste cargos, modo de trabalho e salário em Preferências: o match e os avisos usam essas escolhas.")
)

// Sobreposição escura com recorte no elemento destacado, balão com a explicação, progresso e navegação.
@Composable
fun TourOverlay(registry: TourRegistry, steps: List<TourStep>, onFinish: (completed: Boolean) -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var bubbleHeight by remember { mutableIntStateOf(0) }
    val step = steps[index]
    val target = registry.bounds[step.targetId]?.translate(-origin)
    val highlight = target?.inflate(with(LocalDensity.current) { 6.dp.toPx() })
    val animated by animateRectAsState(highlight ?: Rect.Zero)

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } }
            .testTag("tourOverlay")
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color.Black.copy(alpha = 0.55f))
            if (highlight != null) {
                drawRoundRect(Color.Transparent, animated.topLeft, animated.size, CornerRadius(6f), blendMode = BlendMode.Clear)
                drawRoundRect(WatchJobsColors.Red, animated.topLeft, animated.size, CornerRadius(6f), style = Stroke(width = 3f))
            }
        }

        val bubbleWidth = 340.dp
        val bubbleWidthPx = with(density) { bubbleWidth.toPx() }
        val margin = with(density) { 16.dp.toPx() }
        val x = if (highlight == null) (widthPx - bubbleWidthPx) / 2 else highlight.left.coerceIn(margin, (widthPx - bubbleWidthPx - margin).coerceAtLeast(margin))
        val below = highlight == null || highlight.bottom + margin + bubbleHeight < heightPx
        val y = when {
            highlight == null -> (heightPx - bubbleHeight) / 2
            below -> highlight.bottom + margin
            else -> (highlight.top - margin - bubbleHeight).coerceAtLeast(margin)
        }
        Column(
            Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .width(bubbleWidth)
                .onSizeChanged { bubbleHeight = it.height }
                .background(WatchJobsColors.White)
                .border(1.dp, WatchJobsColors.Ink)
                .padding(18.dp)
                .testTag("tourBubble"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row {
                Text("${index + 1} de ${steps.size}", style = MaterialTheme.typography.labelSmall, color = WatchJobsColors.Red, modifier = Modifier.weight(1f).testTag("tourProgress"))
                GhostButton("Pular", { onFinish(false) }, Modifier.testTag("tourSkip"), color = WatchJobsColors.Muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                steps.indices.forEach { i ->
                    Box(Modifier.weight(1f).height(3.dp).background(if (i <= index) WatchJobsColors.Red else WatchJobsColors.Divider))
                }
            }
            Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WatchJobsColors.Ink)
            Text(step.text, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink)
            if (target == null) {
                Text("Este item não está visível nesta tela ou neste tamanho de janela.", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
            }
            Spacer(Modifier.size(2.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (index > 0) SecondaryButton("Anterior", { index-- }, Modifier.testTag("tourPrevious"))
                Spacer(Modifier.weight(1f))
                if (index < steps.lastIndex) {
                    PrimaryButton("Próximo", { index++ }, modifier = Modifier.testTag("tourNext"))
                } else {
                    PrimaryButton("Finalizar", { onFinish(true) }, modifier = Modifier.testTag("tourFinish"))
                }
            }
        }
    }
}
