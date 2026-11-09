package corsica.erwinn.tracce.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double?
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Récupère la position géographique actuelle de l'appareil.
     * Fonctionne 100% hors-ligne via la puce GPS matérielle native de l'appareil.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): DeviceLocation? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission(context)) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        continuation.invokeOnCancellation {
            cts.cancel()
        }

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    continuation.resume(
                        DeviceLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            altitude = if (location.hasAltitude()) location.altitude else null
                        )
                    )
                } else {
                    // Fallback sur la dernière position connue Fused
                    fusedClient.lastLocation
                        .addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                continuation.resume(
                                    DeviceLocation(
                                        latitude = lastLoc.latitude,
                                        longitude = lastLoc.longitude,
                                        accuracy = lastLoc.accuracy,
                                        altitude = if (lastLoc.hasAltitude()) lastLoc.altitude else null
                                    )
                                )
                            } else {
                                // Fallback direct sur le capteur GPS natif Android (AOSP pur sans réseau)
                                val nativeLoc = getFallbackNativeLocation(context)
                                continuation.resume(nativeLoc)
                            }
                        }
                        .addOnFailureListener {
                            val nativeLoc = getFallbackNativeLocation(context)
                            continuation.resume(nativeLoc)
                        }
                }
            }
            .addOnFailureListener {
                fusedClient.lastLocation
                    .addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            continuation.resume(
                                DeviceLocation(
                                    latitude = lastLoc.latitude,
                                    longitude = lastLoc.longitude,
                                    accuracy = lastLoc.accuracy,
                                    altitude = if (lastLoc.hasAltitude()) lastLoc.altitude else null
                                )
                            )
                        } else {
                            val nativeLoc = getFallbackNativeLocation(context)
                            continuation.resume(nativeLoc)
                        }
                    }
                    .addOnFailureListener {
                        val nativeLoc = getFallbackNativeLocation(context)
                        continuation.resume(nativeLoc)
                    }
            }
    }

    /**
     * Fallback matériel natif Android (LocationManager) qui interroge directement
     * la puce GPS hors-ligne sans dépendre des serveurs Google Play Services.
     */
    @SuppressLint("MissingPermission")
    private fun getFallbackNativeLocation(context: Context): DeviceLocation? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        for (provider in providers) {
            if (lm.isProviderEnabled(provider)) {
                try {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null) {
                        return DeviceLocation(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy,
                            altitude = if (loc.hasAltitude()) loc.altitude else null
                        )
                    }
                } catch (e: SecurityException) {
                    // Ignore
                }
            }
        }
        return null
    }
}
