package corsica.erwinn.tracce.data.model

data class Completion(
    val questId: String,
    val userId: String = "Explorateur",
    val completedAt: Long,
    val lat: Double,
    val lng: Double,
    val photoPath: String? = null,
    val syncedToFirestore: Boolean = false
)
