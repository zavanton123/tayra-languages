package com.tayra.languages.core.data.yomitan

import com.tayra.languages.core.data.dictionary.zlibInflate

internal actual suspend fun inflateRaw(data: ByteArray, size: Int): ByteArray = zlibInflate(data, -15)
