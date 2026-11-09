package corsica.erwinn.tracce.data.local

import androidx.room.Entity
import corsica.erwinn.tracce.data.model.Completion

@Entity(
    tableName = "completions",
    primaryKeys = ["questId", "userId"]
)
data class CompletionEntity(
    val questId: String,
    val userId: String = "Explorateur",
    val completedAt: Long,
    val lat: Double,
    val lng: Double,
    val photoPath: String?,
    val synced: Boolean = false
) {
    fun toDomain(): Completion = Completion(
        questId = questId,
        userId = userId,
        completedAt = completedAt,
        lat = lat,
        lng = lng,
        photoPath = photoPath,
        syncedToFirestore = synced
    )
}

fun Completion.toEntity(userId: String = this.userId): CompletionEntity = CompletionEntity(
    questId = questId,
    userId = userId,
    completedAt = completedAt,
    lat = lat,
    lng = lng,
    photoPath = photoPath,
    synced = syncedToFirestore
)
