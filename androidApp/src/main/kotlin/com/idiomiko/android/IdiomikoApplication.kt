package com.idiomiko.android

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import com.idiomiko.core.domain.account.IdentitySignIn
import com.idiomiko.core.domain.account.StorePurchases
import com.idiomiko.core.ui.androidDebuggable
import com.idiomiko.di.initKoin
import org.koin.dsl.module

class IdiomikoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        androidDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        initKoin(
            listOf(
                module {
                    single<Context> { this@IdiomikoApplication }
                    single<StorePurchases> { PlayStorePurchases(this@IdiomikoApplication, CurrentActivity::get) }
                    single<IdentitySignIn> { AndroidIdentitySignIn(CurrentActivity::get) }
                },
            ),
        )
    }
}
