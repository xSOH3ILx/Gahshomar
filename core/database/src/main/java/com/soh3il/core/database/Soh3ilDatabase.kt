package com.soh3il.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.soh3il.core.database.converter.DatabaseConverters
import com.soh3il.core.database.dao.OccasionDao
import com.soh3il.core.database.dao.ReminderDao
import com.soh3il.core.database.entity.OccasionEntity
import com.soh3il.core.database.entity.ReminderEntity

@Database(
    entities = [
        ReminderEntity::class,
        OccasionEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class Soh3ilDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
    abstract fun occasionDao(): OccasionDao

    companion object {
        private const val DB_NAME = "soh3il_calendar.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN showCountdown INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: Soh3ilDatabase? = null

        fun getInstance(context: Context): Soh3ilDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Soh3ilDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
