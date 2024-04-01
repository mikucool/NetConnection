package com.hzz.netconnection.bean.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "file_info", indices = [Index(value = ["md5"], unique = true)])
data class FileInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val md5: String
)
