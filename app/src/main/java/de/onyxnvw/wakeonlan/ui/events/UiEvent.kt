package de.onyxnvw.wakeonlan.ui.events

import de.onyxnvw.wakeonlan.data.WakeUpResult

sealed class UiEvent {
    data class WakeUpNetworkDeviceResult(val result: WakeUpResult) : UiEvent()
    data class NetworkDeviceStatus(val isAvailable: Boolean) : UiEvent()
}
