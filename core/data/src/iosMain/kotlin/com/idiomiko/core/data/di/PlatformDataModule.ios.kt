package com.idiomiko.core.data.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.idiomiko.core.data.backup.BackupFiles
import com.idiomiko.core.data.bookpack.BookPackFiles
import com.idiomiko.core.data.coursepack.CoursePackFiles
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.dictionary.DictionaryPackStorage
import com.idiomiko.core.data.ocr.VisionTextRecognizer
import com.idiomiko.core.data.settings.KeychainSecureStore
import com.idiomiko.core.data.settings.SecureStore
import com.idiomiko.core.data.speech.FileSpeechAudioCache
import com.idiomiko.core.data.speech.IosSpeechRecognition
import com.idiomiko.core.domain.practice.SpeechRecognition
import com.idiomiko.core.domain.ocr.TextRecognizer
import com.idiomiko.core.domain.service.SpeechAudioCache
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformDataModule: Module = module {
    single { DatabaseDriverFactory() }
    single { BackupFiles() }
    single<SpeechAudioCache> { FileSpeechAudioCache() }
    single<SpeechRecognition> { IosSpeechRecognition() }
    single { DictionaryPackStorage(get()) }
    single { CoursePackFiles(get()) }
    single { BookPackFiles(get()) }
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single<SecureStore> { KeychainSecureStore() }
    single<TextRecognizer> { VisionTextRecognizer() }
}
