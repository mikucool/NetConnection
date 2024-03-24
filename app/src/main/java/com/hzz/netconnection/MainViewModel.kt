package com.hzz.netconnection

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzz.netconnection.data.ConnectionRepository
import com.hzz.netconnection.data.ServiceHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel : ViewModel() {

    private val _serverIp = MutableStateFlow("")
    val serverIp = _serverIp.asStateFlow()
    private val _url = MutableStateFlow("")
    val url = _url.asStateFlow()
    val logList = mutableStateListOf<String>()
    val fileList = mutableStateListOf<String>()
    private val formatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    private val serviceHolder = ServiceHolder()
    private var repository: ConnectionRepository? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    fun getIpInfo(context: Context): String {
        val connectivityManager = getSystemService(context, ConnectivityManager::class.java)
        val currentNetwork = connectivityManager?.activeNetwork
        val linkProperties = connectivityManager?.getLinkProperties(currentNetwork)
        val linkAddresses = linkProperties?.linkAddresses
        val stringBuilder = StringBuilder()
        if (linkAddresses?.size == 2) {
            val ipv6 = "IPv6: " + linkAddresses[0] + "\n"
            val ipv4 = "IPv4: " + linkAddresses[1]
            stringBuilder.append(ipv6)
            stringBuilder.append(ipv4)
        } else if (linkAddresses?.size == 1) {
            val ip = "IP: " + linkAddresses[0]
            stringBuilder.append(ip)
        }
        return stringBuilder.toString()
    }

    fun updateServerIp(ip: String) {
        _serverIp.value = ip
    }

    fun updateUrl(url: String) {
        _url.value = url
    }

    fun pingServer() {
        val runtime = Runtime.getRuntime()
        updateLogList("${formatter.format(Date())} 😘😘ping -c 1 ${serverIp.value}")
        val exec = runtime.exec("/system/bin/ping -c 1 ${serverIp.value}")
        Thread {
            val res = exec.waitFor()
            if (res == 0) {
                updateLogList("${formatter.format(Date())} 🥰🥰successfully.")
            } else {
                updateLogList("${formatter.format(Date())} 😫😫failed.")
            }
            exec.outputStream.close()
            exec.inputStream.close()
            exec.errorStream.close()
        }.start()
    }

    fun httpConnect() {
        viewModelScope.launch {
            try {
                if (url.value.isNotEmpty()) {
                    val baseUrl = url.value
                    if (repository == null || !isConnected.value) {
                        repository = serviceHolder.obtainRepository(baseUrl, logList)
                    }
                    repository?.httpConnect()
                    _isConnected.value = true
                }
            } catch (e: Exception) {
                _isConnected.value = false
                logList.add("${formatter.format(Date())} 😭😭${e.message}\n")
                e.printStackTrace()
            }
        }

    }


    fun toyaudio() {
        viewModelScope.launch {
            try {
                if (url.value.isNotEmpty()) {
                    val baseUrl = url.value
                    if (repository == null || !isConnected.value) {
                        repository = serviceHolder.obtainRepository(baseUrl, logList)
                    }
                    val fileNames = repository?.toyaudio()
                    if (fileNames != null) {
                        fileList.clear()
                        fileList.addAll(fileNames)
                    }
                    println("$fileNames")
                    _isConnected.value = true
                }
            } catch (e: Exception) {
                _isConnected.value = false
                logList.add("${formatter.format(Date())} 😭😭${e.message}\n")
                e.printStackTrace()
            }
        }
    }

    fun downloadFile(fileName: String, downloader: Downloader) {
        val urlFileName = fileName.replace(" ", "+")
        val downloadFileUrl = url.value + "/downloadFile?fileName=$urlFileName"
        logList.add("${formatter.format(Date())} 🥵🥵 download from $downloadFileUrl\n")
        downloader.downloadFile(downloadFileUrl, fileName)
    }

    fun reset() {
        updateLogList("${formatter.format(Date())} 🤪🤪-----Reset-----🤪🤪\n")
        updateUrl("")
        fileList.clear()
        _isConnected.value = false
        repository = null
    }

    private fun updateLogList(log: String) {
        logList.add(log)
    }

}

