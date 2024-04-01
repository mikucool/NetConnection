package com.hzz.netconnection.data

import com.hzz.netconnection.bean.local.FileInfo

class LocalRepository(
    private val fileDao: FileDao,
) {
    fun getAllFileInfo(): List<FileInfo> = fileDao.getAllFileInfo()
    suspend fun insertFileInfo(fileInfo: FileInfo) = fileDao.insert(fileInfo)
}