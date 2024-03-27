package com.hzz.netconnection

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Environment
import android.text.format.Formatter
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzz.netconnection.bean.AudioInfo
import com.hzz.netconnection.data.ConnectionRepository
import com.hzz.netconnection.data.ServiceHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.channels.Channels
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel : ViewModel() {

    private val _pingIp = MutableStateFlow("")
    val pingIp = _pingIp.asStateFlow()
    private val _url = MutableStateFlow("")
    val url = _url.asStateFlow()
    private val _isHttps = MutableStateFlow(false)
    val isHttps = _isHttps.asStateFlow()
    val logList = mutableStateListOf<String>()
    val audioInfoList = mutableStateListOf<AudioInfo>()
    private val formatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    private val serviceHolder = ServiceHolder()
    private var repository: ConnectionRepository? = null
    private val _isUrlPrepared = MutableStateFlow(false)
    val isUrlPrepared = _isUrlPrepared.asStateFlow()
    private var baseUrl = ""
    fun getIpInfo(context: Context): String {
        val wifiManager: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
    }

    fun updateServerIp(ip: String) {
        _pingIp.value = ip
    }

    fun updateUrl(url: String) {
        _url.value = url
    }

    fun updateUrlState(isPrepared: Boolean) {
        baseUrl = if (isHttps.value) "https://${url.value}" else "http://${url.value}"
        _isUrlPrepared.value = isPrepared
        updateLogList("${formatter.format(Date())} 🙃🙃 Remote Server Address: $baseUrl \n")
    }

    fun updateSchema(https: Boolean) {
        _isHttps.value = https
    }

    fun ping() {
        val runtime = Runtime.getRuntime()
        updateLogList("${formatter.format(Date())} 😘😘ping -c 1 ${pingIp.value}\n")
        val exec = runtime.exec("/system/bin/ping -c 1 ${pingIp.value}")

        Thread {
            val res = exec.waitFor()
            if (res == 0) {
                updateLogList("${formatter.format(Date())} 🥰🥰successfully.\n")
            } else {
                updateLogList("${formatter.format(Date())} 😫😫failed.\n")
            }
            exec.outputStream.close()
            exec.inputStream.close()
            exec.errorStream.close()
        }.start()
    }


    fun getAudiosInfo() {
        viewModelScope.launch {
            try {
                if (isUrlPrepared.value) {
                    if (repository == null) {
                        repository = serviceHolder.obtainRepository(baseUrl, logList)
                    }
                    val res = repository?.getAudiosInfo()
                    if (res != null) {
                        audioInfoList.clear()
                        audioInfoList.addAll(res)
                    }
                }
            } catch (e: Exception) {
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

    fun downloadAudio(
        audioInfo: AudioInfo,
        fileName: String,
        context: Context
    ) {
        if (isUrlPrepared.value) {
            if (repository == null) {
                repository = serviceHolder.obtainRepository(baseUrl, logList)
            }
        } else return
        logList.add("${formatter.format(Date())} 🥵🥵 download from ${audioInfo.link}\n")
        viewModelScope.launch {
            try {
                val response = repository?.downloadAudio(audioInfo.link.getAudioPath())
                val responseBody = response?.body()
                val contentSize = response?.raw()?.header("Content-Length")
                if (response != null && responseBody != null && contentSize != null) {
                    launch {
                        saveToLocal(
                            fileName = fileName,
                            source = responseBody.byteStream(),
                            size = contentSize.toLong(),
                            context = context
                        )
                    }
                }
            } catch (e: Exception) {
                logList.add("${formatter.format(Date())} 😭😭${e.message}\n")
                e.printStackTrace()
            }
        }
    }

    fun downloadAllAudio(context: Context) {
        // filter existed audio
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileNameList = dir?.list { _, fileName ->
            fileName.endsWith(".mp3")
        }

        val downloadableAudios = audioInfoList.filter { audioInfo ->
            val fileName = audioInfo.link.linkToFileName()
            if (fileNameList != null) {
                !fileNameList.contains(fileName)
            } else {
                true
            }
        }
        println("downloadableAudios:" + downloadableAudios.size)

        if (isUrlPrepared.value && downloadableAudios.isNotEmpty()) {
            if (repository == null) {
                repository = serviceHolder.obtainRepository(baseUrl, logList)
            }
        } else return
        downloadableAudios.forEach { audioInfo ->
            val fileName = audioInfo.link.linkToFileName()
            downloadAudio(audioInfo, fileName, context)
        }
    }

    private suspend fun saveToLocal(
        fileName: String,
        source: InputStream,
        size: Long,
        context: Context
    ) {
        delay(100)
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, fileName)
        logList.add("${formatter.format(Date())} 😍😍 download successfully\n")
        logList.add("${formatter.format(Date())} 🥺🥺 save to $dir\n")
        try {
            val randomAccessFile = RandomAccessFile(file, "rw")
            val randomAccessFileChannel = randomAccessFile.channel
            val sourceChannel = Channels.newChannel(source)
            var currentPosition = 0L
            var remainSize = size
            while (remainSize > 0) {
                val transferCount = randomAccessFileChannel.transferFrom(
                    sourceChannel,
                    currentPosition,
                    remainSize
                )
                if (transferCount > 0) {
                    remainSize -= transferCount
                    currentPosition += transferCount
                }
            }
            randomAccessFileChannel.close()
            randomAccessFile.close()
            sourceChannel.close()
            logList.add("${formatter.format(Date())} 🥴🥴 $fileName saved successfully\n")
        } catch (e: Exception) {
            e.printStackTrace()
            logList.add("${formatter.format(Date())} 😱😱 ${e.message}\n")
        }

    }

    /**
     * example
     * receiver is "http://host:port/xxx/xx.mp3"
     * return "xxx/xx.mp3"
     */
    private fun String.getAudioPath(): String {
        // replace the space in the url
        val availableUrl = this.replace(" ", "+")
        val split = availableUrl.split("/")
        var res = ""
        if (split.size > 3) {
            val sub = split.subList(3, split.size)
            val resBuilder = StringBuilder()
            for ((index, s) in sub.withIndex()) {
                if (index != sub.size - 1) resBuilder.append("$s/")
                else resBuilder.append(s)
            }
            res = resBuilder.toString()
        }
        return res
    }


    fun reset() {
        updateLogList("${formatter.format(Date())} 🤪🤪-----Reset-----🤪🤪\n")
        updateUrl("")
        audioInfoList.clear()
        _isUrlPrepared.value = false
        repository = null
    }

    private fun updateLogList(log: String) {
        logList.add(log)
    }

    fun clearLog() {
        logList.clear()
    }

}

fun String.linkToFileName(): String {
    val split = this.split("/")
    return split.last()
}