package com.watchjobs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.watchjobs.data.local.AndroidPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // O SharedPreferences precisa de um Context; ele é guardado antes de criar o App().
        AndroidPlatform.init(applicationContext)
        setContent {
            App()
        }
    }
}
