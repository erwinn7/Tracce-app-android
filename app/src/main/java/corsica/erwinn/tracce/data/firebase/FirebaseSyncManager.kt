package corsica.erwinn.tracce.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import corsica.erwinn.tracce.data.local.TracceDatabase
import corsica.erwinn.tracce.data.local.toEntity
import corsica.erwinn.tracce.data.model.Completion
import corsica.erwinn.tracce.data.model.Quest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Gestionnaire central de synchronisation Firebase Firestore & Auth pour Tracce Android.
 * Partage la même base de données cloud (tracce-corse) que la PWA.
 */
object FirebaseSyncManager {
    private const val TAG = "FirebaseSync"

    // Configuration officielle du projet Firebase tracce-corse
    private const val PROJECT_ID = "tracce-corse"
    private val API_KEY: String = corsica.erwinn.tracce.BuildConfig.FIREBASE_API_KEY
    private const val APPLICATION_ID = "1:695640493161:android:corsica.erwinn.tracce"
    private const val STORAGE_BUCKET = "tracce-corse.firebasestorage.app"
    private const val GCM_SENDER_ID = "695640493161"

    private var isInitialized = false
    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var applicationContext: Context? = null
    private var cachedDeviceUid: String? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
        if (isInitialized) return
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(PROJECT_ID)
                    .setApiKey(API_KEY)
                    .setApplicationId(APPLICATION_ID)
                    .setStorageBucket(STORAGE_BUCKET)
                    .setGcmSenderId(GCM_SENDER_ID)
                    .build()
                FirebaseApp.initializeApp(context, options)
            } else {
                FirebaseApp.getInstance()
            }

            firestore = FirebaseFirestore.getInstance(app)
            auth = FirebaseAuth.getInstance(app)
            isInitialized = true
            Log.i(TAG, "Firebase Firestore & Auth initialisés avec succès pour le projet $PROJECT_ID")

            // Connexion anonyme en arrière-plan si activée sur la console
            CoroutineScope(Dispatchers.IO).launch {
                getOrCreateUser()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur initialisation Firebase: ${e.message}", e)
        }
    }

    suspend fun getOrCreateUser(): FirebaseUser? {
        val a = auth ?: return null
        return a.currentUser ?: try {
            val result = a.signInAnonymously().await()
            Log.i(TAG, "Session Firebase Auth anonyme établie (uid=${result.user?.uid})")
            result.user
        } catch (e: Exception) {
            Log.d(TAG, "Auth anonyme non active sur la console (mode fallback UID actif)")
            null
        }
    }

    /**
     * Détermine l'UID effectif :
     * 1. Pseudo de l'explorateur (ex: 'user_erwinn') pour union avec la PWA et isolation par utilisateur
     * 2. UID Firebase Auth si disponible
     * 3. Ou UID matériel persistant pour un explorateur anonyme
     */
    fun getEffectiveUid(username: String): String {
        val cleanUser = username.trim().lowercase()
        if (cleanUser.isNotBlank() && cleanUser != "explorateur" && cleanUser != "explorateur anonyme") {
            return "user_$cleanUser"
        }

        val authUid = auth?.currentUser?.uid
        if (!authUid.isNullOrBlank()) return authUid

        if (cachedDeviceUid == null) {
            val ctx = applicationContext
            if (ctx != null) {
                val prefs = ctx.getSharedPreferences("tracce_firebase", Context.MODE_PRIVATE)
                var uid = prefs.getString("device_uid", null)
                if (uid == null) {
                    uid = "anon_" + java.util.UUID.randomUUID().toString().take(12)
                    prefs.edit().putString("device_uid", uid).apply()
                }
                cachedDeviceUid = uid
            } else {
                cachedDeviceUid = "anon_default"
            }
        }
        return cachedDeviceUid!!
    }

    /**
     * Récupère les complétions sauvegardées dans Firestore pour un utilisateur donné
     * (collection 'users/{uid}/completions' et tableau 'completedQuests').
     */
    suspend fun fetchUserCompletions(username: String): List<Completion> {
        val cleanUser = username.trim()
        if (cleanUser.isBlank() || cleanUser.equals("explorateur", ignoreCase = true) || cleanUser.equals("explorateur anonyme", ignoreCase = true)) {
            return emptyList()
        }
        val db = firestore ?: return emptyList()
        val uid = getEffectiveUid(cleanUser)
        return try {
            val list = mutableListOf<Completion>()
            val snapshot = db.collection("users").document(uid)
                .collection("completions").get().await()

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            for (doc in snapshot.documents) {
                try {
                    val questId = doc.getString("questId") ?: doc.id
                    val completedAtIso = doc.getString("completedAt")
                    val completedAt = if (completedAtIso != null) {
                        try {
                            sdf.parse(completedAtIso)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                    } else {
                        doc.getLong("completedAt") ?: System.currentTimeMillis()
                    }
                    val lat = doc.getDouble("lat") ?: 0.0
                    val lng = doc.getDouble("lng") ?: 0.0

                    list.add(
                        Completion(
                            questId = questId,
                            userId = cleanUser,
                            completedAt = completedAt,
                            lat = lat,
                            lng = lng,
                            photoPath = null,
                            syncedToFirestore = true
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Erreur parsing complétion doc ${doc.id}: ${e.message}")
                }
            }

            // Récupère aussi les quêtes listées dans le tableau completedQuests du doc utilisateur
            val userDoc = db.collection("users").document(uid).get().await()
            val completedQuests = (userDoc.get("completedQuests") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            for (qId in completedQuests) {
                if (list.none { it.questId == qId }) {
                    list.add(
                        Completion(
                            questId = qId,
                            userId = cleanUser,
                            completedAt = System.currentTimeMillis(),
                            lat = 0.0,
                            lng = 0.0,
                            photoPath = null,
                            syncedToFirestore = true
                        )
                    )
                }
            }

            Log.i(TAG, "Chargé ${list.size} complétion(s) depuis Firestore pour '$cleanUser' (users/$uid)")
            list
        } catch (e: Exception) {
            Log.w(TAG, "Impossible de récupérer les complétions de '$cleanUser': ${e.message}")
            emptyList()
        }
    }

    private fun getCurrentIsoDate(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }

    /**
     * Synchronise le document de profil utilisateur dans 'users/{uid}'
     */
    suspend fun syncUserProfile(
        username: String,
        isAdmin: Boolean,
        completedQuestIds: List<String> = emptyList()
    ) {
        val db = firestore ?: return
        val uid = getEffectiveUid(username)
        try {
            val userMap = mutableMapOf<String, Any>(
                "uid" to uid,
                "username" to (username.ifBlank { "Explorateur anonyme" }),
                "isAdmin" to isAdmin,
                "isAnonymous" to (auth?.currentUser?.isAnonymous ?: (username == "Explorateur")),
                "lastSeenAt" to getCurrentIsoDate()
            )
            if (completedQuestIds.isNotEmpty()) {
                userMap["completedQuests"] = FieldValue.arrayUnion(*completedQuestIds.toTypedArray())
            }

            db.collection("users").document(uid)
                .set(userMap, SetOptions.merge())
                .await()
            Log.i(TAG, "Profil utilisateur synchronisé dans Firestore (users/$uid)")
        } catch (e: Exception) {
            Log.w(TAG, "Échec synchronisation profil Firestore: ${e.message}")
        }
    }

    /**
     * Envoie une validation de quête réelle dans 'users/{uid}/completions/{questId}'
     * et met à jour 'users/{uid}' (les validations démo sont bloquées conformément à la règle PWA).
     */
    suspend fun uploadCompletion(
        completion: Completion,
        isDemo: Boolean = false,
        username: String = "Explorateur",
        isAdmin: Boolean = false
    ): Boolean {
        if (isDemo) {
            Log.d(TAG, "Mode Démo actif : écriture Firestore ignorée pour protéger la base.")
            return false
        }
        val db = firestore ?: return false
        val uid = getEffectiveUid(username)
        return try {
            val payload = mapOf(
                "questId" to completion.questId,
                "completedAt" to getCurrentIsoDate(completion.completedAt),
                "lat" to completion.lat,
                "lng" to completion.lng,
                "accuracyM" to 15.0,
                "distanceToTargetM" to 50.0
            )

            // 1. Écriture dans la sous-collection completions
            db.collection("users").document(uid)
                .collection("completions").document(completion.questId)
                .set(payload, SetOptions.merge())
                .await()

            // 2. Mise à jour du document parent utilisateur
            val userUpdate = mapOf(
                "uid" to uid,
                "username" to (username.ifBlank { "Explorateur anonyme" }),
                "isAdmin" to isAdmin,
                "lastSeenAt" to getCurrentIsoDate(),
                "lastCompletedQuest" to completion.questId,
                "lastCompletedAt" to getCurrentIsoDate(completion.completedAt),
                "completedQuests" to FieldValue.arrayUnion(completion.questId)
            )
            db.collection("users").document(uid)
                .set(userUpdate, SetOptions.merge())
                .await()

            Log.i(TAG, "Validation quête '${completion.questId}' synchronisée sur Firestore (users/$uid/completions) !")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Échec synchronisation Firestore complétion: ${e.message}")
            false
        }
    }

    /**
     * Envoie un nouveau lieu créé par l'Administrateur dans la collection racine 'quests/{questId}'
     * afin qu'il soit immédiatement visible par la PWA et par tous les autres appareils.
     */
    suspend fun uploadCustomQuest(quest: Quest): Boolean {
        val db = firestore ?: return false
        return try {
            val questMap = mutableMapOf<String, Any>(
                "id" to quest.id,
                "name" to quest.name,
                "cityId" to quest.cityId,
                "lat" to quest.lat,
                "lng" to quest.lng,
                "description" to quest.description,
                "points" to quest.points,
                "radiusM" to quest.radiusM,
                "type" to quest.type,
                "source" to (quest.source ?: "custom")
            )
            quest.altitudeM?.let { questMap["altitudeM"] = it }

            db.collection("quests").document(quest.id)
                .set(questMap, SetOptions.merge())
                .await()
            Log.i(TAG, "Nouveau lieu admin '${quest.name}' synchronisé dans Firestore collection 'quests' !")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Échec publication quête admin vers Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Supprime une quête personnalisée de Firestore
     */
    suspend fun deleteCustomQuest(questId: String): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("quests").document(questId).delete().await()
            Log.i(TAG, "Quête '$questId' supprimée de Firestore.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Échec suppression quête Firestore: ${e.message}")
            false
        }
    }

    /**
     * Écoute en temps réel la collection 'quests' de Firestore pour importer
     * automatiquement dans SQLite Room les quêtes ajoutées depuis la PWA ou d'autres téléphones.
     */
    fun startQuestsListener(database: TracceDatabase) {
        val db = firestore ?: return
        try {
            db.collection("quests").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Écoute Firestore 'quests' interrompue: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val remoteIds = mutableSetOf<String>()
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getString("id") ?: doc.id
                                remoteIds.add(id)
                                val name = doc.getString("name") ?: continue
                                val cityId = doc.getString("cityId") ?: "ajaccio"
                                val lat = doc.getDouble("lat") ?: continue
                                val lng = doc.getDouble("lng") ?: continue
                                val description = doc.getString("description") ?: ""
                                val points = doc.getLong("points")?.toInt() ?: 10
                                val radiusM = doc.getLong("radiusM")?.toInt() ?: 100
                                val altitudeM = doc.getLong("altitudeM")?.toInt()
                                val type = doc.getString("type") ?: "ville"
                                val source = doc.getString("source") ?: "custom"

                                val quest = Quest(
                                    id = id,
                                    name = name,
                                    cityId = cityId,
                                    lat = lat,
                                    lng = lng,
                                    description = description,
                                    points = points,
                                    radiusM = radiusM,
                                    altitudeM = altitudeM,
                                    type = type,
                                    source = source
                                )
                                database.questDao().upsertQuest(quest.toEntity())
                                Log.d(TAG, "Quête Firestore importée dans Room locale: $id ($name)")
                            } catch (e: Exception) {
                                Log.w(TAG, "Erreur parsing quête Firestore: ${e.message}")
                            }
                        }

                        // Pruning des quêtes personnalisées supprimées dans le Cloud
                        val localCustomQuests = database.questDao().getCustomQuests()
                        for (local in localCustomQuests) {
                            if (local.id !in remoteIds) {
                                database.questDao().deleteQuest(local.id)
                                Log.d(TAG, "Quête supprimée de Room suite à suppression distante: ${local.id}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Impossible d'attacher l'écouteur Firestore: ${e.message}")
        }
    }
}
