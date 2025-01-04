package de.onyxnvw.wakeonlan.ui.events

sealed class UiEvent {
    data class WakeUpNetworkDeviceResult(val success: Boolean) : UiEvent()
    data class NetworkDeviceStatus(val isAvailable: Boolean) : UiEvent()
}
