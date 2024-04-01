package com.hzz.netconnection.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hzz.netconnection.bean.local.FileInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(fileInfo: FileInfo)

    @Query("SELECT * from file_info")
    fun getAllFileInfo(): List<FileInfo>
}