package com.tayra.languages.core.ui.audio

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberMicrophoneAccess(): (onResult: (Boolean) -> Unit) -> Unit {
    val context = LocalContext.current
    val waiting = remember { arrayOfNulls<(Boolean) -> Unit>(1) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        waiting[0]?.invoke(granted)
        waiting[0] = null
    }
    return remember(context, ask) {
        { onResult ->
            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                onResult(true)
            } else {
                waiting[0] = onResult
                ask.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}
