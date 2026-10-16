package com.watchjobs.data.local

import java.util.prefs.Preferences

actual fun createKeyValueStore(): KeyValueStore = PreferencesKeyValueStore()

// Desktop: java.util.prefs, num nó "watchjobs" do usuário.
class PreferencesKeyValueStore : KeyValueStore {
    private val preferences = Preferences.userRoot().node("watchjobs")

    override fun get(key: String): String? = preferences.get(key, null)

    override fun put(key: String, value: String) {
        preferences.put(key, value)
        preferences.flush()
    }

    override fun remove(key: String) {
        preferences.remove(key)
        preferences.flush()
    }
}
