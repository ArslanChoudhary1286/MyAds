package com.appgemz.myads.app

import android.app.Application
import com.appgemz.adgemz.utils.AdsPreferences

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()

        AdsPreferences.init(this)

    }
}