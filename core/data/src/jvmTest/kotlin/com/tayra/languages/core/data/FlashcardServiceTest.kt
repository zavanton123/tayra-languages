package com.tayra.languages.core.data

import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.domain.flashcards.MemoryState
import com.tayra.languages.core.domain.flashcards.CardSchedule
import com.tayra.languages.core.domain.flashcards.BacklogOrder
import com.tayra.languages.core.domain.flashcards.NewCardMix
import com.tayra.languages.core.domain.flashcards.NewCardOrder
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardScheduler
import com.tayra.languages.core.domain.flashcards.ExtraStudy
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.TermService
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import com.tayra.languages.core.domain.flashcards.SentenceTranslations
import com.tayra.languages.core.domain.service.SentenceTranslator

/** Flashcards over a real database: which terms have cards, what is shown when, and how answers move a term's status. */
class FlashcardServiceTest {

    private class MovingClock(var now: Instant) : Clock {
        override fun now() = now
        fun advance(by: Duration) { now += by }
    }

    private val file = File.createTempFile("tayra-cards", ".db").also { it.delete() }
    private val clock = MovingClock(Instant.parse("2026-10-05T12:00:00Z"))
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider, clock)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val cards = FlashcardRepositoryImpl(provider)
    private val termService = TermService(terms, languages)
    private val service = FlashcardService(cards, terms, settings, FlashcardScheduler(timeZone = { TimeZone.UTC }), clock)

    private suspend fun language(): Long = languages.save(Language(name = "Portuguese"))

    private suspend fun term(languageId: Long, text: String, status: TermStatus = TermStatus.NEW_1): Long {
        clock.advance(1.minutes)
        return terms.save(Term(languageId = languageId, text = text, textLc = text, status = status, translation = "<$text>"))
    }

    private suspend fun status(id: Long) = terms.getById(id)!!.status

    /** Answers the card on show, which must be [termId]'s. */
    private suspend fun answer(languageId: Long, termId: Long, rating: Rating): FlashcardService.Answered {
        val session = service.next(languageId)
        assertEquals(termId, session.card?.termId, "the card on show")
        return service.answer(session.card!!, rating)!!
    }

    @Test
    fun moreNewCardsCanBeAddedForTodayAlone() = runTest {
        val id = language()
        val other = languages.save(Language(name = "German"))
        val lobo = term(id, "lobo")
        val gato = term(id, "gato")
        val casa = term(id, "casa")
        term(other, "Haus")
        settings.update { it.copy(flashcardNewPerDay = 1) }
        answer(id, lobo, Rating.EASY)
        assertEquals(0, service.counts(id).new)
        assertEquals(2, service.newBeyondToday(id))

        service.addNewToday(id, 1)
        assertEquals(1, service.counts(id).new)
        assertEquals(1, service.newBeyondToday(id))
        assertEquals(1, service.counts(other).new, "another language keeps its own limit")
        answer(id, gato, Rating.EASY)
        assertEquals(0, service.counts(id).new)

        // The next study day has the usual limit again.
        clock.advance(1.days)
        assertEquals(1, service.counts(id).new)
        service.addNewToday(id, 5)
        assertEquals(1, service.counts(id).new, "no more than wait")
        assertEquals(casa, service.next(id).card?.termId)
    }

    @Test
    fun extraPassesGoOverTheirCards() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        val gato = term(id, "gato")
        val casa = term(id, "casa")
        term(id, "livro")
        answer(id, lobo, Rating.EASY)
        clock.advance(1.hours)
        answer(id, gato, Rating.EASY)
        answer(id, casa, Rating.AGAIN)
        // Easy waits about a week, a week at most while a word is not yet known.
        assertEquals(emptyList(), service.extraCards(id, ExtraStudy.Ahead(2)))
        val ahead = service.extraCards(id, ExtraStudy.Ahead(7))
        assertEquals(setOf(lobo, gato), ahead.toSet())
        assertEquals(ahead.map { cards.get(it)!!.schedule.due }.sorted(), ahead.map { cards.get(it)!!.schedule.due }, "the soonest first")
        assertEquals(listOf(casa), service.extraCards(id, ExtraStudy.Forgotten(1)))

        val learning = setOf(TermStatus.NEW_1, TermStatus.NEW_2, TermStatus.LEARNING_3, TermStatus.LEARNING_4)
        assertEquals(setOf(lobo, gato, casa), service.extraCards(id, ExtraStudy.Practise(learning, 10)).toSet(), "a card never answered is left out")
        assertEquals(setOf(lobo, gato), service.extraCards(id, ExtraStudy.Practise(setOf(TermStatus.LEARNING_4), 10)).toSet())
        assertEquals(1, service.extraCards(id, ExtraStudy.Practise(learning, 1)).size)
        service.setSuspended(gato, true)
        assertEquals(listOf(lobo), service.extraCards(id, ExtraStudy.Ahead(7)), "suspended cards stay aside")

        // Two days on, today's Again is no longer today's.
        clock.advance(2.days)
        assertEquals(emptyList(), service.extraCards(id, ExtraStudy.Forgotten(1)))
        assertEquals(listOf(casa), service.extraCards(id, ExtraStudy.Forgotten(3)))
    }

    /** A review card of a new word due [overdue] days ago, last answered [interval] days before that, holding [stability] days of memory. */
    private suspend fun dueCard(languageId: Long, text: String, overdue: Int, interval: Int, stability: Double): Long {
        val id = term(languageId, text)
        cards.reconcile(clock.now)
        val card = cards.get(id)!!
        val due = clock.now - overdue.days
        cards.save(card.copy(schedule = CardSchedule(CardState.REVIEW, due, MemoryState(stability, 5.0), interval, 0, 3, 0, due - interval.days)))
        return id
    }

    @Test
    fun aBacklogComesInTheOrderChosenForIt() = runTest {
        val id = language()
        // Due longest, but remembered for months: very likely recalled still.
        val waiting = dueCard(id, "esperar", overdue = 10, interval = 10, stability = 200.0)
        // Due a day ago, from a weak memory: the most likely forgotten.
        val slipping = dueCard(id, "escorregar", overdue = 1, interval = 2, stability = 0.5)
        val others = (0 until 29).map { dueCard(id, "palavra$it", overdue = 5, interval = 5, stability = 10.0) }
        clock.advance(1.minutes)

        val atRisk = service.next(id)
        assertEquals(slipping, atRisk.card?.termId, "relative overdueness, by default")
        assertEquals(BacklogOrder.RELATIVE_OVERDUENESS, atRisk.backlog)
        assertEquals(slipping, service.sessionTerms(id).first().id)

        settings.update { it.copy(flashcardBacklogOrder = BacklogOrder.RECALL_HIGH_FIRST) }
        assertEquals(waiting, service.next(id).card?.termId)

        settings.update { it.copy(flashcardBacklogOrder = BacklogOrder.DUE_DATE) }
        val byDate = service.next(id)
        assertEquals(waiting, byDate.card?.termId)
        assertNull(byDate.backlog)

        settings.update { it.copy(flashcardBacklogOrder = BacklogOrder.READING_FIRST) }
        val reading = FlashcardService(cards, terms, settings, FlashcardScheduler(timeZone = { TimeZone.UTC }), clock, wordsAhead = { setOf(others[7], waiting) })
        assertEquals(listOf(waiting, others[7]).toSet(), reading.sessionTerms(id).take(2).map { it.id }.toSet(), "the words ahead first")
        assertEquals(others[7], reading.next(id).card?.termId, "among them, the one more at risk")
        assertEquals(slipping, reading.sessionTerms(id)[2].id, "then the rest by relative overdueness")
    }

    @Test
    fun aDayWithTheUsualReviewsKeepsTheDueOrder() = runTest {
        val id = language()
        val waiting = dueCard(id, "esperar", overdue = 10, interval = 10, stability = 200.0)
        dueCard(id, "escorregar", overdue = 1, interval = 2, stability = 0.5)
        repeat(29) { dueCard(id, "palavra$it", overdue = 5, interval = 5, stability = 10.0) }
        // Twenty reviews a day for the last two weeks: 31 due is an ordinary day.
        cards.addReviews((1..14).flatMap { day ->
            (0 until 20).map { FlashcardReview(termId = waiting, at = clock.now - day.days, rating = Rating.GOOD, stateBefore = CardState.REVIEW, intervalDays = 3, elapsedDays = 3) }
        })
        val session = service.next(id)
        assertEquals(waiting, session.card?.termId)
        assertNull(session.backlog)
    }

    @Test
    fun anAnswersTimeIsKeptToAMinute() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        val gato = term(id, "gato")
        service.answer(service.next(id).card!!, Rating.GOOD, answerTime = 7.seconds)
        service.answer(service.next(id, prefer = gato).card!!, Rating.GOOD, answerTime = 5.minutes)
        assertEquals(7.seconds, service.reviews(lobo).single().answerTime)
        assertEquals(FlashcardService.MAX_ANSWER_TIME, service.reviews(gato).single().answerTime, "the rest was spent away")
    }

    @Test
    fun onlyTermsBeingLearnedHaveCards() = runTest {
        val id = language()
        val learning = term(id, "lobo", TermStatus.NEW_1)
        val later = term(id, "floresta", TermStatus.LEARNING_4)
        val known = term(id, "casa", TermStatus.WELL_KNOWN)
        val ignored = term(id, "de", TermStatus.IGNORED)

        assertEquals(2, service.counts(id).new)
        assertNotNull(service.cardFor(learning))
        assertNotNull(service.cardFor(later))
        assertNull(service.cardFor(known))
        assertNull(service.cardFor(ignored))
        // New cards come in the order their terms were added.
        assertEquals(learning, service.next(id).card?.termId)
    }

    /** A form whose parent is being learned too is learned with the parent's card; one answered before keeps its own. */
    @Test
    fun aFamilyBeingLearnedHasOneCard() = runTest {
        val id = language()
        val lemma = term(id, "diminuir")
        val past = term(id, "diminuíram")
        assertNotNull(service.cardFor(past), "its own card before it has a parent")
        terms.setParents(past, listOf(lemma))
        assertNull(service.cardFor(past), "the unanswered card goes once the parent is being learned")
        assertNotNull(service.cardFor(lemma))
        assertEquals(1, service.browse(id).cards.size)

        // Answered before it was linked, a form keeps its card and history.
        val future = term(id, "diminuirão")
        service.answer(service.next(id, prefer = future).card!!, Rating.GOOD)
        terms.setParents(future, listOf(lemma))
        assertNotNull(service.cardFor(future), "a card with answers stays")

        // With the parent known, the form is learned on a card of its own again.
        terms.updateStatus(listOf(lemma), TermStatus.WELL_KNOWN)
        assertNotNull(service.cardFor(past))
    }

    /** A form chosen by hand is the one the card hides, before any it would find itself; taken back, the card finds its own. */
    @Test
    fun theCardHidesTheFormChosenForIt() = runTest {
        val id = language()
        val temer = term(id, "temer")
        terms.updateSentence(temer, "Ele temia o temer dos outros.")
        assertEquals(12 until 17, service.clozeRange(terms.getById(temer)!!), "the word itself")
        terms.updateCardForm(temer, "temia")
        assertEquals(4 until 9, service.clozeRange(terms.getById(temer)!!))
        terms.updateCardForm(temer, null)
        assertEquals(12 until 17, service.clozeRange(terms.getById(temer)!!))
    }

    /**
     * The cards' sentences without a translation get one, kept with the word; a translator that gives
     * none ends the round, and a sentence changed afterwards leaves its translation behind.
     */
    @Test
    fun sentenceTranslationsAreMadeOnceAndKept() = runTest {
        val id = language()
        val casa = term(id, "casa")
        val lobo = term(id, "lobo")
        val known = term(id, "gato", TermStatus.WELL_KNOWN)
        terms.updateSentence(casa, "A casa é azul.")
        terms.updateSentence(lobo, "O lobo uivou.")
        terms.updateSentence(known, "O gato dorme.")
        val asked = mutableListOf<String>()
        var answering = false
        val translator = object : SentenceTranslator {
            override suspend fun translate(text: String, language: Language): String? { asked += text; return if (answering) "<$text>" else null }
        }
        val sentences = SentenceTranslations(terms, languages, translator)

        sentences.fillMissing()
        assertEquals(listOf("A casa é azul."), asked, "nothing came back, so the round ends")
        assertNull(terms.getById(casa)!!.sentenceTranslation)

        answering = true
        sentences.fillMissing()
        assertEquals("<A casa é azul.>", terms.getById(casa)!!.sentenceTranslation)
        assertEquals("<O lobo uivou.>", terms.getById(lobo)!!.sentenceTranslation)
        assertNull(terms.getById(known)!!.sentenceTranslation, "a word without a card")
        asked.clear()
        sentences.fillMissing()
        assertEquals(emptyList(), asked, "kept, not made again")

        terms.updateSentenceTranslation(casa, "The house is blue.")
        assertEquals("The house is blue.", terms.getById(casa)!!.sentenceTranslation)
        terms.updateSentence(casa, "A casa é grande.")
        assertNull(terms.getById(casa)!!.sentenceTranslation, "a new sentence leaves the old translation behind")
        // A translation made for the old sentence does not stick to the new one.
        terms.fillSentenceTranslation(casa, "A casa é azul.", "The house is blue.")
        assertNull(terms.getById(casa)!!.sentenceTranslation)
    }

    /** A card blanks out the form the sentence holds: the term's own, or a parent's or sibling's when the sentence came through the family. */
    @Test
    fun theCardBlanksOutTheFormOfTheFamilyReadInTheSentence() = runTest {
        val id = language()
        val lemma = term(id, "destruir")
        val past = term(id, "destruíram")
        val future = term(id, "destruirão")
        terms.setParents(past, listOf(lemma))
        terms.setParents(future, listOf(lemma))
        terms.updateSentence(past, "Eu vou destruir esta parede.")
        terms.updateSentence(future, "Eles destruíram a casa.")
        terms.updateSentence(lemma, "Nada aqui.")

        assertEquals(7 until 15, service.clozeRange(terms.getById(past)!!), "the parent's form")
        assertEquals(5 until 15, service.clozeRange(terms.getById(future)!!), "a sibling's form")
        assertNull(service.clozeRange(terms.getById(lemma)!!), "no form of the family in the sentence")
    }

    /** The browser lists every card with its term and history, and acts on many cards at once. */
    @Test
    fun browsingListsEveryCardWithItsTermAndHistory() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        val casa = term(id, "casa")
        term(id, "de", TermStatus.IGNORED)
        answer(id, lobo, Rating.GOOD)
        clock.advance(1.hours)

        val browse = service.browse(id)
        assertEquals(listOf("lobo", "casa"), browse.cards.map { it.term.text })
        val answered = browse.cards.first { it.term.id == lobo }
        assertEquals(CardState.LEARNING, answered.card.schedule.state)
        assertNotNull(answered.retrievability, "answered once: a memory state to recall from")
        assertNull(browse.cards.first { it.term.id == casa }.retrievability)
        assertTrue(browse.endOfDay > browse.now)
        assertEquals(listOf(Rating.GOOD), service.reviews(lobo).map { it.rating })
        assertTrue(service.reviews(casa).isEmpty())

        service.setSuspended(listOf(lobo, casa), true)
        assertTrue(service.browse(id).cards.all { it.card.suspended })
        service.restart(listOf(lobo))
        assertEquals(CardState.NEW, service.cardFor(lobo)!!.schedule.state)
        assertTrue(service.cardFor(lobo)!!.suspended, "a reset keeps the card aside")
    }

    /** Deleting a card deletes its word and history; the other cards stay. */
    @Test
    fun deletingACardDeletesItsWordAndHistory() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        val casa = term(id, "casa")
        answer(id, lobo, Rating.GOOD)

        service.delete(listOf(lobo))
        assertNull(terms.getById(lobo))
        assertNull(service.cardFor(lobo))
        assertTrue(service.reviews(lobo).isEmpty())
        assertEquals(listOf(casa), service.browse(id).cards.map { it.term.id })
    }

    /** The session's cards: one being learned whose wait is over first, then the new ones within the day's limit; suspended ones stay out. */
    @Test
    fun theSessionHoldsTheCardsDueWithinTheLimits() = runTest {
        settings.update { it.copy(flashcardNewPerDay = 3) }
        val id = language()
        val lobo = term(id, "lobo")
        val casa = term(id, "casa")
        val gato = term(id, "gato")
        val mesa = term(id, "mesa")
        val livro = term(id, "livro")
        service.setSuspended(gato, true)
        answer(id, lobo, Rating.AGAIN)
        clock.advance(2.minutes)

        // lobo is back from its one-minute step and new, so one of the three new cards is spent.
        assertEquals(listOf("lobo", "casa", "mesa"), service.sessionTerms(id).map { it.text })
        assertTrue(livro !in service.sessionTerms(id).map { it.id }, "beyond the day's new cards")
    }

    @Test
    fun cardsBelongToTheirLanguage() = runTest {
        val portuguese = language()
        val spanish = languages.save(Language(name = "Spanish"))
        term(portuguese, "lobo")
        val gato = term(spanish, "gato")
        assertEquals(1, service.counts(portuguese).total)
        assertEquals(gato, service.next(spanish).card?.termId)
        assertEquals(2, service.counts(null).total)
    }

    @Test
    fun aNewCardIsLearnedInStepsAndThenWaitsDays() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        val session = service.next(id)
        assertEquals(setOf(Rating.AGAIN, Rating.HARD, Rating.GOOD, Rating.EASY), session.outcomes.keys)

        answer(id, lobo, Rating.GOOD)
        // Ten minutes to wait: nothing else is left, so the card is shown early, as in Anki.
        assertEquals(CardState.LEARNING, service.cardFor(lobo)!!.schedule.state)
        assertEquals(1, service.counts(id).learning)
        clock.advance(10.minutes)
        answer(id, lobo, Rating.GOOD)

        val card = service.cardFor(lobo)!!
        assertEquals(CardState.REVIEW, card.schedule.state)
        assertTrue(card.schedule.intervalDays >= 1)
        assertEquals(TermStatus.NEW_2, status(lobo), "a card that waits a day or more is status 2")
        assertNull(service.next(id).card, "nothing is due until then")
        assertEquals(0, service.counts(id).total)
    }

    @Test
    fun statusRisesAsTheMemoryGrowsAndDropsALevelWhenForgotten() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        answer(id, lobo, Rating.GOOD)
        clock.advance(10.minutes)
        answer(id, lobo, Rating.GOOD)
        assertEquals(TermStatus.NEW_2, status(lobo), "a learned card is status 2")

        val learned = clock.now
        val seen = mutableListOf(status(lobo))
        while (status(lobo) != TermStatus.WELL_KNOWN && seen.size < 10) {
            val waiting = service.cardFor(lobo)!!.schedule
            assertTrue(waiting.intervalDays <= FlashcardService.UNTIL_KNOWN_DAYS, "a word not yet known comes back within a week: $waiting")
            clock.now = waiting.due + 8.hours
            answer(id, lobo, Rating.GOOD)
            seen += status(lobo)
        }
        assertEquals(seen.sortedBy { it.value }, seen, "the status never falls while the card is remembered: $seen")
        assertEquals(TermStatus.WELL_KNOWN, seen.last(), "remembered every time, a word is known: $seen")
        assertTrue(clock.now - learned < 14.days, "within two weeks: ${clock.now - learned}")
        // A term the flashcards made known keeps its card, which waits as long as its memory says.
        val card = service.cardFor(lobo)
        assertNotNull(card)
        assertTrue(card.schedule.intervalDays > FlashcardService.UNTIL_KNOWN_DAYS, "${card.schedule}")

        clock.now = card.schedule.due + 8.hours
        answer(id, lobo, Rating.AGAIN)
        assertEquals(TermStatus.LEARNING_4, status(lobo), "forgetting a review drops one level")
        clock.advance(10.minutes)
        answer(id, lobo, Rating.AGAIN)
        assertEquals(TermStatus.LEARNING_4, status(lobo), "failing again while relearning drops no further")
    }

    @Test
    fun easyOnANewCardWaitsAWeekAtMost() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        val easy = service.next(id).outcomes.getValue(Rating.EASY)
        assertTrue(easy.intervalDays in 5..FlashcardService.UNTIL_KNOWN_DAYS, "the eight days the memory allows are cut to a week, on the lighter of its last days: ${easy.intervalDays}")
        answer(id, lobo, Rating.EASY)
        assertEquals(easy.due, service.cardFor(lobo)!!.schedule.due, "the answer does what its button showed")
        assertEquals(TermStatus.LEARNING_4, status(lobo), "a week of memory is status 4")
    }

    /** A status set by hand leaves the card as it was: a word marked known or ignored is still reviewed, and its answers move its status again. */
    @Test
    fun aStatusSetByHandLeavesTheCard() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        answer(id, lobo, Rating.EASY)
        val answered = service.cardFor(lobo)!!.schedule

        for (status in listOf(TermStatus.LEARNING_4, TermStatus.WELL_KNOWN, TermStatus.IGNORED, TermStatus.UNKNOWN, TermStatus.NEW_2, TermStatus.WELL_KNOWN)) {
            termService.setStatus(listOf(lobo), status)
            assertEquals(answered, service.cardFor(lobo)!!.schedule, "set to $status")
            assertEquals(status, status(lobo))
        }

        // Known by hand, and still due when its day comes; forgetting it brings the word down a level.
        clock.now = answered.due + 1.hours
        assertEquals(1, service.counts(id).review)
        answer(id, lobo, Rating.AGAIN)
        assertEquals(TermStatus.LEARNING_4, status(lobo))
    }

    @Test
    fun aTermTheCardsMadeKnownCarriesOnWhenMovedBackToLearning() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        answer(id, lobo, Rating.EASY)
        while (status(lobo) != TermStatus.WELL_KNOWN) {
            clock.now = service.cardFor(lobo)!!.schedule.due + 8.hours
            answer(id, lobo, Rating.EASY)
        }
        val known = service.cardFor(lobo)!!.schedule
        termService.setStatus(listOf(lobo), TermStatus.LEARNING_3)
        assertEquals(known, service.cardFor(lobo)!!.schedule)
        assertEquals(CardState.REVIEW, known.state)
        assertEquals(TermStatus.LEARNING_3, status(lobo))
    }

    @Test
    fun deletingATermRemovesItsCard() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        answer(id, lobo, Rating.GOOD)
        termService.deleteAll(setOf(lobo))
        assertNull(service.cardFor(lobo))
        assertEquals(0, service.counts(id).total)
    }

    @Test
    fun theDailyLimitsHoldBackNewCardsAndReviews() = runTest {
        val id = language()
        val words = listOf("um", "dois", "três").map { term(id, it) }
        settings.update { it.copy(flashcardNewPerDay = 2) }
        assertEquals(2, service.counts(id).new)
        answer(id, words[0], Rating.EASY)
        answer(id, words[1], Rating.EASY)
        assertNull(service.next(id).card, "the third new card waits for tomorrow")
        assertEquals(0, service.counts(id).new)

        clock.advance(1.days)
        assertEquals(1, service.counts(id).new)
        assertEquals(words[2], service.next(id).card?.termId)

        // Once all three reviews are due, a limit of one shows one a day.
        answer(id, words[2], Rating.EASY)
        clock.now = words.maxOf { service.cardFor(it)!!.schedule.due } + 8.hours
        assertEquals(3, service.counts(id).review)
        settings.update { it.copy(flashcardReviewsPerDay = 1) }
        assertEquals(1, service.counts(id).review)
        answer(id, service.next(id).card!!.termId, Rating.GOOD)
        assertNull(service.next(id).card)
        settings.update { it.copy(flashcardReviewsPerDay = 0) }
        assertEquals(2, service.counts(id).review)
    }

    @Test
    fun anAnswerCanBeTakenBack() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        val before = service.cardFor(lobo)!!
        val answered = answer(id, lobo, Rating.EASY)
        assertEquals(TermStatus.LEARNING_4, status(lobo))

        service.undo(answered)
        assertEquals(before, service.cardFor(lobo))
        assertEquals(TermStatus.NEW_1, status(lobo))
        assertEquals(1, service.counts(id).new, "the day's allowance of new cards is back too")
    }

    @Test
    fun aSuspendedCardIsNotShownAndRestartingForgetsItsHistory() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        val floresta = term(id, "floresta")
        service.setSuspended(lobo, true)
        assertEquals(floresta, service.next(id).card?.termId)
        assertEquals(1, service.counts(id).new)
        service.setSuspended(lobo, false)
        assertEquals(lobo, service.next(id).card?.termId)

        answer(id, lobo, Rating.EASY)
        clock.advance(1.minutes)
        service.restart(lobo)
        val card = service.cardFor(lobo)!!
        assertEquals(CardState.NEW, card.schedule.state)
        assertNull(card.schedule.memory)
        // Restarted cards queue behind the other new ones.
        assertEquals(floresta, service.next(id).card?.termId)
    }

    @Test
    fun aDatabaseFromBeforeFlashcardsGainsThem() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use {
            it.createStatement().execute("DROP TABLE flashcards")
            it.createStatement().execute("DROP TABLE flashcard_reviews")
            it.pretendVersion(9)
        }
        val reopened = DatabaseProvider(DatabaseDriverFactory(file))
        val upgraded = FlashcardService(FlashcardRepositoryImpl(reopened), TermRepositoryImpl(reopened, clock), settings, FlashcardScheduler(timeZone = { TimeZone.UTC }), clock)
        assertEquals(lobo, upgraded.next(id).card?.termId)
    }

    /** A card dates from its word's move to learning, and keeps the date through its answers and later statuses. */
    @Test
    fun aCardDatesFromItsWordsMoveToLearning() = runTest {
        val id = language()
        val lobo = term(id, "lobo", TermStatus.UNKNOWN)
        clock.advance(1.hours)
        terms.updateStatus(listOf(lobo), TermStatus.NEW_1)
        val learning = clock.now
        clock.advance(1.hours)
        assertEquals(learning, service.cardFor(lobo)?.createdAt, "made after the move, dated by it")

        clock.advance(1.days)
        answer(id, lobo, Rating.GOOD)
        assertEquals(learning, service.cardFor(lobo)?.createdAt)
        assertEquals(learning, service.browse(id).cards.single().card.createdAt)

        terms.updateStatus(listOf(lobo), TermStatus.WELL_KNOWN)
        clock.advance(1.days)
        terms.updateStatus(listOf(lobo), TermStatus.NEW_2)
        assertEquals(learning, service.cardFor(lobo)?.createdAt)
    }

    @Test
    fun aCardForgottenAgainAndAgainBecomesALeech() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        settings.update { it.copy(flashcardLeechThreshold = 2, flashcardLeechSuspend = true) }
        answer(id, lobo, Rating.EASY)
        suspend fun forget(): FlashcardService.Answered {
            clock.advance(8.days)
            val forgotten = answer(id, lobo, Rating.AGAIN)
            if (!forgotten.suspended) answer(id, lobo, Rating.GOOD)
            return forgotten
        }
        val first = forget()
        assertTrue(!first.leech, "once is not a leech")
        val second = forget()
        assertTrue(second.leech && second.suspended)
        assertTrue(cards.get(lobo)!!.suspended)
        service.undo(second)
        assertTrue(!cards.get(lobo)!!.suspended, "undone, it is not suspended")

        // Anki tells a leech again every half of the threshold after it.
        assertEquals(listOf(8, 12, 16), (1..17).filter { FlashcardService.leechAt(it, 8) })
        assertEquals(listOf(1, 2, 3), (1..3).filter { FlashcardService.leechAt(it, 1) })
        assertTrue((1..20).none { FlashcardService.leechAt(it, 0) })
    }

    @Test
    fun newCardsComeInTheOrderChosen() = runTest {
        val id = language()
        val words = (1..12).map { term(id, "palavra$it") }
        assertEquals(words.first(), service.next(id).card?.termId, "as saved")
        settings.update { it.copy(flashcardNewOrder = NewCardOrder.NEWEST) }
        assertEquals(words.last(), service.next(id).card?.termId)
        settings.update { it.copy(flashcardNewOrder = NewCardOrder.RANDOM) }
        val today = service.sessionTerms(id).map { it.id }
        assertEquals(words.toSet(), today.toSet())
        assertTrue(today != words && today != words.reversed(), "shuffled")
        assertEquals(today.first(), service.next(id).card?.termId)
        assertEquals(today, service.sessionTerms(id).map { it.id }, "the same all day")
        clock.advance(1.days)
        assertTrue(service.sessionTerms(id).map { it.id } != today, "shuffled again the next day")
    }

    @Test
    fun newCardsAreMixedAmongTheReviewsAsChosen() = runTest {
        val id = language()
        val reviews = (1..4).map { n ->
            val word = term(id, "revisão$n")
            cards.reconcile(clock.now())
            val card = cards.get(word)!!
            cards.save(card.copy(schedule = CardSchedule(CardState.REVIEW, clock.now() - 1.days, MemoryState(5.0, 5.0), 5, 0, 2, 0, clock.now() - 6.days)))
            word
        }
        val news = (1..2).map { term(id, "nova$it") }
        suspend fun kinds() = service.sessionTerms(id).map { if (it.id in news) "N" else "R" }.joinToString("")
        assertEquals("RRRRNN", kinds(), "after the reviews")
        settings.update { it.copy(flashcardNewMix = NewCardMix.BEFORE_REVIEWS) }
        assertEquals("NNRRRR", kinds())
        settings.update { it.copy(flashcardNewMix = NewCardMix.MIXED) }
        assertEquals("RNRRNR", kinds())
        // The cards come up as listed, each answered in turn.
        val shown = buildList {
            repeat(6) {
                val card = service.next(id).card!!
                add(if (card.termId in news) "N" else "R")
                service.answer(card, Rating.EASY)
            }
        }
        assertEquals("RNRRNR", shown.joinToString(""))
        assertTrue(reviews.all { cards.get(it)!!.schedule.due > clock.now() })
    }

    @Test
    fun aSkippedCardWaitsUntilTheNextStudyDay() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        val gato = term(id, "gato")
        assertEquals(2, service.counts(id).new)
        service.skip(listOf(lobo))
        assertEquals(1, service.counts(id).new)
        assertEquals(gato, service.next(id).card?.termId)
        assertEquals(listOf(gato), service.sessionTerms(id).map { it.id })
        assertEquals(CardState.NEW, cards.get(lobo)!!.schedule.state, "its schedule as it was")
        // The study day ends at 4 in the morning.
        clock.advance(16.hours)
        assertEquals(lobo, service.next(id).card?.termId)
    }
}
