package com.watchjobs.data.local

import android.content.Context
import android.content.SharedPreferences

// A MainActivity chama AndroidPlatform.init(applicationContext) antes de mostrar o App().
object AndroidPlatform {
    lateinit var context: Context
        private set

    fun init(context: Context) {
        this.context = context.applicationContext
    }
}

actual fun createKeyValueStore(): KeyValueStore = SharedPreferencesKeyValueStore()

// Android: SharedPreferences.
class SharedPreferencesKeyValueStore : KeyValueStore {
    private val preferences: SharedPreferences =
        AndroidPlatform.context.getSharedPreferences("watchjobs", Context.MODE_PRIVATE)

    override fun get(key: String): String? = preferences.getString(key, null)

    override fun put(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}
