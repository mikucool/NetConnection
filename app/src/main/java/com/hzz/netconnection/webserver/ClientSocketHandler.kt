package com.hzz.netconnection.webserver

import android.content.Context
import android.os.Environment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.Socket
import java.nio.channels.Channels
import kotlin.concurrent.thread

class ClientSocketHandler(private val clientSocket: Socket, private val context: Context) {

    private val readInputStreamBufferSize = 4096

    fun respondAsync() {
        thread(start = true) {
            respond()
        }
    }

    private fun respond() {
        val outputStream = clientSocket.getOutputStream()
        val inputStream = clientSocket.getInputStream()

        // parse request
        val inputStreamBytes = readInputStream(inputStream)
        if (inputStreamBytes.isEmpty()) {
            outputStream.close()
            inputStream.close()
            return
        }
        val requestString = String(inputStreamBytes)
        val stringArray = requestString.split("\n")
        val headerList = mutableListOf<Pair<String, String>>()
        var path = ""
        println(requestString)
        // parse request
        /* example:
            GET /test.mp3 HTTP/1.1
            Content-Type: application/json
            User-Agent: PostmanRuntime/7.37.0
            Postman-Token: e0e7e538-dd3a-4eaa-a072-827a799844e3
            Host: 192.168.2.100:8888
            Accept-Encoding: gzip, deflate, br
            Connection: keep-alive
            Content-Length: 6

            "body"
         */
        for ((index, s) in stringArray.withIndex()) {
            if (s.isEmpty()) break
            if (index != 0) {
                val pair = s.split(": ")
                if (pair.size == 2) {
                    headerList.add(Pair(pair.first(), pair.last()))
                }
            } else {
                val split = s.split(" ")
                if (split.size >= 2) path = split[1]
            }
        }
        // build the response
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val availablePath = path.replace("%20", " ")
        val file = File(dir, availablePath)
        if (!file.exists() || file.isDirectory) {
            println("The source was not found, your path is $availablePath")
            val responseWithoutBody = DefaultBuilder(code = 404, description = "Not Found").build()
            val bytes = responseWithoutBody.toByteArray()
            outputStream.write(bytes)
            inputStream.close()
            outputStream.close()
            return
        }
        val randomAccessFile = RandomAccessFile(file, "rw")
        val randomAccessFileChannel = randomAccessFile.channel
        val responseWithoutBody = DefaultBuilder(code = 200, description = "OK")
            .addHeader("Content-Type: audio/mpeg, charset=utf-8")
            .addHeader("Content-Length: ${randomAccessFile.length()}")
            .addHeader("Content-Disposition: attachment; filename=\"blank\"")
            .build()
        val bytes = responseWithoutBody.toByteArray(Charsets.UTF_8)
        // response header
        outputStream.write(bytes)
        // response file source
        randomAccessFileChannel.transferTo(
            0,
            randomAccessFile.length(),
            Channels.newChannel(outputStream)
        )
        randomAccessFileChannel.close()
        randomAccessFile.close()
        outputStream.flush()
        outputStream.close()
    }

    private fun createBlankFile(file: File, length: Long): RandomAccessFile {
        val randomAccessFile = RandomAccessFile(file, "rw")
        randomAccessFile.setLength(length)
        return randomAccessFile
    }

    private fun readInputStream(inputStream: InputStream): ByteArray {
        val buffer = ByteArray(readInputStreamBufferSize)
        val len: Int = inputStream.read(buffer)
        if (len == -1) return ByteArray(0)
        val byteArrayOutputStream = ByteArrayOutputStream()
        byteArrayOutputStream.write(buffer, 0, len)
        byteArrayOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

}