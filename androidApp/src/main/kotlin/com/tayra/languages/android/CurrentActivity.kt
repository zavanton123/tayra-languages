package com.tayra.languages.android

import android.app.Activity
import java.lang.ref.WeakReference

/** The app's activity while it is open: Play's purchase sheet and Google's sign-in open over it. */
object CurrentActivity {
    private var activity: WeakReference<Activity>? = null

    fun set(activity: Activity) {
        this.activity = WeakReference(activity)
    }

    fun get(): Activity? = activity?.get()
}
