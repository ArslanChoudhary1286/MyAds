package com.appgemz.adgemz.ads

import android.app.Activity
import android.util.Log
import com.appgemz.adgemz.helper.AdManager
import com.appgemz.adgemz.helper.FirebaseRemote
import com.appgemz.adgemz.utils.CoroutineTimer
import com.appgemz.adgemz.utils.NetworkConnectivity
import com.appgemz.adgemz.utils.AdsPreferences
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.delay

object NativeAdLoader {

    private var currentNativeAd: NativeAd? = null
    private var isLoading: Boolean = false
    var isAdUsed: Boolean = false
    private var retryCount = 0
    const val TAG = "NativeAdManager"
    private var timer: CoroutineTimer? = null

    suspend fun loadNativeAd(activity: Activity, nativeId: String, onLoadAd: (NativeAd?) -> Unit = {}) {

        Log.d(TAG, "start load native ad $nativeId")

        if (nativeId.isEmpty() || !NetworkConnectivity.Companion.hasInternetConnection()) {
            Log.d(TAG, "Native Ad ID empty or no internet connection")
            onLoadAd(null)
            return
        }

        // If ad is already loaded and not used, return it
        if (currentNativeAd != null && !isAdUsed) {
            Log.d(TAG, "Reusing cached native ad")
            onLoadAd(currentNativeAd!!)
            return
        }

        // Prevent multiple simultaneous requests
        if (isLoading) {
            Log.d(TAG, "Native Ad Already Loading")

            timer?.cancel()
            timer = CoroutineTimer(
                durationInMs = 3000,
                onTick = { second ->
                    Log.d(TAG, "Tick: $second sec")

                    if (!isLoading && currentNativeAd != null) {
                        timer?.cancel()
                        onLoadAd(currentNativeAd!!)
                    }

                },
                onComplete = {
                    if (!isLoading && currentNativeAd != null) {
                        onLoadAd(currentNativeAd!!)
                    } else onLoadAd(null)
                }
            ).apply { start() }
            Log.d(TAG, "Native Ad Already Loading begin to return")
            return
        }

        isLoading = true

        if (!AdManager.shouldRequestAd(nativeId)) {
            Log.d(TAG, "Ad serving limit reached. Waiting before retrying.")
            isLoading = false
            onLoadAd(null)
            return
        }

        if (retryCount < FirebaseRemote.getConfig().globalSettings.failedAttempt) {
            retryCount++
            val retryDelay = if (retryCount == 1) 0L else FirebaseRemote.getConfig().globalSettings.failCappingMs.toLong()   // Set delay
            Log.d(TAG, "Retrying ad load in $retryDelay ms...")
            delay(retryDelay)
        } else {
            Log.d(TAG, "Max retry limit reached")
            isLoading = false
            return
        }

        destroyCurrentAd() // Destroy old ad if new one is requested

        val adLoader = AdLoader.Builder(activity, nativeId)
            .forNativeAd { nativeAd ->
                isLoading = false
                isAdUsed = false
                retryCount = 0
                currentNativeAd = nativeAd
                onLoadAd(nativeAd)
                Log.d(TAG, "onLoadAd natice ad")
            }
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                    .build()
            )
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Failed to load native ad: Code=${loadAdError.code}, Message=${loadAdError.message}")
                    isLoading = false
                    onLoadAd(null)
                    if (loadAdError.code == AdRequest.ERROR_CODE_NO_FILL) {
                        AdsPreferences.putLong(nativeId, System.currentTimeMillis())
                    }
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun destroyCurrentAd() {
        currentNativeAd?.destroy()
        currentNativeAd = null
        isAdUsed = false
    }
}