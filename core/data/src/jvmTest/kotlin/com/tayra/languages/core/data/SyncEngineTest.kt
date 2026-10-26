package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.data.sync.SyncEngine
import com.tayra.languages.core.data.sync.SyncTransport
import com.tayra.languages.core.domain.account.AccountException
import com.tayra.languages.core.domain.account.AccountProblem
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.sync.PullResponse
import com.tayra.languages.sync.PushRequest
import com.tayra.languages.sync.PushResponse
import com.tayra.languages.sync.SyncMerge
import com.tayra.languages.sync.SyncRecord
import kotlinx.coroutines.runBlocking
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Two devices syncing through an account that merges as the service does: what one saves, changes
 * and deletes reaches the other, changes made on both are kept by the shared rules, and a sync that
 * fails partway changes nothing here.
 */
class SyncEngineTest {

    /** The service's side: each record kept merged, numbered by the change that last changed it. */
    private class Account : SyncTransport {
        val records = linkedMapOf<Pair<String, String>, Pair<SyncRecord, Long>>()
        var seq = 0L
        var account: String? = "http://localhost me@example.com"
        var failPullAfter = Int.MAX_VALUE
        private var pulls = 0

        override fun syncAccount(): String? = account

        override suspend fun push(request: PushRequest): PushResponse {
            for (incoming in request.records) {
                val id = incoming.kind to incoming.key
                val stored = records[id]?.first
                val merged = if (stored == null) incoming else SyncMerge.merge(stored, incoming)
                if (merged != stored) records[id] = merged to ++seq
            }
            return PushResponse(seq, System.currentTimeMillis())
        }

        override suspend fun pull(since: Long, limit: Int): PullResponse {
            if (pulls++ >= failPullAfter) throw AccountException(AccountProblem.OFFLINE, "Gone")
            val after = records.values.filter { it.second > since }.sortedBy { it.second }
            val page = after.take(limit)
            return PullResponse(page.map { it.first }, page.lastOrNull()?.second ?: since, more = after.size > limit)
        }

        fun get(kind: String, key: String) = records[kind to key]?.first
    }

    private class Device(account: Account) {
        val file: File = File.createTempFile("tayra-sync-device", ".db").also { it.delete() }
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val engine = SyncEngine(provider, account, MapSettings(), addLanguage = { code -> if (code == "pt") portuguese() }, userSettings = settings)

        fun <T> sql(block: Connection.() -> T): T =
            DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}", Properties().apply { put("foreign_keys", "true") }).use(block)

        fun long(query: String): Long = sql { createStatement().executeQuery(query).use { it.next(); it.getLong(1) } }

        fun rows(query: String): List<String> = sql {
            createStatement().executeQuery(query).use { r -> buildList { while (r.next()) add((1..r.metaData.columnCount).joinToString("|") { r.getString(it) ?: "" }) } }
        }

        fun run(statements: String) = sql { statements.split(";").filter { it.isNotBlank() }.forEach { createStatement().execute(it) } }

        suspend fun portuguese(): Long = languages.getAll().firstOrNull { it.name == "Portuguese" }?.id ?: languages.save(Language(name = "Portuguese"))

        suspend fun word(text: String, status: TermStatus = TermStatus.NEW_1, translation: String? = null): Long =
            terms.save(Term(languageId = portuguese(), text = text, textLc = text.lowercase(), status = status, translation = translation))

        fun id(text: String) = long("SELECT coalesce((SELECT id FROM terms WHERE text_lc = '$text'), 0)")
    }

    private val account = Account()
    private val phone = Device(account)
    private val desktop = Device(account)

    @Test
    fun aWordWithEverythingKeptForItReachesTheOtherDevice() = runBlocking<Unit> {
        val casa = phone.word("casa", TermStatus.NEW_2, translation = "house")
        val casas = phone.word("casas", TermStatus.NEW_1)
        val pt = phone.portuguese()
        phone.run(
            """
            INSERT INTO term_parents(term_id, parent_id) VALUES ($casas, $casa);
            INSERT INTO level_terms(term_id) VALUES ($casa);
            INSERT INTO vocabulary_levels(language_id, level) VALUES ($pt, 2);
            INSERT INTO flashcards(term_id, state, due, stability, difficulty, interval_days, reps, last_review, seen_status) VALUES ($casa, 2, 5000, 3.5, 4.25, 3, 2, 4000, 2);
            INSERT INTO flashcard_reviews(term_id, reviewed_at, rating, state_before, interval_days, elapsed_days) VALUES ($casa, 4000, 3, 1, 3, 1);
            INSERT INTO practice_progress(term_id, level, streak, asked, right_answers, last_practised) VALUES ($casa, 2, 1, 3, 2, 4500);
            INSERT INTO practice_examples(term_id, sentence, translation, form, form_start, source, fetched_at) VALUES ($casa, 'A casa é azul.', 'The house is blue.', 'casa', 2, 'tatoeba', 100);
            UPDATE languages SET show_romanization = 1 WHERE id = $pt
            """,
        )

        val sent = phone.engine.syncNow()
        assertTrue(sent.sent >= 8, "the words, the card, its review, the progress, the example, the level and the language: $sent")
        val taken = desktop.engine.syncNow()
        assertTrue(taken.changed >= 8, "$taken")

        val copy = desktop.id("casa")
        assertEquals(listOf("casa|2|house"), desktop.rows("SELECT text, status, translation FROM terms WHERE id = $copy"))
        assertEquals(listOf("casa"), desktop.rows("SELECT p.text_lc FROM term_parents tp JOIN terms p ON p.id = tp.parent_id WHERE tp.term_id = ${desktop.id("casas")}"))
        assertEquals(1, desktop.long("SELECT count(*) FROM level_terms WHERE term_id = $copy"))
        assertEquals(listOf("2"), desktop.rows("SELECT level FROM vocabulary_levels"))
        assertEquals(listOf("2|5000|3.5|4.25|3|2|4000|2"), desktop.rows("SELECT state, due, stability, difficulty, interval_days, reps, last_review, seen_status FROM flashcards WHERE term_id = $copy"))
        assertEquals(listOf("4000|3|1|3|1"), desktop.rows("SELECT reviewed_at, rating, state_before, interval_days, elapsed_days FROM flashcard_reviews WHERE term_id = $copy"))
        assertEquals(listOf("2|1|3|2|4500"), desktop.rows("SELECT level, streak, asked, right_answers, last_practised FROM practice_progress WHERE term_id = $copy"))
        assertEquals(listOf("A casa é azul.|The house is blue.|casa|2|tatoeba"), desktop.rows("SELECT sentence, translation, form, form_start, source FROM practice_examples WHERE term_id = $copy"))
        assertEquals(1, desktop.long("SELECT show_romanization FROM languages WHERE name = 'Portuguese'"))

        // What was taken is the account's, not the desktop's own change: it keeps the phone's times, leaves
        // no tombstones, and a second sync has nothing to do.
        assertEquals(phone.long("SELECT updated_at FROM terms WHERE id = $casa"), desktop.long("SELECT updated_at FROM terms WHERE id = $copy"))
        assertEquals(0, desktop.long("SELECT count(*) FROM sync_tombstones"))
        val seq = account.seq
        val again = desktop.engine.syncNow()
        assertEquals(0, again.changed)
        assertEquals(seq, account.seq, "sending back what came from the account changes nothing there")
    }

    @Test
    fun aStatusSetOnOneDeviceAndATranslationEditedOnTheOtherBothStand() = runBlocking<Unit> {
        phone.word("casa", TermStatus.NEW_1, translation = "house")
        phone.engine.syncNow()
        desktop.engine.syncNow()

        val now = System.currentTimeMillis()
        phone.run("UPDATE terms SET status = 3, status_changed_at = $now WHERE text_lc = 'casa'")
        Thread.sleep(5)
        desktop.run("UPDATE terms SET translation = 'home' WHERE text_lc = 'casa'")

        phone.engine.syncNow()
        desktop.engine.syncNow()
        phone.engine.syncNow()
        for (device in listOf(phone, desktop)) {
            assertEquals(listOf("3|home"), device.rows("SELECT status, translation FROM terms WHERE text_lc = 'casa'"))
        }
    }

    @Test
    fun aLanguageSetUpLaterKeepsTheSettingsChosenElsewhere() = runBlocking<Unit> {
        phone.run("UPDATE languages SET show_romanization = 1 WHERE id = ${phone.portuguese()}")
        phone.engine.syncNow()
        Thread.sleep(5)
        // The desktop is installed afterwards: the catalog's defaults are no one's change.
        desktop.portuguese()
        assertEquals(0, desktop.long("SELECT updated_at FROM languages WHERE name = 'Portuguese'"))
        desktop.engine.syncNow()
        assertEquals(1, desktop.long("SELECT show_romanization FROM languages WHERE name = 'Portuguese'"))
    }

    @Test
    fun theLanguagesChosenAndTheLearningSetUpFollowTheAccount() = runBlocking<Unit> {
        val pt = phone.portuguese()
        phone.settings.update { it.copy(myLanguages = "$pt", flashcardNewPerDay = 35, themeId = "dark") }
        phone.engine.syncNow()
        Thread.sleep(5)
        // The desktop, set up afterwards with the defaults, takes them; how it looks stays its own.
        desktop.engine.syncNow()
        val copy = desktop.portuguese()
        assertEquals("$copy", desktop.settings.current.myLanguages, "by this device's own id")
        assertEquals(35, desktop.settings.current.flashcardNewPerDay)
        assertEquals("default", desktop.settings.current.themeId)
        assertEquals(0, desktop.engine.syncNow().changed)

        Thread.sleep(5)
        desktop.settings.update { it.copy(flashcardNewPerDay = 10) }
        desktop.engine.syncNow()
        phone.engine.syncNow()
        assertEquals(10, phone.settings.current.flashcardNewPerDay)
        assertEquals("$pt", phone.settings.current.myLanguages)
    }

    @Test
    fun aWordDeletedOnOneDeviceGoesOnTheOtherWithItsCard() = runBlocking<Unit> {
        val lobo = phone.word("lobo", TermStatus.NEW_2)
        phone.word("casa")
        phone.run("INSERT INTO flashcards(term_id, due, seen_status) VALUES ($lobo, 1000, 2)")
        phone.engine.syncNow()
        desktop.engine.syncNow()
        assertEquals(1, desktop.long("SELECT count(*) FROM flashcards"))

        phone.terms.delete(lobo)
        phone.engine.syncNow()
        assertEquals(0, phone.long("SELECT count(*) FROM sync_tombstones"), "a deletion sent is forgotten")
        assertTrue(account.get("term", "pt/lobo")!!.deleted)

        desktop.engine.syncNow()
        assertEquals(listOf("casa"), desktop.rows("SELECT text_lc FROM terms ORDER BY text_lc"))
        assertEquals(0, desktop.long("SELECT count(*) FROM flashcards"))
        assertEquals(0, desktop.long("SELECT count(*) FROM sync_tombstones"), "applying a deletion is not one to send")
    }

    @Test
    fun aLanguageRemovedOnOneDeviceTakesItsOlderWordsEverywhere() = runBlocking<Unit> {
        phone.word("casa")
        phone.engine.syncNow()
        desktop.engine.syncNow()

        // The phone removes Portuguese, as the app does: the language goes and comes back empty.
        val pt = phone.portuguese()
        phone.languages.delete(pt)
        phone.languages.save(Language(name = "Portuguese"))
        Thread.sleep(5)
        // Meanwhile the desktop saves a word the removal never saw.
        desktop.word("novo")

        phone.engine.syncNow()
        desktop.engine.syncNow()
        phone.engine.syncNow()
        assertEquals(listOf("novo"), desktop.rows("SELECT text_lc FROM terms ORDER BY text_lc"))
        assertEquals(listOf("novo"), phone.rows("SELECT text_lc FROM terms ORDER BY text_lc"))
    }

    @Test
    fun aSyncCutOffWhileTakingChangesAppliesNoneOfThem() = runBlocking<Unit> {
        (1..5).forEach { phone.word("palavra$it") }
        phone.engine.syncNow()

        account.failPullAfter = 0
        assertFailsWith<AccountException> { desktop.engine.syncNow() }
        assertEquals(0, desktop.long("SELECT count(*) FROM terms"))
        assertEquals(0, desktop.long("SELECT last_seq FROM sync_state"))

        account.failPullAfter = Int.MAX_VALUE
        assertEquals(5, desktop.engine.syncNow().changed)
        assertEquals(account.seq, desktop.long("SELECT last_seq FROM sync_state"))
    }

    @Test
    fun anotherAccountGetsTheWholeLibrary() = runBlocking<Unit> {
        (1..3).forEach { phone.word("palavra$it") }
        Thread.sleep(2)
        assertEquals(3, phone.engine.syncNow().sent)
        assertEquals(0, phone.engine.syncNow().sent, "nothing changed since")

        account.account = "http://localhost someone@example.com"
        assertEquals(3, phone.engine.syncNow().sent)

        account.account = null
        assertFailsWith<AccountException> { phone.engine.syncNow() }
    }
}
