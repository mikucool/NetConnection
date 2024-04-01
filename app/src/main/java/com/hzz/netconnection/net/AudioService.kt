package com.hzz.netconnection.net

import com.hzz.netconnection.bean.network.AudioInfo
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface AudioService {
    @GET("/")
    suspend fun httpConnect()

    @GET("/toyaudio")
    suspend fun toyaudio(): List<String>

    @GET("/downloadFile")
    suspend fun downloadFile(@Query("fileName") fileName: String)

    @GET("/getAudios")
    suspend fun getAudiosInfo(): List<AudioInfo>

    @GET
    suspend fun downloadAudio(@Url url: String): Response<ResponseBody>

}