package de.onyxnvw.wakeonlan

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.onyxnvw.wakeonlan.data.ConnectionState
import de.onyxnvw.wakeonlan.data.NetworkDeviceUiState
import de.onyxnvw.wakeonlan.data.WifiUiState
import de.onyxnvw.wakeonlan.ui.events.UiEvent
import de.onyxnvw.wakeonlan.utils.NetworkHelpers
import de.onyxnvw.wakeonlan.utils.NetworkUtility
import de.onyxnvw.wakeonlan.utils.sendWakeUpMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AppViewModel(context: Context) : ViewModel() {
    companion object {
        const val HOME_NETWORK_MASK = "255.255.255.0"
        const val NAS_IP4_ADDRESS = "192.168.2.150" // "10.0.2.22" //
        const val NAS_MAC_ADDRESS = "00:11:32:C2:2F:ED"
    }

    private val networkUtility = NetworkUtility(context)
    private val workManager = WorkManager.getInstance(context)

    /**
     * Activity lifecycle state
     */
    private val _isInForeground = MutableStateFlow(false)

    /**
     * WiFi state
     */
    private val _wifiUiState = MutableStateFlow(
        WifiUiState(
            connectionState = ConnectionState.DISCONNECTED,
            address = "0.0.0.0"
        )
    )
    val wifiUiState: StateFlow<WifiUiState> = _wifiUiState.asStateFlow()

    /**
     * Network device state
     */
    private val _networkDeviceUiState = MutableStateFlow(
        NetworkDeviceUiState(
            connectionState = ConnectionState.UNKNOWN,
            address = NAS_IP4_ADDRESS,
            macAddress = NAS_MAC_ADDRESS
        )
    )
    val networkDeviceUiState: StateFlow<NetworkDeviceUiState> = _networkDeviceUiState.asStateFlow()

    /**
     * UI events
     */
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        Log.d(TAG, "AppViewModel init")
        monitorWifiConnectivity()
        monitorNetDeviceConnectivity()
    }

    private fun monitorWifiConnectivity() {
        viewModelScope.launch {
            Log.d(TAG, "monitorWifiConnectivity launched")
            networkUtility.wifiStatus.collect { status ->
                val previousConnectionState = _wifiUiState.value.connectionState

                Log.d(TAG, "monitorWifiConnectivity status update")
                _wifiUiState.update { currentState ->
                    currentState.copy(
                        connectionState = status.wifiConnState,
                        address = status.ipv4Address,
                        broadcastAddress = NetworkHelpers.getBroadcastAddress(
                            status.ipv4Address,
                            HOME_NETWORK_MASK
                        )
                    )
                }
                /* in case the WiFi network is disconnected the UI shall show an unknown
                 * connection status */
                if (_wifiUiState.value.connectionState == ConnectionState.DISCONNECTED) {
                    Log.d(TAG, "monitorWifiConnectivity Wifi disconnected")
                    _networkDeviceUiState.update { currentState ->
                        currentState.copy(
                            connectionState = ConnectionState.UNKNOWN,
                            address = currentState.address,
                            macAddress = currentState.macAddress
                        )
                    }
                }
                /* in case the WiFi network gets connected the NAS connection state shall be
                 * evaluated once */
                if ((previousConnectionState != ConnectionState.CONNECTED) &&
                    (_wifiUiState.value.connectionState == ConnectionState.CONNECTED)
                ) {
                    Log.d(TAG, "monitorWifiConnectivity Wifi connected")
                    checkNetworkDeviceConnectivity()
                }
            }
        }
    }

    private fun monitorNetDeviceConnectivity() {
        viewModelScope.launch {
            Log.d(TAG, "monitorNetDeviceConnectivity launched")
            workManager.getWorkInfosByTagLiveData("networkDeviceConnectivity").asFlow()
                .collectLatest { workInfos ->
                    Log.d(TAG, "monitorNetDeviceConnectivity workInfo updated")
                    var connectionState: ConnectionState = ConnectionState.UNKNOWN
                    for (workInfo in workInfos) {
                        Log.d(TAG, "monitorNetDeviceConnectivity workInfo iteration")
                        if (workInfo.state.isFinished) {
                            when (workInfo.state) {
                                WorkInfo.State.SUCCEEDED -> {
                                    Log.d(TAG, "monitorNetDeviceConnectivity succeeded")
                                    connectionState = ConnectionState.CONNECTED
                                    if (!_isInForeground.value) {
                                        _uiEvent.emit(UiEvent.NetworkDeviceStatus(true))
                                    }
                                }

                                WorkInfo.State.FAILED -> {
                                    Log.d(TAG, "monitorNetDeviceConnectivity failed")
                                    connectionState = ConnectionState.DISCONNECTED
                                    if (!_isInForeground.value) {
                                        _uiEvent.emit(UiEvent.NetworkDeviceStatus(false))
                                    }
                                }

                                else -> {
                                    Log.d(TAG, "monitorNetDeviceConnectivity unknown")
                                }
                            }
                        } else {
                            connectionState = ConnectionState.PENDING
                        }
                    }
                    _networkDeviceUiState.update { currentState ->
                        currentState.copy(
                            connectionState = connectionState,
                            address = currentState.address,
                            macAddress = currentState.macAddress
                        )
                    }
                }
        }
    }

    fun updateActivityState(isForeground: Boolean) {
        _isInForeground.value = isForeground
    }

    fun checkNetworkDeviceConnectivity(totalAttempts: Int = 1) {
        Log.d(TAG, "checkNetworkDeviceConnectivity entry")
        if (_wifiUiState.value.connectionState == ConnectionState.CONNECTED) {
            if (NetworkHelpers.areInSameSubnet(
                    _wifiUiState.value.address,
                    _networkDeviceUiState.value.address,
                    HOME_NETWORK_MASK
                )
            ) {
                val inputData = Data.Builder()
                    .putString("host", NAS_IP4_ADDRESS)
                    .putInt("attempts", totalAttempts)
                    .build()
                val workRequest: OneTimeWorkRequest =
                    OneTimeWorkRequestBuilder<NetDeviceCheckWorker>()
                        .addTag("networkDeviceConnectivity")
                        .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                        .setInputData(inputData)
                        .build()

                Log.d(TAG, "checkNetworkDeviceConnectivity enqueue workRequest")
                workManager.enqueueUniqueWork(
                    "networkDeviceConnectivity",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
        }
        Log.d(TAG, "checkNetworkDeviceConnectivity exit")
    }

    fun wakeUpNetworkDevice() {
        viewModelScope.launch {
            val result = sendWakeUpMessage(
                _networkDeviceUiState.value.macAddress,
                _wifiUiState.value.broadcastAddress
            )
            result.fold(
                onSuccess = {
                    Log.d(TAG, "wakeUpNetworkDevice success")
                    _uiEvent.emit(UiEvent.WakeUpNetworkDeviceResult(true))
                    checkNetworkDeviceConnectivity(5)
                },
                onFailure = {
                    Log.d(TAG, "wakeUpNetworkDevice failure")
                    _uiEvent.emit(UiEvent.WakeUpNetworkDeviceResult(false))
                }
            )
        }
    }
}