package de.onyxnvw.wakeonlan.utils

import android.util.Log
import de.onyxnvw.wakeonlan.TAG
import de.onyxnvw.wakeonlan.data.ConnectionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object NetworkHelpers {
    fun areInSameSubnet(ip1: String, ip2: String, subnetMask: String): Boolean {
        val ip1Parts = ip1.split(".").map { it.toInt() }
        val ip2Parts = ip2.split(".").map { it.toInt() }
        val subnetMaskParts = subnetMask.split(".").map { it.toInt() }

        if (ip1Parts.size != 4 || ip2Parts.size != 4 || subnetMaskParts.size != 4) {
            throw IllegalArgumentException("Invalid IP address or subnet mask format")
        }

        for (i in 0..3) {
            if ((ip1Parts[i] and subnetMaskParts[i]) != (ip2Parts[i] and subnetMaskParts[i])) {
                return false
            }
        }
        return true
    }

    fun getBroadcastAddress(ipv4Address: String, netmask: String): String {
        val ipv4Parts = ipv4Address.split(".").map { it.toInt() }
        val subnetMaskParts = netmask.split(".").map { it.toInt() }
        val broadcastParts = ipv4Parts.zip(subnetMaskParts) { ipv4Part, maskPart ->
            ipv4Part or maskPart.inv() and 0xFF
        }
        return broadcastParts.joinToString(".")
    }

    suspend fun isDeviceAvailable(ipAddress: String): Result<Boolean> {
        val completableDeferred = CompletableDeferred<Result<Boolean>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inetAddress = InetAddress.getByName(ipAddress)
                completableDeferred.complete(
                    Result.success(
                        inetAddress.isReachable(500)
                    )
                )
            } catch (e: IOException) {
                Log.d(TAG, "isDeviceAvailable IO exception")
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    completableDeferred.complete(Result.failure(e))
                }
            }
        }

        return completableDeferred.await()
    }
}
