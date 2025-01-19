package de.onyxnvw.wakeonlan.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import de.onyxnvw.wakeonlan.TAG
import de.onyxnvw.wakeonlan.data.ConnectionState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.Inet4Address


data class WifiInfo(val wifiConnState: ConnectionState, val ipv4Address: String)


class NetworkUtility(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val wifiStatus: Flow<WifiInfo> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "wifiStatus:onAvailable")
                trySend(WifiInfo(ConnectionState.UNKNOWN, "0.0.0.0")).isSuccess
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "wifiStatus:onLost")
                trySend(WifiInfo(ConnectionState.DISCONNECTED, "0.0.0.0")).isSuccess
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                Log.d(TAG, "wifiStatus:onCapabilitiesChanged")
                val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                if (hasWifi) {
                    val linkProps = connectivityManager.getLinkProperties(network)
                    linkProps?.linkAddresses?.forEach { linkAddress ->
                        Log.d(TAG, "wifiStatus:onCapabilitiesChanged ${linkAddress.address.hostAddress}")
                        if (linkAddress.address is Inet4Address) {
                            val ipAddress = linkAddress.address.hostAddress ?: "0.0.0.0"
                            trySend(WifiInfo(ConnectionState.CONNECTED, ipAddress)).isSuccess
                        }
                    }
                }
                else {
                    trySend(WifiInfo(ConnectionState.DISCONNECTED, "0.0.0.0")).isSuccess
                }
            }

        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}
