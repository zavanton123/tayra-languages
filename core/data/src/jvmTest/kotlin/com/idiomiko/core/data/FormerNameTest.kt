package com.idiomiko.core.data

import com.idiomiko.core.data.account.AccountClient
import com.idiomiko.core.data.account.AccountServer
import com.idiomiko.core.data.account.withCurrentAddress
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.settings.CarriedOverStore
import com.idiomiko.core.data.settings.InMemorySecureStore
import com.idiomiko.core.data.settings.desktopPreferences
import com.idiomiko.sync.DeviceInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import java.io.File
import java.nio.file.Files
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the app kept as Tayra Languages carries over to Idiomiko. */
class FormerNameTest {
    @Test
    fun theFormerLibraryFolderMovesWithItsDatabase() {
        val parent = Files.createTempDirectory("former-name").toFile()
        val former = File(parent, "TayraLanguages").apply { mkdirs() }
        File(former, "tayra.db").writeText("db")
        File(former, "tayra.db-wal").writeText("wal")
        File(former, "speech-cache").mkdirs()
        val folder = File(parent, "Idiomiko")

        DatabaseDriverFactory.moveFormerLibrary(former, folder)

        assertFalse(former.exists())
        assertEquals("db", File(folder, "idiomiko.db").readText())
        assertEquals("wal", File(folder, "idiomiko.db-wal").readText())
        assertTrue(File(folder, "speech-cache").isDirectory)
    }

    @Test
    fun anExistingLibraryIsNeverReplaced() {
        val parent = Files.createTempDirectory("former-name").toFile()
        val former = File(parent, "TayraLanguages").apply { mkdirs() }
        val folder = File(parent, "Idiomiko").apply { mkdirs() }
        File(folder, "idiomiko.db").writeText("new")

        DatabaseDriverFactory.moveFormerLibrary(former, folder)

        assertTrue(former.isDirectory)
        assertEquals("new", File(folder, "idiomiko.db").readText())
    }

    @Test
    fun preferencesKeptUnderTheFormerNameAreCarriedOver() {
        val root = Preferences.userRoot().node("idiomiko-test-${System.nanoTime()}")
        try {
            root.node("com/tayra/languages").put("theme", "dark")
            assertEquals("dark", desktopPreferences(root).get("theme", null))

            root.node("com/tayra/languages").put("theme", "light")
            assertEquals("dark", desktopPreferences(root).get("theme", null), "carried over once, then the app's own win")
        } finally {
            root.removeNode()
        }
    }

    @Test
    fun aSecretKeptUnderTheFormerNameMovesOnFirstUse() {
        val store = InMemorySecureStore()
        val former = InMemorySecureStore().apply { put("token", "abc") }
        val carried = CarriedOverStore(store, former)

        assertEquals("abc", carried.get("token"))
        assertEquals("abc", store.get("token"))
        assertNull(former.get("token"))
    }

    @Test
    fun aSignInAndSyncMadeAtTheFormerAddressCarryOver() {
        val secure = InMemorySecureStore()
        secure.put("account.https://95.111.244.23.sslip.io.refresh_token", "r1")
        secure.put("account.https://95.111.244.23.sslip.io.email", "reader@example.com")
        secure.put("account.https://95.111.244.23.sslip.io.user", "u1")

        val client = AccountClient(HttpClient(MockEngine { error("no request") }), AccountServer { "https://idiomiko.com" }, secure, device = { DeviceInfo("d", "Desk", "desktop") })

        assertEquals("reader@example.com", client.signedIn.value)
        assertEquals("https://idiomiko.com u1", client.syncAccount())
        assertEquals("https://idiomiko.com u1", withCurrentAddress("https://95.111.244.23.sslip.io u1"), "so the library is not sent again as another account's")
        assertNull(secure.get("account.https://95.111.244.23.sslip.io.refresh_token"))
    }
}
