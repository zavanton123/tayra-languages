package com.tayra.languages.core.data.dictionary

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.ZLIB_VERSION
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2_
import platform.zlib.z_stream

/** Decompresses gzip data with the system zlib; Foundation has no gzip API of its own. */
@OptIn(ExperimentalForeignApi::class)
internal fun gunzip(input: ByteArray): ByteArray = memScoped {
    val stream = alloc<z_stream>()
    // 15 window bits plus 16 tells zlib to expect a gzip header rather than a raw deflate stream.
    check(inflateInit2_(stream.ptr, 15 + 16, ZLIB_VERSION, sizeOf<z_stream>().toInt()) == Z_OK) { "zlib could not start inflating" }
    val chunks = mutableListOf<ByteArray>()
    try {
        input.usePinned { pinnedInput ->
            stream.next_in = pinnedInput.addressOf(0).reinterpret()
            stream.avail_in = input.size.toUInt()
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val status = buffer.usePinned { pinnedOut ->
                    stream.next_out = pinnedOut.addressOf(0).reinterpret()
                    stream.avail_out = buffer.size.toUInt()
                    inflate(stream.ptr, Z_NO_FLUSH)
                }
                check(status == Z_OK || status == Z_STREAM_END) { "zlib inflate failed with status $status" }
                val produced = buffer.size - stream.avail_out.toInt()
                if (produced > 0) chunks += buffer.copyOf(produced)
                if (status == Z_STREAM_END) break
            }
        }
    } finally {
        inflateEnd(stream.ptr)
    }
    val output = ByteArray(chunks.sumOf { it.size })
    var offset = 0
    chunks.forEach { chunk ->
        chunk.copyInto(output, offset)
        offset += chunk.size
    }
    output
}
