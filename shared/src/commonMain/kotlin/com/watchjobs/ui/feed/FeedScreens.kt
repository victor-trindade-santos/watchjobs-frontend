@file:OptIn(ExperimentalLayoutApi::class)

package com.watchjobs.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.watchjobs.data.repository.JobSort
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.FacetValue
import com.watchjobs.model.FeedItem
import com.watchjobs.model.JobType
import com.watchjobs.model.WorkMode
import com.watchjobs.ui.components.DropdownField
import com.watchjobs.ui.components.EmptyState
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.LabeledTextField
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageContainer
import com.watchjobs.ui.components.PageHeader
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SearchBar
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.components.SectionHint
import com.watchjobs.ui.components.SectionLabel
import com.watchjobs.ui.jobs.MultiSelectFilter
import com.watchjobs.ui.jobs.SalaryRangeFilter
import com.watchjobs.ui.jobs.SkillsFilter
import com.watchjobs.ui.jobs.TextAction
import com.watchjobs.ui.jobs.formatScore
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.util.postedLabel
import com.watchjobs.util.salaryRange
import com.watchjobs.viewmodel.DiscoverViewModel
import com.watchjobs.viewmodel.FeedViewModel

private val pageWidth = 1000.dp

// Página inicial e "Buscar vagas" numa tela só: vagas ordenadas pelo match do perfil, com filtros opcionais.
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onView: (Long) -> Unit,
    onDiscover: () -> Unit,
    onPreferences: () -> Unit,
    showMessage: (String) -> Unit
) {
    // Telas largas: filtros fixos na lateral. Telas estreitas: filtros num painel que abre acima da lista.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 900.dp) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier.width(300.dp).fillMaxHeight().background(WatchJobsColors.White)
                        .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 28.dp)
                        .testTag("jobSearchFilters")
                ) {
                    JobFilters(viewModel)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(WatchJobsColors.Divider))
                FeedResults(viewModel, onView, onDiscover, onPreferences, showMessage, filtersInline = false, modifier = Modifier.weight(1f))
            }
        } else {
            FeedResults(viewModel, onView, onDiscover, onPreferences, showMessage, filtersInline = true, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun FeedResults(
    viewModel: FeedViewModel,
    onView: (Long) -> Unit,
    onDiscover: () -> Unit,
    onPreferences: () -> Unit,
    showMessage: (String) -> Unit,
    filtersInline: Boolean,
    modifier: Modifier
) {
    val state by viewModel.state.collectAsState()
    val query = state.query
    var showFilters by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(Modifier.widthIn(max = pageWidth).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PageHeader(
                    title = "Buscar vagas",
                    subtitle = if (state.loading) "Atualizando…"
                    else "${state.total} ${if (state.total == 1L) "vaga encontrada" else "vagas encontradas"} · ordenadas pelo seu match"
                ) {
                    SecondaryButton("Preferências", onPreferences)
                    PrimaryButton("Buscar mais vagas", onDiscover, modifier = Modifier.testTag("discoverButton"))
                }
                SearchBar(
                    value = query.text,
                    onValueChange = { v -> viewModel.update(debounce = true) { it.copy(text = v) } },
                    placeholder = "Cargo, empresa ou palavra da descrição (ex.: \"kotlin\", \"vtex\")"
                )
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DropdownField(
                        "Ordenar por", JobSort.entries, query.sort, { it.label },
                        { v -> viewModel.update { it.copy(sort = v ?: JobSort.SCORE_DESC) } },
                        Modifier.widthIn(min = 220.dp, max = 300.dp), allowEmpty = false, tag = "sortBy"
                    )
                    if (filtersInline) {
                        SecondaryButton(
                            (if (showFilters) "Ocultar filtros" else "Filtros") + if (query.activeFilterCount > 0) " (${query.activeFilterCount})" else "",
                            { showFilters = !showFilters },
                            Modifier.testTag("toggleFilters")
                        )
                    }
                }
                if (filtersInline && showFilters) {
                    Column(Modifier.fillMaxWidth().background(WatchJobsColors.White).border(1.dp, WatchJobsColors.Border).padding(18.dp)) {
                        JobFilters(viewModel)
                    }
                }
                state.error?.let { ErrorBanner(it) }
                Spacer(Modifier.height(4.dp))
            }
        }
        when {
            state.loading && state.items.isEmpty() -> item { LoadingBox() }
            state.items.isEmpty() && state.error == null -> item {
                val filtered = query.activeFilterCount > 0 || query.text.isNotBlank()
                EmptyState(
                    title = if (filtered) "Nenhuma vaga com esses filtros" else "Nenhuma vaga com match ainda",
                    message = if (filtered) "Tire algum filtro ou mude o texto da busca."
                    else "Preencha as preferências (cargos e modo) ou use \"Buscar mais vagas\" para trazer vagas novas.",
                    modifier = Modifier.widthIn(max = pageWidth)
                ) {
                    if (filtered) PrimaryButton("Limpar filtros", viewModel::clearFilters) else PrimaryButton("Abrir preferências", onPreferences)
                }
            }
            else -> {
                items(state.items, key = { it.job.id }) { item ->
                    JobMatchCard(
                        item = item,
                        saving = item.job.id in state.savingIds,
                        onView = { onView(item.job.id) },
                        onSave = { viewModel.save(item, showMessage) },
                        modifier = Modifier.widthIn(max = pageWidth).padding(bottom = 12.dp)
                    )
                }
                if (state.hasMore) {
                    item {
                        LaunchedEffect(state.items.size) { viewModel.loadMore() }
                        if (state.loadingMore) LoadingBox()
                    }
                }
            }
        }
    }
}

// Painel de filtros: tudo opcional, a lista atualiza na hora e os filtros ficam guardados ao navegar.
@Composable
private fun JobFilters(viewModel: FeedViewModel) {
    val state by viewModel.state.collectAsState()
    val query = state.query
    val facets = state.facets
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("FILTROS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = WatchJobsColors.Ink, modifier = Modifier.weight(1f))
            TextAction("Limpar tudo", viewModel::clearFilters, Modifier.testTag("resetFilters"), enabled = query.activeFilterCount > 0)
        }
        MultiSelectFilter("Modo de trabalho", WorkMode.selectable, query.workModes, { it.label },
            { s -> viewModel.update { it.copy(workModes = s) } }, "workMode")
        MultiSelectFilter("Nível de senioridade", ExperienceLevel.selectable, query.levels, { it.label },
            { s -> viewModel.update { it.copy(levels = s) } }, "level")
        SalaryRangeFilter(query.salaryMin, query.salaryMax) { min, max ->
            viewModel.update(debounce = true) { it.copy(salaryMin = min, salaryMax = max) }
        }
        SkillsFilter(
            selected = query.skills,
            suggestions = state.skillSuggestions,
            hasMore = state.skillsHasMore,
            loading = state.skillsLoading,
            mode = query.skillMatch,
            onSearch = viewModel::searchSkills,
            onAdd = viewModel::addSkill,
            onRemove = viewModel::removeSkill,
            onClear = { viewModel.update { it.copy(skills = emptyList()) } },
            onLoadMore = viewModel::loadMoreSkills,
            onModeChange = { mode -> viewModel.update { it.copy(skillMatch = mode) } }
        )
        MultiSelectFilter("Tipo de contratação", JobType.selectable, query.jobTypes, { it.label },
            { s -> viewModel.update { it.copy(jobTypes = s) } }, "jobType")
        FacetFilter("Área de atuação", "roleFamily", facets.roleFamilies, query.roleFamilies, ::humanize) { s ->
            viewModel.update { it.copy(roleFamilies = s) }
        }
        FacetFilter("País", "country", facets.countries, query.countries, ::countryName) { s -> viewModel.update { it.copy(countries = s) } }
        FacetFilter("Estado", "state", facets.states, query.states, { it }) { s -> viewModel.update { it.copy(states = s) } }
        FacetFilter("Cidade", "city", facets.cities, query.cities, { it }) { s -> viewModel.update { it.copy(cities = s) } }
        FacetFilter("Empresa", "company", facets.companies, query.companies, { it }) { s -> viewModel.update { it.copy(companies = s) } }
    }
}

@Composable
private fun FacetFilter(
    title: String,
    tag: String,
    facet: List<FacetValue>,
    selected: Set<String>,
    label: (String) -> String,
    onChange: (Set<String>) -> Unit
) {
    val counts = facet.associate { it.value to it.count }
    MultiSelectFilter(title, facet.map { it.value }, selected, label, onChange, tag, count = { counts[it] }, visibleLimit = FACET_VISIBLE)
}

private const val FACET_VISIBLE = 6

// "BACKEND" -> "Backend", "DATA_SCIENCE" -> "Data science".
private fun humanize(code: String): String =
    code.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun countryName(code: String): String = when (code.uppercase()) {
    "BR" -> "Brasil"
    "PT" -> "Portugal"
    "US" -> "Estados Unidos"
    "AR" -> "Argentina"
    else -> code
}

@Composable
fun JobMatchCard(
    item: FeedItem,
    saving: Boolean,
    onView: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val job = item.job
    BoxWithConstraints(
        modifier.fillMaxWidth().background(WatchJobsColors.White).border(1.dp, WatchJobsColors.Border)
            .clickable(onClick = onView).padding(20.dp).testTag("feedCard_${job.id}")
    ) {
        val compact = maxWidth < 640.dp
        val info: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WatchJobsColors.Ink)) { append(job.organizationName) }
                        val place = listOfNotNull(job.placeLabel.ifEmpty { null }, job.workMode?.takeIf { it != WorkMode.UNKNOWN }?.label).joinToString(" · ")
                        if (place.isNotEmpty()) append("   |   $place")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = WatchJobsColors.Muted
                )
                Text(job.title, style = MaterialTheme.typography.titleLarge, color = WatchJobsColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                job.descriptionSnippet?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (item.skills.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.skills.forEach { SkillTag(it) }
                    }
                }
            }
        }
        val facts: @Composable (Alignment.Horizontal) -> Unit = { alignment ->
            Column(horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${formatScore(item.score)}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = WatchJobsColors.Red,
                    modifier = Modifier.testTag("matchScore_${job.id}")
                )
                SectionLabel("Match", color = WatchJobsColors.Muted)
                Spacer(Modifier.height(8.dp))
                Text(
                    salaryRange(job.salaryMin, job.salaryMax, job.salaryCurrency) ?: "Salário não informado",
                    style = MaterialTheme.typography.titleSmall,
                    color = WatchJobsColors.Ink
                )
                postedLabel(job)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted, modifier = Modifier.testTag("posted_${job.id}"))
                }
            }
        }
        val actions: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton("Visualizar", onView, modifier = Modifier.testTag("viewJob"))
                SecondaryButton(
                    when {
                        job.alreadySaved -> "Salva"
                        saving -> "Salvando..."
                        else -> "Salvar"
                    },
                    onSave,
                    enabled = !job.alreadySaved && !saving,
                    modifier = Modifier.testTag("saveJob")
                )
            }
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                info()
                facts(Alignment.Start)
                actions()
            }
        } else {
            Row {
                Column(Modifier.weight(1f)) { info() }
                Spacer(Modifier.width(24.dp))
                Column(Modifier.width(250.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    facts(Alignment.End)
                    actions()
                }
            }
        }
    }
}

// Etiqueta neutra: o vermelho fica para o match e as ações principais.
@Composable
private fun SkillTag(text: String) {
    Box(Modifier.background(WatchJobsColors.Background).border(1.dp, WatchJobsColors.Divider).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Ink)
    }
}

// "Buscar mais vagas" (F5-07): consulta o agregador na hora e pede ao motor as outras fontes.
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onView: (Long) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val result = state.result
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            PageContainer(Modifier.padding(top = 20.dp)) {
                PageHeader("Buscar mais vagas", "Quando o feed não tem o que você procura") {
                    SecondaryButton("Voltar ao início", onBack)
                }
                FlowRow(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LabeledTextField("O que você procura", state.q, { v -> viewModel.update { it.copy(q = v) } },
                        Modifier.widthIn(min = 220.dp, max = 360.dp), placeholder = "Ex.: desenvolvedor kotlin", tag = "discoverQuery")
                    LabeledTextField("Local", state.location, { v -> viewModel.update { it.copy(location = v) } },
                        Modifier.widthIn(min = 180.dp, max = 260.dp), placeholder = "Ex.: São Paulo")
                    DropdownField("Modo", WorkMode.selectable, state.workMode, { it.label }, { v -> viewModel.update { it.copy(workMode = v) } },
                        Modifier.widthIn(min = 160.dp, max = 220.dp), emptyLabel = "Qualquer")
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Buscar", viewModel::discover, loading = state.running, enabled = state.q.isNotBlank(),
                        modifier = Modifier.testTag("discoverSubmit"))
                    if (result != null) SecondaryButton("Atualizar resultados", { viewModel.refresh() })
                }
                state.error?.let { ErrorBanner(it, Modifier.padding(top = 12.dp)) }
                if (result != null) {
                    val notes = buildList {
                        if (result.cacheHit) add("Essa busca foi feita há pouco: resultados do nosso acervo.")
                        if (result.partial) add("Uma das fontes não respondeu a tempo; o resto vem em segundo plano.")
                        if (result.backgroundPending) add("Buscando em mais fontes. Avisaremos no sininho quando terminar (pode levar alguns minutos).")
                        if (result.status == "COMPLETED") add("Busca concluída: ${result.asyncFound} vagas vieram das outras fontes.")
                    }
                    notes.forEach { SectionHint(it, Modifier.padding(top = 10.dp)) }
                }
            }
        }
        if (result != null) {
            if (result.items.isEmpty()) {
                item { PageContainer { EmptyState("Nada por enquanto", "Tente outro termo ou aguarde o aviso das outras fontes.") } }
            } else {
                items(result.items, key = { it.job.id }) { item ->
                    PageContainer { JobMatchCard(item, saving = false, onView = { onView(item.job.id) }, onSave = { onView(item.job.id) }) }
                }
            }
        }
        item { Box(Modifier.padding(bottom = 24.dp)) }
    }
}
