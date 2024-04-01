package com.hzz.netconnection

import android.app.Application
import com.hzz.netconnection.data.LocalDatabase
import com.hzz.netconnection.data.LocalRepository

class NetConnectionApplication : Application() {
    lateinit var localRepository: LocalRepository
    override fun onCreate() {
        super.onCreate()
        val localDatabase = LocalDatabase.getDatabase(this.applicationContext)
        localRepository = LocalRepository(localDatabase.fileDao())
    }
}