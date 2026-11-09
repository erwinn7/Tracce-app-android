package corsica.erwinn.tracce.data.repository

import android.content.Context
import corsica.erwinn.tracce.data.firebase.FirebaseSyncManager
import corsica.erwinn.tracce.data.local.CompletionDao
import corsica.erwinn.tracce.data.local.QuestDao
import corsica.erwinn.tracce.data.local.toEntity
import corsica.erwinn.tracce.data.model.Completion
import corsica.erwinn.tracce.data.model.Quest
import corsica.erwinn.tracce.data.preferences.UserPreferences
import corsica.erwinn.tracce.utils.GeoUtils
import corsica.erwinn.tracce.worker.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class TracceRepository(
    private val questDao: QuestDao,
    private val completionDao: CompletionDao,
    private val context: Context
) {
    val userPreferences = UserPreferences(context)

    val allQuests: Flow<List<Quest>> = questDao.getAllQuests().map { entities ->
        entities.map { it.toDomain() }
    }

    val customQuests: Flow<List<Quest>> = questDao.getCustomQuestsFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    val allCompletions: Flow<List<Completion>> = completionDao.getAllCompletions().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getCompletionsForUser(username: String): Flow<List<Completion>> =
        completionDao.getCompletionsForUser(username).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getCompletedQuestIdsForUser(username: String): List<String> = withContext(Dispatchers.IO) {
        completionDao.getCompletedQuestIdsForUser(username)
    }

    fun getCompletedQuestIds(): Flow<Set<String>> = allCompletions.map { list ->
        list.map { it.questId }.toSet()
    }

    suspend fun getQuestById(id: String): Quest? = withContext(Dispatchers.IO) {
        questDao.getQuestById(id)?.toDomain()
    }

    suspend fun syncRemoteCompletionsForUser(username: String) = withContext(Dispatchers.IO) {
        val clean = username.trim()
        if (clean.isBlank() || clean.equals("explorateur", ignoreCase = true) || clean.equals("explorateur anonyme", ignoreCase = true)) {
            return@withContext
        }
        try {
            val remoteList = FirebaseSyncManager.fetchUserCompletions(clean)
            if (remoteList.isNotEmpty()) {
                val entities = remoteList.map { it.toEntity(userId = clean) }
                completionDao.upsertCompletions(entities)
            }
        } catch (e: Exception) {
            // Silently ignore if offline
        }
    }

    suspend fun validateQuest(
        quest: Quest,
        userLat: Double,
        userLng: Double,
        photoPath: String?,
        isDemoMode: Boolean = false,
        username: String = "Explorateur",
        isAdmin: Boolean = false
    ): Result<Completion> = withContext(Dispatchers.IO) {
        val distMeters = GeoUtils.haversineDistanceMeters(userLat, userLng, quest.lat, quest.lng).toInt()
        val isNearby = distMeters <= quest.radiusM || isDemoMode

        if (isNearby) {
            val completion = Completion(
                questId = quest.id,
                userId = username,
                completedAt = System.currentTimeMillis(),
                lat = userLat,
                lng = userLng,
                photoPath = photoPath,
                syncedToFirestore = false // Marqué non-synchronisé dans Room (Outbox)
            )
            completionDao.upsertCompletion(completion.toEntity(userId = username))

            // Synchronisation immédiate vers Firebase si le réseau est accessible (hors démo)
            if (!isDemoMode) {
                try {
                    val uploaded = withTimeoutOrNull(2500L) {
                        FirebaseSyncManager.uploadCompletion(
                            completion = completion,
                            isDemo = false,
                            username = username,
                            isAdmin = isAdmin
                        )
                    }
                    if (uploaded == true) {
                        completionDao.markSynced(quest.id, username)
                    }
                } catch (e: Exception) {
                    // Si échec réseau direct, le WorkManager ci-dessous s'en chargera
                }
            }

            // Déclenche l'Outbox en arrière-plan via WorkManager (Slide 50 & 54)
            SyncWorker.enqueue(context)

            Result.success(completion)
        } else {
            val distFormatted = if (distMeters >= 1000) {
                String.format(java.util.Locale.FRANCE, "%.1f km", distMeters / 1000.0)
            } else {
                "$distMeters m"
            }
            Result.failure(
                Exception(
                    "Trop éloigné du site : vous êtes à $distFormatted (rayon requis : ${quest.radiusM} m). Rapprochez-vous du site ou activez le Mode Démo pour vos tests !"
                )
            )
        }
    }

    suspend fun triggerSync() = withContext(Dispatchers.IO) {
        SyncWorker.enqueue(context)
    }

    suspend fun addCustomQuest(quest: Quest) = withContext(Dispatchers.IO) {
        // 1. Écriture immédiate dans Room (source de vérité locale)
        questDao.upsertQuest(quest.toEntity())

        // 2. Tentative d'envoi Firestore non-bloquante avec timeout court
        try {
            withTimeoutOrNull(2500L) {
                FirebaseSyncManager.uploadCustomQuest(quest)
            }
        } catch (e: Exception) {
            // Ignorer l'erreur réseau directe, le SyncWorker s'en chargera
        }

        // 3. Planifie la synchronisation en arrière-plan dès que le réseau est disponible (Pattern Outbox)
        SyncWorker.enqueue(context)
    }

    suspend fun deleteCustomQuest(questId: String) = withContext(Dispatchers.IO) {
        // 1. Suppression locale dans Room
        questDao.deleteQuest(questId)

        // 2. Suppression Firestore non-bloquante
        try {
            withTimeoutOrNull(2500L) {
                FirebaseSyncManager.deleteCustomQuest(questId)
            }
        } catch (e: Exception) {
            // Ignorer si hors-ligne
        }
    }
}
