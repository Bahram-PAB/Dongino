package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

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

        // Must live in the companion object because getDatabase() (also in the companion)
        // cannot access instance members of the enclosing class.
        private val sRoomDatabaseCallback = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Enable foreign key enforcement so CASCADE deletes declared in entities actually apply
                // (Room leaves foreign keys OFF by default).
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }

        fun getDatabase(context: Context): DongDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DongDatabase::class.java,
                    "dong_database"
                )
                .addCallback(sRoomDatabaseCallback)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
