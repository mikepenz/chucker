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
internal open class ProxyBufferedSink(val other: BufferedSink) : BufferedSink {

    var interceptedString: String? = null

    override val buffer: Buffer
        get() = other.buffer

    override fun buffer(): Buffer {
        return other.buffer()
    }

    override fun close() {
        other.close()
        complete()
    }

    override fun emit(): BufferedSink {
        return other.emit()
    }

    override fun emitCompleteSegments(): BufferedSink {
        return other.emitCompleteSegments()
    }

    override fun flush() {
        other.flush()
    }

    override fun isOpen(): Boolean {
        return other.isOpen
    }

    override fun outputStream(): OutputStream {
        return other.outputStream()
    }

    override fun timeout(): Timeout {
        return other.timeout()
    }

    override fun write(source: ByteArray): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, write byteArray")
        return other.write(source).also { complete() }
    }

    override fun write(source: ByteArray, offset: Int, byteCount: Int): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, write byteArray offset byteCount")
        return other.write(source, offset, byteCount).also { complete() }
    }

    override fun write(byteString: ByteString): BufferedSink {
        interceptedString = byteString.utf8()
        return other.write(byteString).also { complete() }
    }

    override fun write(byteString: ByteString, offset: Int, byteCount: Int): BufferedSink {
        interceptedString = byteString.utf8()
        Log.i("ProxyBufferedSink", "write split called multiple times?")
        return other.write(byteString, offset, byteCount).also { complete() }
    }

    override fun write(source: Source, byteCount: Long): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, source byteCount")
        return other.write(source, byteCount).also { complete() }
    }

    override fun write(source: Buffer, byteCount: Long) {
        Log.i("ProxyBufferedSink", "Interception unsupported, source byteCount")
        return other.write(source, byteCount).also { complete() }
    }

    override fun write(p0: ByteBuffer?): Int {
        Log.i("ProxyBufferedSink", "Interception unsupported, byteBuffer")
        return other.write(p0).also { complete() }
    }

    override fun writeAll(source: Source): Long {
        Log.i("ProxyBufferedSink", "Interception unsupported, writeAll source")
        return other.writeAll(source).also { complete() }
    }

    override fun writeByte(b: Int): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, writeByte int")
        return other.writeByte(b).also { complete() }
    }

    override fun writeDecimalLong(v: Long): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, writeDecimalLong")
        return other.writeDecimalLong(v).also { complete() }
    }

    override fun writeHexadecimalUnsignedLong(v: Long): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, writeHexadecimalUnsignedLong")
        return other.writeHexadecimalUnsignedLong(v).also { complete() }
    }

    override fun writeInt(i: Int): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, writeInt")
        return other.writeInt(i).also { complete() }
    }

    override fun writeIntLe(i: Int): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, writeIntLe")
        return other.writeIntLe(i).also { complete() }
    }

    override fun writeLong(v: Long): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, writeLong")
        return other.writeLong(v).also { complete() }
    }

    override fun writeLongLe(v: Long): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, writeLongLe")
        return other.writeLongLe(v).also { complete() }
    }

    override fun writeShort(s: Int): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, writeShort")
        return other.writeShort(s).also { complete() }
    }

    override fun writeShortLe(s: Int): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, writeShortLe")
        return other.writeShortLe(s).also { complete() }
    }

    override fun writeString(string: String, charset: Charset): BufferedSink {
        interceptedString = string
        return other.writeString(string, charset).also { complete() }
    }

    override fun writeString(string: String, beginIndex: Int, endIndex: Int, charset: Charset): BufferedSink {
        interceptedString = string
        Log.i("ProxyBufferedSink", "writeString split called multiple times?")
        return other.writeString(string, beginIndex, endIndex, charset).also { complete() }
    }

    override fun writeUtf8(string: String): BufferedSink {
        interceptedString = string
        return other.writeUtf8(string).also { complete() }
    }

    override fun writeUtf8(string: String, beginIndex: Int, endIndex: Int): BufferedSink {
        interceptedString = string
        Log.i("ProxyBufferedSink", "writeUtf8 split called multiple times?")
        return other.writeUtf8(string, beginIndex, endIndex).also { complete() }
    }

    override fun writeUtf8CodePoint(codePoint: Int): BufferedSink {
        Log.i("ProxyBufferedSink", "Interception unsupported, codePoint")
        return other.writeUtf8CodePoint(codePoint).also { complete() }
    }

    open fun complete() {
        // todo
    }
}