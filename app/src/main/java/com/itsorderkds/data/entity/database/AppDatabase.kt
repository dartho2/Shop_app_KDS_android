package com.itsorderkds.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.itsorderkds.data.dao.OrderDao
import com.itsorderkds.data.entity.OrderEntity
import com.itsorderkds.data.entity.converters.Converters

@Database(
    entities = [OrderEntity::class],
    version = 22,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao

    companion object {
        // MIGRACJA: dodanie kolumny 'type' (TEXT NULL)
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE orders ADD COLUMN type TEXT")
            }
        }

        // MIGRACJA: dodanie kolumny 'couponTotalDiscount' (REAL NOT NULL DEFAULT 0.0)
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE orders ADD COLUMN couponTotalDiscount REAL NOT NULL DEFAULT 0.0")
            }
        }
    }
}
