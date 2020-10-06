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

import android.util.Log
import okio.*
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Proxies a buffered sink to another buffered sink, allowing to intercept its data
 */
internal abstract class ProxyBufferedSink(private val other: BufferedSink, val url: String = "") : BufferedSink {

    private var debug = false
    private var sentComplete = false
    var interceptedString: String? = null

    override val buffer: Buffer
        get() {
            if (debug) Log.d("ProxyBufferedSink", "getBuffer :: $url")
            return other.buffer.also { internalComplete() }
        }

    override fun buffer(): Buffer {
        if (debug) Log.d("ProxyBufferedSink", "buffer :: $url")
        return other.buffer().also { internalComplete() }
    }

    override fun close() {
        if (debug) Log.d("ProxyBufferedSink", "close :: $url")
        other.close()
        internalComplete()
    }

    override fun emit(): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, emit :: $url")
        return other.emit()
    }

    override fun emitCompleteSegments(): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, emitCompleteSegments :: $url")
        return other.emitCompleteSegments()
    }

    override fun flush() {
        if (debug) Log.d("ProxyBufferedSink", "flush :: $url")
        other.flush()
        internalComplete()
    }

    override fun isOpen(): Boolean {
        return other.isOpen
    }

    override fun outputStream(): OutputStream {
        if (debug) Log.d("ProxyBufferedSink", "outputStream :: $url")
        return other.outputStream()
    }

    override fun timeout(): Timeout {
        return other.timeout()
    }

    override fun write(source: ByteArray): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, write byteArray :: $url")
        return other.write(source).also { internalComplete() }
    }

    override fun write(source: ByteArray, offset: Int, byteCount: Int): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, write byteArray offset byteCount :: $url")
        return other.write(source, offset, byteCount).also { internalComplete() }
    }

    override fun write(byteString: ByteString): BufferedSink {
        interceptedString = byteString.utf8()
        if (debug) Log.d("ProxyBufferedSink", "write :: $url")
        return other.write(byteString).also { internalComplete() }
    }

    override fun write(byteString: ByteString, offset: Int, byteCount: Int): BufferedSink {
        interceptedString = byteString.utf8()
        if (debug) Log.d("ProxyBufferedSink", "write split called multiple times? :: $url")
        return other.write(byteString, offset, byteCount).also { internalComplete() }
    }

    override fun write(source: Source, byteCount: Long): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, source byteCount :: $url")
        return other.write(source, byteCount).also { internalComplete() }
    }

    override fun write(source: Buffer, byteCount: Long) {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, source byteCount :: $url")
        return other.write(source, byteCount).also { internalComplete() }
    }

    override fun write(p0: ByteBuffer?): Int {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, byteBuffer :: $url")
        return other.write(p0).also { internalComplete() }
    }

    override fun writeAll(source: Source): Long {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, writeAll source :: $url")
        return other.writeAll(source).also { internalComplete() }
    }

    override fun writeByte(b: Int): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, writeByte int :: $url")
        return other.writeByte(b).also { internalComplete() }
    }

    override fun writeDecimalLong(v: Long): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, writeDecimalLong :: $url")
        return other.writeDecimalLong(v).also { internalComplete() }
    }

    override fun writeHexadecimalUnsignedLong(v: Long): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, writeHexadecimalUnsignedLong :: $url")
        return other.writeHexadecimalUnsignedLong(v).also { internalComplete() }
    }

    override fun writeInt(i: Int): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, writeInt :: $url")
        return other.writeInt(i).also { internalComplete() }
    }

    override fun writeIntLe(i: Int): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, writeIntLe :: $url")
        return other.writeIntLe(i).also { internalComplete() }
    }

    override fun writeLong(v: Long): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, writeLong :: $url")
        return other.writeLong(v).also { internalComplete() }
    }

    override fun writeLongLe(v: Long): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, writeLongLe :: $url")
        return other.writeLongLe(v).also { internalComplete() }
    }

    override fun writeShort(s: Int): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, writeShort :: $url")
        return other.writeShort(s).also { internalComplete() }
    }

    override fun writeShortLe(s: Int): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, writeShortLe :: $url")
        return other.writeShortLe(s).also { internalComplete() }
    }

    override fun writeString(string: String, charset: Charset): BufferedSink {
        interceptedString = string
        if (debug) Log.d("ProxyBufferedSink", "writeString :: $url")
        return other.writeString(string, charset).also { internalComplete() }
    }

    override fun writeString(string: String, beginIndex: Int, endIndex: Int, charset: Charset): BufferedSink {
        interceptedString = string
        if (debug) Log.d("ProxyBufferedSink", "writeString split called multiple times? :: $url")
        return other.writeString(string, beginIndex, endIndex, charset).also { internalComplete() }
    }

    override fun writeUtf8(string: String): BufferedSink {
        interceptedString = string
        if (debug) Log.d("ProxyBufferedSink", "writeUtf8 :: $url")
        return other.writeUtf8(string).also { internalComplete() }
    }

    override fun writeUtf8(string: String, beginIndex: Int, endIndex: Int): BufferedSink {
        interceptedString = string
        if (debug) Log.d("ProxyBufferedSink", "writeUtf8 split called multiple times? :: $url")
        return other.writeUtf8(string, beginIndex, endIndex).also { internalComplete() }
    }

    override fun writeUtf8CodePoint(codePoint: Int): BufferedSink {
        if (debug) Log.d("ProxyBufferedSink", "Interception unsupported, codePoint :: $url")
        return other.writeUtf8CodePoint(codePoint).also { internalComplete() }
    }

    internal fun internalComplete() {
        if (debug) Log.d("ProxyBufferedSink", "internalComplete :: $url")
        if (sentComplete) {
            return
        }
        sentComplete = true
        complete()
    }

    abstract fun complete()
}