package com.hzz.netconnection.net

import retrofit2.http.GET
import retrofit2.http.Query

interface ConnectionService {
    @GET("/")
    suspend fun httpConnect()

    @GET("/toyaudio")
    suspend fun toyaudio(): List<String>

    @GET("/downloadFile")
    suspend fun downloadFile(@Query("fileName") fileName: String)
}