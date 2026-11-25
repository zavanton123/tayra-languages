package com.idiomiko.core.data.di

import com.russhwolf.settings.Settings
import com.idiomiko.core.data.backup.BackupFiles
import com.idiomiko.core.data.bookpack.BookPackFiles
import com.idiomiko.core.data.coursepack.CoursePackFiles
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.dictionary.DictionaryPackStorage
import com.idiomiko.core.data.export.AnkiConnectClient
import com.idiomiko.core.data.ocr.TesseractTextRecognizer
import com.idiomiko.core.data.parse.KuromojiBoundaries
import com.idiomiko.core.data.runtime.DesktopAppUpdater
import com.idiomiko.core.data.runtime.DesktopCommandLineTool
import com.idiomiko.core.data.bridge.DesktopBrowserBridge
import com.idiomiko.core.data.bridge.NativeHostInstaller
import com.idiomiko.core.domain.bridge.BridgeAccess
import com.idiomiko.core.domain.bridge.BrowserBridgeApi
import com.idiomiko.core.domain.bridge.BridgeVideos
import com.idiomiko.core.domain.bridge.StatusChanges
import com.idiomiko.core.data.runtime.ManagedPython
import com.idiomiko.core.data.account.DesktopIdentitySignIn
import com.idiomiko.core.data.account.GoogleOAuth
import com.idiomiko.core.data.network.createAccountHttpClient
import com.idiomiko.core.data.settings.SecureStore
import com.idiomiko.core.domain.account.IdentitySignIn
import com.idiomiko.core.data.settings.desktopSecureStore
import com.idiomiko.core.data.settings.desktopPreferences
import com.idiomiko.core.data.settings.desktopSettings
import com.idiomiko.core.data.speech.FileSpeechAudioCache
import com.idiomiko.core.data.speech.KokoroSpeechEngine
import com.idiomiko.core.data.speech.PiperSpeechEngine
import com.idiomiko.core.data.speech.TtsWorker
import com.idiomiko.core.data.speech.DesktopSpeechRecognition
import com.idiomiko.core.data.speech.DesktopVideoTranscriber
import com.idiomiko.core.domain.media.VideoTranscriber
import com.idiomiko.core.domain.practice.SpeechRecognition
import com.idiomiko.core.domain.practice.SpeechRecognitionSetup
import org.koin.dsl.binds
import com.idiomiko.core.data.translation.ArgosSentenceTranslator
import com.idiomiko.core.domain.export.AnkiConnection
import com.idiomiko.core.domain.ocr.TextRecognizer
import com.idiomiko.core.domain.parse.Segmenters
import com.idiomiko.core.domain.service.AppUpdateService
import com.idiomiko.core.domain.service.AppUpdater
import com.idiomiko.core.domain.service.CommandLineTool
import com.idiomiko.core.domain.service.DictionaryService
import com.idiomiko.core.domain.service.LocalSpeech
import com.idiomiko.core.domain.service.LocalTranslation
import com.idiomiko.core.domain.service.SpeechAudioCache
import java.io.File
import java.util.prefs.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single { ManagedPython() }
    single { LocalTranslation(ArgosSentenceTranslator(get(), get())) }
    single { TtsWorker(get()) }
    single { DesktopSpeechRecognition(get(), get(), get()) } binds arrayOf(SpeechRecognition::class, SpeechRecognitionSetup::class)
    single<VideoTranscriber> { DesktopVideoTranscriber(get(), get()) }
    single { LocalSpeech(listOf(PiperSpeechEngine(get(), get()), KokoroSpeechEngine(get(), get()))) }
    single { DatabaseDriverFactory() }
    single<IdentitySignIn> { DesktopIdentitySignIn(GoogleOAuth(createAccountHttpClient())) }
    single { BackupFiles() }
    single<SpeechAudioCache> { FileSpeechAudioCache(File(DatabaseDriverFactory.dataDirectory(), "speech-cache")) }
    single { DictionaryPackStorage(get()) }
    single { CoursePackFiles(get()) }
    single { BookPackFiles(get()) }
    // The settings are a file beside the database, which no other program writes; Java's preferences are
    // shared with every Java app on the computer, whose saves could drop the app's. The usual library
    // starts its file from what it kept there; a data folder of its own shares nothing with it.
    single<Settings> {
        val ownFolder = DatabaseDriverFactory.customDataDirectory() != null
        desktopSettings(DatabaseDriverFactory.dataDirectory(), former = { if (ownFolder) null else desktopPreferences() })
    }
    single<SecureStore> { desktopSecureStore() }
    single<TextRecognizer> { TesseractTextRecognizer(File(DatabaseDriverFactory.dataDirectory(), "ocr/tessdata")) }
    single<CommandLineTool> { DesktopCommandLineTool() }
    single { StatusChanges(get(), get()) }
    single { NativeHostInstaller(File(DatabaseDriverFactory.dataDirectory(), "browser-extension")) }
    single { BridgeAccess(get<NativeHostInstaller>()::token) }
    single { BrowserBridgeApi(get(), get(), get(), get(), get<DictionaryService>(), get(), System.getProperty("idiomiko.version"), getOrNull(), getOrNull(), getOrNull()) }
    single { BridgeVideos(get(), get(), get(), get(), get(), get()) }
    single { DesktopBrowserBridge(get(), get(), get(), nativeHost = get(), videos = get()) }
    single<AppUpdater> { DesktopAppUpdater(get()) }
    single { AppUpdateService(get(), get()) }
    // Anki's AnkiConnect add-on listens on the desktop only.
    single<AnkiConnection> { AnkiConnectClient(get()) }
    // Japanese is cut with MeCab's analysis (Kuromoji) on desktop as well as with the dictionaries.
    single(createdAtStart = true) { KuromojiBoundaries().also { Segmenters.set("ja", it) } }
}
