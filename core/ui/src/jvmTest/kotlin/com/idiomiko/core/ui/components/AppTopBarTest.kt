package com.idiomiko.core.ui.components

import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.graphics.toPixelMap
import com.idiomiko.core.ui.theme.AppAccents
import kotlin.math.abs
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

    /** The settings and the account are under the account menu at the end of the bar, not tabs of their own. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theBarHasNoSettingsOrAccountTab() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        setContent { MaterialTheme { AppTopBar(title = "Idiomiko", onNavigate = {}, section = NavSection.HOME) } }
        for (tab in listOf("Home", "Read", "Playlists", "Flashcards", "Vocabulary")) onNodeWithText(tab).assertExists()
        for (gone in listOf("Settings", "Account", "More")) onNodeWithText(gone).assertDoesNotExist()
        onNodeWithContentDescription("Account and settings").assertExists()
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

    /**
     * Signed out, the bar ends with a person in a circle whose menu says so and leads to signing in, the settings,
     * the backups and About, with nothing to sign out of.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun signedOutThePersonIconOffersSigningInAndTheSettings() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val visited = mutableListOf<Route>()
        setContent { MaterialTheme { AppTopBar(title = "Home", onNavigate = { visited += it }, section = NavSection.HOME) } }
        onNodeWithTag("account-avatar").assertDoesNotExist()
        val pages = listOf("Sign in" to Route.Account, "Settings" to Route.Settings, "About" to Route.About)
        for ((label, _) in pages) {
            onNodeWithTag("account-button").performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-menu").fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Not signed in").assertExists()
            onNodeWithText("Sign out").assertDoesNotExist()
            onNodeWithText("Account").assertDoesNotExist()
            if (label == "Sign in") saveMenu("ACCOUNT_MENU_SCREENSHOT", "-signed-out")
            onNodeWithText(label).performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-menu").fetchSemanticsNodes().isEmpty() }
        }
        assertEquals(pages.map { it.second }, visited)
    }

    /**
     * Signed in, the avatar takes the person's place; its menu names the account and leads to it, the settings, the
     * backups and About, and signs out last, apart from the rest.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun signedInTheAvatarOffersTheAccountTheSettingsAndSigningOut() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val visited = mutableListOf<Route>()
        var opened = 0
        var signedOut = 0
        val badge = AccountBadge("ana@example.com", "Ana Reader", picture = null, signOut = { signedOut++ }) { opened++ }
        setContent {
            MaterialTheme { CompositionLocalProvider(LocalAccountBadge provides badge) { AppTopBar(title = "Home", onNavigate = { visited += it }, section = NavSection.HOME) } }
        }
        onNodeWithTag("account-button").assertDoesNotExist()
        onNodeWithText("A").assertExists()
        fun open() {
            onNodeWithTag("account-avatar").performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-menu").fetchSemanticsNodes().isNotEmpty() }
        }
        open()
        onNodeWithText("Ana Reader").assertExists()
        onNodeWithText("ana@example.com").assertExists()
        onNodeWithText("Sign in").assertDoesNotExist()
        onNodeWithText("Not signed in").assertDoesNotExist()
        saveMenu("ACCOUNT_MENU_SCREENSHOT", "-signed-in")
        onNodeWithText("Account").performClick()
        assertEquals(1, opened)
        for (label in listOf("Settings", "About")) {
            open()
            onNodeWithText(label).performClick()
        }
        assertEquals(listOf<Route>(Route.Settings, Route.About), visited)
        open()
        onNodeWithTag("account-menu-sign-out").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-menu").fetchSemanticsNodes().isEmpty() }
        assertEquals(1, signedOut)
    }

    /** The bar with its open menu drawn over it, saved to the file the environment variable [env] names. */
    @OptIn(ExperimentalTestApi::class)
    private fun androidx.compose.ui.test.ComposeUiTest.saveMenu(env: String, suffix: String) {
        val path = System.getenv(env) ?: return
        waitForIdle()
        val roots = onAllNodes(isRoot())
        val page = roots[0].captureToImage().toAwtImage()
        val canvas = java.awt.image.BufferedImage(page.width, minOf(page.height, 480), java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = canvas.createGraphics()
        g.drawImage(page, 0, 0, null)
        for (i in 1 until roots.fetchSemanticsNodes().size) {
            val bounds = roots[i].fetchSemanticsNode().boundsInWindow
            g.drawImage(roots[i].captureToImage().toAwtImage(), bounds.left.toInt(), bounds.top.toInt(), null)
        }
        g.dispose()
        javax.imageio.ImageIO.write(canvas, "png", java.io.File(path.replace(".png", "$suffix.png")))
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

    /** A phone's bar spins while a sync runs, on pages with a way back too, and shows nothing once it ends; a tap asks to sync. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aPhonesBarSpinsWhileSyncing() = runDesktopComposeUiTest(width = 400, height = 800) {
        var asked = 0
        var entry by mutableStateOf(SyncEntry(running = true) { asked++ })
        setContent {
            MaterialTheme {
                ProvideWindowWidth {
                    CompositionLocalProvider(LocalSyncEntry provides entry) {
                        AppTopBar(title = "Flashcards", onNavigate = {}, onBack = {}, section = NavSection.FLASHCARDS)
                    }
                }
            }
        }
        onNodeWithContentDescription("Syncing…").assertExists()
        onNodeWithTag("sync-spinner").performClick()
        assertEquals(1, asked)
        saveBar("SYNC_SPINNER_SCREENSHOT")
        entry = SyncEntry(running = false, lastSynced = kotlin.time.Clock.System.now()) { asked++ }
        waitForIdle()
        onNodeWithTag("sync-spinner").assertDoesNotExist()
        onNodeWithTag("sync-button").assertDoesNotExist()
    }

    /**
     * Signed out, a phone's bar shows a person in a circle in the avatar's place; it opens the account menu, as on the
     * wide bar, whose Settings leads to the phone's list of the settings pages.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aPhonesBarOpensTheAccountMenu() = runDesktopComposeUiTest(width = 400, height = 800) {
        val visited = mutableListOf<Route>()
        setContent { MaterialTheme { ProvideWindowWidth { AppTopBar(title = "Home", onNavigate = { visited += it }, section = NavSection.HOME) } } }
        onNodeWithTag("account-avatar").assertDoesNotExist()
        System.getenv("PHONE_BAR_SCREENSHOT")?.let { path ->
            waitForIdle()
            javax.imageio.ImageIO.write(onAllNodes(isRoot()).onFirst().captureToImage().toAwtImage(), "png", java.io.File(path))
        }
        for (label in listOf("Sign in", "Settings", "About")) {
            onNodeWithTag("account-button").performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-menu").fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText(label).performClick()
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("account-menu").fetchSemanticsNodes().isEmpty() }
        }
        assertEquals(listOf<Route>(Route.Account, Route.SettingsMenu, Route.About), visited)
    }

    @Test
    fun theAvatarsLetterIsTheNamesOrTheEmails() {
        assertEquals("A", initial("ana reader", "zed@example.com"))
        assertEquals("Z", initial(null, "zed@example.com"))
        assertEquals("Z", initial("  ", "zed@example.com"))
    }

    /** The sync status shows only with an account service and asks to sync; green only while synced with nothing left to send, it says when the device last synced, that changes wait, or that a sync runs or failed. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theSyncStatusOpensTheQuestion() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        var asked = 0
        var entry by mutableStateOf(SyncEntry(running = false) { asked++ })
        setContent {
            MaterialTheme { CompositionLocalProvider(LocalSyncEntry provides entry) { AppTopBar(title = "Home", onNavigate = {}, section = NavSection.HOME) } }
        }
        onNodeWithContentDescription("Sync").assertExists()
        onNodeWithTag("sync-button").performClick()
        assertEquals(1, asked)
        entry = SyncEntry(running = true) { asked++ }
        waitForIdle()
        onNodeWithContentDescription("Syncing…").assertExists()
        onNodeWithTag("sync-button").performClick()
        assertEquals(2, asked)
        entry = SyncEntry(running = false, lastSynced = kotlin.time.Clock.System.now() - kotlin.time.Duration.parse("2m")) { asked++ }
        waitForIdle()
        onNodeWithContentDescription("Synced 2 minutes ago").assertExists()
        assertEquals(true, cloudIsGreen(), "synced, with nothing left to send")
        saveBar("SYNC_STATUS_SCREENSHOT")
        // Something changed here since: the cloud turns grey until the next sync.
        entry = SyncEntry(running = false, lastSynced = kotlin.time.Clock.System.now() - kotlin.time.Duration.parse("2m"), unsent = true) { asked++ }
        waitForIdle()
        onNodeWithContentDescription("Changes not synced yet").assertExists()
        assertEquals(false, cloudIsGreen())
        saveBar("SYNC_STATUS_SCREENSHOT", "-unsent")
        // Never synced, it asks to sync whatever this device holds.
        entry = SyncEntry(running = false, unsent = true) { asked++ }
        waitForIdle()
        onNodeWithContentDescription("Sync").assertExists()
        assertEquals(false, cloudIsGreen())
        entry = SyncEntry(running = false, failed = true) { asked++ }
        waitForIdle()
        onNodeWithContentDescription("Sync failed").assertExists()
        assertEquals(false, cloudIsGreen())
    }

    /** Whether the bar's cloud is drawn in the green that says all is synced. */
    @OptIn(ExperimentalTestApi::class)
    private fun androidx.compose.ui.test.ComposeUiTest.cloudIsGreen(): Boolean {
        val pixels = onNodeWithTag("sync-icon", useUnmergedTree = true).captureToImage().toPixelMap()
        val green = AppAccents.green
        return (0 until pixels.width).any { x ->
            (0 until pixels.height).any { y ->
                val c = pixels[x, y]
                abs(c.red - green.red) < 0.03f && abs(c.green - green.green) < 0.03f && abs(c.blue - green.blue) < 0.03f
            }
        }
    }

    /** The bar, with the cloud, saved to the file the environment variable [env] names. */
    @OptIn(ExperimentalTestApi::class)
    private fun androidx.compose.ui.test.ComposeUiTest.saveBar(env: String, suffix: String = "") {
        val path = System.getenv(env) ?: return
        val image = onRoot().captureToImage().toAwtImage()
        val strip = image.getSubimage(0, 0, image.width, minOf(image.height, 72))
        javax.imageio.ImageIO.write(strip, "png", java.io.File(path.replace(".png", "$suffix.png")))
    }

    /** In a narrower window the tabs that do not fit fold into More, whose menu still reaches their pages; the account menu stays at the end. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tabsThatDoNotFitFoldIntoMore() = runDesktopComposeUiTest(width = 760, height = 900) {
        val visited = mutableListOf<Route>()
        val state = LearningLanguageState(listOf(1L to "Portuguese"), 1L, knownWords = 1369) {}
        setContent { MaterialTheme { CompositionLocalProvider(LocalLearningLanguage provides state) { AppTopBar(title = "Statistics", onNavigate = { visited += it }, section = NavSection.TERMS) } } }
        onNodeWithText("Home").assertExists()
        onNodeWithContentDescription("Account and settings").assertExists()
        System.getenv("NARROW_BAR_SCREENSHOT")?.let { path ->
            waitForIdle()
            javax.imageio.ImageIO.write(onAllNodes(isRoot()).onFirst().captureToImage().toAwtImage(), "png", java.io.File(path))
        }
        onNodeWithText("More").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Statistics").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Settings").assertDoesNotExist()
        onAllNodesWithText("Statistics").onLast().performClick()
        assertEquals(listOf<Route>(Route.Stats), visited)
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

    /** With no language chosen, the bar still offers one to choose, which opens the languages. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun withNoLanguageChosenTheBarOffersToChooseOne() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val state = LearningLanguageState(listOf(1L to "Portuguese", 2L to "German"), 0L) {}
        setContent { MaterialTheme { CompositionLocalProvider(LocalLearningLanguage provides state) { AppTopBar(title = "Home", onNavigate = {}, section = NavSection.HOME) } } }
        onNodeWithText("Choose a language to learn").assertExists()
        onNodeWithTag("choose-language").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("language-dialog").fetchSemanticsNodes().isNotEmpty() }
    }

}
