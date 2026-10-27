package com.watchjobs.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberFilePicker(extensions: List<String>, onPicked: (PickedFile?) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onPicked)
    val accepted by rememberUpdatedState(extensions)
    return remember {
        {
            val dialog = FileDialog(null as Frame?, "Escolha o arquivo", FileDialog.LOAD)
            dialog.setFilenameFilter { _, name -> accepted.any { name.lowercase().endsWith(".$it") } }
            dialog.isVisible = true
            val name = dialog.file
            val directory = dialog.directory
            callback(if (name == null || directory == null) null else File(directory, name).let { PickedFile(it.name, it.readBytes()) })
        }
    }
}
