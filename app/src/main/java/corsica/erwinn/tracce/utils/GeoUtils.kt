package corsica.erwinn.tracce.utils

import kotlin.math.*

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /**
     * Calcule la distance orthodromique (Haversine) en mètres entre deux coordonnées GPS.
     */
    fun haversineDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) + cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Vérifie si l'utilisateur est dans le rayon de validation de la quête.
     */
    fun isWithinTargetRadius(
        userLat: Double, userLng: Double,
        targetLat: Double, targetLng: Double,
        radiusM: Int
    ): Boolean {
        val distance = haversineDistanceMeters(userLat, userLng, targetLat, targetLng)
        return distance <= radiusM
    }
}