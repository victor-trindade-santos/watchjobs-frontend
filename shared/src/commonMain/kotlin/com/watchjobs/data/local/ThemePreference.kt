package com.watchjobs.data.local

import com.watchjobs.ui.theme.ThemeMode
import com.watchjobs.ui.theme.WatchJobsColors

// Tema escolhido na navbar (chave "theme"), guardado no mesmo armazenamento local do token e do quadro.
class ThemePreference(private val store: KeyValueStore) {
    fun get(): ThemeMode = store.get(KEY)?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } } ?: ThemeMode.LIGHT

    // Aplica o tema salvo antes da primeira tela, para não piscar o tema errado.
    fun applySaved() = WatchJobsColors.apply(get())

    fun set(mode: ThemeMode) {
        store.put(KEY, mode.name)
        WatchJobsColors.apply(mode)
    }

    fun toggle(): ThemeMode {
        val next = if (WatchJobsColors.mode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        set(next)
        return next
    }

    private companion object {
        const val KEY = "theme"
    }
}
