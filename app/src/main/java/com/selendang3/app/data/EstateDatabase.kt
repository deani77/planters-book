package com.selendang3.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Empty by default. Operational data must come from a future authenticated import.
@Entity(tableName = "estate_fields")
data class EstateField(
    @PrimaryKey val code: String,
    val alternateCode: String?,
    val hectares: Double,
    val palms: Int,
    val reportingYear: Int,
    val verified: Boolean = false,
)

@Entity(tableName = "sync_state")
data class SyncState(@PrimaryKey val id: Int = 1, val lastSuccessEpochMillis: Long?)

@Entity(tableName = "estate_snapshot")
data class EstateSnapshot(@PrimaryKey val slot: Int = 1, val revision: Long,
    val content: String, val updatedAt: Long, val source: String)

@Dao
interface EstateDao {
    @Query("SELECT * FROM estate_fields ORDER BY code")
    fun observeFields(): Flow<List<EstateField>>

    @Query("SELECT * FROM sync_state WHERE id = 1")
    fun observeSync(): Flow<SyncState?>

    @Query("SELECT * FROM estate_snapshot WHERE slot = 1")
    fun observeSnapshot(): Flow<EstateSnapshot?>
    @Query("SELECT * FROM estate_snapshot WHERE slot = 1")
    suspend fun currentSnapshot(): EstateSnapshot?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSnapshot(value: EstateSnapshot)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSync(value: SyncState)
}

@Database(entities = [EstateField::class, SyncState::class, EstateSnapshot::class], version = 2, exportSchema = true)
abstract class EstateDatabase : RoomDatabase() {
    abstract fun estateDao(): EstateDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `estate_snapshot` (`slot` INTEGER NOT NULL, `revision` INTEGER NOT NULL, `content` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`slot`))")
            }
        }
        @Volatile private var instance: EstateDatabase? = null
        fun get(context: Context): EstateDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, EstateDatabase::class.java, "estate.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
