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
import okhttp3.Headers
import okhttp3.Request
import okio.Buffer
import okio.EOFException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream

/**
 * Helper class to unbundle the processing of the request from the interceptor
 */
object ChuckerOkHttpProcessRequestUtil {
    private const val CONTENT_ENCODING = "Content-Encoding"

    /**
     * Processes a [Request] and populates corresponding fields of a [HttpTransaction].
     */
    internal fun processRequest(
            collector: ChuckerCollector,
            io: IOUtils,
            request: Request,
            transaction: HttpTransaction): Request {

        transaction.apply {
            setRequestHeaders(request.headers)
            populateUrl(request.url)

            requestDate = System.currentTimeMillis()
            method = request.method
            requestContentType = request.body?.contentType()?.toString()
            requestContentLength = request.body?.contentLength() ?: 0L
        }

        val body = request.body
        if (body != null && body.contentLength() > 0) {
            val headers = request.headers

            // Request body headers are only present when installed as a network interceptor. When not
            // already present, force them to be included (if available) so their values are known.
            body.contentType()?.let {
                if (headers["Content-Type"] == null) {
                    transaction.requestContentType = it.type
                }
            }

            if (bodyHasUnknownEncoding(request.headers)) {
                transaction.requestBody = "(encoded body omitted)"
            } else if (body.isDuplex()) {
                transaction.requestBody = "(duplex request body omitted)"
            } else if (body.isOneShot()) {
                transaction.requestBody = "(one-shot body omitted)"
            } else {
                val buffer = Buffer()
                body.writeTo(buffer)

                val contentType = body.contentType()
                val charset: Charset = contentType?.charset(UTF_8) ?: UTF_8

                if (buffer.isProbablyUtf8()) {
                    transaction.requestBody = buffer.readString(charset)
                } else if (request.headers[CONTENT_ENCODING].equals("gzip", ignoreCase = true)) {
                    val nonCompressed = gzipUncompress(buffer.readByteArray())
                    transaction.isRequestBodyPlainText = false
                    if (nonCompressed?.isProbablyUtf8() == true) {
                        try {
                            val string = String(nonCompressed, Charset.forName(charset.name()))
                            transaction.requestBody = string.substring(0, 500_000.coerceAtMost(string.length))
                            transaction.isRequestBodyPlainText = true
                        } catch (ex: java.lang.Exception) {
                            // no-op
                        }
                    }
                } else {
                    transaction.isRequestBodyPlainText = false
                }
            }
        }

        collector.onRequestSent(transaction)
        return request
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
                !contentEncoding.equals("gzip", ignoreCase = true)
    }
}

/**
 * Returns true if the body in question probably contains human readable text. Uses a small
 * sample of code points to detect unicode control characters commonly used in binary file
 * signatures.
 */
internal fun Buffer.isProbablyUtf8(): Boolean {
    try {
        val prefix = Buffer()
        val byteCount = size.coerceAtMost(64)
        copyTo(prefix, 0, byteCount)
        for (i in 0 until 16) {
            if (prefix.exhausted()) {
                break
            }
            val codePoint = prefix.readUtf8CodePoint()
            if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                return false
            }
        }
        return true
    } catch (_: EOFException) {
        return false // Truncated UTF-8 sequence.
    }
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
