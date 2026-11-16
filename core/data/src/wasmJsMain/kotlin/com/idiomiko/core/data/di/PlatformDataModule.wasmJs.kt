package com.idiomiko.core.data.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import com.idiomiko.core.data.backup.BackupFiles
import com.idiomiko.core.data.bookpack.BookPackFiles
import com.idiomiko.core.data.coursepack.CoursePackFiles
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.dictionary.DictionaryPackStorage
import com.idiomiko.core.data.account.AccountPlatform
import com.idiomiko.core.data.account.WebAccountStore
import com.idiomiko.core.data.account.WebIdentitySignIn
import com.idiomiko.core.data.account.webAccountHttpClient
import com.idiomiko.core.data.settings.BrowserSecureStore
import com.idiomiko.core.data.settings.SecureStore
import com.idiomiko.core.domain.account.IdentitySignIn
import com.idiomiko.core.data.speech.WebKokoroEngine
import com.idiomiko.core.data.speech.WebPiperEngine
import com.idiomiko.core.domain.ocr.NoTextRecognizer
import com.idiomiko.core.domain.ocr.TextRecognizer
import com.idiomiko.core.domain.service.LocalSpeech
import com.idiomiko.core.domain.service.LocalTranslation
import com.idiomiko.core.domain.service.MemorySpeechAudioCache
import com.idiomiko.core.domain.service.SpeechAudioCache
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single { LocalTranslation(null) }
    single { DatabaseDriverFactory() }
    single { BackupFiles() }
    single<SpeechAudioCache> { MemorySpeechAudioCache() }
    single { DictionaryPackStorage() }
    single { CoursePackFiles() }
    single { BookPackFiles() }
    single<Settings> { StorageSettings() }
    single<SecureStore> { BrowserSecureStore() }
    single { AccountPlatform(webAccountHttpClient(), WebAccountStore(), sharedSignIn = true) }
    single<IdentitySignIn> { WebIdentitySignIn() }
    single<TextRecognizer> { NoTextRecognizer }
    single { LocalSpeech(listOf(WebPiperEngine(), WebKokoroEngine())) }
}
