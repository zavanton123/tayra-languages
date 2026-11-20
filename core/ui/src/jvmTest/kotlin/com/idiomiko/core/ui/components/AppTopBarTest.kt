package com.idiomiko.core.ui.components

import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.idiomiko.core.ui.navigation.Route
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AppTopBarTest {
    @get:Rule
    val rule = createComposeRule()

    /** The Settings menu is tall enough for all its entries: its list does not scroll, so it shows no scrollbar. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theSettingsMenuFitsWithoutScrolling() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        setContent { MaterialTheme { AppTopBar(title = "Idiomiko", onNavigate = {}, section = NavSection.HOME) } }
        onNodeWithText("Settings").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("About").fetchSemanticsNodes().isNotEmpty() }
        val lists = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and hasAnyDescendant(hasText("About"))).fetchSemanticsNodes()
        System.getenv("SETTINGS_MENU_SCREENSHOT")?.let { path ->
            waitForIdle()
            javax.imageio.ImageIO.write(onAllNodes(isRoot())[1].captureToImage().toAwtImage(), "png", java.io.File(path))
        }
        for (list in lists) assertEquals(0f, list.config[SemanticsProperties.VerticalScrollAxisRange].maxValue(), "nothing is left to scroll to")
    }

    /** The courses, the books and the news share the Read menu, which has no Courses or News tab beside it. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theReadMenuHoldsTheCoursesTheBooksAndTheNews() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        val visited = mutableListOf<Route>()
        setContent { MaterialTheme { AppTopBar(title = "Courses", onNavigate = { visited += it }, section = NavSection.COURSES) } }
        for (gone in listOf("Courses", "Books", "News", "All books")) onNodeWithText(gone).assertDoesNotExist()
        val pages = listOf("Courses" to Route.Courses, "Books" to Route.Books, "Find free books" to Route.FindBooks, "Book archive" to Route.ArchivedBooks, "News" to Route.News)
        for ((label, _) in pages) {
            onNodeWithText("Read").performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText(label).performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithText(label).fetchSemanticsNodes().isEmpty() }
        }
        assertEquals(pages.map { it.second }, visited)
    }

    /** The Downloads page is in the Settings menu, not a tab of its own. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theSettingsMenuLeadsToTheDownloads() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        val visited = mutableListOf<Route>()
        setContent { MaterialTheme { AppTopBar(title = "Books", onNavigate = { visited += it }, section = NavSection.BOOKS) } }
        onNodeWithText("Downloads").assertDoesNotExist()
        onNodeWithText("Settings").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Downloads").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Downloads").performClick()
        assertEquals(listOf<Route>(Route.Downloads), visited)
    }

    /** Signed in, the account's avatar ends the bar and takes the Account tab's place; it opens the account page. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theAvatarLeadsToTheAccount() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        var opened = 0
        var badge by mutableStateOf<AccountBadge?>(null)
        setContent { MaterialTheme { CompositionLocalProvider(LocalAccountBadge provides badge) { AppTopBar(title = "Home", onNavigate = {}, section = NavSection.HOME) } } }
        onNodeWithText("Account").assertExists()
        onNodeWithTag("account-avatar").assertDoesNotExist()
        badge = AccountBadge("ana@example.com", "Ana Reader", picture = null) { opened++ }
        waitForIdle()
        onNodeWithText("Account").assertDoesNotExist()
        onNodeWithText("A").assertExists()
        onNodeWithTag("account-avatar").performClick()
        assertEquals(1, opened)
    }

    /** A phone's bar shows the avatar on a page with a way back as well. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aPhonesBarKeepsTheAvatarBesideTheWayBack() = runDesktopComposeUiTest(width = 400, height = 800) {
        setContent {
            MaterialTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalAccountBadge provides AccountBadge("ana@example.com", "Ana Reader", picture = null) {}) {
                        AppTopBar(title = "Flashcards", onNavigate = {}, onBack = {}, section = NavSection.FLASHCARDS)
                    }
                }
            }
        }
        onNodeWithContentDescription("Back").assertExists()
        onNodeWithTag("account-avatar").assertExists()
    }

    @Test
    fun theAvatarsLetterIsTheNamesOrTheEmails() {
        assertEquals("A", initial("ana reader", "zed@example.com"))
        assertEquals("Z", initial(null, "zed@example.com"))
        assertEquals("Z", initial("  ", "zed@example.com"))
    }

    /** The sync status shows only with an account service and asks to sync; it says when the device last synced, or that a sync runs or failed. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theSyncStatusOpensTheQuestion() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        var asked = 0
        var entry by mutableStateOf(SyncEntry(running = false) { asked++ })
        setContent {
            MaterialTheme { CompositionLocalProvider(LocalSyncEntry provides entry) { AppTopBar(title = "Home", onNavigate = {}, section = NavSection.HOME) } }
        }
        onNodeWithText("Sync").assertExists()
        onNodeWithTag("sync-button").performClick()
        assertEquals(1, asked)
        entry = SyncEntry(running = true) { asked++ }
        waitForIdle()
        onNodeWithText("Syncing…").assertExists()
        onNodeWithTag("sync-button").performClick()
        assertEquals(2, asked)
        entry = SyncEntry(running = false, lastSynced = kotlin.time.Clock.System.now() - kotlin.time.Duration.parse("2m")) { asked++ }
        waitForIdle()
        onNodeWithText("Synced 2 minutes ago").assertExists()
        System.getenv("SYNC_STATUS_SCREENSHOT")?.let { path ->
            javax.imageio.ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", java.io.File(path))
        }
        entry = SyncEntry(running = false, failed = true) { asked++ }
        waitForIdle()
        onNodeWithText("Sync failed").assertExists()
    }

    /** In a narrower window the tabs that do not fit fold into More, whose menu still reaches their pages. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tabsThatDoNotFitFoldIntoMore() = runDesktopComposeUiTest(width = 880, height = 900) {
        val visited = mutableListOf<Route>()
        val state = LearningLanguageState(listOf(1L to "Portuguese"), 1L, knownWords = 1369) {}
        setContent { MaterialTheme { CompositionLocalProvider(LocalLearningLanguage provides state) { AppTopBar(title = "Books", onNavigate = { visited += it }, section = NavSection.SETTINGS) } } }
        onNodeWithText("Home").assertExists()
        System.getenv("NARROW_BAR_SCREENSHOT")?.let { path ->
            waitForIdle()
            javax.imageio.ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", java.io.File(path))
        }
        onNodeWithText("More").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("About").fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText("Downloads").onLast().performClick()
        assertEquals(listOf<Route>(Route.Downloads), visited)
    }

    @Test
    fun theFlashcardsMenuLeadsToTheFlashcardStatistics() {
        val visited = mutableListOf<Route>()
        rule.setContent { MaterialTheme { AppTopBar(title = "Flashcards", onNavigate = { visited += it }, section = NavSection.FLASHCARDS) } }
        rule.onNodeWithText("Flashcards").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Flashcard statistics").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Flashcard statistics").performClick()
        assertEquals(listOf<Route>(Route.FlashcardStats), visited)
    }

    @Test
    fun theLogoAlwaysShowsTheAppNameAndLeadsHome() {
        val visited = mutableListOf<Route>()
        rule.setContent { MaterialTheme { AppTopBar(title = "Statistics", onNavigate = { visited += it }, section = NavSection.TERMS) } }
        rule.onNodeWithText("Idiomiko").performClick()
        assertEquals(listOf<Route>(Route.Home), visited)
        assertEquals(0, rule.onAllNodesWithText("Statistics").fetchSemanticsNodes().size, "the screen's own title stays out of the wide bar")
    }

    @Test
    fun theLanguageSelectorShowsTheWordsKnownInTheLanguage() {
        val state = LearningLanguageState(listOf(1L to "Portuguese", 2L to "German"), 1L, knownWords = 1234) {}
        rule.setContent {
            MaterialTheme { CompositionLocalProvider(LocalLearningLanguage provides state) { AppTopBar(title = "Books", onNavigate = {}, section = NavSection.BOOKS) } }
        }
        rule.onNodeWithText("Portuguese (1,234)").assertExists()
        rule.onNodeWithContentDescription("Learning language: Portuguese (1,234)").assertExists()
    }

    /** The selector opens a menu of the languages being learned, each with its words; choosing another one switches to it. */
    @Test
    fun theLanguageMenuSwitchesBetweenTheLanguagesBeingLearned() {
        val chosen = mutableListOf<Long>()
        val state = LearningLanguageState(
            listOf(1L to "Portuguese", 2L to "German", 3L to "Russian", 4L to "Spanish"), 1L, knownWords = 586,
            mine = listOf(1L, 2L), wordCounts = mapOf(1L to 586, 2L to 12),
        ) { chosen += it }
        rule.setContent {
            MaterialTheme { CompositionLocalProvider(LocalLearningLanguage provides state) { AppTopBar(title = "Books", onNavigate = {}, section = NavSection.BOOKS) } }
        }
        rule.onNodeWithContentDescription("Learning language: Portuguese (586)").performClick()
        rule.onNodeWithTag("my-language-pt").assertExists()
        rule.onNodeWithText("586 words").assertExists()
        rule.onNodeWithText("12 words").assertExists()
        assertEquals(0, rule.onAllNodesWithTag("my-language-es").fetchSemanticsNodes().size, "only the languages being learned")
        rule.onNodeWithTag("my-language-de").performClick()
        assertEquals(listOf(2L), chosen)
        assertEquals(0, rule.onAllNodesWithTag("language-menu").fetchSemanticsNodes().size, "the menu closes with the choice")
    }

    /** Adding a language opens a dialog of those not being learned, as tiles that the search narrows; the native one cannot be chosen. */
    @Test
    fun aLanguageIsAddedFromTheMenu() {
        val chosen = mutableListOf<Long>()
        val state = LearningLanguageState(
            listOf(1L to "Portuguese", 2L to "German", 3L to "Russian", 4L to "Spanish"), 1L, nativeIds = setOf(3L), knownWords = 586, mine = listOf(1L, 4L),
        ) { chosen += it }
        rule.setContent {
            MaterialTheme { CompositionLocalProvider(LocalLearningLanguage provides state) { AppTopBar(title = "Books", onNavigate = {}, section = NavSection.BOOKS) } }
        }
        rule.onNodeWithContentDescription("Learning language: Portuguese (586)").performClick()
        rule.onNodeWithTag("add-language").performClick()
        rule.onNodeWithText("Add a language").assertExists()
        rule.onNodeWithText("2 languages").assertExists()
        assertEquals(0, rule.onAllNodesWithTag("language-es").fetchSemanticsNodes().size, "a language being learned is not offered")
        rule.onNodeWithText("Your native language").assertExists()
        rule.onNodeWithTag("language-ru").assertIsNotEnabled()

        rule.onNodeWithTag("language-search").performTextInput("ger")
        rule.onNodeWithText("1 language").assertExists()
        assertEquals(0, rule.onAllNodesWithTag("language-ru").fetchSemanticsNodes().size)
        rule.onNodeWithTag("language-de").performClick()
        assertEquals(listOf(2L), chosen)
        assertEquals(0, rule.onAllNodesWithText("Add a language").fetchSemanticsNodes().size, "the dialog closes with the choice")
    }
}
