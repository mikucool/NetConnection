package com.hzz.netconnection.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hzz.netconnection.bean.local.FileInfo

@Database(entities = [FileInfo::class], version = 1, exportSchema = false)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao

    companion object {
        private var Instance: LocalDatabase? = null
        fun getDatabase(context: Context): LocalDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, LocalDatabase::class.java, "file_database")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}