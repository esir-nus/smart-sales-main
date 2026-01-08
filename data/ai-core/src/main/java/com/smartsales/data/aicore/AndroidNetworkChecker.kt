// File: data/ai-core/src/main/java/com/smartsales/data/aicore/AndroidNetworkChecker.kt
// Module: :data:ai-core
// Summary: Android implementation of NetworkChecker using ConnectivityManager
// Author: created on 2026-01-08
package com.smartsales.data.aicore

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of NetworkChecker.
 * Checks if device has active internet connectivity.
 */
@Singleton
class AndroidNetworkChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkChecker {
    
    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as? ConnectivityManager ?: return false
        
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
