package corsica.erwinn.tracce.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import corsica.erwinn.tracce.TracceApplication
import corsica.erwinn.tracce.TracceContainer
import corsica.erwinn.tracce.data.firebase.FirebaseSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * PATTERN OUTBOX & ARRIÈRE-PLAN (Slides 50 à 56 du cours) :
 * Ce Worker s'exécute quand le réseau est disponible (même si l'application est tuée ou fermée).
 * Il lit dans Room les complétions en attente (synced = 0) et les marque synchronisées / envoyées.
 */
class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userPrefs = TracceContainer.repository.userPreferences
            val username = userPrefs.username.first()
            val isAdmin = userPrefs.isAdmin.first()

            val unsyncedList = TracceContainer.database.completionDao().getUnsynced()
            var sentCount = 0

            for (comp in unsyncedList) {
                // Envoi réel vers Firebase Cloud Firestore (users/{uid}/completions/{questId})
                val compUser = comp.userId.ifBlank { username }
                val uploaded = FirebaseSyncManager.uploadCompletion(
                    completion = comp.toDomain(),
                    isDemo = false,
                    username = compUser,
                    isAdmin = isAdmin
                )
                if (uploaded) {
                    TracceContainer.database.completionDao().markSynced(comp.questId, comp.userId)
                    sentCount++
                }
            }

            // Synchronisation des lieux personnalisés créés en mode hors-ligne
            val customQuests = TracceContainer.database.questDao().getCustomQuests()
            for (questEntity in customQuests) {
                try {
                    FirebaseSyncManager.uploadCustomQuest(questEntity.toDomain())
                } catch (e: Exception) {
                    // Silently continue
                }
            }

            // Mise à jour globale du profil utilisateur dans Firestore
            val allIds = TracceContainer.database.completionDao().getCompletedQuestIdsForUser(username)
            FirebaseSyncManager.syncUserProfile(username, isAdmin, allIds)

            if (sentCount > 0) {
                showSyncNotification(sentCount)
            }

            Result.success(workDataOf("synced_count" to sentCount))
        } catch (e: IOException) {
            // Panne réseau temporaire : on demande à l'OS de réessayer plus tard (Slide 55)
            Result.retry()
        } catch (e: Exception) {
            // Erreur fatale : abandon
            Result.failure()
        }
    }

    private fun showSyncNotification(count: Int) {
        val notification = NotificationCompat.Builder(context, TracceApplication.CHANNEL_SYNC_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("☁️ Tracce — Synchronisation")
            .setContentText("$count quête(s) synchronisée(s) avec succès dans le Cloud !")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2001, notification)
    }

    companion object {
        fun enqueue(context: Context) {
            // Contrainte stricte : uniquement quand le réseau est connecté (Slide 54 & 96)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            // KEEP : Si une demande est déjà en file d'attente, on garde celle existante
            WorkManager.getInstance(context).enqueueUniqueWork(
                "tracce_sync_outbox",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
