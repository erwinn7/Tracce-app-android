package corsica.erwinn.tracce.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY cityId, name")
    fun getAllQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE id = :id")
    fun getQuestById(id: String): QuestEntity?

    @Query("SELECT * FROM quests WHERE cityId = :cityId")
    fun getQuestsByCity(cityId: String): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE source = 'custom' ORDER BY name")
    fun getCustomQuestsFlow(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE source = 'custom'")
    fun getCustomQuests(): List<QuestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertQuests(quests: List<QuestEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertQuest(quest: QuestEntity)

    @Query("DELETE FROM quests WHERE id = :id")
    fun deleteQuest(id: String): Int
}
