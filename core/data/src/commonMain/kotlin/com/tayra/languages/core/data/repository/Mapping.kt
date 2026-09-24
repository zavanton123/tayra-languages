package com.tayra.languages.core.data.repository

import com.tayra.languages.core.data.db.Books
import com.tayra.languages.core.data.db.Language_dictionaries
import com.tayra.languages.core.data.db.Languages
import com.tayra.languages.core.data.db.Pages
import com.tayra.languages.core.data.db.Terms
import com.tayra.languages.core.domain.model.Book
import com.tayra.languages.core.domain.model.DictionaryType
import com.tayra.languages.core.domain.model.DictionaryUse
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.LanguageDictionary
import com.tayra.languages.core.domain.model.Page
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermRef
import com.tayra.languages.core.domain.model.TermStatus
import kotlin.time.Instant

internal fun Languages.toDomain(dictionaries: List<Language_dictionaries>): Language = Language(
    id = id,
    name = name,
    dictionaries = dictionaries.map { it.toDomain() },
    characterSubstitutions = character_substitutions,
    regexpSplitSentences = regexp_split_sentences,
    exceptionsSplitSentences = exceptions_split_sentences,
    wordCharacters = word_characters,
    rightToLeft = right_to_left,
    showRomanization = show_romanization,
    parserType = parser_type,
)

internal fun Language_dictionaries.toDomain(): LanguageDictionary = LanguageDictionary(
    id = id,
    useFor = DictionaryUse.fromKey(use_for),
    type = DictionaryType.fromKey(dict_type),
    url = url,
    isActive = is_active,
    sortOrder = sort_order.toInt(),
)

internal fun Books.toDomain(tags: List<String>): Book = Book(
    id = id,
    languageId = language_id,
    title = title,
    sourceUri = source_uri,
    currentPageId = current_page_id,
    archived = archived,
    audioFilename = audio_filename,
    audioCurrentPos = audio_current_pos,
    audioBookmarks = audio_bookmarks,
    tags = tags,
)

internal fun Pages.toDomain(): Page = Page(
    id = id,
    bookId = book_id,
    order = page_order.toInt(),
    text = text,
    wordCount = word_count.toInt(),
    startDate = start_date?.toInstant(),
    readDate = read_date?.toInstant(),
)

internal fun Terms.toDomain(parents: List<TermRef>): Term = Term(
    id = id,
    languageId = language_id,
    text = text,
    textLc = text_lc,
    status = TermStatus.fromValueOrNull(status.toInt()) ?: TermStatus.UNKNOWN,
    translation = translation,
    romanization = romanization,
    tokenCount = token_count.toInt(),
    syncStatus = sync_status,
    flashMessage = flash_message,
    parents = parents,
)

internal fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)
internal fun Instant.toEpochMillis(): Long = toEpochMilliseconds()

/** Escapes LIKE wildcards in user input. */
internal fun likeEscape(text: String): String = text.replace("%", "").replace("_", " ")
