package com.selendang3.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun connectivity(context: Context) = callbackFlow {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    fun update() {
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        trySend(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
    }
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = update()
        override fun onLost(network: Network) = update()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = update()
    }
    manager.registerDefaultNetworkCallback(callback)
    update()
    awaitClose { manager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
