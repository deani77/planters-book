package com.selendang3.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Empty by default. Users provide operational data through local imports and entry forms.
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


/** Stable estate and field identities for future offline entry screens. */
@Entity(tableName = "estate_identity")
data class EstateIdentity(
    @PrimaryKey val estateId: String,
    val displayName: String,
)

@Entity(
    tableName = "field_identity",
    primaryKeys = ["estateId", "fieldId"],
    foreignKeys = [ForeignKey(entity = EstateIdentity::class, parentColumns = ["estateId"], childColumns = ["estateId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["estateId"])]
)
data class FieldIdentity(val estateId: String, val fieldId: String, val displayCode: String)

/** All measurements are nullable: missing is not the same as zero. */
@Entity(
    tableName = "field_crop_entry",
    primaryKeys = ["estateId", "fieldId", "entryDate"],
    foreignKeys = [ForeignKey(entity = FieldIdentity::class, parentColumns = ["estateId", "fieldId"], childColumns = ["estateId", "fieldId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["estateId", "fieldId"])]
)
data class FieldCropEntry(
    val estateId: String, val fieldId: String, val entryDate: String,
    val harvestedHectares: Double?, val cropMt: Double?, val bunches: Int?,
)

@Entity(
    tableName = "field_upkeep_entry",
    primaryKeys = ["estateId", "fieldId", "entryDate", "activity"],
    foreignKeys = [ForeignKey(entity = FieldIdentity::class, parentColumns = ["estateId", "fieldId"], childColumns = ["estateId", "fieldId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["estateId", "fieldId"])]
)
data class FieldUpkeepEntry(
    val estateId: String, val fieldId: String, val entryDate: String,
    val activity: String, val areaHa: Double?, val quantity: Double?, val unit: String?,
)

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
    @Query("SELECT * FROM estate_snapshot WHERE slot = 2")
    suspend fun previousSnapshot(): EstateSnapshot?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSnapshot(value: EstateSnapshot)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSync(value: SyncState)
}

@Database(entities = [EstateField::class, SyncState::class, EstateSnapshot::class, EstateIdentity::class, FieldIdentity::class, FieldCropEntry::class, FieldUpkeepEntry::class], version = 3, exportSchema = true)
abstract class EstateDatabase : RoomDatabase() {
    abstract fun estateDao(): EstateDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `estate_snapshot` (`slot` INTEGER NOT NULL, `revision` INTEGER NOT NULL, `content` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`slot`))")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `estate_identity` (`estateId` TEXT NOT NULL, `displayName` TEXT NOT NULL, PRIMARY KEY(`estateId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `field_identity` (`estateId` TEXT NOT NULL, `fieldId` TEXT NOT NULL, `displayCode` TEXT NOT NULL, PRIMARY KEY(`estateId`, `fieldId`), FOREIGN KEY(`estateId`) REFERENCES `estate_identity`(`estateId`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_field_identity_estateId` ON `field_identity` (`estateId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `field_crop_entry` (`estateId` TEXT NOT NULL, `fieldId` TEXT NOT NULL, `entryDate` TEXT NOT NULL, `harvestedHectares` REAL, `cropMt` REAL, `bunches` INTEGER, PRIMARY KEY(`estateId`, `fieldId`, `entryDate`), FOREIGN KEY(`estateId`, `fieldId`) REFERENCES `field_identity`(`estateId`, `fieldId`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_field_crop_entry_estateId_fieldId` ON `field_crop_entry` (`estateId`, `fieldId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `field_upkeep_entry` (`estateId` TEXT NOT NULL, `fieldId` TEXT NOT NULL, `entryDate` TEXT NOT NULL, `activity` TEXT NOT NULL, `areaHa` REAL, `quantity` REAL, `unit` TEXT, PRIMARY KEY(`estateId`, `fieldId`, `entryDate`, `activity`), FOREIGN KEY(`estateId`, `fieldId`) REFERENCES `field_identity`(`estateId`, `fieldId`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_field_upkeep_entry_estateId_fieldId` ON `field_upkeep_entry` (`estateId`, `fieldId`)")
            }
        }
        @Volatile private var instance: EstateDatabase? = null
        fun get(context: Context): EstateDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, EstateDatabase::class.java, "estate.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
