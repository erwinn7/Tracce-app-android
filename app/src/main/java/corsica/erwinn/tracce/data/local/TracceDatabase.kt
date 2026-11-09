package corsica.erwinn.tracce.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [QuestEntity::class, CompletionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class TracceDatabase : RoomDatabase() {
    abstract fun questDao(): QuestDao
    abstract fun completionDao(): CompletionDao

    companion object {
        @Volatile
        private var INSTANCE: TracceDatabase? = null

        /**
         * Migration 1 -> 2 (Slide 12 du cours : jamais fallbackToDestructiveMigration)
         * Ajout de la clé composite (questId, userId) pour partitionner les complétions par utilisateur.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS completions_new (
                        questId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        completedAt INTEGER NOT NULL,
                        lat REAL NOT NULL,
                        lng REAL NOT NULL,
                        photoPath TEXT,
                        synced INTEGER NOT NULL,
                        PRIMARY KEY(questId, userId)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO completions_new (questId, userId, completedAt, lat, lng, photoPath, synced)
                    SELECT questId, 'Explorateur', completedAt, lat, lng, photoPath, synced FROM completions
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE completions")
                db.execSQL("ALTER TABLE completions_new RENAME TO completions")
            }
        }

        fun getDatabase(context: Context): TracceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TracceDatabase::class.java,
                    "tracce.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pré-remplissage avec les 12 quêtes officielles de la PWA Tracce
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        database.questDao().upsertQuests(INITIAL_QUESTS)
                    }
                }
            }
        }

        val INITIAL_QUESTS = listOf(
            // AJACCIO
            QuestEntity("ajaccio-bonaparte", "ajaccio", "ville", "Maison Bonaparte", "Maison natale de Napoléon Bonaparte, au cœur de la vieille ville génoise.", 41.9180, 8.7386, 15, 80, 10, "osm"),
            QuestEntity("ajaccio-citadelle", "ajaccio", "ville", "Citadelle Miollis", "Forteresse génoise dominant le golfe d'Ajaccio et le port Tino Rossi.", 41.9168, 8.7397, 20, 100, 10, "osm"),
            QuestEntity("ajaccio-parata", "ajaccio", "site-naturel", "Pointe de la Parata", "Promontoire sauvage face aux îles Sanguinaires coiffé d'une tour génoise.", 41.8970, 8.6080, 55, 120, 25, "osm"),
            QuestEntity("ajaccio-gozzi", "ajaccio", "sommet", "Rocher des Gozzi", "Dôme de granit rouge culminant au-dessus d'Appietto avec vue plongeante.", 41.9961, 8.7711, 716, 120, 50, "osm"),

            // BASTIA
            QuestEntity("bastia-st-nicolas", "bastia", "ville", "Place Saint-Nicolas", "L'une des plus grandes places de France bordée de palmiers face à la mer.", 42.6995, 9.4503, 10, 100, 10, "osm"),
            QuestEntity("bastia-citadelle", "bastia", "ville", "Palais des Gouverneurs", "Ancienne résidence des gouverneurs génois dominant le vieux port de Bastia.", 42.6931, 9.4496, 45, 90, 10, "osm"),
            QuestEntity("bastia-biguglia", "bastia", "site-naturel", "Étang de Biguglia", "Plus grande lagune humide de Corse et refuge de milliers d'oiseaux migrateurs.", 42.6100, 9.4750, 2, 150, 25, "osm"),
            QuestEntity("bastia-serra-pigno", "bastia", "sommet", "Serra di Pigno", "Point culminant au-dessus de Bastia offrant une double vue sur les deux mers.", 42.6983, 9.3980, 960, 120, 50, "osm"),

            // CORTE
            QuestEntity("corte-belvedere", "corte", "ville", "Belvédère de Corte", "Point de vue spectaculaire sur le nid d'aigle de la citadelle perchée.", 42.3060, 9.1495, 430, 80, 10, "osm"),
            QuestEntity("corte-paoli", "corte", "ville", "Place Paoli & Musée", "Cœur historique de la capitale de la Corse indépendante du XVIIIe siècle.", 42.3048, 9.1510, 410, 90, 10, "osm"),
            QuestEntity("corte-restonica", "corte", "site-naturel", "Gorges de la Restonica", "Canyon spectaculaire creusé par les torrents de montagne aux eaux cristallines.", 42.2780, 9.1020, 650, 130, 25, "osm"),
            QuestEntity("corte-rotondo", "corte", "sommet", "Monte Rotondo", "Deuxième sommet de Corse à 2622 m offrant un panorama alpin à 360°.", 42.2164, 9.0589, 2622, 150, 50, "osm")
        )
    }
}
