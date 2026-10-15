package com.tayra.languages.core.data.yomitan

import java.util.zip.Inflater

internal actual suspend fun inflateRaw(data: ByteArray, size: Int): ByteArray {
    val inflater = Inflater(true)
    try {
        inflater.setInput(data)
        val out = ByteArray(size)
        var done = 0
        while (done < size) {
            val n = inflater.inflate(out, done, size - done)
            if (n == 0 && (inflater.finished() || inflater.needsInput())) break
            done += n
        }
        if (done != size) throw ZipException("Damaged zip: an entry inflated to $done of $size bytes")
        return out
    } finally {
        inflater.end()
    }
}
