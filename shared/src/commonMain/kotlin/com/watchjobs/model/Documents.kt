package com.watchjobs.model

import kotlinx.serialization.Serializable

// Currículo adaptado e cover letter por vaga (F10).
enum class DocumentType(val label: String) { RESUME("Currículo adaptado"), COVER_LETTER("Cover letter") }

enum class ExportFormat(val label: String) { PDF("PDF"), DOCX("Word") }

@Serializable
data class GenerateDocumentRequest(val savedJobId: Long, val type: String, val instructions: String? = null)

@Serializable
data class EditDocumentRequest(val content: String)

@Serializable
data class DocumentVersionSummary(
    val version: Int,
    val source: String,
    val model: String? = null,
    val warnings: List<String> = emptyList(),
    val length: Int = 0,
    val createdAt: String
) {
    val sourceLabel: String
        get() = when (source) {
            "AI" -> "Gerada pela IA"
            "EDIT" -> "Editada por você"
            "RESTORE" -> "Restaurada"
            else -> source
        }
}

@Serializable
data class DocumentSummary(
    val id: Long,
    val savedJobId: Long,
    val type: String,
    val title: String,
    val positionRole: String,
    val organizationName: String,
    val currentVersion: Int,
    val updatedAt: String
)

@Serializable
data class DocumentDetail(
    val id: Long,
    val savedJobId: Long,
    val type: String,
    val title: String,
    val positionRole: String,
    val organizationName: String,
    val currentVersion: Int,
    val content: String,
    val warnings: List<String> = emptyList(),
    val versions: List<DocumentVersionSummary> = emptyList(),
    val updatedAt: String
)

@Serializable
data class VersionContent(val version: Int, val source: String, val content: String, val warnings: List<String> = emptyList(), val createdAt: String)

@Serializable
data class ExportFile(val fileName: String, val mimeType: String, val base64: String)

@Serializable
data class BaseResume(val content: String)

@Serializable
data class DocumentsStatus(val enabled: Boolean, val dailyLimit: Int, val usedToday: Int)
