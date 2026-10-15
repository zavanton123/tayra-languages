package com.tayra.languages.core.data.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.tayra.languages.core.data.backup.BackupFiles
import com.tayra.languages.core.data.bookpack.BookPackFiles
import com.tayra.languages.core.data.coursepack.CoursePackFiles
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.ocr.AndroidTextRecognizer
import com.tayra.languages.core.data.ocr.TesseractTextRecognizer
import com.tayra.languages.core.data.settings.AndroidSecureStore
import com.tayra.languages.core.data.settings.SecureStore
import com.tayra.languages.core.data.speech.AndroidSherpaRuntime
import com.tayra.languages.core.data.speech.FileSpeechAudioCache
import com.tayra.languages.core.data.speech.SherpaKokoroEngine
import com.tayra.languages.core.data.speech.SherpaPiperEngine
import com.tayra.languages.core.data.translation.MlKitSentenceTranslator
import com.tayra.languages.core.domain.ocr.TextRecognizer
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalTranslation
import com.tayra.languages.core.domain.service.SpeechAudioCache
import java.io.File
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single { LocalTranslation(MlKitSentenceTranslator(get())) }
    single { AndroidSherpaRuntime(get<Context>()) }
    single { LocalSpeech(listOf(SherpaPiperEngine(get<AndroidSherpaRuntime>()), SherpaKokoroEngine(get<AndroidSherpaRuntime>()))) }
    single { DatabaseDriverFactory(get<Context>()) }
    single { BackupFiles(get<Context>()) }
    single<SpeechAudioCache> { FileSpeechAudioCache(File(get<Context>().cacheDir, "speech-cache")) }
    single { DictionaryPackStorage(get<Context>(), get()) }
    single { CoursePackFiles(get<Context>(), get()) }
    single { BookPackFiles(get<Context>(), get()) }
    single<Settings> { SharedPreferencesSettings(get<Context>().getSharedPreferences("tayra_settings", Context.MODE_PRIVATE)) }
    single<SecureStore> { AndroidSecureStore(get<Context>()) }
    single<TextRecognizer> { AndroidTextRecognizer(get<Context>(), TesseractTextRecognizer(File(get<Context>().filesDir, "ocr/tessdata"))) }
}
