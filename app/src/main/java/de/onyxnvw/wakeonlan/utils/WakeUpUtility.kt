package de.onyxnvw.wakeonlan.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

suspend fun sendWakeUpMessage(
    macAddress: String,
    broadcastAddress: String,
): Result<Unit> {
    val macBytes = macAddress.split(":").map { it.toInt(16).toByte() }.toByteArray()
    val message = ByteArray(102).apply {
        for (i in 0..5) this[i] = 0xFF.toByte()
        for (i in 6 until this.size) this[i] = macBytes[(i - 6) % macBytes.size]
    }
    val port = 9
    val completableDeferred = CompletableDeferred<Result<Unit>>()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val address = InetAddress.getByName(broadcastAddress)

            DatagramSocket().use {
                it.send(DatagramPacket(message, message.size, address, port))
            }

            completableDeferred.complete(Result.success(Unit))
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                e.printStackTrace()
                completableDeferred.complete(Result.failure(e))
            }
        }
    }

    return completableDeferred.await()
}
