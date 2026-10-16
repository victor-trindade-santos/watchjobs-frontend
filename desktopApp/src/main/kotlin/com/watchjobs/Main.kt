package com.watchjobs

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.watchjobs.ui.components.WatchJobsMark

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "WatchJobs",
        icon = rememberVectorPainter(WatchJobsMark),
        state = rememberWindowState(width = 1280.dp, height = 820.dp)
    ) {
        App()
    }
}
