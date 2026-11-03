package com.tayra.languages.feature.settings

import com.tayra.languages.core.domain.flashcards.BacklogOrder
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertTextContains
import kotlin.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.flashcards.PersonalScheduling
import com.tayra.languages.core.domain.flashcards.Fsrs
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** The daily limits, the wanted retention and the learning steps of the flashcards. */
@OptIn(ExperimentalTestApi::class)
class FlashcardSettingsScreenTest {

    @Test
    fun limitsAndStepsAreSavedAsTheyChange() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val settings = SettingsRepositoryImpl(MapSettings())
        setContent { Hosted(settings) { FlashcardSettingsScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings)) } }
        waitForIdle()
        System.getenv("FLASHCARD_SETTINGS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }

        onNodeWithContentDescription("Increase new cards a day").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardNewPerDay == 25 }

        // Reviews are unlimited until a limit is switched on, which starts at Anki's 200.
        assertEquals(0, settings.current.flashcardReviewsPerDay)
        assertEquals(0, onAllNodesWithText("Reviews a day").fetchSemanticsNodes().size)
        onAllNodes(isToggleable())[0].performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardReviewsPerDay == 200 }
        onNodeWithContentDescription("Decrease reviews a day").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardReviewsPerDay == 190 }

        // Steps are saved once they read as steps; anything else is flagged and left unsaved.
        onNodeWithContentDescription("Learning steps").performTextClearance()
        onNodeWithContentDescription("Learning steps").performTextInput("30s 5m 1h")
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardLearnSteps == "30s 5m 1h" }
        onNodeWithContentDescription("Relearning steps").performTextClearance()
        onNodeWithContentDescription("Relearning steps").performTextInput("soon")
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Use steps such as 1m 10m").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("", settings.current.flashcardRelearnSteps, "clearing the field saved no steps; the unreadable text was not saved")

        onNodeWithText("Two answer buttons").performScrollTo()
        onAllNodes(isToggleable()).fetchSemanticsNodes().size.let { n -> onAllNodes(isToggleable())[n - 1].performClick() }
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardTwoButtons }

        onNodeWithText("Reset to defaults").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardNewPerDay == 20 && settings.current.flashcardLearnSteps == "1m 10m" && settings.current.flashcardRelearnSteps == "10m" }
        assertEquals(false, settings.current.flashcardTwoButtons, "reset brings the four buttons back")
        assertEquals(0, settings.current.flashcardReviewsPerDay)
    }

    @Test
    fun loadBalancingAndEasyDaysAreSetWithADayAlwaysNormal() = runDesktopComposeUiTest(width = 1586, height = 1400) {
        val settings = SettingsRepositoryImpl(MapSettings())
        runBlocking { settings.update { it.copy(flashcardEasyDays = "MMMMMMN") } }
        setContent { Hosted(settings) { FlashcardSettingsScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings)) } }
        onNodeWithTag("load-balancing").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { !settings.current.flashcardLoadBalancing }

        // Sunday is the only normal day: it cannot be made lighter.
        onNodeWithTag("easy-day-6").performScrollTo().performClick()
        onNodeWithText("Reduced").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("easy-days-no-normal").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("MMMMMMN", settings.current.flashcardEasyDays)

        onNodeWithTag("easy-day-5").performClick()
        onNodeWithText("Normal").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardEasyDays == "MMMMMNN" }
        onNodeWithTag("easy-day-5").assertTextContains("Sat · Normal", substring = true)
        System.getenv("EASY_DAYS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }

        onNodeWithText("Reset to defaults").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardEasyDays == "NNNNNNN" && settings.current.flashcardLoadBalancing }
    }

    @Test
    fun theOrderOfABacklogIsChosenWithWhatEachDoes() = runDesktopComposeUiTest(width = 1586, height = 1200) {
        val settings = SettingsRepositoryImpl(MapSettings())
        setContent { Hosted(settings) { FlashcardSettingsScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings)) } }
        onNodeWithTag("backlog-order-explanation").performScrollTo().assertTextContains("most at risk", substring = true)
        onNodeWithTag("backlog-order").performClick()
        onNodeWithText("Words I'm reading first").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardBacklogOrder == BacklogOrder.READING_FIRST }
        onNodeWithTag("backlog-order-explanation").assertTextContains("the five pages after it", substring = true)
        onNodeWithText("Reset to defaults").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardBacklogOrder == BacklogOrder.RELATIVE_OVERDUENESS }
    }

    /** A language with [count] words reviewed for months by a learner who forgets sooner than the defaults expect. */
    private suspend fun reviewedLanguage(provider: DatabaseProvider, count: Int): Long {
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val cards = FlashcardRepositoryImpl(provider)
        val id = languages.save(Language(name = "Portuguese"))
        val truth = Fsrs.DEFAULT_PARAMETERS.copyOf().also { it[0] = 0.1; it[1] = 0.6; it[2] = 1.2; it[3] = 4.0; it[8] = 1.4; it[20] = 0.3 }
        val fsrs = Fsrs(truth)
        val random = Random(4)
        val start = Instant.parse("2026-01-05T12:00:00Z")
        repeat(count) { n ->
            val termId = terms.save(Term(languageId = id, text = "w$n", textLc = "w$n", status = TermStatus.NEW_1))
            var day = random.nextInt(40)
            var state = fsrs.next(null, 0, Rating.GOOD)
            val reviews = mutableListOf(FlashcardReview(termId = termId, at = start + day.days, rating = Rating.GOOD, stateBefore = CardState.NEW, intervalDays = 0, elapsedDays = 0))
            while (true) {
                val interval = fsrs.interval(state.stability, 0.9).toInt().coerceAtLeast(1)
                if (day + interval >= 150) break
                day += interval
                val rating = if (random.nextDouble() < fsrs.retrievability(interval.toDouble(), state.stability)) Rating.GOOD else Rating.AGAIN
                state = fsrs.next(state, interval, rating)
                reviews += FlashcardReview(termId = termId, at = start + day.days, rating = rating, stateBefore = CardState.REVIEW, intervalDays = interval, elapsedDays = interval)
            }
            cards.addReviews(reviews)
        }
        return id
    }

    @Test
    fun aLanguageIsOptimisedOnItsAnswersAndCanGoBackToTheDefaults() = runDesktopComposeUiTest(width = 1586, height = 1200) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-personal-ui", ".db").also { it.delete() }))
        val pt = runBlocking { reviewedLanguage(provider, 300) }
        val personal = PersonalSchedulingViewModel(PersonalScheduling(FlashcardRepositoryImpl(provider), settings), LanguageRepositoryImpl(provider))
        setContent { Hosted(settings) { FlashcardSettingsScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings), personal = personal) } }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("personal-status-$pt").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("personal-status-$pt").assertTextContains("The defaults, with", substring = true)
        assertEquals(0, onAllNodesWithTag("personal-defaults-$pt").fetchSemanticsNodes().size)

        onNodeWithTag("personal-optimise-$pt").performScrollTo().performClick()
        waitUntil(timeoutMillis = 30_000) { onAllNodesWithText("Your own: trained on", substring = true).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("personal-status-$pt").assertTextContains("better than the defaults", substring = true)
        System.getenv("PERSONAL_SCHEDULING_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }

        onNodeWithTag("personal-defaults-$pt").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("The defaults, with", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    /** A phone gets compact rows, the retention slider under its label, the explanation folded away and Reset at the bottom. */
    @Test
    fun aPhoneChangesTheSettingsInCompactRows() = runDesktopComposeUiTest(width = 400, height = 1300) {
        val settings = SettingsRepositoryImpl(MapSettings())
        setContent { Hosted(settings) { FlashcardSettingsScreen(onNavigate = {}, onBack = {}, viewModel = SettingsViewModel(settings)) } }
        waitForIdle()
        onNodeWithText("Daily limits and review scheduling.").assertExists()
        onNodeWithText("How many cards a day, for each language.").assertDoesNotExist()
        System.getenv("FLASHCARD_SETTINGS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it.replace(".png", "-phone.png"))) }

        onNodeWithContentDescription("Increase new cards a day").performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardNewPerDay == 25 }
        onAllNodes(isToggleable())[0].performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardReviewsPerDay == 200 }
        onNodeWithTag("phone-retention").performSemanticsAction(SemanticsActions.SetProgress) { it(85f) }
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardRetention == 85 }
        onNodeWithText("85%").assertExists()
        onNodeWithContentDescription("Learning steps").performTextClearance()
        onNodeWithContentDescription("Learning steps").performTextInput("30s 5m")
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardLearnSteps == "30s 5m" }

        onNodeWithTag("phone-two-buttons").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardTwoButtons }
        onNodeWithTag("phone-how-it-works").performScrollTo().performClick()
        onNodeWithText("Every word at status 1 to 4 has a card.", substring = true).assertExists()
        onNodeWithTag("phone-reset-flashcards").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { settings.current.flashcardNewPerDay == 20 && settings.current.flashcardLearnSteps == "1m 10m" && settings.current.flashcardRetention == 90 }
    }
}
