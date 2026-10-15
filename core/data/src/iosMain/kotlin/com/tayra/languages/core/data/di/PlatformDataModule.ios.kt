package com.tayra.languages.core.data.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.backup.BackupFiles
import com.tayra.languages.core.data.bookpack.BookPackFiles
import com.tayra.languages.core.data.coursepack.CoursePackFiles
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.ocr.VisionTextRecognizer
import com.tayra.languages.core.data.settings.KeychainSecureStore
import com.tayra.languages.core.data.settings.SecureStore
import com.tayra.languages.core.data.speech.FileSpeechAudioCache
import com.tayra.languages.core.domain.ocr.TextRecognizer
import com.tayra.languages.core.domain.service.SpeechAudioCache
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformDataModule: Module = module {
    single { DatabaseDriverFactory() }
    single { BackupFiles() }
    single<SpeechAudioCache> { FileSpeechAudioCache() }
    single { DictionaryPackStorage(get()) }
    single { CoursePackFiles(get()) }
    single { BookPackFiles(get()) }
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single<SecureStore> { KeychainSecureStore() }
    single<TextRecognizer> { VisionTextRecognizer() }
}
