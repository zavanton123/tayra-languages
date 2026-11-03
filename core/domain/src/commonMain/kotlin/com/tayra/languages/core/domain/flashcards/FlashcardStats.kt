package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.repository.FlashcardRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration

/** The span the charts over time cover. */
enum class StatsPeriod(val days: Int?) {
    MONTH(30),
    THREE_MONTHS(90),
    YEAR(365),
    ALL(null),
}

/** What a card was when it was answered, as Anki tells answers apart: young below 21 days' interval, mature from it. */
enum class ReviewKind { LEARNING, YOUNG, MATURE, RELEARNING }

/** The spans true retention is told for. */
enum class RetentionSpan(val days: Int?) {
    TODAY(1),
    YESTERDAY(1),
    WEEK(7),
    MONTH(30),
    YEAR(365),
    ALL(null),
}

/** How many of [total] answers were remembered. */
data class Pass(val passed: Int = 0, val total: Int = 0) {
    val rate: Double? get() = if (total == 0) null else passed.toDouble() / total

    operator fun plus(remembered: Boolean) = Pass(passed + if (remembered) 1 else 0, total + 1)
}

/** Today's answers: how many, the time they took, how many were forgotten, by kind of card. */
data class TodayStats(
    val studied: Int = 0,
    val time: Duration = Duration.ZERO,
    val again: Int = 0,
    val kinds: Map<ReviewKind, Int> = emptyMap(),
    val mature: Pass = Pass(),
) {
    val correct: Pass get() = Pass(studied - again, studied)
}

/** One study day's answers by kind of card, and the time they took. */
data class DayReviews(val kinds: Map<ReviewKind, Int>, val time: Duration) {
    val total: Int get() = kinds.values.sum()
}

data class CardCounts(
    val new: Int = 0,
    val learning: Int = 0,
    val relearning: Int = 0,
    val young: Int = 0,
    val mature: Int = 0,
    val suspended: Int = 0,
) {
    val total: Int get() = new + learning + relearning + young + mature + suspended
}

/** Answers at an hour of the day. */
data class HourStats(val hour: Int, val answers: Pass)

/**
 * A language's flashcards in figures, as Anki's statistics give them, days told by their study-day
 * numbers: today's work, the reviews ahead, the answers over [firstDay] to [today], the cards by
 * state, how their memory is spread, true retention, what is known now, and how the answers went.
 */
data class FlashcardStatistics(
    val today: Long,
    val firstDay: Long,
    val todayStats: TodayStats,
    /** Reviews due on each day from today on, by days from today; [overdue] were due before today. */
    val dueByDay: Map<Int, Int>,
    val overdue: Int,
    val reviewsByDay: Map<Long, DayReviews>,
    val counts: CardCounts,
    /** The intervals, in days, of the cards in review. */
    val intervals: List<Int>,
    /** The stability, in days, of the answered cards. */
    val stabilities: List<Double>,
    /** The difficulty of the answered cards, from 0 (easiest) to 1 (hardest), as Anki shows it. */
    val difficulties: List<Double>,
    /** The chance of recall now of the cards in review. */
    val recall: List<Double>,
    val retention: Map<RetentionSpan, Map<ReviewKind, Pass>>,
    val desiredRetention: Double,
    /** How many of the answered cards would be recalled now, as the sum of their chances, and of how many. */
    val knowledge: Double,
    val knowledgeOf: Int,
    val hours: List<HourStats>,
    /** How often each rating, Again first, was given to learning, young and mature cards. */
    val buttons: Map<ReviewKind, List<Int>>,
    /** The cards made on each day. */
    val added: Map<Long, Int>,
    /** How the answers were given, and how many of each were remembered. */
    val answeredBy: Map<AnswerMode, Pass>,
)

/** Works out a language's [FlashcardStatistics] from its cards and their answers. */
class FlashcardStatsService(
    private val cards: FlashcardRepository,
    private val settings: SettingsRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val scheduler = FlashcardScheduler(timeZone = timeZone)

    suspend fun statistics(languageId: Long, period: StatsPeriod): FlashcardStatistics {
        val now = clock.now()
        cards.reconcile(now)
        val config = FlashcardService.schedulerConfig(settings.current).copy(parameters = cards.parameters(languageId)?.parameters)
        val today = scheduler.day(now, config)
        val all = cards.list(languageId)
        val answers = kinds(cards.reviewsOf(languageId))
        val firstAnswer = answers.minOfOrNull { scheduler.day(it.first.at, config) } ?: today
        val firstDay = period.days?.let { today - it + 1 } ?: firstAnswer
        val inPeriod = answers.filter { scheduler.day(it.first.at, config) >= firstDay }

        val todays = answers.filter { scheduler.day(it.first.at, config) == today }
        val todayStats = TodayStats(
            studied = todays.size,
            time = todays.fold(Duration.ZERO) { sum, (review, _) -> sum + (review.answerTime ?: Duration.ZERO) },
            again = todays.count { it.first.rating == Rating.AGAIN },
            kinds = todays.groupingBy { it.second }.eachCount(),
            mature = todays.filter { it.second == ReviewKind.MATURE }.fold(Pass()) { pass, (review, _) -> pass + (review.rating != Rating.AGAIN) },
        )

        val active = all.filter { !it.suspended }
        val horizon = period.days ?: 365
        val due = active.filter { it.schedule.state != CardState.NEW }.map { scheduler.day(it.schedule.due, config) - today }
        val dueByDay = due.filter { it in 0 until horizon }.groupingBy { it.toInt() }.eachCount()

        val reviewsByDay = inPeriod.groupBy { scheduler.day(it.first.at, config) }.mapValues { (_, day) ->
            DayReviews(day.groupingBy { it.second }.eachCount(), day.fold(Duration.ZERO) { sum, (review, _) -> sum + (review.answerTime ?: Duration.ZERO) })
        }

        val counts = CardCounts(
            new = active.count { it.schedule.state == CardState.NEW },
            learning = active.count { it.schedule.state == CardState.LEARNING },
            relearning = active.count { it.schedule.state == CardState.RELEARNING },
            young = active.count { it.schedule.state.isReview && it.schedule.intervalDays < MATURE_DAYS },
            mature = active.count { it.schedule.state.isReview && it.schedule.intervalDays >= MATURE_DAYS },
            suspended = all.count { it.suspended },
        )

        val reviewing = active.filter { it.schedule.state.isReview }
        val remembered = active.filter { it.schedule.memory != null && it.schedule.state != CardState.NEW }
        val chances = remembered.mapNotNull { scheduler.retrievability(it.schedule, now, config) }

        val retention = RetentionSpan.entries.associateWith { span ->
            val from = when (span) {
                RetentionSpan.TODAY -> today
                RetentionSpan.YESTERDAY -> today - 1
                else -> span.days?.let { today - it + 1 } ?: Long.MIN_VALUE
            }
            val until = if (span == RetentionSpan.YESTERDAY) today - 1 else today
            answers.filter { (_, kind) -> kind == ReviewKind.YOUNG || kind == ReviewKind.MATURE }
                .filter { scheduler.day(it.first.at, config) in from..until }
                .groupBy({ it.second }, { it.first.rating != Rating.AGAIN })
                .mapValues { (_, outcomes) -> outcomes.fold(Pass()) { pass, kept -> pass + kept } }
        }

        val hours = (0 until 24).map { hour ->
            HourStats(hour, inPeriod.filter { it.first.at.toLocalDateTime(timeZone()).hour == hour }.fold(Pass()) { pass, (review, _) -> pass + (review.rating != Rating.AGAIN) })
        }

        val buttons = inPeriod.groupBy { if (it.second == ReviewKind.RELEARNING) ReviewKind.LEARNING else it.second }.mapValues { (_, kind) ->
            Rating.entries.map { rating -> kind.count { it.first.rating == rating } }
        }

        val added = all.mapNotNull { it.createdAt?.let { at -> scheduler.day(at, config) } }.filter { it >= firstDay }.groupingBy { it }.eachCount()

        val answeredBy = inPeriod.groupBy { it.first.answeredBy }.mapValues { (_, given) -> given.fold(Pass()) { pass, (review, _) -> pass + (review.rating != Rating.AGAIN) } }

        return FlashcardStatistics(
            today = today,
            firstDay = firstDay,
            todayStats = todayStats,
            dueByDay = dueByDay,
            overdue = due.count { it < 0 },
            reviewsByDay = reviewsByDay,
            counts = counts,
            intervals = reviewing.map { it.schedule.intervalDays },
            stabilities = remembered.mapNotNull { it.schedule.memory?.stability },
            difficulties = remembered.mapNotNull { it.schedule.memory?.difficulty?.let { d -> ((d - 1) / 9).coerceIn(0.0, 1.0) } },
            recall = reviewing.mapNotNull { scheduler.retrievability(it.schedule, now, config) },
            retention = retention,
            desiredRetention = config.desiredRetention,
            knowledge = chances.sum(),
            knowledgeOf = remembered.size,
            hours = hours,
            buttons = buttons,
            added = added,
            answeredBy = answeredBy,
        )
    }

    /**
     * Each answer with the kind of card it was given to: a review is young or mature by the interval
     * the answer before it gave, or, without one, by the days since it.
     */
    private fun kinds(reviews: List<FlashcardReview>): List<Pair<FlashcardReview, ReviewKind>> =
        reviews.groupBy { it.termId }.values.flatMap { answers ->
            var interval: Int? = null
            answers.sortedWith(compareBy({ it.at }, { it.id })).map { review ->
                val kind = when (review.stateBefore) {
                    CardState.NEW, CardState.LEARNING -> ReviewKind.LEARNING
                    CardState.RELEARNING -> ReviewKind.RELEARNING
                    CardState.REVIEW -> if ((interval ?: review.elapsedDays) >= MATURE_DAYS) ReviewKind.MATURE else ReviewKind.YOUNG
                }
                if (review.intervalDays > 0) interval = review.intervalDays
                review to kind
            }
        }

    private companion object {
        /** From this interval on a card is mature, as in Anki. */
        const val MATURE_DAYS = 21
    }
}
