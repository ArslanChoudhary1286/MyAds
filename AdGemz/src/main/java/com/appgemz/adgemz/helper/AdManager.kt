package com.appgemz.adgemz.helper

import android.app.Activity
import android.content.Context
import android.util.Log
import com.appgemz.adgemz.enums.AdPlace
import com.appgemz.adgemz.enums.AdType
import com.appgemz.adgemz.extensions.isDebugMode
import com.appgemz.adgemz.models.AdsConfigModel
import com.appgemz.adgemz.utils.Constants
import com.appgemz.adgemz.utils.Constants.TEST_DEVICE_HASHED_IDS
import com.appgemz.adgemz.utils.NetworkConnectivity
import com.appgemz.adgemz.utils.AdsPreferences
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {

    const val TAG = "AdManager"

    private val isMobileAdsInitialized = AtomicBoolean(false)
    private lateinit var googleMobileConsentManager: ConsentManager

    /**
     * Checks if an ad request should be made, ensuring at least 30 minutes
     * have passed since the last request to avoid hitting the ad limit.
     *
     * @return `true` if an ad request can be made, `false` otherwise.
     */
    fun shouldRequestAd(adUnitId: String): Boolean {
        val lastRequestTime = AdsPreferences.getLong(adUnitId) // Get the last ad request timestamp
        val currentTime = System.currentTimeMillis()               // Get the current system time
        val config = FirebaseRemote.getConfig().globalSettings
        return (currentTime - lastRequestTime) > config.failCappingMs
    }

    /**
     * Checks whether the user can click an ad based on the time limit.
     *
     * @param timeKey Key for tracking the last ad click time.
     * @param countKey Key for tracking the number of ad clicks in a day.
     * @return `true` if the user can click the ad, `false` if the limit is reached.
     */
    fun canClickAd(timeKey: String, countKey: String): Boolean {
        val lastClickTime = AdsPreferences.getLong(timeKey, 0L)
        val clickCount = AdsPreferences.getLong(countKey, 0L)
        val currentTime = System.currentTimeMillis()

        val config = FirebaseRemote.getConfig().globalSettings
        val intervalMs = config.clickIntervalMs

        // Check if the interval has passed — reset if yes
        if (currentTime - lastClickTime >= intervalMs) {
            AdsPreferences.putLong(countKey, 0L)
            AdsPreferences.putLong(timeKey, currentTime)
            return true
        }

        // Allow click if under the limit within the interval
        return clickCount < config.clicksInInterval
    }


    /**
     * Updates the ad click count and last click time to track user interactions.
     *
     * @param timeKey Key for storing the last ad click timestamp.
     * @param countKey Key for storing the total clicks in a day.
     */
    fun registerAdClick(timeKey: String, countKey: String) {
        val newClickCount = AdsPreferences.getLong(countKey, 0L) + 1
        AdsPreferences.putLong(countKey, newClickCount)
        AdsPreferences.putLong(timeKey, System.currentTimeMillis())
    }

    fun printAdvertisingId(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
                val adId = info.id
                Log.d("AdvertisingID", "Google Advertising ID: $adId")
            } catch (e: Exception) {
                Log.e("AdvertisingID", "Failed to get Advertising ID: ${e.message}")
            }
        }
    }

    /**
     * Initializes the Google Mobile Ads SDK with user consent handling.
     *
     * @param context The current activity.
     * @param onSdkInitializationWithConsentStart Callback triggered when consent gathering starts.
     * @param onInitialized Callback triggered when initialization is complete.
     */
    suspend fun initAdmobSDK(
        context: Activity,
        onSdkInitializationWithConsentStart: () -> Unit = {},
        onInitialized: (Boolean) -> Unit = {}
    ) {
        Log.d(TAG, "Google Mobile Ads SDK Version: ${MobileAds.getVersion()}")

        if (!NetworkConnectivity.hasInternetConnection()) {
            Log.d(TAG, "SDK not Initialized without internet connection")
            onInitialized(false)
            return
        }

        // If already initialized, return success
        if (isMobileAdsInitialized.get()) {
            onInitialized(true)
            return
        }

        googleMobileConsentManager = ConsentManager.getInstance(context)

        // Gather user consent before loading ads
        onSdkInitializationWithConsentStart()
        googleMobileConsentManager.gatherConsent(context) { consentError ->
            consentError?.let {
                Log.w(TAG, "Consent error: ${it.errorCode}. ${it.message}")
            }

            if (!googleMobileConsentManager.canRequestAds) {
                onInitialized(false)
                return@gatherConsent
            }

            // Ensure Mobile Ads SDK is not initialized multiple times
            if (isMobileAdsInitialized.getAndSet(true)) {
                onInitialized(true)
                return@gatherConsent
            }

            // Configure test device IDs for debugging
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
//                    .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                    .setTestDeviceIds(TEST_DEVICE_HASHED_IDS)
                    .build()
            )

            // Initialize Google Mobile Ads SDK on the main thread
            CoroutineScope(Dispatchers.Main).launch {
                MobileAds.initialize(context) {
                    Log.d(TAG, "Google Mobile Ads SDK initialized successfully")
                    onInitialized(true)
                }
            }
        }

    }


    fun getAdId(adName: String, adType: AdType): String {
        val config = FirebaseRemote.getConfig()
        val placements = config.adPlacements
        val isDebug = isDebugMode()

        // ===== DEBUG MODE TEST IDS =====
        if (isDebug) return getTestIds(adType)

        // ===== REMOTE CONFIG IDS =====
        val remoteAdId = when (adType) {
            AdType.BANNER -> placements.banners.find {
                it.placementName.equals(adName, ignoreCase = true) && it.enabled
            }?.adUnitId

            AdType.INTERSTITIAL -> placements.interstitial.find {
                it.placementName.equals(adName, ignoreCase = true) && it.enabled
            }?.adUnitId

            AdType.NATIVE -> placements.nativeAds.find {
                it.placementName.equals(adName, ignoreCase = true) && it.enabled
            }?.adUnitId

            AdType.APP_OPEN -> placements.appOpen.adUnitId
        }

        // ===== FALLBACK TO DEFAULT =====
        return remoteAdId ?: getTestIds(adType)
    }


    fun getTestIds(adType: AdType) = when (adType) {
        AdType.BANNER -> Constants.TEST_BANNER_ID
        AdType.INTERSTITIAL -> Constants.TEST_INTERSTITIAL_ID
        AdType.NATIVE -> Constants.TEST_NATIVE_ID
        AdType.APP_OPEN -> Constants.TEST_APP_OPEN_ID
    }

    fun getTemplateId(adName: String): Int? {
        val config = FirebaseRemote.getConfig() ?: return null
        val placements = config.adPlacements

        // Check global ads enabled
        if (!config.globalSettings.adsEnabled) return null

        // Search in native ads placements only
        return placements.nativeAds.find {
            it.placementName.equals(adName, ignoreCase = true) && it.enabled
        }?.templateNo
    }

    fun getTemplateStyle(adName: String): AdsConfigModel.AdPlacements.NativeAd.Style? {
        val config = FirebaseRemote.getConfig() ?: return null
        val placements = config.adPlacements

        // Check global ads enabled
        if (!config.globalSettings.adsEnabled) return null

        // Search in native ads placements only
        return placements.nativeAds.find {
            it.placementName.equals(adName, ignoreCase = true) && it.enabled
        }?.style
    }

    fun getInterAd(adPlace: AdPlace): AdsConfigModel.AdPlacements.Interstitial? {
        val config = FirebaseRemote.getConfig()
        val placements = config.adPlacements

        // Check global ads enabled
        if (!config.globalSettings.adsEnabled) return null

        // Search in native ads placements only
        return placements.interstitial.find {
            it.placementName.equals(adPlace.name, ignoreCase = true)
        }
    }

    fun getNativeAd(adPlace: AdPlace): AdsConfigModel.AdPlacements.NativeAd? {
        val config = FirebaseRemote.getConfig()
        val placements = config.adPlacements

        // Check global ads enabled
        if (!config.globalSettings.adsEnabled) return null

        // Search in native ads placements only
        return placements.nativeAds.find {
            it.placementName.equals(adPlace.name, ignoreCase = true)
        }
    }


    fun isAdEnabled(adName: String, adType: AdType): Boolean {
        val config = FirebaseRemote.getConfig()
        val placements = config.adPlacements

        // First check global toggle
        if (!config.globalSettings.adsEnabled) return false

        return when (adType) {
            AdType.BANNER -> {
                placements.banners.any {
                    it.placementName.equals(adName, ignoreCase = true) && it.enabled
                }
            }

            AdType.INTERSTITIAL -> {
                placements.interstitial.any {
                    it.placementName.equals(adName, ignoreCase = true) && it.enabled
                }
            }

            AdType.NATIVE -> {
                placements.nativeAds.any {
                    it.placementName.equals(adName, ignoreCase = true) && it.enabled
                }
            }

            AdType.APP_OPEN -> {
                placements.appOpen.enabled
            }
        }
    }

    fun isAnyAdEnabled(): Boolean {
        val config = FirebaseRemote.getConfig()
        val placements = config.adPlacements
        val global = config.globalSettings

        // If global ads are disabled, no ad is enabled
        if (!global.adsEnabled) return false

        // Check App Open Ad
        if (placements.appOpen.enabled) return true

        // Check Banners
        if (placements.banners.any { it.enabled }) return true

        // Check Interstitials
        if (placements.interstitial.any { it.enabled }) return true

        // Check Native Ads
        if (placements.nativeAds.any { it.enabled }) return true

        // Check AppOpen Ads
        if (placements.appOpen.enabled) return true

        // No ad type enabled
        return false
    }
}