package com.tayra.languages.android

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import com.tayra.languages.core.domain.account.IdentitySignIn
import com.tayra.languages.core.domain.account.StorePurchases
import com.tayra.languages.core.ui.androidDebuggable
import com.tayra.languages.di.initKoin
import org.koin.dsl.module

class TayraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        androidDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        initKoin(
            listOf(
                module {
                    single<Context> { this@TayraApplication }
                    single<StorePurchases> { PlayStorePurchases(this@TayraApplication, CurrentActivity::get) }
                    single<IdentitySignIn> { AndroidIdentitySignIn(CurrentActivity::get) }
                },
            ),
        )
    }
}
