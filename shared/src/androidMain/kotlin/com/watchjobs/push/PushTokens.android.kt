package com.watchjobs.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.watchjobs.BuildEnv
import com.watchjobs.data.local.AndroidPlatform

const val PUSH_CHANNEL_ID = "watchjobs_avisos"

// Firebase iniciado à mão, com os dados do build (-Pwatchjobs.fcm.*), sem precisar do google-services.json.
object PushSupport {
    val configured: Boolean
        get() = BuildEnv.FCM_APP_ID.isNotBlank() && BuildEnv.FCM_API_KEY.isNotBlank() &&
            BuildEnv.FCM_PROJECT_ID.isNotBlank() && BuildEnv.FCM_SENDER_ID.isNotBlank()

    fun ensureInitialized(context: Context): Boolean {
        if (!configured) return false
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId(BuildEnv.FCM_APP_ID)
                .setApiKey(BuildEnv.FCM_API_KEY)
                .setProjectId(BuildEnv.FCM_PROJECT_ID)
                .setGcmSenderId(BuildEnv.FCM_SENDER_ID)
                .build()
            FirebaseApp.initializeApp(context, options)
        }
        createChannel(context)
        return true
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(PUSH_CHANNEL_ID, "Avisos do WatchJobs", NotificationManager.IMPORTANCE_DEFAULT))
    }
}

actual fun requestPushToken(onToken: (String) -> Unit) {
    runCatching {
        if (!PushSupport.ensureInitialized(AndroidPlatform.context)) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> if (!token.isNullOrBlank()) onToken(token) }
    }
}

// Com o app aberto, o FCM não mostra a notificação sozinho: ela é montada aqui.
class WatchJobsMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        PushSupport.createChannel(this)
        val launch = packageManager.getLaunchIntentForPackage(packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        val pending = launch?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
        @Suppress("DEPRECATION")
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, PUSH_CHANNEL_ID)
        } else {
            android.app.Notification.Builder(this)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .apply { if (pending != null) setContentIntent(pending) }
            .build()
        getSystemService(NotificationManager::class.java)?.notify(message.messageId?.hashCode() ?: title.hashCode(), notification)
    }

    // O token novo é registrado no backend na próxima abertura do app (requestPushToken).
    override fun onNewToken(token: String) = Unit
}
