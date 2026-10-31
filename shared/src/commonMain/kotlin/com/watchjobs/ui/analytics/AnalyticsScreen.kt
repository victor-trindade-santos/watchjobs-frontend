package com.watchjobs.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchjobs.model.Analytics
import com.watchjobs.model.PeriodPoint
import com.watchjobs.ui.components.EmptyState
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.KpiStrip
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageContainer
import com.watchjobs.ui.components.PageHeader
import com.watchjobs.ui.components.Panel
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.util.elapsedLabel
import com.watchjobs.viewmodel.AnalyticsViewModel

// Analytics de candidaturas (F09): funil, evolução semanal, áreas, etapas, conversão, atividade e histórico.
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel, onOpenBoard: () -> Unit, onOpenSavedJob: (Long) -> Unit) {
    val state by viewModel.state.collectAsState()
    val data = state.data
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp).testTag("analyticsScreen")) {
        PageContainer(maxWidth = 1200.dp) {
            PageHeader(
                "Analytics de candidaturas",
                data?.let { "Calculado a partir das suas movimentações reais" + (it.lastEventAt?.let { at -> " · última em ${elapsedLabel(at)}" } ?: "") }
            ) {
                SecondaryButton("Atualizar", viewModel::load, Modifier.testTag("analyticsRefresh"), enabled = !state.loading)
            }
            Spacer(Modifier.height(16.dp))
            PeriodSelector(state.weeks, viewModel::setWeeks)
            Spacer(Modifier.height(20.dp))
            when {
                state.loading && data == null -> LoadingBox()
                data == null -> ErrorBanner(state.error ?: "Não foi possível carregar os indicadores")
                data.isEmpty -> EmptyState(
                    "Ainda não há movimentações",
                    "Salve vagas e mova os cards no quadro de candidaturas: os indicadores aparecem aqui conforme você avança.",
                    Modifier.testTag("analyticsEmpty")
                ) { PrimaryButton("Abrir candidaturas", onOpenBoard) }
                else -> Content(data, onOpenSavedJob)
            }
            state.error?.takeIf { data != null }?.let { ErrorBanner(it, Modifier.padding(top = 12.dp)) }
        }
    }
}

@Composable
private fun PeriodSelector(weeks: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Período:", style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted)
        AnalyticsViewModel.PERIODS.forEach { option ->
            val selected = option == weeks
            Text(
                periodLabel(option),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) WatchJobsColors.White else WatchJobsColors.Ink,
                modifier = Modifier
                    .background(if (selected) WatchJobsColors.Ink else WatchJobsColors.White)
                    .border(1.dp, WatchJobsColors.Ink)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("period_$option")
            )
        }
    }
}

@Composable
private fun Content(data: Analytics, onOpenSavedJob: (Long) -> Unit) {
    val totals = data.totals
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        KpiStrip(
            listOf(
                Triple("Salvas", totals.saved.toString(), null),
                Triple("Candidaturas", totals.applied.toString(), percentLabel(rateOf(totals.applied, totals.saved))),
                Triple("Em andamento", totals.active.toString(), null),
                Triple("Entrevistas", totals.interviews.toString(), percentLabel(rateOf(totals.interviews, totals.applied))),
                Triple("Propostas", totals.offers.toString(), null),
                Triple("Aceitas", totals.accepted.toString(), null)
            ),
            Modifier.testTag("analyticsKpis")
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 900.dp
            val left: @Composable ColumnScope.() -> Unit = {
                Panel("Funil de candidaturas", Modifier.testTag("analyticsFunnel")) { Funnel(data) }
                Panel("Evolução semanal", Modifier.testTag("analyticsWeekly")) {
                    Legend(listOf("Salvas" to WatchJobsColors.Border, "Candidaturas" to WatchJobsColors.Ink, "Avanços" to WatchJobsColors.Red))
                    BarChart(
                        data.weekly,
                        listOf<(PeriodPoint) -> Int>({ it.saved }, { it.applied }, { it.advanced }),
                        listOf(WatchJobsColors.Border, WatchJobsColors.Ink, WatchJobsColors.Red),
                        "Gráfico semanal de vagas salvas, candidaturas e avanços"
                    )
                }
                Panel("Atividade ao longo do tempo (12 meses)", Modifier.testTag("analyticsActivity")) {
                    BarChart(data.monthly, listOf<(PeriodPoint) -> Int>({ it.events }), listOf(WatchJobsColors.Ink), "Gráfico de atividade mensal", monthly = true)
                    Text(
                        "Cada barra soma as movimentações do mês: vagas salvas, candidaturas, mudanças de etapa e resultados.",
                        style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted
                    )
                }
            }
            val right: @Composable ColumnScope.() -> Unit = {
                Panel("Candidaturas por etapa (agora)", Modifier.testTag("analyticsStages")) {
                    val max = data.byStage.maxOfOrNull { it.count } ?: 0
                    data.byStage.forEach { HorizontalBar(it.label, it.count, max, WatchJobsColors.Ink, Modifier.testTag("stage_${it.column}")) }
                }
                Panel("Conversão entre etapas", Modifier.testTag("analyticsConversion")) {
                    data.funnel.zipWithNext().forEach { (from, to) ->
                        Row(Modifier.fillMaxWidth().testTag("conversion_${to.stage}"), verticalAlignment = Alignment.CenterVertically) {
                            Text("${from.label} → ${to.label}", style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink, modifier = Modifier.weight(1f))
                            Text(percentLabel(to.conversionFromPrevious) ?: "—", style = MaterialTheme.typography.titleSmall, color = WatchJobsColors.Ink)
                        }
                    }
                    Text("“—” quando ainda não há nada na etapa anterior.", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
                }
                Panel("Áreas profissionais", Modifier.testTag("analyticsFamilies")) {
                    val max = data.byRoleFamily.maxOfOrNull { it.saved } ?: 0
                    data.byRoleFamily.take(8).forEach { family ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            HorizontalBar(family.label, family.saved, max, WatchJobsColors.Red)
                            Text(
                                "${family.applied} candidaturas · ${family.interviews} entrevistas · ${family.offers} propostas",
                                style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted
                            )
                        }
                    }
                }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(20.dp), content = left)
                    Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(20.dp), content = right)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    left()
                    right()
                }
            }
        }
        Panel("Histórico de movimentação", Modifier.testTag("analyticsHistory")) {
            if (data.history.isEmpty()) Text("Nenhuma movimentação ainda.", style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted)
            data.history.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenSavedJob(item.savedJobId) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).background(if (item.type == "SAVED") WatchJobsColors.Border else WatchJobsColors.Red))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${item.positionRole} · ${item.organizationName}", style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.description, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
                    }
                    Text(elapsedLabel(item.occurredAt), style = MaterialTheme.typography.labelMedium, color = WatchJobsColors.Muted)
                }
            }
        }
    }
}

// Funil: cada etapa é uma barra proporcional à primeira, com a conversão em relação à anterior.
@Composable
private fun Funnel(data: Analytics) {
    val top = data.funnel.firstOrNull()?.count ?: 0
    data.funnel.forEachIndexed { index, step ->
        Row(Modifier.fillMaxWidth().testTag("funnel_${step.stage}"), verticalAlignment = Alignment.CenterVertically) {
            Text(step.label, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink, modifier = Modifier.width(150.dp))
            Box(Modifier.weight(1f).height(22.dp).background(WatchJobsColors.Surface)) {
                val fraction = barFraction(step.count, top)
                if (fraction > 0f) {
                    Box(Modifier.fillMaxWidth(fraction).height(22.dp).background(if (index == 0) WatchJobsColors.Ink else WatchJobsColors.Red))
                }
            }
            Text(step.count.toString(), style = MaterialTheme.typography.titleSmall, color = WatchJobsColors.Ink, modifier = Modifier.widthIn(min = 44.dp).padding(start = 10.dp))
            Text(
                if (index == 0) "" else percentLabel(step.conversionFromPrevious) ?: "—",
                style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted, modifier = Modifier.widthIn(min = 44.dp)
            )
        }
    }
}

@Composable
private fun HorizontalBar(label: String, value: Int, max: Int, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink, modifier = Modifier.width(130.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.weight(1f).height(14.dp).background(WatchJobsColors.Surface)) {
            val fraction = barFraction(value, max)
            if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).height(14.dp).background(color))
        }
        Text(value.toString(), style = MaterialTheme.typography.labelLarge, color = WatchJobsColors.Ink, modifier = Modifier.widthIn(min = 36.dp).padding(start = 8.dp))
    }
}

@Composable
private fun Legend(items: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
            }
        }
    }
}

// Barras agrupadas por período; períodos sem movimentação aparecem vazios (o backend completa com zero).
@Composable
private fun BarChart(
    points: List<PeriodPoint>,
    series: List<(PeriodPoint) -> Int>,
    colors: List<Color>,
    description: String,
    monthly: Boolean = false
) {
    val max = points.maxOfOrNull { p -> series.maxOf { it(p) } } ?: 0
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(max.toString(), style = MaterialTheme.typography.labelSmall, color = WatchJobsColors.Muted, modifier = Modifier.width(28.dp))
            Canvas(Modifier.weight(1f).height(160.dp).semantics { contentDescription = description }) {
                val slot = size.width / points.size.coerceAtLeast(1)
                val gap = slot * 0.2f
                val barWidth = (slot - gap) / series.size
                drawLine(WatchJobsColors.Divider, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1f)
                points.forEachIndexed { i, point ->
                    series.forEachIndexed { s, value ->
                        val v = value(point)
                        if (v > 0 && max > 0) {
                            val h = size.height * v / max
                            drawRect(colors[s], Offset(i * slot + gap / 2 + s * barWidth, size.height - h), Size(barWidth, h))
                        }
                    }
                }
            }
        }
        val step = labelStep(points.size)
        Row(Modifier.fillMaxWidth().padding(start = 28.dp, top = 4.dp)) {
            points.forEachIndexed { i, point ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                    if (i % step == 0 || i == points.lastIndex) {
                        Text(
                            if (monthly) monthLabel(point.start) else dayMonthLabel(point.start),
                            style = MaterialTheme.typography.labelSmall, color = WatchJobsColors.Muted, maxLines = 1,
                            fontWeight = if (i == points.lastIndex) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// --- Formatação (puras, testadas em commonTest) ---

internal fun percentLabel(rate: Double?): String? = rate?.let { "${kotlin.math.round(it * 100).toInt()}%" }

internal fun rateOf(count: Int, total: Int): Double? = if (total == 0) null else count.toDouble() / total

internal fun barFraction(value: Int, max: Int): Float = if (max <= 0 || value <= 0) 0f else (value.toFloat() / max).coerceIn(0.02f, 1f)

// Mostra no máximo ~8 rótulos no eixo, para não sobrepor em 26 ou 52 semanas.
internal fun labelStep(count: Int): Int = ((count + 7) / 8).coerceAtLeast(1)

internal fun periodLabel(weeks: Int): String = when (weeks) {
    4 -> "4 semanas"
    12 -> "3 meses"
    26 -> "6 meses"
    52 -> "12 meses"
    else -> "$weeks semanas"
}

private val MONTHS = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")

// "2026-01-05" -> "05/01"
internal fun dayMonthLabel(isoDate: String): String =
    if (isoDate.length >= 10) "${isoDate.substring(8, 10)}/${isoDate.substring(5, 7)}" else isoDate

// "2026-01-01" -> "jan/26"
internal fun monthLabel(isoDate: String): String {
    val month = isoDate.substring(5, 7).toIntOrNull() ?: return isoDate
    return "${MONTHS.getOrElse(month - 1) { "?" }}/${isoDate.substring(2, 4)}"
}
