package com.tayra.languages.core.data.export

import com.tayra.languages.core.domain.export.AnkiPackager

/** Writes Anki packages where SQLite files and zips can be made: on desktop and Android. */
expect class AnkiPackagerImpl() : AnkiPackager
