package com.hal.carlistmanager

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CarEntry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carEntryDao(): CarEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "car_entry_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
