package com.tayra.languages.core.data.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.tayra.languages.core.data.settings.KeychainSecureStore
import com.tayra.languages.core.data.settings.SecureStore
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.coursepack.CoursePackFiles
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.backup.BackupFiles
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults
import com.tayra.languages.core.domain.service.SpeechAudioCache
import com.tayra.languages.core.data.speech.FileSpeechAudioCache

actual val platformDataModule: Module = module {
    single { DatabaseDriverFactory() }
    single { BackupFiles() }
    single<SpeechAudioCache> { FileSpeechAudioCache() }
    single { DictionaryPackStorage(get()) }
    single { CoursePackFiles(get()) }
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single<SecureStore> { KeychainSecureStore() }
}
