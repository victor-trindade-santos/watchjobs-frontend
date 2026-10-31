package com.watchjobs.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

private class PendingFile {
    var bytes: ByteArray? = null
}

@Composable
actual fun rememberFileSaver(onResult: (saved: Boolean) -> Unit): (fileName: String, mimeType: String, bytes: ByteArray) -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    val pending = remember { PendingFile() }
    // O usuário escolhe a pasta e o nome; o tipo vem da extensão do nome sugerido.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
        val bytes = pending.bytes
        pending.bytes = null
        if (uri == null || bytes == null) {
            callback(false)
        } else {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("sem acesso ao destino")
            }.isSuccess
            callback(saved)
        }
    }
    return { fileName, _, bytes ->
        pending.bytes = bytes
        launcher.launch(fileName)
    }
}
