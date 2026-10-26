package com.tayra.languages.core.data.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.backup.BackupFiles
import com.tayra.languages.core.data.bookpack.BookPackFiles
import com.tayra.languages.core.data.coursepack.CoursePackFiles
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.export.AnkiConnectClient
import com.tayra.languages.core.data.ocr.TesseractTextRecognizer
import com.tayra.languages.core.data.parse.KuromojiBoundaries
import com.tayra.languages.core.data.runtime.DesktopAppUpdater
import com.tayra.languages.core.data.runtime.DesktopCommandLineTool
import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.data.account.DesktopIdentitySignIn
import com.tayra.languages.core.data.account.GoogleOAuth
import com.tayra.languages.core.data.network.createAccountHttpClient
import com.tayra.languages.core.data.settings.SecureStore
import com.tayra.languages.core.domain.account.IdentitySignIn
import com.tayra.languages.core.data.settings.desktopSecureStore
import com.tayra.languages.core.data.settings.fileSettings
import com.tayra.languages.core.data.speech.FileSpeechAudioCache
import com.tayra.languages.core.data.speech.KokoroSpeechEngine
import com.tayra.languages.core.data.speech.PiperSpeechEngine
import com.tayra.languages.core.data.speech.TtsWorker
import com.tayra.languages.core.data.speech.DesktopSpeechRecognition
import com.tayra.languages.core.domain.practice.SpeechRecognition
import com.tayra.languages.core.domain.practice.SpeechRecognitionSetup
import org.koin.dsl.binds
import com.tayra.languages.core.data.translation.ArgosSentenceTranslator
import com.tayra.languages.core.domain.export.AnkiConnection
import com.tayra.languages.core.domain.ocr.TextRecognizer
import com.tayra.languages.core.domain.parse.Segmenters
import com.tayra.languages.core.domain.service.AppUpdateService
import com.tayra.languages.core.domain.service.AppUpdater
import com.tayra.languages.core.domain.service.CommandLineTool
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.SpeechAudioCache
import java.io.File
import java.util.prefs.Preferences
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single { ManagedPython() }
    single { LocalTranslation(ArgosSentenceTranslator(get(), get())) }
    single { TtsWorker(get()) }
    single { DesktopSpeechRecognition(get(), get(), get()) } binds arrayOf(SpeechRecognition::class, SpeechRecognitionSetup::class)
    single { LocalSpeech(listOf(PiperSpeechEngine(get(), get()), KokoroSpeechEngine(get(), get()))) }
    single { DatabaseDriverFactory() }
    single<IdentitySignIn> { DesktopIdentitySignIn(GoogleOAuth(createAccountHttpClient())) }
    single { BackupFiles() }
    single<SpeechAudioCache> { FileSpeechAudioCache(File(DatabaseDriverFactory.dataDirectory(), "speech-cache")) }
    single { DictionaryPackStorage(get()) }
    single { CoursePackFiles(get()) }
    single { BookPackFiles(get()) }
    // A data folder of its own keeps its settings beside the database, so it shares nothing with the usual library.
    single<Settings> { DatabaseDriverFactory.customDataDirectory()?.let { fileSettings(File(it, "settings.properties")) } ?: PreferencesSettings(Preferences.userRoot().node("com/tayra/languages")) }
    single<SecureStore> { desktopSecureStore() }
    single<TextRecognizer> { TesseractTextRecognizer(File(DatabaseDriverFactory.dataDirectory(), "ocr/tessdata")) }
    single<CommandLineTool> { DesktopCommandLineTool() }
    single<AppUpdater> { DesktopAppUpdater(get()) }
    single { AppUpdateService(get(), get()) }
    // Anki's AnkiConnect add-on listens on the desktop only.
    single<AnkiConnection> { AnkiConnectClient(get()) }
    // Japanese is cut with MeCab's analysis (Kuromoji) on desktop as well as with the dictionaries.
    single(createdAtStart = true) { KuromojiBoundaries().also { Segmenters.set("ja", it) } }
}
