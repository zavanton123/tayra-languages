package com.idiomiko.core.data.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.idiomiko.core.data.backup.BackupFiles
import com.idiomiko.core.data.bookpack.BookPackFiles
import com.idiomiko.core.data.coursepack.CoursePackFiles
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.dictionary.DictionaryPackStorage
import com.idiomiko.core.data.ocr.AndroidTextRecognizer
import com.idiomiko.core.data.ocr.TesseractTextRecognizer
import com.idiomiko.core.data.settings.AndroidSecureStore
import com.idiomiko.core.data.settings.SecureStore
import com.idiomiko.core.data.speech.AndroidSherpaRuntime
import com.idiomiko.core.data.speech.AndroidSherpaTranscription
import com.idiomiko.core.data.speech.SherpaVideoTranscriber
import com.idiomiko.core.domain.media.VideoTranscriber
import com.idiomiko.core.data.speech.AndroidSpeechRecognition
import com.idiomiko.core.domain.practice.SpeechRecognition
import com.idiomiko.core.data.speech.FileSpeechAudioCache
import com.idiomiko.core.data.speech.SherpaKokoroEngine
import com.idiomiko.core.data.speech.SherpaPiperEngine
import com.idiomiko.core.data.translation.MlKitSentenceTranslator
import com.idiomiko.core.domain.ocr.TextRecognizer
import com.idiomiko.core.domain.service.LocalSpeech
import com.idiomiko.core.domain.service.LocalTranslation
import com.idiomiko.core.domain.service.SpeechAudioCache
import java.io.File
import org.koin.core.module.Module
import com.idiomiko.core.data.network.AndroidNetworkStatus
import com.idiomiko.core.domain.service.NetworkStatus
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<NetworkStatus> { AndroidNetworkStatus(get<Context>()) }
    single { LocalTranslation(MlKitSentenceTranslator(get())) }
    single { AndroidSherpaRuntime(get<Context>()) }
    single<VideoTranscriber> { SherpaVideoTranscriber(get<AndroidSherpaRuntime>(), AndroidSherpaTranscription(get<Context>())) }
    single { LocalSpeech(listOf(SherpaPiperEngine(get<AndroidSherpaRuntime>()), SherpaKokoroEngine(get<AndroidSherpaRuntime>()))) }
    single<SpeechRecognition> { AndroidSpeechRecognition(get<Context>()) }
    single { DatabaseDriverFactory(get<Context>()) }
    single { BackupFiles(get<Context>()) }
    single<SpeechAudioCache> { FileSpeechAudioCache(File(get<Context>().cacheDir, "speech-cache")) }
    single { DictionaryPackStorage(get<Context>(), get()) }
    single { CoursePackFiles(get<Context>(), get()) }
    single { BookPackFiles(get<Context>(), get()) }
    single<Settings> { SharedPreferencesSettings(get<Context>().getSharedPreferences("idiomiko_settings", Context.MODE_PRIVATE)) }
    single<SecureStore> { AndroidSecureStore(get<Context>()) }
    single<TextRecognizer> { AndroidTextRecognizer(get<Context>(), TesseractTextRecognizer(File(get<Context>().filesDir, "ocr/tessdata"))) }
}
