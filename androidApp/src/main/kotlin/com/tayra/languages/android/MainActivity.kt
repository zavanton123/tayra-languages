package com.tayra.languages.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tayra.languages.App
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import org.koin.mp.KoinPlatform
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        // Play's purchase sheet opens over this activity.
        KoinPlatform.getKoin().get<PlayStorePurchases>().activity = WeakReference(this)
        setContent { App() }
    }
}
