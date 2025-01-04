package de.onyxnvw.wakeonlan.utils

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
}