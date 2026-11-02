package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.FlashcardRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.settings.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The flashcards of the terms being learned. A term moved to status 1 to 4 gets a card; answering
 * cards schedules them with FSRS and moves the term's status with what the reviews show:
 *
 * - up, as the memory grows (its stability: the days until recall drops to 90%): a learned card
 *   is status 2, three days 3, a week 4 and two weeks Known;
 * - down one level when a review is forgotten.
 *
 * Until its word is Known, a card comes back within a week, so a word remembered every time is
 * Known in about nine days.
 *
 * A status set by hand leaves the card as it is: a term marked known is still reviewed, as in
 * Anki, and its card goes only with the term.
 */
class FlashcardService(
    private val cards: FlashcardRepository,
    private val terms: TermRepository,
    private val settings: SettingsRepository,
    private val scheduler: FlashcardScheduler = FlashcardScheduler(),
    private val clock: Clock = Clock.System,
    /** The dictionary forms a lowercase word of a language is a form of, from the installed dictionary; none without one. */
    private val formLemmas: (suspend (languageId: Long, formLc: String) -> List<String>)? = null,
) {
    /** The card to show next with its term and what each answer would do, or no card when the day's work is done. */
    data class Session(
        val card: Flashcard?,
        val term: Term?,
        val outcomes: Map<Rating, CardSchedule>,
        val counts: DueCounts,
        /** When the next card being learned comes back, if one does later today. */
        val nextLearningAt: Instant?,
        val shownAt: Instant,
    )

    /** A card with its term, as the browser lists them. */
    data class BrowseCard(
        val card: Flashcard,
        val term: Term,
        /** The chance, from 0 to 1, of recalling the card now; null before its first answer. */
        val retrievability: Double?,
    )

    /** Every card of a language with its term; the current study day runs from [startOfDay] to [endOfDay], by which the cards due today come. */
    data class Browse(val cards: List<BrowseCard>, val now: Instant, val endOfDay: Instant, val startOfDay: Instant = endOfDay)

    /** What an answer changed, enough to take it back. */
    data class Answered(val before: Flashcard, val statusBefore: TermStatus, val statusAfter: TermStatus, val reviewId: Long)

    /** Where the word a card blanks out stands in [term]'s sentence (see [Cloze.cardRange]); null without a sentence or a match. */
    suspend fun clozeRange(term: Term): IntRange? =
        term.sentence?.trim()?.takeIf { it.isNotEmpty() }?.let { Cloze.cardRange(it, term, terms, formLemmas?.let { lemmas -> { form -> lemmas(term.languageId, form) } }) }

    /** Gives [termId]'s card [sentence] to blank its word out of; where the word stands in it, null when it does not. */
    suspend fun replaceSentence(termId: Long, sentence: String, translation: String? = null): IntRange? {
        terms.updateSentence(termId, sentence.trim())
        translation?.trim()?.takeIf { it.isNotEmpty() }?.let { terms.updateSentenceTranslation(termId, it) }
        return terms.getById(termId)?.let { clozeRange(it) }
    }

    private fun config(s: UserSettings = settings.current) = schedulerConfig(s)

    private data class Limits(val newLeft: Int, val reviewsLeft: Int)

    private suspend fun limits(languageId: Long?, now: Instant, config: SchedulerConfig): Limits {
        val s = settings.current
        val today = scheduler.day(now, config)
        val since = scheduler.dayStart(today, config)
        val allowed = s.flashcardNewPerDay + ExtraNew.of(s.flashcardExtraNew, today, languageId)
        val newLeft = (allowed - cards.reviewsSince(languageId, CardState.NEW, since)).coerceAtLeast(0)
        val reviewsLeft = if (s.flashcardReviewsPerDay == 0) Int.MAX_VALUE
        else (s.flashcardReviewsPerDay - cards.reviewsSince(languageId, CardState.REVIEW, since)).coerceAtLeast(0)
        return Limits(newLeft, reviewsLeft)
    }

    /** Adds [count] new cards to today's limit of [languageId] (null for every language), on this device. */
    suspend fun addNewToday(languageId: Long?, count: Int) {
        if (count <= 0) return
        val today = scheduler.day(clock.now(), config())
        settings.update { it.copy(flashcardExtraNew = ExtraNew.add(it.flashcardExtraNew, today, languageId, count)) }
    }

    /** The terms of the cards [study] goes over, in the order they come up; suspended cards are left out. */
    suspend fun extraCards(languageId: Long?, study: ExtraStudy, random: Random = Random.Default): List<Long> {
        val now = clock.now()
        cards.reconcile(now)
        val config = config()
        val today = scheduler.day(now, config)
        val active = cards.list(languageId).filter { !it.suspended }
        return when (study) {
            is ExtraStudy.Ahead -> {
                val by = scheduler.dayStart(today + study.days + 1, config)
                active.filter { it.schedule.state.isReview && it.schedule.due < by }.sortedWith(compareBy({ it.schedule.due }, { it.termId })).map { it.termId }
            }
            is ExtraStudy.Forgotten -> {
                val forgotten = cards.forgottenSince(languageId, scheduler.dayStart(today - (study.days - 1).coerceAtLeast(0), config))
                active.filter { it.termId in forgotten && it.schedule.state != CardState.NEW }.sortedWith(compareBy({ it.schedule.due }, { it.termId })).map { it.termId }
            }
            is ExtraStudy.Practise -> {
                val answered = active.filter { it.schedule.state != CardState.NEW }
                val statuses = terms.getByIds(answered.map { it.termId }).associate { it.id to it.status }
                answered.filter { statuses[it.termId] in study.statuses }.map { it.termId }.shuffled(random).take(study.count)
            }
        }
    }

    /** The new cards of [languageId] waiting beyond today's limit. */
    suspend fun newBeyondToday(languageId: Long?): Int {
        val now = clock.now()
        cards.reconcile(now)
        val config = config()
        val waiting = cards.count(languageId, setOf(CardState.NEW), Instant.DISTANT_FUTURE)
        return (waiting - counts(languageId, now, config, limits(languageId, now, config)).new).coerceAtLeast(0)
    }

    /** The cards of [languageId] (null for every language) waiting today, within the daily limits. */
    suspend fun counts(languageId: Long?): DueCounts {
        val now = clock.now()
        cards.reconcile(now)
        val config = config()
        return counts(languageId, now, config, limits(languageId, now, config))
    }

    private suspend fun counts(languageId: Long?, now: Instant, config: SchedulerConfig, limits: Limits): DueCounts {
        val endOfDay = scheduler.dayStart(scheduler.day(now, config) + 1, config)
        return DueCounts(
            new = minOf(cards.count(languageId, setOf(CardState.NEW), Instant.DISTANT_FUTURE), limits.newLeft),
            learning = cards.count(languageId, STEPPING, endOfDay),
            review = minOf(cards.count(languageId, setOf(CardState.REVIEW), now), limits.reviewsLeft),
        )
    }

    /**
     * The next card: one being learned whose wait is over, then a review that is due, then a new
     * card, and when only cards being learned are left, one whose wait ends within twenty minutes.
     * [prefer] names a term whose card is shown first if it has one.
     */
    suspend fun next(languageId: Long?, prefer: Long? = null): Session {
        val now = clock.now()
        cards.reconcile(now)
        val config = config()
        val limits = limits(languageId, now, config)
        // [prefer] puts a card first whatever its turn: the one an answer was just taken back from.
        val card = prefer?.let { cards.get(it) }?.takeIf { !it.suspended }
            ?: cards.next(languageId, STEPPING, now)
            ?: (if (limits.reviewsLeft > 0) cards.next(languageId, setOf(CardState.REVIEW), now) else null)
            ?: (if (limits.newLeft > 0) cards.next(languageId, setOf(CardState.NEW), Instant.DISTANT_FUTURE) else null)
            ?: cards.next(languageId, STEPPING, now + LEARN_AHEAD)
        val term = card?.let { terms.getById(it.termId) }
        val counts = counts(languageId, now, config, limits)
        if (card == null || term == null) {
            val endOfDay = scheduler.dayStart(scheduler.day(now, config) + 1, config)
            val later = cards.earliestDue(languageId, STEPPING)?.takeIf { it < endOfDay }
            return Session(null, null, emptyMap(), counts, later, now)
        }
        return Session(card, term, outcomes(card, term.status, now, config), counts, null, now)
    }

    /**
     * The terms of the cards today's session holds, roughly in the order they come up: cards being
     * learned whose wait is over, the reviews due and the new cards within the day's limits, then
     * the cards being learned that come back later today.
     */
    suspend fun sessionTerms(languageId: Long?): List<Term> {
        val now = clock.now()
        cards.reconcile(now)
        val config = config()
        val limits = limits(languageId, now, config)
        val endOfDay = scheduler.dayStart(scheduler.day(now, config) + 1, config)
        val active = cards.list(languageId).filter { !it.suspended }.sortedWith(compareBy({ it.schedule.due }, { it.termId }))
        val stepping = active.filter { it.schedule.state in STEPPING && it.schedule.due < endOfDay }
        val ordered = stepping.filter { it.schedule.due <= now } +
            active.filter { it.schedule.state == CardState.REVIEW && it.schedule.due <= now }.take(limits.reviewsLeft) +
            active.filter { it.schedule.state == CardState.NEW }.take(limits.newLeft) +
            stepping.filter { it.schedule.due > now }
        val byId = terms.getByIds(ordered.map { it.termId }).associateBy { it.id }
        return ordered.mapNotNull { byId[it.termId] }
    }

    /** Answers [card], typed or said first as [answeredBy] says, and moves its term's status with it. */
    suspend fun answer(card: Flashcard, rating: Rating, answeredBy: AnswerMode = AnswerMode.SHOW): Answered? {
        val now = clock.now()
        val term = terms.getById(card.termId) ?: return null
        val config = config()
        val before = card.schedule
        val after = outcomes(card, term.status, now, config).getValue(rating)
        val status = statusAfter(term.status, before, after, rating)
        val elapsed = before.lastReview?.let { (scheduler.day(now, config) - scheduler.day(it, config)).coerceAtLeast(0).toInt() } ?: 0
        val reviewId = cards.addReview(
            FlashcardReview(
                termId = card.termId,
                at = now,
                rating = rating,
                stateBefore = before.state,
                intervalDays = if (after.state.isReview) after.intervalDays else 0,
                elapsedDays = elapsed,
                answeredBy = answeredBy,
            ),
        )
        // The card records the status it gives the term, so the change is not taken for one made elsewhere.
        cards.save(card.copy(schedule = after, seenStatus = status))
        if (status != term.status) terms.updateStatus(listOf(term.id), status)
        return Answered(card, term.status, status, reviewId)
    }

    /** Takes [answered] back: the card, its term's status and the history are as before. */
    suspend fun undo(answered: Answered) {
        cards.deleteReview(answered.reviewId)
        cards.save(answered.before)
        if (answered.statusAfter != answered.statusBefore) terms.updateStatus(listOf(answered.before.termId), answered.statusBefore)
    }

    /** Every card of [languageId] (null for every language) with its term, suspended cards included. */
    suspend fun browse(languageId: Long?): Browse {
        val now = clock.now()
        cards.reconcile(now)
        val config = config()
        val all = cards.list(languageId)
        val byId = terms.getByIds(all.map { it.termId }).associateBy { it.id }
        val listed = all.mapNotNull { card -> byId[card.termId]?.let { BrowseCard(card, it, scheduler.retrievability(card.schedule, now, config)) } }
        val day = scheduler.day(now, config)
        return Browse(listed, now, endOfDay = scheduler.dayStart(day + 1, config), startOfDay = scheduler.dayStart(day, config))
    }

    /** The answers given to the term's card, the latest first. */
    suspend fun reviews(termId: Long): List<FlashcardReview> = cards.reviews(termId)

    /** The term's card, if it has one. */
    suspend fun cardFor(termId: Long): Flashcard? {
        cards.reconcile(clock.now())
        return cards.get(termId)
    }

    suspend fun setSuspended(termId: Long, suspended: Boolean) = setSuspended(listOf(termId), suspended)

    suspend fun setSuspended(termIds: Collection<Long>, suspended: Boolean) {
        cards.reconcile(clock.now())
        termIds.forEach { cards.setSuspended(it, suspended) }
    }

    /**
     * Deletes the cards with their words, as Anki deletes a card's note: a word at status 1 to 4
     * always has a card, so the card cannot go while the word stays (Suspend keeps a word without
     * reviews). The cards' review history goes with them.
     */
    suspend fun delete(termIds: Collection<Long>) {
        if (termIds.isEmpty()) return
        terms.deleteAll(termIds)
        cards.reconcile(clock.now())
    }

    /** Forgets the card's history: it is a new card again. */
    suspend fun restart(termId: Long) = restart(listOf(termId))

    suspend fun restart(termIds: Collection<Long>) {
        val now = clock.now()
        cards.reconcile(now)
        termIds.forEach { cards.restart(it, now) }
    }

    /** Emits whenever terms, cards or reviews change, here or by sync. */
    fun observeChanges(): Flow<Unit> = cards.observeChanges()

    /** How many cards of the language being learned wait today; follows changes and the clock. */
    fun observeDueCount(): Flow<Int> {
        val everyMinute = flow {
            while (true) {
                emit(Unit)
                delay(1.minutes)
            }
        }
        // Conflated: a burst of writes, such as marking a page's words known, is counted once.
        return combine(cards.observeChanges().conflate(), settings.settings, everyMinute) { _, s, _ -> counts(s.currentLanguageId.takeIf { it != 0L }).total }
            .distinctUntilChanged()
    }

    /** What each answer would make of [card], whose word is at [status]: a review that leaves the word short of Known waits a week at most. */
    private fun outcomes(card: Flashcard, status: TermStatus, now: Instant, config: SchedulerConfig): Map<Rating, CardSchedule> =
        scheduler.outcomes(card.schedule, now, config, fuzz(card)).mapValues { (rating, after) ->
            val known = statusAfter(status, card.schedule, after, rating) == TermStatus.WELL_KNOWN
            if (known || !status.isLearning) after else scheduler.within(after, UNTIL_KNOWN_DAYS, now, config)
        }

    /** The same fuzz for a card until it is answered, so the buttons show the intervals the answers give. */
    private fun fuzz(card: Flashcard): Double = Random(card.termId * 31 + card.schedule.reps).nextDouble()

    companion object {
        private val STEPPING = setOf(CardState.LEARNING, CardState.RELEARNING)

        /** A card being learned is shown this much early when nothing else is left, as in Anki. */
        private val LEARN_AHEAD = 20.minutes

        /** The longest a review waits while its word is not Known yet. */
        const val UNTIL_KNOWN_DAYS = 7

        /** The scheduling the flashcard settings in [s] ask for. */
        fun schedulerConfig(s: UserSettings) = SchedulerConfig(
            desiredRetention = s.flashcardRetention / 100.0,
            learnSteps = LearningSteps.parseOr(s.flashcardLearnSteps, UserSettings().flashcardLearnSteps),
            relearnSteps = LearningSteps.parseOr(s.flashcardRelearnSteps, UserSettings().flashcardRelearnSteps),
        )

        /** The status a learned card has earned with [stabilityDays] of memory. */
        fun statusFor(stabilityDays: Double): TermStatus = when {
            stabilityDays >= 14 -> TermStatus.WELL_KNOWN
            stabilityDays >= 7 -> TermStatus.LEARNING_4
            stabilityDays >= 3 -> TermStatus.LEARNING_3
            else -> TermStatus.NEW_2
        }

        /** The status [card] has earned, from its memory, or its interval when it has none. */
        fun statusFor(card: CardSchedule): TermStatus = statusFor(card.memory?.stability ?: card.intervalDays.toDouble())

        private fun oneLower(status: TermStatus): TermStatus = when (status) {
            TermStatus.WELL_KNOWN -> TermStatus.LEARNING_4
            TermStatus.LEARNING_4 -> TermStatus.LEARNING_3
            TermStatus.LEARNING_3 -> TermStatus.NEW_2
            else -> TermStatus.NEW_1
        }

        /** The term's status after an answer: one lower when a review was forgotten, otherwise never lower than it was. */
        fun statusAfter(current: TermStatus, before: CardSchedule, after: CardSchedule, rating: Rating): TermStatus {
            if (rating == Rating.AGAIN && before.state.isReview) return oneLower(current)
            if (!after.state.isReview) return current
            val earned = statusFor(after)
            return if (earned.value > current.value) earned else current
        }
    }
}

/** A pass over cards past the day's limits; every answer in it is a review like any other. */
sealed interface ExtraStudy {
    /** The reviews due in the next [days] study days, today's among them, the soonest first. */
    data class Ahead(val days: Int) : ExtraStudy

    /** The cards answered Again in the last [days] study days, today being the first. */
    data class Forgotten(val days: Int) : ExtraStudy

    /** Up to [count] answered cards of words at [statuses], in no order. */
    data class Practise(val statuses: Set<TermStatus>, val count: Int) : ExtraStudy
}

/** The new cards added to a day's limit, kept as "day|language:count,…", a language of 0 standing for every language. */
internal object ExtraNew {
    private fun parse(text: String): Pair<Long, Map<Long, Int>>? {
        val day = text.substringBefore('|', "").toLongOrNull() ?: return null
        val counts = text.substringAfter('|').split(',').mapNotNull { part ->
            val language = part.substringBefore(':').toLongOrNull() ?: return@mapNotNull null
            val count = part.substringAfter(':').toIntOrNull() ?: return@mapNotNull null
            language to count
        }.toMap()
        return day to counts
    }

    /** The extra new cards of [languageId] on study day [today]. */
    fun of(text: String, today: Long, languageId: Long?): Int = parse(text)?.takeIf { it.first == today }?.second?.get(languageId ?: 0) ?: 0

    /** [text] with [count] more for [languageId] on [today]; another day's extras are dropped. */
    fun add(text: String, today: Long, languageId: Long?, count: Int): String {
        val counts = parse(text)?.takeIf { it.first == today }?.second.orEmpty().toMutableMap()
        counts[languageId ?: 0] = (counts[languageId ?: 0] ?: 0) + count
        return "$today|" + counts.entries.joinToString(",") { "${it.key}:${it.value}" }
    }
}
