package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DongGroup::class,
        GroupMember::class,
        Expense::class,
        Settlement::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DongDatabase : RoomDatabase() {
    abstract fun dongDao(): DongDao

    companion object {
        @Volatile
        private var INSTANCE: DongDatabase? = null

        fun getDatabase(context: Context): DongDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DongDatabase::class.java,
                    "dong_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
