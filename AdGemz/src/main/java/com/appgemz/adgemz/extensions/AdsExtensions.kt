package com.appgemz.adgemz.extensions

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.appgemz.adgemz.BuildConfig



fun AppCompatActivity.isAppInForeground(): Boolean {
    return lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
}

fun isDebugMode(): Boolean = BuildConfig.DEBUG

fun Fragment.name() : String {
    return "splash"
}

fun showLog(tag: String, message: String) {
    if (isDebugMode()) Log.d(tag, message)
}

fun errorLog(tag: String, message: String) {
    if (isDebugMode()) Log.e(tag, message)
}