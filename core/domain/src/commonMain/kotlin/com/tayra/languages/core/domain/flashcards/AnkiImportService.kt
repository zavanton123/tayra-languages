package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.FlashcardRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.term.TermTextNormalizer
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Brings the cards of an Anki package into the flashcards. A note is read for its word: a note
 * laid out as Tayra and Language Reactor export them by its Word field, a cloze note by its first
 * blank. Anki's scheduling wins: a word's card takes Anki's state, due date, interval, memory,
 * reviews and lapses. A word without a card gets one, and a word not saved yet is saved, at the
 * status its interval earns as reviewing would give it; a word already Known or Ignored keeps its
 * status. Anki's answers join the card's history, those already in it left out, so importing the
 * same package again changes nothing.
 */
class AnkiImportService(
    private val reader: AnkiArchiveReader,
    private val cards: FlashcardRepository,
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val scheduler: FlashcardScheduler = FlashcardScheduler(),
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val fsrs: Fsrs = Fsrs(),
) {
    /**
     * What an import did: words saved, saved words given a card, cards brought up to date, answers
     * added to the history, notes with no word to read, and the languages of notes left out as not
     * learned here.
     */
    data class Report(val added: Int, val carded: Int, val updated: Int, val reviews: Int, val unreadable: Int, val missingLanguages: List<String>)

    val available: Boolean get() = reader.available

    /** Imports the package in [bytes]; [onProgress] reports how many words are done out of the total. */
    suspend fun import(bytes: ByteArray, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): Report {
        val archive = reader.read(bytes)
        val now = clock.now()
        val config = FlashcardService.schedulerConfig(settings.current)
        val known = languages.getAll()
        val current = known.firstOrNull { it.id == settings.current.currentLanguageId }
        var unreadable = 0
        val missing = linkedSetOf<String>()
        val entries = archive.notes.mapNotNull { note ->
            val card = note.cards.maxWithOrNull(compareBy<AnkiArchiveCard> { it.reps }.thenBy { it.lastReview })
            val read = card?.let { readNote(note, it) }
            if (read == null) {
                unreadable++
                return@mapNotNull null
            }
            val language = languageOf(read, card, known, current)
            if (language == null) {
                missing += read.languageCode ?: card.deck
                return@mapNotNull null
            }
            Entry(read, language, TermTextNormalizer.termFromText(language, read.word))
        }
        // A word in several notes is taken from its most answered card.
        val chosen = entries.groupBy { it.language.id to it.spec.textLc }.values
            .map { same -> same.maxWith(compareBy<Entry> { it.note.card.reps }.thenBy { it.note.card.lastReview }) }

        var added = 0
        var carded = 0
        var updated = 0
        var reviews = 0
        val inAnki = mutableListOf<Long>()
        val stored = chosen.groupBy { it.language.id }.flatMap { (languageId, group) ->
            terms.findByTextLcs(languageId, group.map { it.spec.textLc })
        }.associateBy { it.languageId to it.textLc }
        chosen.forEachIndexed { index, entry ->
            onProgress(index, chosen.size)
            val note = entry.note
            val (schedule, suspended) = schedule(note.card, note.created ?: now, archive.dayZero, config)
            val earned = if (schedule.state.isReview || schedule.state == CardState.RELEARNING) FlashcardService.statusFor(schedule.intervalDays) else TermStatus.NEW_1
            val term = stored[entry.language.id to entry.spec.textLc]
            // Read before the word changes: a word made learning gets a plain card of its own as soon as the cards are next looked at.
            val before = term?.let { cards.get(it.id) }
            val termId: Long
            val status: TermStatus
            if (term == null) {
                status = earned
                termId = terms.save(
                    entry.spec.copy(
                        status = status,
                        translation = note.translation,
                        romanization = entry.spec.romanization ?: note.romanization,
                        sentence = note.sentence,
                    ),
                )
                added++
            } else {
                termId = term.id
                status = when {
                    term.status == TermStatus.UNKNOWN -> earned
                    term.status.isLearning && earned.value > term.status.value -> earned
                    else -> term.status
                }
                if (status != term.status || (term.sentence == null && note.sentence != null)) {
                    terms.save(term.copy(status = status, sentence = term.sentence ?: note.sentence))
                }
                if (term.translation.isNullOrBlank() && note.translation != null) terms.fillTranslation(termId, note.translation)
            }
            when {
                term == null -> Unit
                before == null -> carded++
                else -> updated++
            }
            if (term?.ankiExportedAt == null) inAnki += termId
            cards.save(Flashcard(termId, schedule, suspended, seenStatus = status, createdAt = before?.createdAt ?: note.created ?: now))
            reviews += addReviews(termId, note.card, config)
        }
        // Their notes are in Anki already, so exporting them would only add copies.
        terms.markAnkiExported(inAnki, now)
        onProgress(chosen.size, chosen.size)
        return Report(added, carded, updated, reviews, unreadable, missing.toList())
    }

    /** A note read for its word, with the card chosen for it. */
    internal class ReadNote(
        val word: String,
        val languageCode: String?,
        val sentence: String?,
        val translation: String?,
        val romanization: String?,
        val created: Instant?,
        val card: AnkiArchiveCard,
    )

    private class Entry(val note: ReadNote, val language: Language, val spec: Term)

    /** The note's word and what goes with it; null when it has none to read. */
    internal fun readNote(note: AnkiArchiveNote, card: AnkiArchiveCard): ReadNote? {
        val fields = note.fields
        val code = fields["Language"]?.trim()?.lowercase()?.ifEmpty { null }
            ?: note.tags.firstNotNullOfOrNull { LANGUAGE_TAG.matchEntire(it)?.groupValues?.get(1)?.lowercase() }
        val noteCreated = note.id.takeIf { it > EARLIEST_NOTE_ID }?.let { Instant.fromEpochMilliseconds(it) }
        val word = fields["Word"]?.let(::plain)?.ifEmpty { null }
        if (word != null) {
            val sentence = (fields["Subtitle"] ?: fields["Context"])?.let(::plain)?.ifEmpty { null }?.takeIf { it != word }
            return ReadNote(
                word = word,
                languageCode = code,
                sentence = sentence,
                translation = fields["Word Definition"]?.let(::plain)?.ifEmpty { null },
                romanization = fields["Word Transliteration"]?.let(::plain)?.ifEmpty { null },
                created = fields["Date Created"]?.let(::stamp) ?: noteCreated,
                card = card,
            )
        }
        val text = fields.values.firstOrNull { CLOZE.containsMatchIn(it) } ?: return null
        val blank = CLOZE.find(text)!!
        return ReadNote(
            word = plain(blank.groupValues[1]).ifEmpty { return null },
            languageCode = code,
            sentence = plain(CLOZE.replace(text) { it.groupValues[1] }).ifEmpty { null },
            translation = blank.groupValues[2].let(::plain).ifEmpty { null },
            romanization = null,
            created = noteCreated,
            card = card,
        )
    }

    /** The note's language: from its Language field or tag, else the deck it is in, else the language being learned when nothing names one. */
    private fun languageOf(read: ReadNote, card: AnkiArchiveCard, known: List<Language>, current: Language?): Language? {
        read.languageCode?.let { code -> return known.firstOrNull { LanguageCodes.codeFor(it.name) == code } }
        val deck = card.deck.substringAfterLast("::").trim()
        return known.firstOrNull { it.name.equals(deck, ignoreCase = true) } ?: current
    }

    /** The card as scheduled here, and whether it is suspended. */
    internal fun schedule(card: AnkiArchiveCard, created: Instant, dayZero: Instant, config: SchedulerConfig): Pair<CardSchedule, Boolean> {
        val state = when (card.type) {
            1 -> CardState.LEARNING
            2 -> CardState.REVIEW
            3 -> CardState.RELEARNING
            else -> CardState.NEW
        }
        val due = card.originalDue ?: card.due
        // Day 0 starts at Anki's rollover hour; half a day in keeps a change of clocks from moving the day.
        fun onDay(day: Long) = scheduler.dayStart(scheduler.day(dayZero + day.days + 12.hours, config), config)
        val dueAt = when (state) {
            CardState.NEW -> created
            CardState.REVIEW -> onDay(due)
            else -> if (due > EPOCH_SECONDS_FROM) Instant.fromEpochSeconds(due) else onDay(due)
        }
        val steps = if (state == CardState.RELEARNING) config.relearnSteps else config.learnSteps
        val stepping = state == CardState.LEARNING || state == CardState.RELEARNING
        val memory = if (card.stability != null && card.difficulty != null) MemoryState(card.stability, card.difficulty) else replayed(card, config)
        val schedule = CardSchedule(
            state = state,
            due = dueAt,
            memory = memory.takeIf { state != CardState.NEW },
            intervalDays = if (state == CardState.REVIEW || state == CardState.RELEARNING) card.intervalDays.coerceAtLeast(0) else 0,
            remainingSteps = if (stepping && steps.isNotEmpty()) (card.left % 1000).coerceIn(1, steps.size) else 0,
            reps = card.reps,
            lapses = card.lapses,
            lastReview = (card.lastReview ?: card.reviews.filter { it.ease in 1..4 }.maxOfOrNull { it.at }).takeIf { state != CardState.NEW },
        )
        return schedule to (card.queue == -1)
    }

    /**
     * The memory state of a card Anki kept none for, scheduled before FSRS was turned on: its
     * answers played through FSRS as Anki does, or with no answers kept, as stable as its interval.
     */
    private fun replayed(card: AnkiArchiveCard, config: SchedulerConfig): MemoryState? {
        val answers = card.reviews.filter { it.ease in 1..4 && it.type < 4 }.sortedBy { it.at }
        if (answers.isEmpty()) return if (card.intervalDays > 0) MemoryState(card.intervalDays.toDouble(), UNKNOWN_DIFFICULTY) else null
        var memory: MemoryState? = null
        var previous: Instant? = null
        for (answer in answers) {
            val elapsed = previous?.let { (scheduler.day(answer.at, config) - scheduler.day(it, config)).coerceAtLeast(0).toInt() } ?: 0
            memory = fsrs.next(memory, elapsed, Rating.of(answer.ease))
            previous = answer.at
        }
        return memory
    }

    /** Adds the card's answers not in the term's history yet; returns how many. */
    private suspend fun addReviews(termId: Long, card: AnkiArchiveCard, config: SchedulerConfig): Int {
        val recorded = cards.reviews(termId).map { it.at.toEpochMilliseconds() }.toSet()
        var previous: Instant? = null
        val added = mutableListOf<FlashcardReview>()
        for (review in card.reviews.sortedBy { it.at }) {
            // Reschedules by hand carry no answer.
            if (review.ease !in 1..4 || review.type >= 4) continue
            val before = when (review.type) {
                1, 3 -> CardState.REVIEW
                2 -> CardState.RELEARNING
                else -> if (previous == null) CardState.NEW else CardState.LEARNING
            }
            val elapsed = previous?.let { (scheduler.day(review.at, config) - scheduler.day(it, config)).coerceAtLeast(0).toInt() } ?: 0
            if (review.at.toEpochMilliseconds() !in recorded) {
                added += FlashcardReview(termId = termId, at = review.at, rating = Rating.of(review.ease), stateBefore = before, intervalDays = review.intervalDays.coerceAtLeast(0), elapsedDays = elapsed)
            }
            previous = review.at
        }
        cards.addReviews(added)
        return added.size
    }

    /** "2026-10-15 21:14", as Tayra and Language Reactor write a note's dates. */
    private fun stamp(text: String): Instant? = runCatching { LocalDateTime.parse(text.trim().replace(' ', 'T')).toInstant(timeZone()) }.getOrNull()

    internal companion object {
        /** A cloze and its hint: {{c1::word}} or {{c1::word::hint}}. */
        private val CLOZE = Regex("""\{\{c\d+::([\s\S]*?)(?:::([\s\S]*?))?\}\}""")
        private val LANGUAGE_TAG = Regex(""".*::lang::([A-Za-z-]+)""")
        private val TAG = Regex("""<[^>]*>""")
        private val SOUND = Regex("""\[sound:[^\]]*\]""")
        private val SPACE = Regex("""\s+""")

        /** Note ids are the moment the note was made; earlier ones are not times. */
        private const val EARLIEST_NOTE_ID = 946_684_800_000L

        /** FSRS's difficulty runs from 1 to 10; with nothing to go on a card is taken as middling. */
        private const val UNKNOWN_DIFFICULTY = 5.0

        /** A learning card's due above this is a time in seconds; below it, a day. */
        private const val EPOCH_SECONDS_FROM = 1_000_000_000L

        /** A field's text without its markup and sounds. */
        fun plain(html: String): String = html
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), " ")
            .replace(TAG, "")
            .replace(SOUND, "")
            .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&")
            .replace(SPACE, " ")
            .trim()
    }
}
