package com.hzz.netconnection

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Environment
import android.text.format.Formatter
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hzz.netconnection.bean.AudioInfo
import com.hzz.netconnection.data.ConnectionRepository
import com.hzz.netconnection.data.ServiceHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

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
    private var autoSync = true
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
        logList.addWithCheckSize("${formatter.format(Date())} 🙃🙃 Remote Server Address: $baseUrl \n")
    }

    fun updateSchema(https: Boolean) {
        _isHttps.value = https
    }

    fun ping() {
        val runtime = Runtime.getRuntime()
        logList.addWithCheckSize("${formatter.format(Date())} 😘😘ping -c 1 ${pingIp.value}\n")
        val exec = runtime.exec("/system/bin/ping -c 1 ${pingIp.value}")

        Thread {
            val res = exec.waitFor()
            if (res == 0) {
                logList.addWithCheckSize("${formatter.format(Date())} 🥰🥰successfully.\n")
            } else {
                logList.addWithCheckSize("${formatter.format(Date())} 😫😫failed.\n")
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
                logList.addWithCheckSize("${formatter.format(Date())} 😭😭${e.message}\n")
                e.printStackTrace()
            }
        }
    }

    fun downloadAudio(
        path: String,
        fileName: String,
        context: Context
    ) {
        if (isUrlPrepared.value) {
            if (repository == null) {
                repository = serviceHolder.obtainRepository(baseUrl, logList)
            }
        } else return
        logList.addWithCheckSize("${formatter.format(Date())} 🥵🥵 download $fileName\n")
        viewModelScope.launch {
            try {
                val response = repository?.downloadAudio(path)
                val responseBody = response?.body()
                logList.addWithCheckSize("${formatter.format(Date())} 😍😍 $fileName downloaded finished\n")
                if (response != null && responseBody != null) {
                    launch(Dispatchers.IO) {
                        saveToLocal(
                            fileName = fileName,
                            source = responseBody.byteStream(),
                            context = context
                        )
                    }
                }
            } catch (e: Exception) {
                logList.addWithCheckSize("${formatter.format(Date())} 😭😭${e.message}\n")
                e.printStackTrace()
            }
        }
    }

    fun switchAutoSyncMode() {
        autoSync = !autoSync
    }

    fun autoSync(context: Context) {
        thread(true) {
            runBlocking {
                while (autoSync) {
                    repository?.getAudiosInfo()
                    downloadAllAudio(context)
                    delay(3000)
                }
            }
        }
    }

    private fun downloadAllAudio(context: Context) {
        // filter existed audio
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileNameList = dir?.list { _, fileName ->
            fileName.endsWith(".mp3")
        }
        val availableAudios = audioInfoList.filter { audioInfo ->
            val fileName = audioInfo.link.linkToFileName()
            if (fileNameList != null) {
                !fileNameList.contains(fileName)
            } else {
                true
            }
        }
        logList.addWithCheckSize("${formatter.format(Date())} 😪😪 update audio count:${availableAudios.size}\n")
        if (!isUrlPrepared.value || availableAudios.isEmpty()) return
        if (repository == null) {
            repository = serviceHolder.obtainRepository(baseUrl, logList)
        }
        val linkList = availableAudios
            .map { audioInfo -> audioInfo.link }
            .distinct()
        linkList.forEach { link ->
            val fileName = link.linkToFileName()
            val path = link.linkToPath()
            downloadAudio(path, fileName, context)
        }
    }

    private suspend fun saveToLocal(
        fileName: String,
        source: InputStream,
        context: Context
    ) {
        delay(100)
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, fileName)
        logList.addWithCheckSize("${formatter.format(Date())} 🥺🥺 save to $dir\n")
        try {
            val buffer = ByteArray(4096)
            val outputStream = FileOutputStream(file)
            while (true) {
                val count = source.read(buffer)
                if (count == -1) break
                outputStream.write(buffer, 0, count)
            }
            outputStream.close()
            source.close()
            logList.addWithCheckSize("${formatter.format(Date())} 🥴🥴 $fileName saved successfully\n")
        } catch (e: Exception) {
            e.printStackTrace()
            logList.addWithCheckSize("${formatter.format(Date())} 😱😱 ${e.message}\n")
        }

    }

    fun reset() {
        logList.addWithCheckSize("${formatter.format(Date())} 🤪🤪-----Reset-----🤪🤪\n")
        updateUrl("")
        audioInfoList.clear()
        _isUrlPrepared.value = false
        repository = null
        autoSync = false
    }

    fun clearLog() {
        logList.clear()
    }

}

/**
 * example
 * receiver: "http://host:port/xxx/xx.mp3"
 * return: "xxx/xx.mp3"
 */
fun String.linkToPath(): String {
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

fun String.linkToFileName(): String {
    val split = this.split("/")
    return split.last()
}

fun SnapshotStateList<String>.addWithCheckSize(text: String) {
    if (this.size > 256) this.clear()
    this.add(text)
}