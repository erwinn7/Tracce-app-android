package corsica.erwinn.tracce.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import corsica.erwinn.tracce.data.model.Quest

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val cityId: String,
    val type: String,
    val name: String,
    val description: String,
    val lat: Double,
    val lng: Double,
    val altitudeM: Int?,
    val radiusM: Int,
    val points: Int,
    val source: String
) {
    fun toDomain(): Quest = Quest(
        id = id,
        cityId = cityId,
        type = type,
        name = name,
        description = description,
        lat = lat,
        lng = lng,
        altitudeM = altitudeM,
        radiusM = radiusM,
        points = points,
        source = source
    )
}

fun Quest.toEntity(): QuestEntity = QuestEntity(
    id = id,
    cityId = cityId,
    type = type,
    name = name,
    description = description,
    lat = lat,
    lng = lng,
    altitudeM = altitudeM,
    radiusM = radiusM,
    points = points,
    source = source
)