package com.watchjobs.data.local

import kotlinx.browser.localStorage

actual fun createKeyValueStore(): KeyValueStore = LocalStorageKeyValueStore()

// Web: localStorage do navegador, com prefixo para não misturar com outras chaves.
class LocalStorageKeyValueStore : KeyValueStore {
    override fun get(key: String): String? = localStorage.getItem(PREFIX + key)

    override fun put(key: String, value: String) {
        localStorage.setItem(PREFIX + key, value)
    }

    override fun remove(key: String) {
        localStorage.removeItem(PREFIX + key)
    }

    private companion object {
        const val PREFIX = "watchjobs."
    }
}
