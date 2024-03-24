package com.hzz.netconnection.data

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.GsonBuilder
import com.hzz.netconnection.net.ConnectionService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface ConnectionRepository {
    suspend fun httpConnect()
    suspend fun toyaudio(): List<String>
}

class DefaultConnectionRepository(private val service: ConnectionService) : ConnectionRepository {
    override suspend fun httpConnect() = service.httpConnect()
    override suspend fun toyaudio(): List<String> = service.toyaudio()
}

class ServiceHolder {
    private val formatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    private val loggingInterceptor =
        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS)

    fun obtainRepository(
        baseUrl: String,
        logList: SnapshotStateList<String>
    ): ConnectionRepository {
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
        val service = retrofit.create(ConnectionService::class.java)
        return DefaultConnectionRepository(service)
    }
}