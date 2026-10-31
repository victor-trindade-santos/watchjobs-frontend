package com.watchjobs.data.local

import com.watchjobs.model.BoardView

// Preferência local da visualização do quadro (chave "boardView"); o padrão é a simplificada.
class BoardViewPreference(private val store: KeyValueStore) {
    fun get(): BoardView = store.get(KEY)?.let { saved -> BoardView.entries.firstOrNull { it.name == saved } } ?: BoardView.SIMPLE

    fun set(view: BoardView) = store.put(KEY, view.name)

    private companion object {
        const val KEY = "boardView"
    }
}
