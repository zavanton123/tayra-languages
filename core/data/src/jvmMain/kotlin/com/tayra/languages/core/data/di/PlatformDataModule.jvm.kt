package com.tayra.languages.core.data.di

import com.russhwolf.settings.PreferencesSettings
import com.tayra.languages.core.data.settings.fileSettings
import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.runtime.DesktopCommandLineTool
import com.tayra.languages.core.data.runtime.ManagedPython
import com.tayra.languages.core.domain.service.CommandLineTool
import com.tayra.languages.core.data.settings.SecureStore
import com.tayra.languages.core.data.speech.KokoroSpeechEngine
import com.tayra.languages.core.data.speech.PiperSpeechEngine
import com.tayra.languages.core.data.speech.TtsWorker
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.data.settings.desktopSecureStore
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.coursepack.CoursePackFiles
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.translation.ArgosSentenceTranslator
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.data.backup.BackupFiles
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences
import java.io.File
import com.tayra.languages.core.domain.service.SpeechAudioCache
import com.tayra.languages.core.data.speech.FileSpeechAudioCache

actual val platformDataModule: Module = module {
    single { ManagedPython() }
    single { LocalTranslation(ArgosSentenceTranslator(get(), get())) }
    single { TtsWorker(get()) }
    single { LocalSpeech(listOf(PiperSpeechEngine(get(), get()), KokoroSpeechEngine(get(), get()))) }
    single { DatabaseDriverFactory() }
    single { BackupFiles() }
    single<SpeechAudioCache> { FileSpeechAudioCache(File(DatabaseDriverFactory.dataDirectory(), "speech-cache")) }
    single { DictionaryPackStorage(get()) }
    single { CoursePackFiles(get()) }
    // A data folder of its own keeps its settings beside the database, so it shares nothing with the usual library.
    single<Settings> { DatabaseDriverFactory.customDataDirectory()?.let { fileSettings(File(it, "settings.properties")) } ?: PreferencesSettings(Preferences.userRoot().node("com/tayra/languages")) }
    single<SecureStore> { desktopSecureStore() }
    single<CommandLineTool> { DesktopCommandLineTool() }
}
