package com.haziel.ofmusic

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

sealed class ConnectionState {
    object Available : ConnectionState()
    object Unavailable : ConnectionState()
}

fun Context.observeConnectivityAsFlow() = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(ConnectionState.Available)
        }
        override fun onLost(network: Network) {
            trySend(ConnectionState.Unavailable)
        }
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            trySend(if (hasInternet) ConnectionState.Available else ConnectionState.Unavailable)
        }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    runCatching {
        connectivityManager.registerNetworkCallback(request, callback)
    }

    val isCurrentlyConnected = runCatching {
        val active = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(active)
        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }.getOrDefault(false)

    trySend(if (isCurrentlyConnected) ConnectionState.Available else ConnectionState.Unavailable)

    awaitClose {
        runCatching {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

@Composable
fun rememberConnectivityState(): State<ConnectionState> {
    val context = LocalContext.current
    return produceState<ConnectionState>(initialValue = ConnectionState.Unavailable) {
        context.observeConnectivityAsFlow().collect { value = it }
    }
}