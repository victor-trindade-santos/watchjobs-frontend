@file:OptIn(ExperimentalLayoutApi::class)

package com.watchjobs.ui.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchjobs.data.repository.SkillMatchMode
import com.watchjobs.model.JobSkillFacet
import com.watchjobs.ui.components.CheckIcon
import com.watchjobs.ui.components.CloseIcon
import com.watchjobs.ui.components.LabeledTextField
import com.watchjobs.ui.components.SectionLabel
import com.watchjobs.ui.theme.WatchJobsColors

// Peças dos filtros da busca de vagas. Todos os grupos são opcionais e mexem na lista na hora.

@Composable
fun FilterGroup(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(title, color = WatchJobsColors.Muted)
        content()
    }
}

// Link discreto (texto sublinhado), para "Selecionar todos" e "Limpar".
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = if (enabled) WatchJobsColors.Ink else WatchJobsColors.Border,
        textDecoration = TextDecoration.Underline,
        modifier = modifier.clickable(enabled = enabled, onClick = onClick)
    )
}

@Composable
fun CheckOption(label: String, checked: Boolean, onToggle: () -> Unit, tag: String, count: Long? = null) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(16.dp).background(if (checked) WatchJobsColors.Ink else WatchJobsColors.White)
                .border(1.dp, if (checked) WatchJobsColors.Ink else WatchJobsColors.Border),
            contentAlignment = Alignment.Center
        ) {
            if (checked) CheckIcon()
        }
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (count != null) Text(count.toString(), style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
    }
}

@Composable
fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit, tag: String) {
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

// Multi-seleção com "Selecionar todos" e "Limpar seleção". Listas longas ganham busca e "Ver mais".
@Composable
fun <T> MultiSelectFilter(
    title: String,
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onChange: (Set<T>) -> Unit,
    tag: String,
    count: (T) -> Long? = { null },
    visibleLimit: Int = Int.MAX_VALUE
) {
    var search by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    // Seleções que não estão mais nas opções (ex.: a vaga venceu) continuam visíveis para poderem ser removidas.
    val all = options + selected.filter { it !in options }
    if (all.isEmpty()) return
    val searchable = all.size > SEARCH_FROM
    val matching = if (search.isBlank()) all else all.filter { label(it).contains(search.trim(), ignoreCase = true) }
    val shown = if (expanded || matching.size <= visibleLimit) matching else matching.take(visibleLimit)

    FilterGroup(title) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TextAction("Selecionar todos", { onChange(selected + matching) }, Modifier.testTag("${tag}_all"), enabled = matching.any { it !in selected })
            TextAction("Limpar seleção", { onChange(emptySet()) }, Modifier.testTag("${tag}_clear"), enabled = selected.isNotEmpty())
        }
        if (searchable) {
            LabeledTextField("Buscar", search, { search = it }, placeholder = "Filtrar a lista", tag = "${tag}_search")
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            shown.forEachIndexed { index, option ->
                CheckOption(
                    label(option), option in selected,
                    { onChange(if (option in selected) selected - option else selected + option) },
                    "${tag}_$index", count(option)
                )
            }
        }
        if (matching.size > visibleLimit) {
            TextAction(if (expanded) "Ver menos" else "Ver mais (${matching.size - visibleLimit})", { expanded = !expanded })
        }
    }
}

// Faixa em reais por mês: só mínimo, só máximo, os dois ou nenhum.
@Composable
fun SalaryRangeFilter(min: Long?, max: Long?, onChange: (Long?, Long?) -> Unit) {
    var minText by remember(min) { mutableStateOf(min?.toString().orEmpty()) }
    var maxText by remember(max) { mutableStateOf(max?.toString().orEmpty()) }
    FilterGroup("Faixa salarial (R$ por mês)") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LabeledTextField(
                "Mínimo", minText,
                { v -> minText = v.filter(Char::isDigit).take(9); onChange(minText.toLongOrNull()?.takeIf { it > 0 }, max) },
                Modifier.weight(1f), placeholder = "5000", tag = "salaryMin"
            )
            LabeledTextField(
                "Máximo", maxText,
                { v -> maxText = v.filter(Char::isDigit).take(9); onChange(min, maxText.toLongOrNull()?.takeIf { it > 0 }) },
                Modifier.weight(1f), placeholder = "15000", tag = "salaryMax"
            )
        }
        if (min != null && max != null && min > max) {
            Text("O mínimo é maior que o máximo.", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Error)
        }
        if (min != null || max != null) {
            TextAction("Sem limite", { onChange(null, null) }, Modifier.testTag("salaryClear"))
        }
    }
}

// Skills: busca com sugestões, chips removíveis, contador e E/OU. As sugestões vêm paginadas.
@Composable
fun SkillsFilter(
    selected: List<JobSkillFacet>,
    suggestions: List<JobSkillFacet>,
    hasMore: Boolean,
    loading: Boolean,
    mode: SkillMatchMode,
    onSearch: (String) -> Unit,
    onAdd: (JobSkillFacet) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
    onLoadMore: () -> Unit,
    onModeChange: (SkillMatchMode) -> Unit
) {
    var term by remember { mutableStateOf("") }
    val chosen = selected.map { it.id }.toSet()
    val options = suggestions.filter { it.id !in chosen }
    FilterGroup("Skills / Tecnologias") {
        LabeledTextField("Pesquisar skill", term, { term = it; onSearch(it) }, placeholder = "Ex.: Spring", tag = "skillSearch")
        if (term.isNotBlank()) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 220.dp).background(WatchJobsColors.White).border(1.dp, WatchJobsColors.Divider)
                    .verticalScroll(rememberScrollState()).testTag("skillSuggestions")
            ) {
                if (options.isEmpty()) {
                    Text(
                        if (loading) "Buscando…" else "Nenhuma skill encontrada",
                        style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted, modifier = Modifier.padding(10.dp)
                    )
                }
                options.forEachIndexed { index, skill ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onAdd(skill); term = ""; onSearch("") }.padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("skillOption_$index"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(skill.name, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink, modifier = Modifier.weight(1f))
                        Text("${skill.count}", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
                    }
                }
                if (hasMore) {
                    TextAction("Carregar mais", onLoadMore, Modifier.padding(10.dp).testTag("skillMore"), enabled = !loading)
                }
            }
        }
        if (selected.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Selecionadas (${selected.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = WatchJobsColors.Ink, modifier = Modifier.weight(1f))
                TextAction("Limpar todas", onClear, Modifier.testTag("skillClear"))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                selected.forEach { skill ->
                    Row(
                        Modifier.border(1.dp, WatchJobsColors.Ink).background(WatchJobsColors.White)
                            .clickable { onRemove(skill.id) }.padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("skillChip_${skill.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(skill.name, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Ink)
                        CloseIcon()
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OptionChip("Qualquer uma (OU)", mode == SkillMatchMode.ANY, { onModeChange(SkillMatchMode.ANY) }, "skillMode_ANY")
                OptionChip("Todas (E)", mode == SkillMatchMode.ALL, { onModeChange(SkillMatchMode.ALL) }, "skillMode_ALL")
            }
            Text(
                if (mode == SkillMatchMode.ANY) "Vagas com pelo menos uma das skills." else "Só vagas com todas as skills.",
                style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted
            )
        }
    }
}

private const val SEARCH_FROM = 8
