package com.watchjobs.ui.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.JobDetail
import com.watchjobs.model.JobType
import com.watchjobs.model.WorkMode
import com.watchjobs.ui.components.Breadcrumb
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.GhostButton
import com.watchjobs.ui.components.Hairline
import com.watchjobs.ui.components.LoadingBox
import com.watchjobs.ui.components.PageContainer
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.components.SectionLabel
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.util.salaryRange
import com.watchjobs.viewmodel.JobDetailViewModel

// Detalhes da vaga: leitura completa dentro do WatchJobs, com Salvar e o link para a página original.
@Composable
fun JobDetailScreen(
    viewModel: JobDetailViewModel,
    onBack: () -> Unit,
    onOpenSaved: (Long?) -> Unit,
    showMessage: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val detail = state.detail

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)) {
        PageContainer {
            Breadcrumb(
                "Buscar vagas",
                detail?.let { "${it.job.organizationName} / ${it.job.title}" } ?: "Detalhes da vaga",
                onBack
            )
            Hairline(Modifier.padding(top = 8.dp, bottom = 20.dp))
            when {
                state.loading -> LoadingBox()
                detail == null -> ErrorBanner(state.error ?: "Vaga não encontrada")
                else -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val main: @Composable (Modifier) -> Unit = { modifier -> DetailContent(detail, modifier) }
                    val side: @Composable (Modifier) -> Unit = { modifier ->
                        DetailActions(
                            detail = detail,
                            saving = state.saving,
                            onSave = { viewModel.save(showMessage) },
                            onOpenSaved = { onOpenSaved(state.savedJobId) },
                            onOpenSource = showMessage,
                            modifier = modifier
                        )
                    }
                    if (maxWidth >= 860.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            main(Modifier.weight(2f))
                            side(Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            side(Modifier.fillMaxWidth())
                            main(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailContent(detail: JobDetail, modifier: Modifier) {
    val job = detail.job
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        JobHeadline(
            company = job.organizationName,
            title = job.title,
            meta = listOfNotNull(
                job.placeLabel.ifEmpty { null },
                salaryRange(job.salaryMin, job.salaryMax, job.salaryCurrency),
                job.workMode?.takeIf { it != WorkMode.UNKNOWN }?.label,
                job.postedAt?.let { "Publicada em ${isoDateToBr(it)}" }
            )
        )
        JobFacts(
            listOf(
                "Nível" to (job.experienceLevel?.takeIf { it != ExperienceLevel.UNKNOWN }?.label ?: "Não informado"),
                "Contrato" to (job.jobType?.takeIf { it != JobType.UNKNOWN }?.label ?: "Não informado"),
                "Fonte" to job.sourceName
            )
        )
        JobDescriptionSections(detail.description, job.descriptionSnippet)
        JobSkillsSection(detail.skills)
        JobOtherInfo(
            listOf(
                "Área" to job.roleFamily,
                "Local informado" to job.rawLocation,
                "País" to job.locationCountryCode,
                "Publicada em" to job.postedAt?.let(::isoDateToBr),
                "Descoberta em" to job.discoveredAt?.let(::isoDateToBr),
                "Fica na busca até" to job.expiresAt?.let(::isoDateToBr)
            )
        )
    }
}

@Composable
private fun DetailActions(
    detail: JobDetail,
    saving: Boolean,
    onSave: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenSource: (String) -> Unit,
    modifier: Modifier
) {
    val job = detail.job
    val uriHandler = LocalUriHandler.current
    Column(modifier.background(WatchJobsColors.White).border(1.dp, WatchJobsColors.Border)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                salaryRange(job.salaryMin, job.salaryMax, job.salaryCurrency) ?: "Salário não informado",
                style = MaterialTheme.typography.headlineMedium,
                color = WatchJobsColors.Ink
            )
            SectionLabel(job.expiresLabel, color = WatchJobsColors.Muted)
            Hairline()
            PrimaryButton(
                if (job.alreadySaved) "Vaga salva" else "Salvar vaga",
                onSave,
                Modifier.fillMaxWidth().testTag("detailSaveJob"),
                enabled = !job.alreadySaved,
                loading = saving
            )
            SecondaryButton(
                "Acessar página original",
                {
                    runCatching { uriHandler.openUri(job.jobUrl) }
                        .onFailure { onOpenSource("Não foi possível abrir o link da vaga.") }
                },
                Modifier.fillMaxWidth().testTag("openJob")
            )
            if (job.alreadySaved) {
                GhostButton("Abrir em Vagas salvas", onOpenSaved, Modifier.fillMaxWidth().testTag("openSavedJob"))
            }
            Text(
                "Salvar guarda uma cópia em Vagas salvas, que não expira. Nada é enviado à empresa.",
                style = MaterialTheme.typography.bodySmall,
                color = WatchJobsColors.Muted
            )
        }
        Hairline()
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Fonte")
            Text("via ${job.sourceName}", style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink)
            if (job.requiresAttribution) {
                Text("Vaga fornecida por ${job.sourceName}.", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
            }
        }
    }
}

// "2026-10-20T13:00:00Z" -> "20/10/2026".
private fun isoDateToBr(iso: String): String {
    val parts = iso.take(10).split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else iso
}
