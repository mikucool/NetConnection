package com.hzz.netconnection.data

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.GsonBuilder
import com.hzz.netconnection.bean.network.AudioInfo
import com.hzz.netconnection.net.AudioService
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface NetworkRepository {
    suspend fun httpConnect()
    suspend fun toyaudio(): List<String>

    suspend fun getAudiosInfo(): List<AudioInfo>

    suspend fun downloadAudio(url: String): Response<ResponseBody>
}

class DefaultNetworkRepository(private val service: AudioService) : NetworkRepository {
    override suspend fun httpConnect() = service.httpConnect()
    override suspend fun toyaudio(): List<String> = service.toyaudio()

    override suspend fun getAudiosInfo(): List<AudioInfo> = service.getAudiosInfo()

    override suspend fun downloadAudio(url: String): Response<ResponseBody> = service.downloadAudio(url = url)

}

class ServiceHolder {
    private val formatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    private val loggingInterceptor =
        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS)

    fun obtainRepository(
        baseUrl: String,
        logList: SnapshotStateList<String>
    ): NetworkRepository {
        // okHttpClient
        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                // adding log to list
                val stringBuilder = StringBuilder()
                val request = chain.request()
                stringBuilder.append("${formatter.format(Date())} 😘😘 ${request.method}\n")
                stringBuilder.append("${request.url}\n")
                request.headers.forEach { header ->
                    stringBuilder.append("${header.first}: ${header.second}\n")
                }
                logList.add(stringBuilder.toString())
                stringBuilder.clear()
                val response = chain.proceed(request)
                stringBuilder.append("${formatter.format(Date())} 🥳🥳Response Code: " + response.code.toString() + "\n")
                response.headers.forEach { header ->
                    stringBuilder.append("${header.first}: ${header.second}\n")
                }
                logList.add(stringBuilder.toString())
                response
            }
            .build()
        // retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().setLenient().create()
                )
            )
            .client(okHttpClient)
            .build()
        // service
        val service = retrofit.create(AudioService::class.java)
        return DefaultNetworkRepository(service)
    }
}