package com.watchjobs.ui.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.watchjobs.model.DocumentType
import com.watchjobs.model.ExportFile
import com.watchjobs.model.ExportFormat
import com.watchjobs.ui.components.ConfirmDialog
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
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.components.SectionHint
import com.watchjobs.ui.components.Tag
import com.watchjobs.ui.components.rememberFileSaver
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.util.elapsedLabel
import com.watchjobs.viewmodel.DocumentsViewModel
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Currículo adaptado e cover letter (F10): a IA só reorganiza o que está no perfil; tudo é revisável e versionado.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentsScreen(viewModel: DocumentsViewModel, onOpenProfile: () -> Unit, showMessage: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    val saver = rememberFileSaver { saved -> if (saved) showMessage("Arquivo salvo.") }
    val save: (ExportFile) -> Unit = { file ->
        val bytes = decodeBase64(file.base64)
        if (bytes == null) showMessage("Arquivo inválido recebido do servidor.") else saver(file.fileName, file.mimeType, bytes)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp).testTag("documentsScreen")) {
        PageContainer(maxWidth = 1200.dp) {
            PageHeader("Currículos e cartas", "Documentos adaptados para cada vaga salva, a partir do seu perfil (currículo base).") {
                GhostButton("Editar perfil", onOpenProfile)
            }
            Spacer(Modifier.height(16.dp))
            state.status?.let { status ->
                if (!status.enabled) {
                    ErrorBanner("A geração com IA está desligada no servidor. Você ainda pode editar e exportar documentos existentes e o currículo base.")
                } else {
                    SectionHint("A IA usa somente o que está no seu perfil e na vaga. Revise sempre: avisos apontam o que não foi encontrado no perfil. " +
                        "Gerações hoje: ${status.usedToday} de ${status.dailyLimit}.")
                }
                Spacer(Modifier.height(16.dp))
            }
            when {
                state.loading -> LoadingBox()
                state.error != null && state.jobs.isEmpty() -> ErrorBanner(state.error ?: "")
                state.jobs.isEmpty() -> EmptyState("Nenhuma vaga salva", "Salve uma vaga para gerar um currículo adaptado ou uma cover letter para ela.")
                else -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val wide = maxWidth >= 960.dp
                    val side: @Composable ColumnScope.() -> Unit = {
                        Panel("Vaga") {
                            DropdownField(
                                "Vaga salva", state.jobs, state.selectedJob, { "${it.positionRole} · ${it.organizationName}" },
                                { viewModel.selectJob(it?.id) }, allowEmpty = false, tag = "documentsJob"
                            )
                            LabeledTextField(
                                "Instruções para a IA (opcional)", state.instructions, viewModel::updateInstructions,
                                singleLine = false, minLines = 2, placeholder = "Ex.: destaque liderança técnica; tom mais formal", tag = "documentsInstructions"
                            )
                            val canGenerate = state.status?.enabled == true && state.selectedJobId != null
                            DocumentType.entries.forEach { type ->
                                PrimaryButton(
                                    if (state.documents.any { it.type == type.name }) "Gerar novamente: ${type.label}" else "Gerar ${type.label.lowercase()}",
                                    { viewModel.generate(type, showMessage) },
                                    Modifier.fillMaxWidth().testTag("generate_${type.name}"),
                                    enabled = canGenerate && state.generating == null,
                                    loading = state.generating == type
                                )
                            }
                        }
                        Panel("Documentos desta vaga") {
                            if (state.documents.isEmpty()) Text("Nenhum documento ainda.", style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted)
                            state.documents.forEach { doc ->
                                val selected = doc.id == state.current?.id
                                Column(
                                    Modifier.fillMaxWidth()
                                        .border(1.dp, if (selected) WatchJobsColors.Red else WatchJobsColors.Divider)
                                        .clickable { viewModel.open(doc.id) }
                                        .padding(10.dp)
                                        .testTag("document_${doc.id}")
                                ) {
                                    Text(DocumentType.entries.firstOrNull { it.name == doc.type }?.label ?: doc.type, style = MaterialTheme.typography.titleSmall)
                                    Text("Versão ${doc.currentVersion} · ${elapsedLabel(doc.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted)
                                }
                            }
                        }
                        Panel("Currículo base") {
                            Text(
                                "É o seu perfil, sem IA. Fica separado dos currículos adaptados.",
                                style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExportFormat.entries.forEach { format ->
                                    SecondaryButton(format.label, { viewModel.exportBase(format, save, showMessage) },
                                        Modifier.testTag("exportBase_${format.name}"), enabled = !state.exporting)
                                }
                            }
                        }
                    }
                    val editor: @Composable ColumnScope.() -> Unit = { Editor(viewModel, save, showMessage) { confirmDelete = true } }
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp), content = side)
                            Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(16.dp), content = editor)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            side()
                            editor()
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            "Excluir documento",
            "O documento e todas as versões serão excluídos. O currículo base (perfil) não é alterado.",
            "Excluir",
            onConfirm = { confirmDelete = false; viewModel.delete(showMessage) },
            onDismiss = { confirmDelete = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Editor(viewModel: DocumentsViewModel, save: (ExportFile) -> Unit, showMessage: (String) -> Unit, onDelete: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val current = state.current
    if (current == null) {
        Panel("Documento") {
            Text(
                "Escolha a vaga e gere um currículo adaptado ou uma cover letter. O resultado aparece aqui para revisão e edição.",
                style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted
            )
        }
        return
    }
    val preview = state.preview
    Panel(current.title) {
        val warnings = preview?.warnings ?: current.warnings
        if (warnings.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().background(WatchJobsColors.RedSoft).border(1.dp, WatchJobsColors.Red).padding(12.dp).testTag("documentWarnings"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Revise antes de usar", style = MaterialTheme.typography.titleSmall, color = WatchJobsColors.RedDark)
                warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.RedDark) }
            }
        }
        if (preview != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Visualizando a versão ${preview.version} (somente leitura)", style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted,
                    modifier = Modifier.weight(1f))
                GhostButton("Restaurar esta versão", { viewModel.restore(preview.version, showMessage) }, Modifier.testTag("restoreVersion"), color = WatchJobsColors.Red)
                GhostButton("Voltar à atual", viewModel::closePreview)
            }
        }
        OutlinedTextField(
            value = preview?.content ?: state.draft,
            onValueChange = viewModel::updateDraft,
            readOnly = preview != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 420.dp).testTag("documentEditor"),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            shape = RectangleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WatchJobsColors.Ink,
                unfocusedBorderColor = WatchJobsColors.Border,
                focusedContainerColor = WatchJobsColors.White,
                unfocusedContainerColor = WatchJobsColors.White,
                cursorColor = WatchJobsColors.Red
            )
        )
        Text(
            "Formato: \"# \" título, \"## \" seção, \"### \" subtítulo, \"- \" tópico. É assim que o PDF e o Word são montados.",
            style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Salvar versão", { viewModel.save(showMessage) }, Modifier.testTag("saveDocument"),
                enabled = preview == null && state.hasUnsavedChanges, loading = state.saving)
            if (state.hasUnsavedChanges) SecondaryButton("Descartar alterações", viewModel::discardChanges)
            ExportFormat.entries.forEach { format ->
                SecondaryButton("Exportar ${format.label}", { viewModel.export(format, save, showMessage) },
                    Modifier.testTag("export_${format.name}"), enabled = !state.exporting)
            }
            GhostButton("Excluir", onDelete, Modifier.testTag("deleteDocument"), color = WatchJobsColors.Red)
        }
    }
    Panel("Versões") {
        current.versions.forEach { version ->
            val isCurrent = version.version == current.currentVersion
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.preview(version.version) }.padding(vertical = 4.dp).testTag("version_${version.version}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("v${version.version}", style = MaterialTheme.typography.titleSmall, color = WatchJobsColors.Ink)
                Text(version.sourceLabel, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink, modifier = Modifier.weight(1f))
                if (version.warnings.isNotEmpty()) Tag("${version.warnings.size} aviso(s)")
                if (isCurrent) Tag("Atual", filled = true)
                Text(elapsedLabel(version.createdAt), style = MaterialTheme.typography.labelMedium, color = WatchJobsColors.Muted)
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
internal fun decodeBase64(value: String): ByteArray? = runCatching { Base64.decode(value) }.getOrNull()
