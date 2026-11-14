package com.tayra.languages.core.data.sync

import com.russhwolf.settings.Settings
import com.tayra.languages.core.domain.account.PackChoices
import com.tayra.languages.core.domain.books.BookPackService
import com.tayra.languages.core.domain.books.BookPacks
import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.courses.CoursePacks
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.sync.SyncKinds
import com.tayra.languages.sync.SyncRecord
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock

// Packs travel as their choices: a record per pack, kept while it is installed and deleted when it is
// removed on purpose, so the account's other devices install and remove it too.

/** A pack installed or removed here, and when. */
data class PackChoice(val pack: String, val installed: Boolean, val at: Long) {
    fun record(device: String) =
        SyncRecord(SyncKinds.PACK, pack, at, deleted = !installed, data = if (installed) JsonObject(emptyMap()) else null, deviceId = device)
}

/** The packs chosen on this device, kept with the settings. */
class SyncedPacks(private val store: Settings, private val clock: Clock = Clock.System) : PackChoices {
    override fun chose(pack: String, installed: Boolean) = take(PackChoice(pack, installed, clock.now().toEpochMilliseconds()))

    override fun found(pack: String) {
        if (!store.hasKey(atKey(pack))) chose(pack, installed = true)
    }

    /** The choices of the packs this version knows. */
    fun choices(): List<PackChoice> = KNOWN.mapNotNull { pack ->
        val at = store.getLongOrNull(atKey(pack)) ?: return@mapNotNull null
        PackChoice(pack, !store.getBoolean(removedKey(pack), false), at)
    }

    /** Keeps [choice] with its own time, as one taken from the account. */
    fun take(choice: PackChoice) {
        store.putBoolean(removedKey(choice.pack), !choice.installed)
        store.putLong(atKey(choice.pack), choice.at)
    }

    private fun atKey(pack: String) = "sync.pack.$pack"
    private fun removedKey(pack: String) = "sync.pack.$pack.removed"

    private companion object {
        val KNOWN = CoursePacks.all.map { PackChoices.coursePack(it.id) } + BookPacks.all.map { PackChoices.bookPack(it.id) }
    }
}

/** Installs and removes this device's packs as the account's choices ask. */
interface PackInstaller {
    /** The packs installed here, each noted as found. */
    suspend fun installed(): Set<String>

    /** Downloads and installs the pack; whether it is installed now. */
    suspend fun install(pack: String): Boolean

    suspend fun remove(pack: String)
}

/** The pack installer of the app: its course and book packs. */
class ServicePacks(private val courses: CoursePackService, private val books: BookPackService) : PackInstaller {
    override suspend fun installed(): Set<String> {
        courses.refresh()
        books.refresh()
        return courses.packs.value.filter { it.state is PackState.Installed }.map { PackChoices.coursePack(it.pack.id) }.toSet() +
            books.packs.value.filter { it.state is PackState.Installed }.map { PackChoices.bookPack(it.pack.id) }
    }

    override suspend fun install(pack: String): Boolean {
        CoursePacks.all.firstOrNull { PackChoices.coursePack(it.id) == pack }?.let { p ->
            courses.download(p)
            return courses.packs.value.any { it.pack.id == p.id && it.state is PackState.Installed }
        }
        BookPacks.all.firstOrNull { PackChoices.bookPack(it.id) == pack }?.let { p ->
            books.download(p)
            return books.packs.value.any { it.pack.id == p.id && it.state is PackState.Installed }
        }
        return false
    }

    override suspend fun remove(pack: String) {
        CoursePacks.all.firstOrNull { PackChoices.coursePack(it.id) == pack }?.let { courses.remove(it) }
        BookPacks.all.firstOrNull { PackChoices.bookPack(it.id) == pack }?.let { books.remove(it) }
    }
}

/**
 * The pack a lesson's or pack book's text comes from, for records sent by versions that do not send
 * their packs: a pack lesson's id starts with its language's code ("pt-mini-0100-01"), and a pack
 * book's address names its pack ("tayra-books://books-pt/…").
 */
internal fun packOfText(key: String, language: String?): String? = when {
    key.startsWith("lesson:") -> {
        val code = key.removePrefix("lesson:").substringBefore('-')
        if (language != null && language != code) null
        else CoursePacks.forLanguage(code).firstOrNull()?.let { PackChoices.coursePack(it.id) }
    }
    key.startsWith("uri:${BookPacks.SOURCE_PREFIX}") -> {
        val id = key.removePrefix("uri:${BookPacks.SOURCE_PREFIX}").substringBefore('/')
        BookPacks.all.firstOrNull { it.id == id }?.let { PackChoices.bookPack(it.id) }
    }
    else -> null
}
