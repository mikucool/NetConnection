package com.hzz.netconnection.webserver

import java.lang.StringBuilder

// without response body
interface ResponseBuilder {
    val schema: String
    val code: Int
    val description: String
    val headerBuilder: StringBuilder
    fun addHeader(header: String): ResponseBuilder
    fun build(): String
}

class DefaultBuilder(
    override val schema: String = "HTTP/1.1",
    override val code: Int,
    override val description: String
) : ResponseBuilder {
    override val headerBuilder: StringBuilder = StringBuilder()
    override fun addHeader(header: String): ResponseBuilder {
        headerBuilder.appendLine(header)
        return this
    }

    override fun build(): String {
        headerBuilder.appendLine()
        return headerBuilder.toString()
    }

    init {
        if (code < 100 || code > 599) throw Exception("illegal status code")
        headerBuilder.appendLine("$schema $code $description")
    }


}
