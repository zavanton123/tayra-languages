package com.tayra.languages.core.domain.export

import com.tayra.languages.core.domain.flashcards.AutoAnswer
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.EasyDay
import com.tayra.languages.core.domain.flashcards.Flashcard
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.domain.flashcards.FlashcardScheduler
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.Fsrs
import com.tayra.languages.core.domain.flashcards.MemoryState
import com.tayra.languages.core.domain.flashcards.NewCardMix
import com.tayra.languages.core.domain.flashcards.NewCardOrder
import com.tayra.languages.core.domain.flashcards.SchedulerConfig
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.FlashcardRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.settings.UserSettings
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Exports a language's flashcards as an Anki deck with their progress: each card's state, due
 * date, interval, steps, FSRS memory, reps, lapses and suspension, every answer of its history in
 * Anki's review log, and the scheduling settings as the deck's preset. The notes are those of
 * [AnkiExportService], with the sentence translated and read aloud, but in a deck and under guids
 * of their own: Anki never changes the progress of a card it has, so a card already sent there
 * would otherwise come in without its own.
 */
class AnkiDeckExportService(
    private val cards: FlashcardRepository,
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val notes: AnkiExportService,
    private val packager: AnkiPackager,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val scheduler = FlashcardScheduler(timeZone = timeZone)

    /** The deck of [languageId]'s cards; null when it has none. [onProgress] reports how many cards are done out of the total. */
    suspend fun export(languageId: Long, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): AnkiExportService.Export? {
        val now = clock.now()
        cards.reconcile(now)
        val language = languages.getById(languageId) ?: return null
        val all = cards.list(languageId)
        if (all.isEmpty()) return null
        val s = settings.current
        val config = FlashcardService.schedulerConfig(s)
        val today = scheduler.day(now, config)
        val parameters = cards.parameters(languageId)?.parameters ?: Fsrs.DEFAULT_PARAMETERS.toList()
        val fsrs = Fsrs(parameters.toDoubleArray())
        val history = cards.reviewsOf(languageId).groupBy { it.termId }
        val positions = newPositions(all, s.flashcardNewOrder)
        val byId = terms.getByIds(all.map { it.termId }).associateBy { it.id }
        val native = s.nativeLanguage.trim().lowercase().ifEmpty { "en" }
        val code = LanguageCodes.codeFor(language.name) ?: language.name.lowercase()
        val deck = "Tayra Languages::${language.name} (progress)"
        val media = LinkedHashMap<String, AnkiMedia>()
        val exported = mutableListOf<Long>()
        val made = all.mapIndexedNotNull { index, card ->
            onProgress(index, all.size)
            val term = byId[card.termId] ?: return@mapIndexedNotNull null
            val note = notes.noteFor(term, language, native, now, media)
            val leech = FlashcardService.isLeech(card.schedule.lapses, s.flashcardLeechThreshold)
            exported += term.id
            note.copy(
                guid = Sha1.hex("tayra|progress|$code|${term.textLc}".encodeToByteArray()).take(16),
                deck = deck,
                tags = listOf("Tayra_Languages::lang::$code", "Tayra_Languages::source::flashcards") + listOfNotNull("leech".takeIf { leech }),
                card = ankiCard(card, history[card.termId].orEmpty(), positions[card.termId] ?: 0, now, config, fsrs, parameters),
            )
        }
        onProgress(all.size, all.size)
        // Anki's collections begin as their first day starts; today is day 0.
        val dayZero = scheduler.dayStart(today, config)
        val days = AnkiDays(dayZero, config.dayStartHour, timeZone().offsetAt(dayZero).totalSeconds / 60)
        val collection = AnkiCollection(AnkiExportService.noteType(), made, media.values.toList(), days, preset(language, s, config, parameters))
        val fileName = "${language.name} ${notes.stamp(now)}.apkg"
        return AnkiExportService.Export(fileName, packager.pack(collection, now), exported, made.size, 0)
    }

    /** Records that the cards of [export] are in Anki now, once its file was saved, so that reading does not send them again. */
    suspend fun markExported(export: AnkiExportService.Export) = terms.markAnkiExported(export.termIds, clock.now())

    /** New cards' positions from 1, in the order they are learned here; a random order is left for Anki to draw. */
    private fun newPositions(all: List<Flashcard>, order: NewCardOrder): Map<Long, Int> {
        val saved = all.filter { it.schedule.state == CardState.NEW }.sortedWith(compareBy({ it.schedule.due }, { it.termId }))
        val ordered = if (order == NewCardOrder.NEWEST) saved.reversed() else saved
        return ordered.withIndex().associate { (i, card) -> card.termId to i + 1 }
    }

    /** The card as Anki schedules it, at [position] among the new cards when it is one, with its answers. */
    internal fun ankiCard(card: Flashcard, reviews: List<FlashcardReview>, position: Int, now: Instant, config: SchedulerConfig, fsrs: Fsrs, parameters: List<Double>): AnkiCard {
        val schedule = card.schedule
        val today = scheduler.day(now, config)
        val stepping = schedule.state == CardState.LEARNING || schedule.state == CardState.RELEARNING
        // A step that falls on a later study day than it was given waits in Anki's queue of days, not of seconds.
        val laterDay = stepping && scheduler.day(schedule.due, config) > scheduler.day(schedule.lastReview ?: now, config)
        val queue = when {
            card.suspended -> -1
            schedule.state == CardState.NEW -> 0
            schedule.state == CardState.REVIEW -> 2
            laterDay -> 3
            else -> 1
        }
        val due = when {
            schedule.state == CardState.NEW -> position.toLong()
            schedule.state == CardState.REVIEW || laterDay -> scheduler.day(schedule.due, config) - today
            else -> schedule.due.epochSeconds
        }
        val remembered = schedule.state == CardState.REVIEW || schedule.state == CardState.RELEARNING
        val memory = schedule.memory.takeIf { schedule.state != CardState.NEW }
        val data = if (memory == null) "" else buildJsonObject {
            put("s", memory.stability)
            put("d", memory.difficulty)
            put("dr", config.desiredRetention)
            parameters.getOrNull(DECAY)?.let { put("decay", it) }
            schedule.lastReview?.let { put("lrt", it.epochSeconds) }
        }.toString()
        return AnkiCard(
            type = when (schedule.state) {
                CardState.NEW -> 0
                CardState.LEARNING -> 1
                CardState.REVIEW -> 2
                CardState.RELEARNING -> 3
            },
            queue = queue,
            due = due,
            intervalDays = if (remembered) schedule.intervalDays.coerceAtLeast(1) else 0,
            factor = if (remembered) SM2_EASE else 0,
            reps = schedule.reps,
            lapses = schedule.lapses,
            // Anki keeps the steps left in the last three digits and, before them, the ones left today.
            left = if (stepping) schedule.remainingSteps * 1001 else 0,
            data = data,
            reviews = revlog(reviews, config, fsrs),
        )
    }

    /**
     * The answers as Anki logs them. The difficulty each answer left is worked out again by FSRS,
     * as Anki logs it in place of the ease; a step's interval, not kept here, is taken as the
     * first of its steps.
     */
    private fun revlog(reviews: List<FlashcardReview>, config: SchedulerConfig, fsrs: Fsrs): List<AnkiRevlog> {
        var memory: MemoryState? = null
        var last = 0
        return reviews.sortedWith(compareBy({ it.at }, { it.id })).map { review ->
            memory = fsrs.next(memory, review.elapsedDays, review.rating)
            val relearning = review.stateBefore == CardState.REVIEW || review.stateBefore == CardState.RELEARNING
            val steps = if (relearning) config.relearnSteps else config.learnSteps
            val interval = if (review.intervalDays > 0) review.intervalDays else -(steps.firstOrNull()?.inWholeSeconds?.toInt() ?: MINUTE)
            AnkiRevlog(
                at = review.at,
                ease = review.rating.value,
                interval = interval,
                lastInterval = last,
                factor = (((memory!!.difficulty - 1) / 9 + 0.1) * 1000).toInt(),
                millis = review.answerTime?.inWholeMilliseconds?.coerceIn(0, MAX_ANSWER_MILLIS)?.toInt() ?: 0,
                type = when (review.stateBefore) {
                    CardState.NEW, CardState.LEARNING -> 0
                    CardState.REVIEW -> 1
                    CardState.RELEARNING -> 2
                },
            ).also { last = interval }
        }
    }

    /** The scheduling settings as the deck's preset, one per language for its own FSRS parameters. */
    private fun preset(language: Language, s: UserSettings, config: SchedulerConfig, parameters: List<Double>) = AnkiPreset(
        name = "Tayra Languages: ${language.name}",
        newPerDay = s.flashcardNewPerDay,
        reviewsPerDay = s.flashcardReviewsPerDay,
        learnSteps = config.learnSteps.map { it.inWholeSeconds / 60.0 },
        relearnSteps = config.relearnSteps.map { it.inWholeSeconds / 60.0 },
        maximumInterval = config.maximumIntervalDays,
        leechThreshold = s.flashcardLeechThreshold,
        leechSuspend = s.flashcardLeechSuspend,
        desiredRetention = config.desiredRetention,
        parameters = parameters,
        easyDays = config.easyDays.map {
            when (it) {
                EasyDay.NORMAL -> 1.0
                EasyDay.REDUCED -> 0.5
                EasyDay.MINIMUM -> 0.0
            }
        },
        newMix = when (s.flashcardNewMix) {
            NewCardMix.MIXED -> 0
            NewCardMix.AFTER_REVIEWS -> 1
            NewCardMix.BEFORE_REVIEWS -> 2
        },
        newGather = LOWEST_POSITION,
        newSort = if (s.flashcardNewOrder == NewCardOrder.RANDOM) AnkiCollectionSql.NEW_SORT_RANDOM else BY_TEMPLATE,
        autoplay = s.flashcardAutoplay,
        timer = s.flashcardShowTimer,
        secondsToShowQuestion = s.flashcardAutoReveal,
        secondsToShowAnswer = s.flashcardAutoAnswer,
        answerAction = when (s.flashcardAutoAnswerAction) {
            AutoAnswer.SKIP -> 0
            AutoAnswer.AGAIN -> 1
            AutoAnswer.GOOD -> 2
            AutoAnswer.HARD -> 3
        },
    )

    private companion object {
        /** FSRS-6's decay is its last parameter. */
        const val DECAY = 20
        const val SM2_EASE = 2500
        const val MINUTE = 60
        /** Anki counts no answer as longer than its preset's limit, a minute by default. */
        const val MAX_ANSWER_MILLIS = 60_000L
        const val LOWEST_POSITION = 1
        const val BY_TEMPLATE = 0
    }
}
