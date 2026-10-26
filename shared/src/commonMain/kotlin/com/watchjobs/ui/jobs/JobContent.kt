package com.watchjobs.ui.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.watchjobs.model.JobSkill
import com.watchjobs.ui.components.SectionLabel
import com.watchjobs.ui.components.Tag
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.util.splitJobDescription

// Blocos de conteúdo da vaga no estilo do Job Details dos protótipos, usados em Detalhes da vaga e Editar vaga.

@Composable
fun JobHeadline(company: String, title: String, meta: List<String>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(company, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = WatchJobsColors.Red)
        Text(title, style = MaterialTheme.typography.headlineLarge, color = WatchJobsColors.Ink, modifier = Modifier.testTag("jobTitle"))
        if (meta.isNotEmpty()) {
            Text(meta.joinToString("   ·   "), style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted)
        }
    }
}

// Faixa com borda e células (TEAM | STAGE | PROCESS no protótipo).
@Composable
fun JobFacts(facts: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    if (facts.isEmpty()) return
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).border(1.dp, WatchJobsColors.Border).background(WatchJobsColors.White)) {
        facts.forEachIndexed { index, (label, value) ->
            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(WatchJobsColors.Border))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(label, color = WatchJobsColors.Muted)
                Text(value, style = MaterialTheme.typography.titleSmall, color = WatchJobsColors.Ink)
            }
        }
    }
}

@Composable
fun JobSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = WatchJobsColors.Ink)
        content()
    }
}

// Descrição completa, quebrada em seções; sem descrição, avisa e aponta para a página original.
@Composable
fun JobDescriptionSections(description: String?, fallbackSnippet: String? = null) {
    val sections = splitJobDescription(description ?: fallbackSnippet)
    if (sections.isEmpty()) {
        JobSection("A vaga") {
            BodyText("A fonte não forneceu a descrição completa. Abra a página original para ler o anúncio inteiro.")
        }
        return
    }
    sections.forEach { section ->
        JobSection(section.title) {
            if (section.asList) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    section.items.forEach { BodyText("•  $it") }
                }
            } else {
                BodyText(section.items.joinToString(" "))
            }
        }
    }
    if (description == null && fallbackSnippet != null) {
        Text("Trecho do anúncio. A descrição completa está na página original.", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobSkillsSection(skills: List<JobSkill>) {
    if (skills.isEmpty()) return
    val (required, optional) = skills.partition { it.required }
    JobSection("Skills e requisitos técnicos") {
        if (required.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                required.forEach { Tag(it.name) }
            }
        }
        if (optional.isNotEmpty()) {
            Text("Desejáveis", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                optional.forEach { Tag(it.name) }
            }
        }
    }
}

// Dados que não têm campo próprio na aplicação (família do cargo, local como veio da fonte etc.).
@Composable
fun JobOtherInfo(items: List<Pair<String, String?>>) {
    val filled = items.mapNotNull { (label, value) -> value?.takeIf { it.isNotBlank() }?.let { label to it } }
    if (filled.isEmpty()) return
    JobSection("Outras informações") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            filled.forEach { (label, value) ->
                Row {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted, modifier = Modifier.width(170.dp))
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = WatchJobsColors.Ink)
}
