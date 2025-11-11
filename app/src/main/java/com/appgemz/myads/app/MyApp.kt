package com.appgemz.myads.app

import android.app.Application
import com.appgemz.adgemz.utils.PreferencesManager

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()

        PreferencesManager.init(this)

    }
}