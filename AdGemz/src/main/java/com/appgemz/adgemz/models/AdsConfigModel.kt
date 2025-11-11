package com.appgemz.adgemz.models

import com.google.gson.annotations.SerializedName

data class AdsConfigModel(
    @SerializedName("ad_placements")
    val adPlacements: AdPlacements = AdPlacements(),

    @SerializedName("global_settings")
    val globalSettings: GlobalSettings = GlobalSettings()
) {
    data class AdPlacements(
        @SerializedName("app_open")
        val appOpen: AppOpen = AppOpen(),

        @SerializedName("banners")
        val banners: List<Banner> = emptyList(),

        @SerializedName("interstitial")
        val interstitial: List<Interstitial> = emptyList(),

        @SerializedName("native_ads")
        val nativeAds: List<NativeAd> = emptyList()
    ) {
        data class AppOpen(
            @SerializedName("ad_unit_id")
            val adUnitId: String = "",
            @SerializedName("enabled")
            val enabled: Boolean = false,
            @SerializedName("preload")
            val preload: Boolean = false,
            @SerializedName("show_loading")
            val showLoading: Boolean = true,
            @SerializedName("reload_on_dismiss")
            val reloadOnDismiss: Boolean = false,
            @SerializedName("reload_on_failed")
            val reloadOnFailed: Boolean = false
        )

        data class Banner(
            @SerializedName("ad_unit_id")
            val adUnitId: String = "",
            @SerializedName("auto_refresh_ms")
            val autoRefreshMs: Int = 30000,
            @SerializedName("enabled")
            val enabled: Boolean = false,
            @SerializedName("placement_name")
            val placementName: String = "",
            @SerializedName("preload")
            val preload: Boolean = false,
            @SerializedName("show_loading")
            val showLoading: Boolean = true,
            @SerializedName("reload_on_failed")
            val reloadOnFailed: Boolean = false
        )

        data class Interstitial(
            @SerializedName("ad_unit_id")
            val adUnitId: String = "",
            @SerializedName("enabled")
            val enabled: Boolean = false,
            @SerializedName("placement_name")
            val placementName: String = "",
            @SerializedName("preload")
            val preload: Boolean = false,
            @SerializedName("show_loading")
            val showLoading: Boolean = true,
            @SerializedName("reload_on_dismiss")
            val reloadOnDismiss: Boolean = false,
            @SerializedName("reload_on_failed")
            val reloadOnFailed: Boolean = false
        )

        data class NativeAd(
            @SerializedName("ad_unit_id")
            val adUnitId: String = "",
            @SerializedName("enabled")
            val enabled: Boolean = false,
            @SerializedName("placement_name")
            val placementName: String = "",
            @SerializedName("reload_on_failed")
            val reloadOnFailed: Boolean = false,
            @SerializedName("preload")
            val preload: Boolean = false,
            @SerializedName("show_loading")
            val showLoading: Boolean = true,
            @SerializedName("style")
            val style: Style = Style(),
            @SerializedName("templateNo")
            val templateNo: Int = 1
        ) {
            data class Style(
                @SerializedName("backgroundColor")
                val backgroundColor: String = "#FFFFFF",
                @SerializedName("bodyColor")
                val bodyColor: String = "#000000",
                @SerializedName("ctaColor")
                val ctaColor: String = "#2196F3",
                @SerializedName("headlineColor")
                val headlineColor: String = "#000000"
            )
        }
    }

    data class GlobalSettings(
        @SerializedName("ads_batch_ms")
        val adsBatchMs: Int = 30000,
        @SerializedName("ads_batch_size")
        val adsBatchSize: Int = 5,
        @SerializedName("ads_enabled")
        val adsEnabled: Boolean = true,
        @SerializedName("capping_ms")
        val cappingMs: Int = 60000,
        @SerializedName("fail_capping_ms")
        val failCappingMs: Int = 30000,
        @SerializedName("failed_attempt")
        val failedAttempt: Int = 3,
        @SerializedName("click_interval_ms")
        val clickIntervalMs: Int = 3600000, // click interval 1 hour
        @SerializedName("clicks_in_interval")
        val clicksInInterval: Int = 3, // user can 3 clicks in interval
        @SerializedName("mediation_enabled")
        val mediationEnabled: Boolean = false,
        @SerializedName("test_mode")
        val testMode: Boolean = false,
        @SerializedName("runtime_ad_load_ms")
        val runtimeAdLoadMs: Int = 10000
    )
}
