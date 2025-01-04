package de.onyxnvw.wakeonlan.data

/**
 * Data class that represents the current UI state for WIFI connection
 */
data class WifiUiState (
    /** WiFi connection state */
    val connectionState: ConnectionState = ConnectionState.UNKNOWN,
    /** IPv4 address for WiFi connection of this mobile device */
    val address: String = "?.?.?.?",
    /** IPv4 broadcast address for WiFi connection of this mobile device */
    val broadcastAddress: String = "?.?.?.?"
)
