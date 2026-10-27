package com.watchjobs.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

// Tipos MIME de cada extensão aceita; o seletor do Android filtra por MIME, não por nome.
private val MIME_TYPES = mapOf(
    "zip" to "application/zip",
    "pdf" to "application/pdf",
    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "txt" to "text/plain"
)

@Composable
actual fun rememberFilePicker(extensions: List<String>, onPicked: (PickedFile?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) {
            callback(null)
        } else {
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "arquivo"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            callback(bytes?.let { PickedFile(name, it) })
        }
    }
    val mimes = extensions.mapNotNull { MIME_TYPES[it] }.ifEmpty { listOf("*/*") }.toTypedArray()
    return { launcher.launch(mimes) }
}
