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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration
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
 *
 * A card asks its word in a sentence: a word without one has no card yet, and a card that has
 * lost its sentence is left out until [CardSentences] or the browser gives it one.
 */
class FlashcardService(
    private val cards: FlashcardRepository,
    private val terms: TermRepository,
    private val settings: SettingsRepository,
    private val scheduler: FlashcardScheduler = FlashcardScheduler(),
    private val clock: Clock = Clock.System,
    /** The dictionary forms a lowercase word of a language is a form of, from the installed dictionary; none without one. */
    private val formLemmas: (suspend (languageId: Long, formLc: String) -> List<String>)? = null,
    /** The terms whose cards hold the words ahead in a language's text read last; none without it. */
    private val wordsAhead: (suspend (languageId: Long) -> Set<Long>)? = null,
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
        /** The order today's reviews take as far more are due than usual; null on a day like any other. */
        val backlog: BacklogOrder? = null,
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

    /**
     * What an answer changed, enough to take it back. [leech] says the answer forgot the card often
     * enough to make it a leech, and [suspended] that it was suspended for it.
     */
    data class Answered(
        val before: Flashcard,
        val statusBefore: TermStatus,
        val statusAfter: TermStatus,
        val reviewId: Long,
        val leech: Boolean = false,
        val suspended: Boolean = false,
        val lapses: Int = 0,
    )

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

    /** The scheduling of a card of [languageId]: the settings', with the parameters trained on the language's answers. */
    private suspend fun configFor(languageId: Long): SchedulerConfig = config().copy(parameters = cards.parameters(languageId)?.parameters)

    /** [configFor] with the reviews of [languageId] due on each day ahead, for an answer to even them out. */
    private suspend fun configWithLoad(languageId: Long, now: Instant): SchedulerConfig {
        val config = configFor(languageId)
        if (!config.loadBalancing) return config
        val today = scheduler.day(now, config)
        val due = cards.dueTimes(languageId, setOf(CardState.REVIEW), scheduler.dayStart(today + 1, config), scheduler.dayStart(today + LOAD_DAYS + 1, config))
        return config.copy(load = due.groupingBy { scheduler.day(it, config) }.eachCount())
    }

    /** What the day's limits leave, and the new cards and reviews answered so far today. */
    private data class Limits(val newLeft: Int, val reviewsLeft: Int, val newDone: Int = 0, val reviewsDone: Int = 0)

    private suspend fun limits(languageId: Long?, now: Instant, config: SchedulerConfig): Limits {
        val s = settings.current
        val today = scheduler.day(now, config)
        val since = scheduler.dayStart(today, config)
        val allowed = s.flashcardNewPerDay + ExtraNew.of(s.flashcardExtraNew, today, languageId)
        val newDone = cards.reviewsSince(languageId, CardState.NEW, since)
        val reviewsDone = cards.reviewsSince(languageId, CardState.REVIEW, since)
        val newLeft = (allowed - newDone).coerceAtLeast(0)
        val reviewsLeft = if (s.flashcardReviewsPerDay == 0) Int.MAX_VALUE else (s.flashcardReviewsPerDay - reviewsDone).coerceAtLeast(0)
        return Limits(newLeft, reviewsLeft, newDone, reviewsDone)
    }

    /** The terms whose cards were skipped until the next study day. */
    private suspend fun skipped(now: Instant, config: SchedulerConfig): Set<Long> =
        Skipped.of(settings.current.flashcardSkipped, scheduler.day(now, config)) + cards.wordOnly()

    /** Leaves the cards of [termIds] out until the next study day, their schedules as they are, as Anki buries a card. */
    suspend fun skip(termIds: Collection<Long>) {
        if (termIds.isEmpty()) return
        val today = scheduler.day(clock.now(), config())
        settings.update { it.copy(flashcardSkipped = Skipped.add(it.flashcardSkipped, today, termIds)) }
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
        val wordOnly = cards.wordOnly()
        val active = cards.list(languageId).filter { !it.suspended && it.termId !in wordOnly }
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
        val skipped = skipped(now, config)
        return DueCounts(
            new = minOf(cards.count(languageId, setOf(CardState.NEW), Instant.DISTANT_FUTURE, skipped), limits.newLeft),
            learning = cards.count(languageId, STEPPING, endOfDay, skipped),
            review = minOf(cards.count(languageId, setOf(CardState.REVIEW), now, skipped), limits.reviewsLeft),
        )
    }

    /**
     * The next card: one being learned whose wait is over, then a review that is due or a new card,
     * as the new cards are mixed among the reviews, and when only cards being learned are left, one
     * whose wait ends within twenty minutes. [prefer] names a term whose card is shown first if it has one.
     */
    suspend fun next(languageId: Long?, prefer: Long? = null): Session {
        val now = clock.now()
        cards.reconcile(now)
        val config = config()
        val limits = limits(languageId, now, config)
        val backlog = backlogOrder(languageId, now, config)
        val counts = counts(languageId, now, config, limits)
        val newFirst = newTurn(settings.current.flashcardNewMix, limits.newDone, limits.reviewsDone, counts.new, counts.review)
        val skipped = skipped(now, config)
        // [prefer] puts a card first whatever its turn: the one an answer was just taken back from.
        val card = prefer?.let { cards.get(it) }?.takeIf { !it.suspended }
            ?: cards.next(languageId, STEPPING, now, skipped)
            ?: (if (newFirst) newCards(languageId, now, config).firstOrNull() else null)
            ?: (if (limits.reviewsLeft > 0) nextReview(languageId, now, backlog, skipped) else null)
            ?: (if (limits.newLeft > 0) newCards(languageId, now, config).firstOrNull() else null)
            ?: cards.next(languageId, STEPPING, now + LEARN_AHEAD, skipped)
        val term = card?.let { terms.getById(it.termId) }
        if (card == null || term == null) {
            val endOfDay = scheduler.dayStart(scheduler.day(now, config) + 1, config)
            val later = cards.earliestDue(languageId, STEPPING, skipped)?.takeIf { it < endOfDay }
            return Session(null, null, emptyMap(), counts, later, now)
        }
        return Session(card, term, outcomes(card, term.status, now, configWithLoad(term.languageId, now)), counts, null, now, backlog)
    }

    /** [languageId]'s new cards, in the order the settings ask for. */
    private suspend fun newCards(languageId: Long?, now: Instant, config: SchedulerConfig): List<Flashcard> =
        ordered(cards.dueInStates(languageId, setOf(CardState.NEW), Instant.DISTANT_FUTURE, skipped(now, config)), scheduler.day(now, config))

    /** New cards [waiting] in the settings' order; a random order is the same all through study day [today]. */
    private fun ordered(waiting: List<Flashcard>, today: Long): List<Flashcard> {
        val saved = waiting.sortedWith(compareBy({ it.schedule.due }, { it.termId }))
        return when (settings.current.flashcardNewOrder) {
            NewCardOrder.SAVED -> saved
            NewCardOrder.NEWEST -> saved.reversed()
            NewCardOrder.RANDOM -> saved.sortedBy { Random(it.termId * 7919 + today).nextLong() }
        }
    }

    private suspend fun nextReview(languageId: Long?, now: Instant, backlog: BacklogOrder?, skipped: Set<Long>): Flashcard? =
        if (backlog == null) cards.next(languageId, setOf(CardState.REVIEW), now, skipped) else dueReviews(languageId, now, backlog).firstOrNull()

    /**
     * The order the due reviews of [languageId] take: the one chosen for a backlog when more are due
     * than [BACKLOG_MIN] and twice the reviews of an average day of the last [BACKLOG_DAYS]; null,
     * the cards due longest first, on any other day.
     */
    private suspend fun backlogOrder(languageId: Long?, now: Instant, config: SchedulerConfig): BacklogOrder? {
        val order = settings.current.flashcardBacklogOrder.takeIf { it != BacklogOrder.DUE_DATE } ?: return null
        val due = cards.count(languageId, setOf(CardState.REVIEW), now, skipped(now, config))
        if (due <= BACKLOG_MIN) return null
        val since = scheduler.dayStart(scheduler.day(now, config) - BACKLOG_DAYS, config)
        val usual = cards.reviewsSince(languageId, CardState.REVIEW, since).toDouble() / BACKLOG_DAYS
        return order.takeIf { due > 2 * usual }
    }

    /** [languageId]'s reviews due now in [order]; ties go to the card due longest. */
    private suspend fun dueReviews(languageId: Long?, now: Instant, order: BacklogOrder): List<Flashcard> {
        val config = languageId?.let { configFor(it) } ?: config()
        val due = cards.dueInStates(languageId, setOf(CardState.REVIEW), now, skipped(now, config))
        val recall = due.associate { it.termId to (scheduler.retrievability(it.schedule, now, config) ?: 1.0) }
        fun byRecall(lowFirst: Boolean) = Comparator<Flashcard> { a, b ->
            val ra = recall.getValue(a.termId)
            val rb = recall.getValue(b.termId)
            if (ra != rb) (if (lowFirst) ra.compareTo(rb) else rb.compareTo(ra)) else compareValuesBy(a, b, { it.schedule.due }, { it.termId })
        }
        return when (order) {
            // With one desired retention, slipping furthest past it is being least likely to be recalled.
            BacklogOrder.RELATIVE_OVERDUENESS -> due.sortedWith(byRecall(lowFirst = true))
            BacklogOrder.RECALL_HIGH_FIRST -> due.sortedWith(byRecall(lowFirst = false))
            BacklogOrder.READING_FIRST -> {
                val ahead = wordsAhead(languageId, now)
                val atRisk = byRecall(lowFirst = true)
                due.sortedWith { a, b -> compareValues(a.termId !in ahead, b.termId !in ahead).takeIf { it != 0 } ?: atRisk.compare(a, b) }
            }
            BacklogOrder.DUE_DATE -> due
        }
    }

    /** The words ahead in the text read last, found again once they are [AHEAD_FRESH] old, as reading moves on. */
    private var ahead: Triple<Long, Instant, Set<Long>>? = null

    private suspend fun wordsAhead(languageId: Long?, now: Instant): Set<Long> {
        val id = languageId ?: return emptySet()
        val find = wordsAhead ?: return emptySet()
        ahead?.takeIf { it.first == id && now - it.second < AHEAD_FRESH }?.let { return it.third }
        return find(id).also { ahead = Triple(id, now, it) }
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
        val skipped = skipped(now, config)
        val active = cards.list(languageId).filter { !it.suspended && it.termId !in skipped }.sortedWith(compareBy({ it.schedule.due }, { it.termId }))
        val stepping = active.filter { it.schedule.state in STEPPING && it.schedule.due < endOfDay }
        val reviews = backlogOrder(languageId, now, config)?.let { dueReviews(languageId, now, it) }
            ?: active.filter { it.schedule.state == CardState.REVIEW && it.schedule.due <= now }
        val due = reviews.take(limits.reviewsLeft)
        val new = ordered(active.filter { it.schedule.state == CardState.NEW }, scheduler.day(now, config)).take(limits.newLeft)
        val mix = settings.current.flashcardNewMix
        // The reviews and new cards in the turns they take.
        val today = buildList {
            var r = 0
            var n = 0
            while (r < due.size || n < new.size) {
                if (newTurn(mix, limits.newDone + n, limits.reviewsDone + r, new.size - n, due.size - r)) add(new[n++]) else add(due[r++])
            }
        }
        val ordered = stepping.filter { it.schedule.due <= now } + today + stepping.filter { it.schedule.due > now }
        val byId = terms.getByIds(ordered.map { it.termId }).associateBy { it.id }
        return ordered.mapNotNull { byId[it.termId] }
    }

    /**
     * Answers [card], typed or said first as [answeredBy] says, after [answerTime] on it, and moves
     * its term's status with it. A time past [MAX_ANSWER_TIME] counts as that, as Anki counts it:
     * the rest was spent away.
     */
    suspend fun answer(card: Flashcard, rating: Rating, answeredBy: AnswerMode = AnswerMode.SHOW, answerTime: Duration? = null): Answered? {
        val now = clock.now()
        val term = terms.getById(card.termId) ?: return null
        val config = configWithLoad(term.languageId, now)
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
                answerTime = answerTime?.coerceIn(Duration.ZERO, MAX_ANSWER_TIME),
            ),
        )
        val s = settings.current
        val leech = after.lapses > before.lapses && leechAt(after.lapses, s.flashcardLeechThreshold)
        val suspend = leech && s.flashcardLeechSuspend
        // The card records the status it gives the term, so the change is not taken for one made elsewhere.
        cards.save(card.copy(schedule = after, seenStatus = status, suspended = card.suspended || suspend))
        if (status != term.status) terms.updateStatus(listOf(term.id), status)
        return Answered(card, term.status, status, reviewId, leech, suspend, after.lapses)
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
        val configs = byId.values.map { it.languageId }.distinct().associateWith { configFor(it) }
        val listed = all.mapNotNull { card -> byId[card.termId]?.let { BrowseCard(card, it, scheduler.retrievability(card.schedule, now, configs[it.languageId] ?: config)) } }
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

    /**
     * Sets the answered cards among [termIds] to come up [days] study days from today, each on a
     * day picked within [days], as Anki's Set due date sets them: a review keeps its memory and
     * interval, and a card on its steps becomes a review. New cards stay as they are; returns how
     * many were set.
     */
    suspend fun setDue(termIds: Collection<Long>, days: IntRange, random: Random = Random.Default): Int {
        val now = clock.now()
        cards.reconcile(now)
        val config = config()
        val today = scheduler.day(now, config)
        return termIds.mapNotNull { cards.get(it) }.count { card ->
            val s = card.schedule
            if (s.state == CardState.NEW || (s.state != CardState.REVIEW && s.memory == null)) return@count false
            val day = days.random(random)
            val due = scheduler.dayStart(today + day, config)
            val set = if (s.state == CardState.REVIEW) s.copy(due = due) else s.copy(state = CardState.REVIEW, due = due, intervalDays = day.coerceAtLeast(1), remainingSteps = 0)
            cards.save(card.copy(schedule = set))
            true
        }
    }

    /**
     * Moves the new cards among [termIds] to the front of the new cards, [first], or to their end,
     * in the order they had; returns how many moved.
     */
    suspend fun reposition(termIds: Collection<Long>, first: Boolean): Int {
        cards.reconcile(clock.now())
        val ids = termIds.toSet()
        val waiting = cards.dueInStates(null, setOf(CardState.NEW), Instant.DISTANT_FUTURE)
        val moving = waiting.filter { it.termId in ids }.sortedWith(compareBy({ it.schedule.due }, { it.termId }))
        if (moving.isEmpty()) return 0
        val rest = waiting.filter { it.termId !in ids }.map { it.schedule.due }
        // Newest first learns the latest-dated first, so its front is the other end.
        val earliest = first != (settings.current.flashcardNewOrder == NewCardOrder.NEWEST)
        val start = if (earliest) (rest.minOrNull() ?: moving.first().schedule.due) - moving.size.milliseconds
        else (rest.maxOrNull() ?: moving.last().schedule.due) + 1.milliseconds
        moving.forEachIndexed { i, card -> cards.save(card.copy(schedule = card.schedule.copy(due = start + i.milliseconds))) }
        return moving.size
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
            if (known || !status.isLearning) after else scheduler.within(after, UNTIL_KNOWN_DAYS, now, config, fuzz(card))
        }

    /** The same fuzz for a card until it is answered, so the buttons show the intervals the answers give. */
    private fun fuzz(card: Flashcard): Double = Random(card.termId * 31 + card.schedule.reps).nextDouble()

    companion object {
        private val STEPPING = setOf(CardState.LEARNING, CardState.RELEARNING)

        /** A card being learned is shown this much early when nothing else is left, as in Anki. */
        private val LEARN_AHEAD = 20.minutes

        /** A backlog is more reviews due than this, and than twice those of an average day of the last [BACKLOG_DAYS]. */
        private const val BACKLOG_MIN = 30
        private const val BACKLOG_DAYS = 14

        /** The days ahead whose reviews load balancing counts: the longest interval it balances, and a little more. */
        private const val LOAD_DAYS = 99

        /** How long the words found ahead in the text read last are taken as still ahead. */
        private val AHEAD_FRESH = 10.minutes

        /** The longest an answer is taken to have taken. */
        val MAX_ANSWER_TIME = 60.seconds

        /** The longest a review waits while its word is not Known yet. */
        const val UNTIL_KNOWN_DAYS = 7

        /** The scheduling the flashcard settings in [s] ask for. */
        fun schedulerConfig(s: UserSettings) = SchedulerConfig(
            maximumIntervalDays = s.flashcardMaxInterval,
            dayStartHour = s.flashcardDayStartHour,
            loadBalancing = s.flashcardLoadBalancing,
            easyDays = EasyDay.parse(s.flashcardEasyDays),
            desiredRetention = s.flashcardRetention / 100.0,
            learnSteps = LearningSteps.parseOr(s.flashcardLearnSteps, UserSettings().flashcardLearnSteps),
            relearnSteps = LearningSteps.parseOr(s.flashcardRelearnSteps, UserSettings().flashcardRelearnSteps),
        )

        /**
         * Whether a new card comes next rather than a review, with [newDone] and [reviewsDone]
         * answered today and [newLeft] and [reviewsLeft] still to come: mixed, a new card takes its
         * turn once as many reviews have gone before it as its share of the day's cards gives.
         */
        fun newTurn(mix: NewCardMix, newDone: Int, reviewsDone: Int, newLeft: Int, reviewsLeft: Int): Boolean = when {
            newLeft <= 0 -> false
            reviewsLeft <= 0 -> true
            mix == NewCardMix.AFTER_REVIEWS -> false
            mix == NewCardMix.BEFORE_REVIEWS -> true
            else -> (newDone + 1).toLong() * (reviewsDone + reviewsLeft) <= (reviewsDone + 1).toLong() * (newDone + newLeft)
        }

        /** Whether a card forgotten [lapses] times is a leech, as [threshold] has it. */
        fun isLeech(lapses: Int, threshold: Int): Boolean = threshold > 0 && lapses >= threshold

        /**
         * Whether the lapse that brought a card to [lapses] tells it is a leech: the one reaching
         * [threshold], and every half of it after, as Anki tells it again.
         */
        fun leechAt(lapses: Int, threshold: Int): Boolean =
            isLeech(lapses, threshold) && (lapses - threshold) % ((threshold + 1) / 2).coerceAtLeast(1) == 0

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

/** The cards skipped until the next study day, kept as "day|term,term"; another day's are gone. */
internal object Skipped {
    /** The terms skipped on study day [today]. */
    fun of(text: String, today: Long): Set<Long> {
        if (text.substringBefore('|', "").toLongOrNull() != today) return emptySet()
        return text.substringAfter('|').split(',').mapNotNull { it.toLongOrNull() }.toSet()
    }

    /** [text] with [termIds] skipped on [today] too; another day's are dropped. */
    fun add(text: String, today: Long, termIds: Collection<Long>): String = "$today|" + (of(text, today) + termIds).joinToString(",")
}
