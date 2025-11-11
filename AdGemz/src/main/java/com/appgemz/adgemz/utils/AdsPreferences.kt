package com.appgemz.adgemz.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Singleton class to manage shared preferences in the application.
 */
object AdsPreferences {

    lateinit var preferences: SharedPreferences
    const val PREF_NAME = "my_local_preference"

    /**
     * Initializes the PreferencesManager with the application context.
     * Should be called once in the application class or main activity.
     *
     * @param context Application context
     */
    fun init(context: Context) {
        if (!::preferences.isInitialized) preferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if the PreferencesManager has been initialized.
     *
     * @return True if initialized, false otherwise.
     */
    fun hasInstance(): Boolean {
        return this::preferences.isInitialized
    }

    /**
     * Stores a string value in shared preferences.
     *
     * @param key Key for the preference
     * @param value Value to be stored
     */
    fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    /**
     * Stores a long value in shared preferences.
     *
     * @param key Key for the preference
     * @param value Value to be stored
     */
    fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    /**
     * Stores an integer value in shared preferences.
     *
     * @param key Key for the preference
     * @param value Value to be stored
     */
    fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    /**
     * Stores a boolean value in shared preferences.
     *
     * @param key Key for the preference
     * @param value Value to be stored
     */
    fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    /**
     * Retrieves a string value from shared preferences.
     *
     * @param key Key for the preference
     * @param defValue Default value to return if the key does not exist
     * @return The stored string value or the default value
     */
    fun getString(key: String, defValue: String = ""): String {
        return preferences.getString(key, defValue) ?: defValue
    }

    /**
     * Retrieves a long value from shared preferences.
     *
     * @param key Key for the preference
     * @param defValue Default value to return if the key does not exist
     * @return The stored long value or the default value
     */
    fun getLong(key: String, defValue: Long = 0L): Long {
        return preferences.getLong(key, defValue)
    }

    /**
     * Retrieves an integer value from shared preferences.
     *
     * @param key Key for the preference
     * @param defValue Default value to return if the key does not exist
     * @return The stored integer value or the default value
     */
    fun getInt(key: String, defValue: Int = 0): Int {
        return preferences.getInt(key, defValue)
    }

    /**
     * Retrieves a boolean value from shared preferences.
     *
     * @param key Key for the preference
     * @param defValue Default value to return if the key does not exist
     * @return The stored boolean value or the default value
     */
    fun getBoolean(key: String, defValue: Boolean = false): Boolean {
        return preferences.getBoolean(key, defValue)
    }

    /**
     * Checks if the shared preferences contains a specific key.
     *
     * @param key Key to check
     * @return True if the key exists, false otherwise
     */
    fun contains(key: String): Boolean {
        return preferences.contains(key)
    }

    /**
     * Removes a specific key from shared preferences.
     *
     * @param key Key to be removed
     */
    fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }


    // ✅ Save List of Models
    fun <T> putModelList(key: String, list: List<T>) {
        val json = Gson().toJson(list)
        preferences.edit().putString(key, json).apply()
    }

    // ✅ Retrieve List of Models
    inline fun <reified T> getModelList(key: String): List<T> {
        val json = preferences.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<T>>() {}.type
        return Gson().fromJson(json, type)
    }

    // ✅ Save single model
    fun <T> putModel(key: String, model: T) {
        val json = Gson().toJson(model)
        preferences.edit().putString(key, json).apply()
    }

    // ✅ Retrieve single model
    inline fun <reified T> getModel(key: String): T? {
        val json = preferences.getString(key, null) ?: return null
        return Gson().fromJson(json, T::class.java)
    }

    /**
     * Clears all data from shared preferences.
     */
    fun clear() {
        preferences.edit().clear().apply()
    }

}
