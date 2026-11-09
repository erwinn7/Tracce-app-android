package corsica.erwinn.tracce.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import corsica.erwinn.tracce.MainActivity
import corsica.erwinn.tracce.TracceApplication
import corsica.erwinn.tracce.TracceContainer
import corsica.erwinn.tracce.data.model.Quest
import corsica.erwinn.tracce.utils.GeoUtils
import corsica.erwinn.tracce.utils.LocationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * FEATURE NATIVE IMPOSSIBLE EN PWA (Slides 6 & 13 du cours) :
 * Foreground Service de type "location" qui écoute la position GPS en continu
 * même lorsque l'application est en arrière-plan ou que l'écran est éteint dans la poche.
 * Déclenche une notification locale d'alerte dès que l'utilisateur approche d'un monument à < 250m.
 */
class ProximityRadarService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val alertedQuests = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    checkProximityToQuests(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Vérification stricte des permissions requises par Android 14+ (targetSdk 37)
        if (!LocationHelper.hasLocationPermission(this)) {
            Log.w(TAG, "Démarrage annulé : permission GPS manquante.")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            // 1. Notification persistante obligatoire avec spécification du type location (Slide 61)
            val notification = createPersistentNotification("Radar GPS actif : surveillance des sites à proximité...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    FOREGROUND_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification)
            }

            // 2. Démarrage de la surveillance GPS
            startLocationUpdates()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur au lancement du Foreground Service: ${e.message}", e)
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!LocationHelper.hasLocationPermission(this)) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L // Relevé toutes les 10 secondes
        ).apply {
            setMinUpdateIntervalMillis(5_000L)
            setMinUpdateDistanceMeters(10f) // Dès que l'utilisateur bouge de 10m
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException dans location updates: ${e.message}")
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Exception dans location updates: ${e.message}")
            stopSelf()
        }
    }

    private fun checkProximityToQuests(userLocation: Location) {
        serviceScope.launch {
            val quests = TracceContainer.repository.allQuests.first()
            val completedIds = TracceContainer.repository.getCompletedQuestIds().first()

            for (quest in quests) {
                if (quest.id in completedIds) continue // Ne pas alerter pour les quêtes déjà faites

                val distance = GeoUtils.haversineDistanceMeters(
                    lat1 = userLocation.latitude,
                    lon1 = userLocation.longitude,
                    lat2 = quest.lat,
                    lon2 = quest.lng
                )

                // Alerte si à moins de 250m et pas encore alerté dans cette session
                if (distance <= 250.0 && quest.id !in alertedQuests) {
                    alertedQuests.add(quest.id)
                    sendProximityAlert(quest, distance.toInt())
                }
            }
        }
    }

    private fun sendProximityAlert(quest: Quest, distanceMeters: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("questId", quest.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            quest.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alertNotification = NotificationCompat.Builder(this, TracceApplication.CHANNEL_TRACKER_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("🎯 Lieu à proximité (${distanceMeters}m) !")
            .setContentText("Vous approchez de « ${quest.name} » (+${quest.points} pts). Sortez votre appareil !")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(quest.id.hashCode(), alertNotification)
    }

    private fun createPersistentNotification(status: String) =
        NotificationCompat.Builder(this, TracceApplication.CHANNEL_TRACKER_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("🌿 Tracce — Radar de Randonnée")
            .setContentText(status)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            // Ignorer si déjà détaché
        }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ProximityRadarService"
        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_RADAR"
        const val ACTION_STOP = "ACTION_STOP_RADAR"

        fun startService(context: Context): Boolean {
            if (!LocationHelper.hasLocationPermission(context)) {
                Log.w(TAG, "Permission GPS non accordée, impossible de démarrer le radar.")
                return false
            }
            return try {
                val intent = Intent(context, ProximityRadarService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Échec lors de l'appel startForegroundService: ${e.message}", e)
                false
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, ProximityRadarService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Échec arrêt service: ${e.message}")
            }
        }
    }
}
