package com.tayra.languages.core.data.export

import com.tayra.languages.core.domain.export.AnkiCollection
import com.tayra.languages.core.domain.export.AnkiPackager
import kotlin.time.Instant

actual class AnkiPackagerImpl : AnkiPackager {
    override suspend fun pack(collection: AnkiCollection, now: Instant): ByteArray =
        throw UnsupportedOperationException("Anki export is available on desktop and Android for now")
}
