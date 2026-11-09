package corsica.erwinn.tracce.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import corsica.erwinn.tracce.MainActivity
import corsica.erwinn.tracce.R
import corsica.erwinn.tracce.TracceApplication

/**
 * Service de réception des notifications push Firebase Cloud Messaging (FCM).
 * Conforme aux spécifications du TP Android (Slide 5, 9, 13) :
 * - onNewToken : stockage du token FCM
 * - onMessageReceived : affiche la notification avec PendingIntent ouvrant le bon lieu concerné (questId).
 */
class TracceMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "Nouveau jeton FCM généré : $token")
        // Sauvegarder dans les préférences pour copie/test facile
        val prefs = applicationContext.getSharedPreferences("tracce_fcm", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "Notification push FCM reçue : ${remoteMessage.data}")

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "Nouvelle quête Tracce"
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: "Un nouveau lieu d'exploration est disponible en Corse !"

        val questId = remoteMessage.data["questId"]

        showPushNotification(applicationContext, title, body, questId)
    }

    companion object {
        private const val TAG = "TracceFCM"
        private const val NOTIFICATION_ID_BASE = 2000

        fun showPushNotification(
            context: Context,
            title: String,
            body: String,
            questId: String? = null
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (!questId.isNullOrBlank()) {
                    putExtra("questId", questId)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                questId.hashCode().takeIf { it != 0 } ?: NOTIFICATION_ID_BASE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, TracceApplication.CHANNEL_PUSH_ID)
                .setSmallIcon(R.drawable.tracce_logo)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(questId.hashCode().takeIf { it != 0 } ?: NOTIFICATION_ID_BASE, notification)
        }
    }
}
