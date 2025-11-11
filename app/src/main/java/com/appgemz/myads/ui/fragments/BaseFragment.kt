package com.appgemz.myads.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.appgemz.adgemz.ads.InterAdManager
import com.appgemz.adgemz.enums.AdPlace
import com.appgemz.adgemz.enums.AdType
import com.appgemz.adgemz.extensions.isAppInForeground
import com.appgemz.adgemz.extensions.name
import com.appgemz.adgemz.helper.AdManager
import com.appgemz.adgemz.helper.FirebaseRemote
import com.appgemz.adgemz.utils.CoroutineTimer
import com.appgemz.myads.R
import com.appgemz.myads.ui.dialogs.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class BaseFragment : Fragment() {

    private var timer: CoroutineTimer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                if (this@BaseFragment is SplashFragment) {
                    fetchRemoteConfigAndLoadAd()
                }
            }
        }


    }

    suspend fun fetchRemoteConfigAndLoadAd() {

        val isDataFetched = FirebaseRemote.getRemoteData()

        if (isDataFetched) {

            if (AdManager.isAnyAdEnabled()) {

                AdManager.initAdmobSDK(requireContext() as AppCompatActivity) {

                    loadAndShowInterstitialAd(AdPlace.SPLASH) {
                        gotoNextScreen()
                    }

                }

            } else {
                gotoNextScreen()
            }

        } else {
            gotoNextScreen()
        }

    }

    private fun loadAndShowInterstitialAd(adPlace: AdPlace, actionEnd: () -> Unit) {

        val duration: Int = FirebaseRemote.getConfig().globalSettings.runtimeAdLoadMs
        val interModel = AdManager.getInterAd(AdPlace.SPLASH)
        InterAdManager.load(requireContext(), adPlace)

        timer = CoroutineTimer(duration, onTick = { ms ->
            Log.d("SplashTimer", "Tick: $ms ms")
            if (InterAdManager.isLoaded()) {
                timer?.cancel()
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main.immediate) {
                    if (isRemoving || activity?.isFinishing == true) {
                        actionEnd.invoke()
                        return@launch
                    }

                    try {
                        if (!InterAdManager.isLoaded()) {
                            actionEnd.invoke()
                            return@launch
                        }

                        val context = requireContext() as AppCompatActivity

                        if (interModel?.showLoading==true) {
                            LoadingDialog.show(context)
                            delay(1000)
                        }

                        LoadingDialog.dismiss(context)
                        if (isRemoving || activity?.isFinishing == true || !context.isAppInForeground()) {
                            actionEnd.invoke()
                            return@launch
                        }

                        InterAdManager.show(requireActivity()) {
                            actionEnd.invoke()
                        }

                    } catch (e: IllegalStateException) {
                        Log.e("SplashFragment", "Dialog error: ${e.message}")
                        actionEnd.invoke()
                    }
                }
            } else if (!InterAdManager.isLoading()) {
                Log.d("SplashTimer", "Timer cancel with ad failed to load")
                timer?.cancel()
                actionEnd.invoke()
            }
        }, onComplete = {
            Log.d("SplashTimer", "Timer completed at $duration sec!")
            actionEnd.invoke()
        }).apply { start() }
    }

    private fun gotoNextScreen() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val navController = (requireActivity() as AppCompatActivity)
                    .supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment_content_main)
                    ?.findNavController() ?: return@repeatOnLifecycle

                val currentDestId = navController.currentDestination?.id
                if (currentDestId != R.id.splashFragment) {
                    Log.e("SplashFragment", "Skip navigation: Not on splash anymore")
                    return@repeatOnLifecycle
                }

                Log.e("SplashFragment", "gotoNextScreen")

                // ✅ Common NavOptions to clear Splash from stack
                val navOptions = navOptions {
                    popUpTo(R.id.splashFragment) { inclusive = true }
                    launchSingleTop = true
                }

                findNavController().navigate(R.id.action_splashFragment_to_languageFragment)

//                if (this@BaseFragment is SplashFragment) {
//                    navController.navigate(
//                        R.id.action_splashFragment_to_languageFragment,
//                        null,
//                        navOptions
//                    )
//                }



//                when {
//                    PreferencesManager.getString(Constants.KEY_APP_LANGUAGE).isEmpty() -> {
//                        navController.navigate(
//                            R.id.action_splashFragment_to_languageFragment,
//                            null,
//                            navOptions
//                        )
//                        Log.e("SplashFragment", "gotoNextScreen → LanguageFragment")
//                    }
//
//                    !PreferencesManager.getBoolean(Constants.ALREADY_INTRO_OPENED) -> {
//                        navController.navigate(
//                            R.id.action_splashFragment_to_boardingFragment,
//                            null,
//                            navOptions
//                        )
//                        Log.e("SplashFragment", "gotoNextScreen → BoardingFragment")
//                    }
//
//                    else -> {
//                        PreferencesManager.putBoolean(Constants.KEY_RETURN_USER, true)
//                        navController.navigate(
//                            R.id.action_splashFragment_to_homeFragment,
//                            null,
//                            navOptions
//                        )
//                        Log.e("SplashFragment", "gotoNextScreen → HomeFragment")
//                    }
//                }
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}
