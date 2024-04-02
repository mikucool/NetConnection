package com.hzz.netconnection

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Environment
import android.text.format.Formatter
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hzz.netconnection.bean.local.FileInfo
import com.hzz.netconnection.bean.network.AudioInfo
import com.hzz.netconnection.data.NetworkRepository
import com.hzz.netconnection.data.LocalRepository
import com.hzz.netconnection.data.ServiceHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

class MainViewModel(private val localRepository: LocalRepository) : ViewModel() {

    private val _pingIp = MutableStateFlow("")
    val pingIp = _pingIp.asStateFlow()
    private val _url = MutableStateFlow("")
    val url = _url.asStateFlow()
    private val _isHttps = MutableStateFlow(false)
    val isHttps = _isHttps.asStateFlow()
    val logList = mutableStateListOf<String>()
    var audioInfoList = mutableStateListOf<AudioInfo>()
    private val formatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    private val serviceHolder = ServiceHolder()
    private var networkRepository: NetworkRepository? = null
    private val _isUrlPrepared = MutableStateFlow(false)
    val isUrlPrepared = _isUrlPrepared.asStateFlow()
    private var baseUrl = ""
    private val _autoSync = MutableStateFlow(false)
    val autoSync = _autoSync.asStateFlow()
    private val tempMd5List = mutableListOf<String>()
    private val saveJobs = mutableListOf<Job>()
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
        logList.addWithSizeCheck("${formatter.format(Date())} 🙃🙃 Remote Server Address: $baseUrl \n")
    }

    fun updateSchema(https: Boolean) {
        _isHttps.value = https
    }

    fun ping() {
        val runtime = Runtime.getRuntime()
        logList.addWithSizeCheck("${formatter.format(Date())} 😘😘ping -c 1 ${pingIp.value}\n")
        val exec = runtime.exec("/system/bin/ping -c 1 ${pingIp.value}")

        Thread {
            val res = exec.waitFor()
            if (res == 0) {
                logList.addWithSizeCheck("${formatter.format(Date())} 🥰🥰successfully.\n")
            } else {
                logList.addWithSizeCheck("${formatter.format(Date())} 😫😫failed.\n")
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
                    if (networkRepository == null) {
                        networkRepository = serviceHolder.obtainRepository(baseUrl, logList)
                    }
                    val res = networkRepository?.getAudiosInfo()
                    if (res != null) {
                        audioInfoList.clear()
                        audioInfoList.addAll(res)
                    }
                }
            } catch (e: Exception) {
                logList.addWithSizeCheck("${formatter.format(Date())} 😭😭${e.message}\n")
                e.printStackTrace()
            }
        }
    }

    fun downloadAudio(
        audioInfo: AudioInfo,
        context: Context
    ) {
        if (isUrlPrepared.value) {
            if (networkRepository == null) {
                networkRepository = serviceHolder.obtainRepository(baseUrl, logList)
            }
        } else return
        viewModelScope.launch {
            try {
                val response = networkRepository?.downloadAudio(audioInfo.link.linkToPath())
                val responseBody = response?.body()
                logList.addWithSizeCheck("${formatter.format(Date())} 😍😍 ${audioInfo.link.linkToFileName()} download......\n")
                if (response != null && responseBody != null) {
                    val job = launch(Dispatchers.IO) {
                        saveToLocal(
                            audioInfo = audioInfo,
                            source = responseBody.byteStream(),
                            context = context
                        )
                    }
                    saveJobs.add(job)
                }
            } catch (e: Exception) {
                logList.addWithSizeCheck("${formatter.format(Date())} 😭😭${e.message}\n")
                e.printStackTrace()
            }
        }
    }

    fun turnOffAutoSyncMode() {
        _autoSync.value = false
    }

    fun turnOnAutoSyncMode(context: Context) {
        autoSync(context)
    }

    private fun autoSync(context: Context) {
        if (autoSync.value) return
        thread(true) {
            _autoSync.value = true
            runBlocking {
                while (autoSync.value) {
                    delay(SYNC_INTERVAL_TIME)
                    val newerAudioInfoList = networkRepository?.getAudiosInfo()
                    if (!newerAudioInfoList.isNullOrEmpty()) {
                        if (audioInfoList.size != newerAudioInfoList.size) {
                            updateAudioInfoList(newerAudioInfoList)
                            downloadAllAudio(context)
                        } else if (audioInfoList.toList().containsAll(newerAudioInfoList)) {
                            continue
                        } else {
                            updateAudioInfoList(newerAudioInfoList)
                            downloadAllAudio(context)
                        }
                    }
                }
            }
        }
    }

    private fun updateAudioInfoList(list: List<AudioInfo>) {
        audioInfoList.clear()
        audioInfoList.addAll(list)
    }

    private fun downloadAllAudio(context: Context) {
        val fileMd5List = localRepository.getAllFileInfo().map { fileInfo -> fileInfo.md5 }
        // filter audio
        val availableAudios = audioInfoList
            .distinctBy { audioInfo -> audioInfo.link }
            .filter { audioInfo ->
                val audioMd5 = audioInfo.md5
                // if md5 existed in file_info table, indicate the audio is already downloaded
                // if md5 existed in tempMd5List, indicate the audio being processed
                if (fileMd5List.contains(audioMd5) || tempMd5List.contains(audioMd5)) {
                    false
                } else {
                    tempMd5List.add(audioMd5)
                    true
                }
            }
        logList.addWithSizeCheck("${formatter.format(Date())} 😪😪 update audio count:${availableAudios.size}\n")
        if (!isUrlPrepared.value || availableAudios.isEmpty()) return
        if (networkRepository == null) {
            networkRepository = serviceHolder.obtainRepository(baseUrl, logList)
        }

        availableAudios.forEach { audioInfo ->
            downloadAudio(audioInfo, context)
        }
    }

    private suspend fun saveToLocal(
        audioInfo: AudioInfo,
        source: InputStream,
        context: Context
    ) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, audioInfo.link.linkToFileName())
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
            logList.addWithSizeCheck("${formatter.format(Date())} 🥵🥵 ${audioInfo.link.linkToFileName()} download finished\n")
            logList.addWithSizeCheck("${formatter.format(Date())} 🥴🥴 save to $dir\n")
            localRepository.insertFileInfo(audioInfo.audioInfoToFileInfo())
        } catch (e: Exception) {
            e.printStackTrace()
            tempMd5List.remove(audioInfo.md5)
            logList.addWithSizeCheck("${formatter.format(Date())} 😱😱 ${e.message}\n")
        }

    }

    fun reset() {
        logList.addWithSizeCheck("${formatter.format(Date())} 🤪🤪-----Reset-----🤪🤪\n")
        saveJobs.forEach { job ->
            if (job.isActive) job.cancel()
        }
        saveJobs.clear()
        tempMd5List.clear()
        updateUrl("")
        audioInfoList.clear()
        _isUrlPrepared.value = false
        networkRepository = null
        _autoSync.value = false
    }

    fun clearLog() {
        logList.clear()
    }

    companion object {
        const val SYNC_INTERVAL_TIME = 3_000L
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as NetConnectionApplication)
                MainViewModel(application.localRepository)
            }
        }
    }

}

fun AudioInfo.audioInfoToFileInfo(): FileInfo {
    return FileInfo(id = 0, name = this.link.linkToFileName(), md5 = this.md5)
}

/**
 * example
 * receiver: "http://host:port/xxx/xx.mp3"
 * return: "xxx/xx.mp3"
 */
fun String.linkToPath(): String {
    // replace the space of the url passed
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

const val LOG_SIZE = 256
fun SnapshotStateList<String>.addWithSizeCheck(text: String) {
    if (this.size > LOG_SIZE) this.clear()
    this.add(text)
}