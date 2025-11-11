package com.appgemz.adgemz.ads

import android.app.Activity
import android.content.Context
import com.appgemz.adgemz.enums.AdPlace
import com.appgemz.adgemz.extensions.showLog
import com.appgemz.adgemz.helper.AdManager
import com.appgemz.adgemz.helper.FirebaseRemote
import com.appgemz.adgemz.utils.Constants.KEY_CLICK_COUNT
import com.appgemz.adgemz.utils.Constants.KEY_LAST_CLICK_TIME
import com.appgemz.adgemz.utils.NetworkConnectivity
import com.appgemz.adgemz.utils.PreferencesManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.*

object InterAdManager {

    private const val TAG = "InterstitialAdManager"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var retryCount = 0

    private val config get() = FirebaseRemote.getConfig().globalSettings

    /** ✅ Check if ad is loaded */
    fun isLoaded(): Boolean = interstitialAd != null
    fun isLoading(): Boolean = isLoading

    /** ✅ Load Interstitial Ad */
    fun load(context: Context, place: AdPlace) = CoroutineScope(Dispatchers.Main).launch {
        if (isLoading || interstitialAd != null) {
            showLog(TAG, "Ad already loading or loaded.")
            return@launch
        }

        val adModel = AdManager.getInterAd(place)
        val hasInternet = NetworkConnectivity.hasInternetConnection()

        when {
            adModel == null -> {
                showLog(TAG, "Config not found for $place.")
                return@launch
            }

            !adModel.enabled -> {
                showLog(TAG, "Ad disabled for $place.")
                return@launch
            }

            adModel.adUnitId.isBlank() -> {
                showLog(TAG, "Ad unit ID missing for $place.")
                return@launch
            }

            !hasInternet -> {
                showLog(TAG, "No internet connection.")
                return@launch
            }

            !AdManager.canClickAd(KEY_LAST_CLICK_TIME, KEY_CLICK_COUNT) -> {
                showLog(TAG, "Click limit reached.")
                return@launch
            }

            !AdManager.shouldRequestAd(adModel.adUnitId) -> {
                showLog(TAG, "Ad serving limit reached for ${adModel.adUnitId}")
                return@launch
            }

            retryCount >= config.failedAttempt -> {
                showLog(TAG, "Retry limit reached.")
                return@launch
            }
        }

        isLoading = true
        val delayBeforeLoad = if (retryCount == 0) 0L else config.failCappingMs.toLong()
        retryCount++
        showLog(TAG, "Loading ad after delay $delayBeforeLoad ms")
        delay(delayBeforeLoad)

        InterstitialAd.load(context, adModel.adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    retryCount = 0
                    PreferencesManager.putLong(adModel.adUnitId, 0L)
                    showLog(TAG, "Interstitial loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    showLog(TAG, "Load failed: ${error.message} (${error.code})")
                    interstitialAd = null
                    isLoading = false

                    if (error.code == AdRequest.ERROR_CODE_NO_FILL) {
                        PreferencesManager.putLong(adModel.adUnitId, System.currentTimeMillis())
                        showLog(TAG, "No fill – caching timestamp.")
                    }

                    if (adModel.reloadOnFailed) {
                        reload(context, place)
                    }
                }
            }
        )
    }

    /** ✅ Reload with coroutine */
    private fun reload(context: Context, place: AdPlace) {
        CoroutineScope(Dispatchers.Main).launch {
            load(context, place)
        }
    }

    /** ✅ Show Ad if available */
    fun show(activity: Activity, onClosed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            showLog(TAG, "No ad loaded.")
            onClosed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                showLog(TAG, "Ad dismissed.")
                interstitialAd = null
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showLog(TAG, "Show failed: ${error.message}")
                interstitialAd = null
                onClosed()
            }

            override fun onAdClicked() {
                AdManager.registerAdClick(KEY_LAST_CLICK_TIME, KEY_CLICK_COUNT)
                showLog(TAG, "Ad clicked.")
            }

            override fun onAdShowedFullScreenContent() {
                showLog(TAG, "Ad showed successfully.")
            }
        }

        ad.show(activity)
    }
}
