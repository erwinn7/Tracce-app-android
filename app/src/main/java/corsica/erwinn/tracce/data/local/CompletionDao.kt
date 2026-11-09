package corsica.erwinn.tracce.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions WHERE userId = :userId ORDER BY completedAt DESC")
    fun getCompletionsForUser(userId: String): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions ORDER BY completedAt DESC")
    fun getAllCompletions(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE questId = :questId AND userId = :userId LIMIT 1")
    fun getCompletion(questId: String, userId: String): CompletionEntity?

    @Query("SELECT * FROM completions WHERE questId = :questId LIMIT 1")
    fun getCompletionByQuestId(questId: String): CompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertCompletion(completion: CompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertCompletions(completions: List<CompletionEntity>)

    @Query("SELECT * FROM completions WHERE synced = 0")
    fun getUnsynced(): List<CompletionEntity>

    @Query("UPDATE completions SET synced = 1 WHERE questId = :questId AND userId = :userId")
    fun markSynced(questId: String, userId: String): Int

    @Query("UPDATE completions SET synced = 1 WHERE questId = :questId")
    fun markSyncedForQuest(questId: String): Int

    @Query("SELECT questId FROM completions WHERE userId = :userId")
    fun getCompletedQuestIdsForUser(userId: String): List<String>

    @Query("SELECT questId FROM completions")
    fun getAllCompletedQuestIds(): List<String>
}
