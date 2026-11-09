package corsica.erwinn.tracce.data.model

data class Quest(
    val id: String,
    val cityId: String,       // "ajaccio", "bastia", "corte"
    val type: String,         // "ville", "colline", "site-naturel", "sommet"
    val name: String,
    val description: String,
    val lat: Double,
    val lng: Double,
    val altitudeM: Int? = null,
    val radiusM: Int = 100,
    val points: Int = 10,
    val source: String = "osm"
)