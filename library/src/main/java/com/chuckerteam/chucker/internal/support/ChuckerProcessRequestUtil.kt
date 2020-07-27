/*
 * Copyright (C) 2020 Mike Penz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chuckerteam.chucker.internal.support

import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream

/**
 * Helper class to unbundle the processing of the request from the interceptor
 */
object ChuckerProcessRequestUtil {
    private val UTF8 = Charset.forName("UTF-8")
    private const val CONTENT_ENCODING = "Content-Encoding"

    /**
     * Processes a [Request] and populates corresponding fields of a [HttpTransaction].
     */
    internal fun processRequest(
            collector: ChuckerCollector,
            io: IOUtils,
            request: Request,
            transaction: HttpTransaction): Request {
        val encodingIsSupported = io.bodyHasSupportedEncoding(request.headers[CONTENT_ENCODING])

        transaction.apply {
            setRequestHeaders(request.headers)
            populateUrl(request.url)

            isRequestBodyPlainText = encodingIsSupported
            requestDate = System.currentTimeMillis()
            method = request.method
        }

        val body = request.body
        return if (body != null) {
            if (body.contentLength() > 0) {
                request.newBuilder().method(request.method, InterceptedRequestBody(request, body, collector, transaction, encodingIsSupported)).build()
            } else {
                transaction.requestContentType = body.contentType()?.toString()
                transaction.requestContentLength = body.contentLength()
                collector.onRequestSent(transaction)
                request
            }
        } else {
            collector.onRequestSent(transaction)
            request
        }
    }

    private class InterceptedRequestBody(
            val request: Request,
            val orgBody: RequestBody,
            val collector: ChuckerCollector,
            val transaction: HttpTransaction,
            val encodingIsSupported: Boolean
    ) : RequestBody() {
        override fun contentType(): MediaType? {
            return orgBody.contentType().also {
                transaction.requestContentType = it?.toString()
            }
        }

        override fun contentLength(): Long {
            return orgBody.contentLength().also {
                transaction.requestContentLength = it
            }
        }

        override fun writeTo(sink: BufferedSink) {
            orgBody.writeTo(object : ProxyBufferedSink(sink) {
                private var proxiedOutputStream: ProxyOutputStream? = null

                override fun outputStream(): OutputStream {
                    return object : ProxyOutputStream(sink.outputStream(), ByteArrayOutputStream()) {
                        override fun close() {
                            super.close()
                            internalComplete()
                        }
                    }.also {
                        proxiedOutputStream = it
                    }
                }

                override fun complete() {
                    if (!interceptedString.isNullOrEmpty()) {
                        transaction.requestBody = interceptedString?.substring(0, 500_000.coerceAtMost(interceptedString?.length ?: 0))
                    } else if (encodingIsSupported && proxiedOutputStream != null) {
                        var charset: Charset = UTF8
                        val contentType = orgBody.contentType()
                        if (contentType != null) {
                            charset = contentType.charset(UTF8) ?: UTF8
                        }

                        proxiedOutputStream?.let {
                            val byteArray = if (request.headers[CONTENT_ENCODING].equals("gzip", ignoreCase = true)) {
                                gzipUncompress(it.interceptingOutput.toByteArray())
                            } else {
                                it.interceptingOutput.toByteArray()
                            }

                            // try to identify if we are UTF-8
                            if (byteArray?.isProbablyUtf8() == true) {
                                val string = it.interceptingOutput.toString(charset.name())
                                transaction.requestBody = string.substring(0, 500_000.coerceAtMost(string.length))
                            } else {
                                transaction.isRequestBodyPlainText = false
                            }
                        } ?: kotlin.run {
                            transaction.isRequestBodyPlainText = false
                        }
                    } else if(orgBody is FormBody) {
                        transaction.requestBody = orgBody.toString()
                    } else {
                        transaction.isRequestBodyPlainText = false
                    }
                    collector.onRequestSent(transaction)
                }
            })
        }
    }
}


/**
 * Returns true if the body in question probably contains human readable text. Uses a small
 * sample of code points to detect unicode control characters commonly used in binary file
 * signatures.
 */
private fun ByteArray.isProbablyUtf8(): Boolean {
    val pText = this
    var expectedLength = 0
    var i = 0
    while (i < pText.size) {
        expectedLength = when {
            pText[i].toInt() and 128 == 0 -> 1
            pText[i].toInt() and 224 == 192 -> 2
            pText[i].toInt() and 240 == 224 -> 3
            pText[i].toInt() and 248 == 240 -> 4
            pText[i].toInt() and 252 == 248 -> 5
            pText[i].toInt() and 254 == 252 -> 6
            else -> return false
        }
        while (--expectedLength > 0) {
            if (++i >= pText.size) {
                return false
            }
            if (pText[i].toInt() and 192 != 128) {
                return false
            }
        }
        i++
    }
    return true
}

/**
 * Tries to uncrompress gzip content
 */
private fun gzipUncompress(compressedData: ByteArray): ByteArray? {
    try {
        ByteArrayInputStream(compressedData).use { bis ->
            ByteArrayOutputStream().use { bos ->
                GZIPInputStream(bis).use { gzipIS ->
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (gzipIS.read(buffer).also { len = it } != -1) {
                        bos.write(buffer, 0, len)
                    }
                    return bos.toByteArray()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}