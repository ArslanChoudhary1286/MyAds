package com.appgemz.adgemz.helper

import android.util.Log
import com.appgemz.adgemz.models.AdsConfigModel
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.tasks.await

object FirebaseRemote {

    private const val TAG = "RemoteAdManager"

    private val firebaseRemoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setFetchTimeoutInSeconds(60)
                .setMinimumFetchIntervalInSeconds(
                    0
                )
                .build()
            setConfigSettingsAsync(settings)
        }
    }

    private var adConfig: AdsConfigModel? = AdsConfigModel()

    fun getConfig(): AdsConfigModel = adConfig ?: AdsConfigModel()

    suspend fun getRemoteData(): Boolean {
        return try {
            // Fetch from Firebase Remote Config
            firebaseRemoteConfig.fetch(0).await()

            // Activate fetched data
            val activated = firebaseRemoteConfig.activate().await()

            if (activated) {
                adConfig = parseRemoteConfig()
                Log.d(TAG, "✅ Remote config fetched and parsed successfully.")
                true
            } else {
                Log.e(TAG, "⚠️ Activation failed, using cached config if available.")
                adConfig = parseRemoteConfig()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Remote fetch failed: ${e.message}")
            // Try to activate cached version as fallback
            return try {
                firebaseRemoteConfig.activate().await()
                adConfig = parseRemoteConfig()
                true
            } catch (ex: Exception) {
                Log.e(TAG, "❌ Failed to activate cached config: ${ex.message}")
                false
            }
        }
    }

    private fun parseRemoteConfig(): AdsConfigModel {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val gson = Gson()

        return try {
            // Get the JSON string from Remote Config
            val jsonString = firebaseRemoteConfig.getString("ad_config")

            Log.d(TAG, "json -> $jsonString")

            if (jsonString.isEmpty()) {
                Log.w(TAG, "⚠️ Remote config JSON is empty, using default config")
                AdsConfigModel() // Return default model
            } else {
                // Parse JSON string to model
                gson.fromJson(jsonString, AdsConfigModel::class.java)
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "❌ JSON parsing error: ${e.message}")
            AdsConfigModel() // Return default model on failure
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error: ${e.message}")
            AdsConfigModel()
        }
    }



}
