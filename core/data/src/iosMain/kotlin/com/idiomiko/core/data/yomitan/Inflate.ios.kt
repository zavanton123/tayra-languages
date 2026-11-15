package com.idiomiko.core.data.yomitan

import com.idiomiko.core.data.dictionary.zlibInflate

internal actual suspend fun inflateRaw(data: ByteArray, size: Int): ByteArray = zlibInflate(data, -15)
