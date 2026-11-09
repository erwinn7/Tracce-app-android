package corsica.erwinn.tracce

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import corsica.erwinn.tracce.data.firebase.FirebaseSyncManager
import corsica.erwinn.tracce.data.local.TracceDatabase
import corsica.erwinn.tracce.data.repository.TracceRepository

object TracceContainer {
    lateinit var database: TracceDatabase
        private set
    lateinit var repository: TracceRepository
        private set

    fun init(context: Context) {
        database = TracceDatabase.getDatabase(context)
        repository = TracceRepository(
            questDao = database.questDao(),
            completionDao = database.completionDao(),
            context = context
        )
    }
}

class TracceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 1. Initialisation du conteneur d'architecture
        TracceContainer.init(this)

        // 2. Initialisation de Firebase Firestore & Auth partagés avec la PWA
        FirebaseSyncManager.init(this)
        FirebaseSyncManager.startQuestsListener(TracceContainer.database)

        // 3. Création des canaux de notification dès le démarrage (Slide 67 & 70)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Canal 1 : Radar GPS (Foreground Service de proximité)
            val trackerChannel = NotificationChannel(
                CHANNEL_TRACKER_ID,
                "Radar de Quêtes GPS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes de proximité quand vous approchez d'un lieu à explorer"
            }

            // Canal 2 : Synchronisation en arrière-plan
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC_ID,
                "Synchronisation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications d'envoi et de synchronisation des données"
            }

            // Canal 3 : Notifications Push FCM (Slide 13)
            val pushChannel = NotificationChannel(
                CHANNEL_PUSH_ID,
                "Nouvelles Quêtes & Alertes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications push d'exploration et annonces de rally photo"
            }

            notificationManager.createNotificationChannel(trackerChannel)
            notificationManager.createNotificationChannel(syncChannel)
            notificationManager.createNotificationChannel(pushChannel)
        }
    }

    companion object {
        const val CHANNEL_TRACKER_ID = "tracce_proximity_tracker"
        const val CHANNEL_SYNC_ID = "tracce_sync"
        const val CHANNEL_PUSH_ID = "tracce_push"
    }
}