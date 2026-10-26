package com.tayra.languages.android

import android.app.Application
import android.content.Context
import com.tayra.languages.core.domain.account.IdentitySignIn
import com.tayra.languages.core.domain.account.StorePurchases
import com.tayra.languages.di.initKoin
import org.koin.dsl.module

class TayraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
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
