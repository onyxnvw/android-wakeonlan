package de.onyxnvw.wakeonlan.data

/**
 * Data class that represents the current UI state for network device
 */
data class NetworkDeviceUiState(
    /** network device connection state */
    val connectionState: ConnectionState = ConnectionState.UNKNOWN,
    /** IPv4 address of network device */
    val address: String = "?.?.?.?",
    /** MAC address of network device */
    val macAddress: String = "??:??:??:??:??:??"
)
