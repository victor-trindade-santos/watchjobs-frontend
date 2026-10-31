package com.watchjobs.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberFileSaver(onResult: (saved: Boolean) -> Unit): (fileName: String, mimeType: String, bytes: ByteArray) -> Unit {
    val callback by rememberUpdatedState(onResult)
    return remember {
        { fileName, _, bytes ->
            val dialog = FileDialog(null as Frame?, "Salvar arquivo", FileDialog.SAVE)
            dialog.file = fileName
            dialog.isVisible = true
            val name = dialog.file
            val directory = dialog.directory
            val saved = if (name == null || directory == null) false else runCatching { File(directory, name).writeBytes(bytes) }.isSuccess
            callback(saved)
        }
    }
}
