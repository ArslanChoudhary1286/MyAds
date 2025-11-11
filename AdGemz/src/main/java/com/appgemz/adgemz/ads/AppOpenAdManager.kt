package com.appgemz.adgemz.ads

import android.app.Activity
import android.app.Application
import android.util.Log
import com.appgemz.adgemz.enums.AdType
import com.appgemz.adgemz.helper.AdManager
import com.appgemz.adgemz.helper.FirebaseRemote
import com.appgemz.adgemz.utils.CoroutineTimer
import com.appgemz.adgemz.utils.NetworkConnectivity
import com.appgemz.adgemz.utils.AdsPreferences
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AppOpenAdManager {

    private const val TAG = "AppOpenAdManager"
    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var isAdLoading = false
    private var retryCount = 0
    private const val KEY_LAST_CLICK_TIME = "app_open_last_click_time"
    private const val KEY_CLICK_COUNT = "app_open_click_count"
    private var timer: CoroutineTimer? = null

    fun loadAd(application: Application) = CoroutineScope(Dispatchers.Main).launch {


        val config = FirebaseRemote.getConfig().adPlacements.appOpen

        if (!AdManager.isAdEnabled(config.adUnitId, AdType.APP_OPEN))  {
            Log.d(TAG, "App open ad disabled")
            return@launch
        }

        if (isAdLoading) {
            Log.e(TAG, "AppOpen In Process")
            return@launch
        }

        isAdLoading = true

        if (isAdAvailable()) {
            Log.e(TAG, "AppOpen Ad Already Loading")
            isAdLoading = false
            return@launch
        }

        val adUnitId = config.adUnitId

        if (adUnitId.isEmpty() && !NetworkConnectivity.Companion.hasInternetConnection()) {
            Log.d(TAG, "Interstitial Ad Failed due to Empty ID no internet connection")
            isAdLoading = false
            return@launch
        }

        if (!AdManager.canClickAd(KEY_LAST_CLICK_TIME, KEY_CLICK_COUNT)) {
            Log.d(TAG, "Interstitial Ad Failed due to click limit exceed")
            isAdLoading = false
            return@launch
        }


        // Check if we should request an ad (to avoid ad serving limit issues)
        if (!AdManager.shouldRequestAd(adUnitId)) {
            Log.d(TAG, "Ad serving limit reached. Waiting before retrying.")
            isAdLoading = false
            return@launch
        }

        if (retryCount < FirebaseRemote.getConfig().globalSettings.failedAttempt) {
            retryCount++
            val retryDelay =
                ((if (retryCount == 1) 0L else 4000L) * retryCount) // Increase delay on each attempt
            Log.d(TAG, "Retrying ad load in $retryDelay ms...")
            delay(retryDelay)
        } else {
            Log.d(TAG, "Ad retry limit exceed restart app to load app open ad")
            isAdLoading = false
            return@launch
        }

        Log.d(TAG, "start loading ............................")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            application,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "onAdLoaded")
                    appOpenAd = ad
                    retryCount = 0 // Reset retry count on success
                    loadTime = System.currentTimeMillis()
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isAdLoading = false
                    Log.e(TAG, "Ad failed to load: ${error.message}")
                    // Retry again
                    if (error.code == AdRequest.ERROR_CODE_NO_FILL) {
                        AdsPreferences.putLong(adUnitId, System.currentTimeMillis())
                    }

//                    reloadAd(application)
                }
            }
        )
    }

    private fun reloadAd(application: Application) {

        CoroutineScope(Dispatchers.Main).launch {
            loadAd(application)
        }

    }


    fun showAdIfAvailable(activity: Activity) {
        Log.d(TAG, "showAdIfAvailable")
        if (!isAdAvailable() || isShowingAd) {
//            loadAd(activity.application)
            return
        }
        Log.d(TAG, "showAdIfAvailable1")
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "onAdDismissedFullScreenContent")
                appOpenAd = null
                isShowingAd = false
//                loadAd(activity.application)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.d(TAG, "onAdFailedToShowFullScreenContent")
                appOpenAd = null
                isShowingAd = false
//                loadAd(activity.application)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "onAdShowedFullScreenContent")
                isShowingAd = true
            }

            override fun onAdClicked() {
                super.onAdClicked()
                Log.d(TAG, "app open Ad click.")
                // Register the click after showing the ad
                AdManager.registerAdClick(KEY_LAST_CLICK_TIME, KEY_CLICK_COUNT)
            }
        }

        appOpenAd?.show(activity)
    }

    fun isAdAvailable(): Boolean {
        return appOpenAd != null && (System.currentTimeMillis() - loadTime) < 4 * 60 * 60 * 1000
    }
}