package com.hzz.netconnection.webserver

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class FileServer(var port: Int, private val context: Context) {
    companion object;

    private lateinit var receiveThread: Thread
    private var listenerSocket: ServerSocket

    init {
        val wifiManager: WifiManager =
            this.context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiIpAddress: String = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        // setting socket's configuration
        listenerSocket = ServerSocket()
        listenerSocket.reuseAddress = true
        var isAvailablePort = false
        while (!isAvailablePort) {
            try {
                listenerSocket.bind(InetSocketAddress(wifiIpAddress, port))
                isAvailablePort = true
            } catch (e: Exception) {
                port += 1
                e.printStackTrace()
            }
        }
    }

    fun start(keepRunning: Boolean = true) {

        receiveThread = thread(true) {
            while (keepRunning) {
                val clientSocket: Socket = listenerSocket.accept()
                clientSocket.soTimeout = 5000
                val clientSocketHandler = ClientSocketHandler(clientSocket, context)
                clientSocketHandler.respondAsync()
            }
        }
    }

}