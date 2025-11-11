package com.appgemz.adgemz.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class NetworkConnectivity(context: Context) {

    companion object {

        suspend fun hasInternetConnection(): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    // Try to connect to a reliable external server (Google's public DNS)
                    val socket = Socket()
                    socket.connect(InetSocketAddress("8.8.8.8", 53), 1500) // 1.5 second timeout
                    socket.close()
                    true
                } catch (e: IOException) {
                    e.printStackTrace()
                    false
                }
            }
        }
    }

    // LiveData to observe network connection status
    private val _isNetworkAvailable = MutableLiveData<Boolean>()
    val isNetworkAvailable: LiveData<Boolean> = _isNetworkAvailable

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Initialize network callback
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            // Network is available, now check if it has internet access
            checkInternetConnection()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            _isNetworkAvailable.postValue(false) // Update LiveData that the network is lost
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            // Re-check internet connection if network capabilities have changed
            checkInternetConnection()
        }
    }

    init {
        // Register network callback for network changes
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Initial network status check
        checkInternetConnection()
    }

    // Function to check if the network has internet access
    private fun checkInternetConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            val hasInternet = hasInternetConnection()
            _isNetworkAvailable.postValue(hasInternet)
        }
    }

    // Function to check if the current network connection has internet access
    private fun hasInternetConnection(): Boolean {
        return try {
            // Try to connect to a reliable external server (Google's public DNS)
            val socket = Socket()
            socket.connect(InetSocketAddress("8.8.8.8", 53), 1500) // 1.5 second timeout
            socket.close()
            true
        } catch (e: IOException) {
            false
        }
    }

    // Cleanup when the app is closed
    fun unregisterCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }


}