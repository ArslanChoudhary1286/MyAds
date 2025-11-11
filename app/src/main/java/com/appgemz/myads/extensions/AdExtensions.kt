package com.appgemz.myads.extensions

import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.appgemz.adgemz.ads.NativeAdLoader
import com.appgemz.adgemz.enums.AdPlace
import com.appgemz.adgemz.enums.AdType
import com.appgemz.adgemz.helper.AdManager
import com.appgemz.adgemz.helper.FirebaseRemote
import com.appgemz.adgemz.models.AdsConfigModel
import com.appgemz.myads.databinding.NativeBanner2Binding
import com.appgemz.myads.databinding.NativeBannerBinding
import com.appgemz.myads.databinding.NativeFullScreenBinding
import com.appgemz.myads.databinding.NativeSimpleBinding
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import com.appgemz.adgemz.ads.NativeAdLoader.isAdUsed

const val TAG = "NATIVEAD"

fun FragmentActivity.loadNativesAd(
    adContainer: FrameLayout,
    adPlace: AdPlace,
    showHideBtn: Boolean = false
) {
    val adModel = AdManager.getNativeAd(adPlace)
    val adId = AdManager.getAdId(adPlace.name, AdType.NATIVE).orEmpty()
    val adNo = AdManager.getTemplateId(adPlace.name)

    if (!AdManager.isAdEnabled(adPlace.name, AdType.NATIVE)) return

    lifecycleScope.launch {
        // Common validation
        if (adId.isBlank()) {
            adContainer.visibility = View.GONE
            return@launch
        }

        val (binding, shimmerView, adView) = when (adNo) {
            1 -> {
                val b = NativeBannerBinding.inflate(layoutInflater)
                Triple(b, b.shimmerViewContainer.shimmer, b.adView)
            }
            2 -> {
                val b = NativeBanner2Binding.inflate(layoutInflater)
                Triple(b, b.shimmerViewContainer.shimmer, b.adView)
            }
            else -> {
                val b = NativeSimpleBinding.inflate(layoutInflater)
                Triple(b, b.shimmerViewContainer.shimmer, b.adView)
            }
        }

        adContainer.removeAllViews()
        adContainer.addView(binding.root)
        adContainer.visibility = View.VISIBLE

        // Handle shimmer loading logic consistently
        if (adModel?.showLoading == true) shimmerView.startShimmer() else shimmerView.visibility = View.GONE

        // Load Native Ad
        NativeAdLoader.loadNativeAd(this@loadNativesAd, adId) { nativeAd ->
            if (isDestroyed || isFinishing || isChangingConfigurations) return@loadNativeAd

            if (nativeAd != null) {
                val style = AdManager.getTemplateStyle(adPlace.name)
                shimmerView.stopShimmer()
                shimmerView.visibility = View.GONE
                adView.visibility = View.VISIBLE

                when (binding) {
                    is NativeSimpleBinding -> populateNativeAdView(nativeAd, binding, style)
                    is NativeBannerBinding -> populateNativeAdView(nativeAd, binding, style, showHideBtn)
                    is NativeBanner2Binding -> populateNativeAdView(nativeAd, binding, style, showHideBtn)
                }
            } else {
                shimmerView.stopShimmer()
                shimmerView.visibility = View.GONE
                adContainer.removeAllViews()
                adContainer.visibility = View.GONE
            }
        }
    }
}



fun populateNativeAdView(
    nativeAd: NativeAd,
    binding: NativeSimpleBinding,
    style: AdsConfigModel.AdPlacements.NativeAd.Style? = null
) {

    isAdUsed = true
    val nativeAdView = binding.root.apply {
        headlineView = binding.adHeadline
        bodyView = binding.adBody
        callToActionView = binding.adCallToAction
        iconView = binding.adAppIcon
        priceView = binding.adPrice
        starRatingView = binding.adStars
        storeView = binding.adStore
        advertiserView = binding.adAdvertiser
    }

    // Apply ad data
    binding.adHeadline.text = nativeAd.headline

    binding.adBody.apply {
        visibility = if (nativeAd.body.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        text = nativeAd.body ?: ""
    }

    binding.adCallToAction.apply {
        visibility = if (nativeAd.callToAction.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        text = nativeAd.callToAction ?: ""
    }

    binding.adAppIcon.apply {
        visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
        nativeAd.icon?.drawable?.let { setImageDrawable(it) }
    }

    binding.adAdvertiser.apply {
        visibility = if (nativeAd.advertiser.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        text = nativeAd.advertiser ?: ""
    }

    Log.d("NATIVECHECK", "style $style")

    // ✅ Apply style colors dynamically
    style?.let {
        try {
            val bgColor = it.backgroundColor.toColorInt()
            val headlineColor = it.headlineColor.toColorInt()
            val bodyColor = it.bodyColor.toColorInt()
            val ctaColor = it.ctaColor.toColorInt()

            // Apply colors to respective views
            binding.root.setBackgroundColor(bgColor)
            binding.adHeadline.setTextColor(headlineColor)
            binding.adBody.setTextColor(bodyColor)
            binding.adAdvertiser.setTextColor(bodyColor)

            // CTA (button) style
            binding.adCallToAction.setBackgroundColor(ctaColor)
            binding.adCallToAction.setTextColor(Color.WHITE)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.w("NativeAdStyle", "Invalid color format in style: ${e.message}")
        }
    }

    nativeAdView.setNativeAd(nativeAd)
}


fun populateNativeAdView(
    nativeAd: NativeAd,
    binding: NativeBannerBinding,
    style: AdsConfigModel.AdPlacements.NativeAd.Style? = null,
    showHideBtn: Boolean = false
) {
    isAdUsed = true
    val nativeAdView = binding.root.apply {
        mediaView = binding.adMedia
        headlineView = binding.adHeadline
        bodyView = binding.adBody
        callToActionView = binding.adCallToAction
        iconView = binding.adAppIcon
        priceView = binding.adPrice
        advertiserView = binding.adAdvertiser
    }

    // ====== Populate content ======
    binding.adHeadline.text = nativeAd.headline
    nativeAd.mediaContent?.let { binding.adMedia.mediaContent = it }

    binding.adBody.apply {
        visibility = if (nativeAd.body.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        text = nativeAd.body ?: ""
    }

    binding.adCallToAction.apply {
        visibility = if (nativeAd.callToAction.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        text = nativeAd.callToAction ?: ""
    }

    binding.adAppIcon.apply {
        visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
        nativeAd.icon?.drawable?.let { setImageDrawable(it) }
    }

    binding.adAdvertiser.apply {
        visibility = if (nativeAd.advertiser.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        text = nativeAd.advertiser ?: ""
    }

    nativeAdView.setNativeAd(nativeAd)

    // ====== Handle video lifecycle (if any) ======
    nativeAd.mediaContent?.videoController?.let { vc ->
        if (nativeAd.mediaContent!!.hasVideoContent()) {
            vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoEnd() {
                    Log.d("NativeBannerAd", "Video playback has ended.")
                    super.onVideoEnd()
                }
            }
        } else {
            Log.d("NativeBannerAd", "Ad does not contain a video asset.")
        }
    }

    // ====== Handle Hide Ad Button ======
    binding.btnHideAd.visibility = if (showHideBtn) View.VISIBLE else View.GONE
    binding.btnHideAd.setOnClickListener { binding.root.visibility = View.GONE }

    // ====== Apply dynamic style ======
    style?.let {
        try {
            val bgColor = it.backgroundColor.toColorInt()
            val headlineColor = it.headlineColor.toColorInt()
            val bodyColor = it.bodyColor.toColorInt()
            val ctaColor = it.ctaColor.toColorInt()

            // Background
            binding.root.setBackgroundColor(bgColor)

            // Text colors
            binding.adHeadline.setTextColor(headlineColor)
            binding.adBody.setTextColor(bodyColor)
            binding.adAdvertiser.setTextColor(bodyColor)

            // CTA (Call To Action) button
            binding.adCallToAction.apply {
                setBackgroundColor(ctaColor)
                setTextColor(Color.WHITE)
            }

            // Optional: style for hide button if visible
            if (showHideBtn) {
//                binding.btnHideAd.setTextColor(Color.WHITE)
//                binding.btnHideAd.setBackgroundColor("#88000000".toColorInt()) // semi-transparent dark
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.w("NativeBannerStyle", "Invalid color format in style: ${e.message}")
        }
    }
}

fun populateNativeAdView(
    nativeAd: NativeAd,
    binding: NativeBanner2Binding,
    style: AdsConfigModel.AdPlacements.NativeAd.Style? = null,
    showHideBtn: Boolean = false
) {
    isAdUsed = true

    val nativeAdView = binding.root.apply {
        mediaView = binding.adMedia
        headlineView = binding.adHeadline
        callToActionView = binding.adCallToAction
        advertiserView = binding.adAdvertiser
    }

    // ================= APPLY STYLE =================
    style?.let {
        try {
            binding.root.setBackgroundColor(it.backgroundColor.toColorInt())
            binding.adHeadline.setTextColor(it.headlineColor.toColorInt())
            binding.adAdvertiser.setTextColor(it.bodyColor.toColorInt())
            binding.adCallToAction.apply {
                setBackgroundColor(it.ctaColor.toColorInt())
                setTextColor(Color.WHITE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying style: ${e.message}")
        }
    }

    // ================= SET AD CONTENT =================
    binding.adHeadline.text = nativeAd.headline
    nativeAd.mediaContent?.let { binding.adMedia.mediaContent = it }

    binding.adCallToAction.apply {
        visibility = if (nativeAd.callToAction.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        text = nativeAd.callToAction ?: ""
    }

    binding.adAdvertiser.apply {
        visibility = if (nativeAd.advertiser.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        text = nativeAd.advertiser ?: ""
    }

    nativeAdView.setNativeAd(nativeAd)

    // ================= HANDLE VIDEO CONTENT =================
    nativeAd.mediaContent?.videoController?.let { vc ->
        if (nativeAd.mediaContent!!.hasVideoContent()) {
            vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoEnd() {
                    Log.d(TAG, "Video playback has ended.")
                    super.onVideoEnd()
                }
            }
        } else {
            Log.d(TAG, "Ad does not contain a video asset.")
        }
    }

    // ================= HIDE BUTTON LOGIC =================
    binding.btnHideAd.apply {
        visibility = if (showHideBtn) View.VISIBLE else View.GONE
        setOnClickListener {
            binding.root.visibility = View.GONE
        }
    }
}


fun populateNativeAdView(nativeAd: NativeAd, binding: NativeFullScreenBinding) {

    isAdUsed = true
    val nativeAdView = binding.root.apply {
        mediaView = binding.adMedia
        headlineView = binding.adHeadline
        bodyView = binding.adBody
        callToActionView = binding.adCallToAction
        iconView = binding.adAppIcon
        starRatingView = binding.adStars
        advertiserView = binding.adAdvertiser
    }

    binding.adHeadline.text = nativeAd.headline
    nativeAd.mediaContent?.let { binding.adMedia.mediaContent = it }

    binding.adBody.apply {
        visibility = if (nativeAd.body.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        text = nativeAd.body ?: ""
    }

    binding.adCallToAction.apply {
        visibility = if (nativeAd.callToAction.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        text = nativeAd.callToAction ?: ""
    }

    binding.adAppIcon.apply {
        visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
        nativeAd.icon?.drawable?.let { setImageDrawable(it) }
    }

    binding.adAdvertiser.apply {
        visibility = if (nativeAd.advertiser.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        text = nativeAd.advertiser ?: ""
    }

    nativeAdView.setNativeAd(nativeAd)

    nativeAd.mediaContent?.videoController?.let { vc ->
        if (nativeAd.mediaContent!!.hasVideoContent()) {
            vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoEnd() {
                    Log.d(TAG, "Video playback has ended.")
                    super.onVideoEnd()
                }
            }
        } else {
            Log.d(TAG, "Ad does not contain a video asset.")
        }
    }
}