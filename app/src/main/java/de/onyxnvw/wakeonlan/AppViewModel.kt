package de.onyxnvw.wakeonlan

import android.content.Context
import android.util.Log
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat.getString
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import de.onyxnvw.wakeonlan.data.ConnectionState
import de.onyxnvw.wakeonlan.data.NetworkDeviceUiState
import de.onyxnvw.wakeonlan.data.PreferenceSetting
import de.onyxnvw.wakeonlan.data.WakeUpResult
import de.onyxnvw.wakeonlan.data.WifiUiState
import de.onyxnvw.wakeonlan.ui.events.UiEvent
import de.onyxnvw.wakeonlan.utils.NetworkHelpers
import de.onyxnvw.wakeonlan.utils.NetworkUtility
import de.onyxnvw.wakeonlan.utils.sendWakeUpMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.TimeUnit

class AppViewModel(context: Context) : ViewModel() {
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
    private var _wifiSubnetMask = "0.0.0.0"

    /**
     * Network device state
     */
    private val _networkDeviceUiState = MutableStateFlow(
        NetworkDeviceUiState(
            connectionState = ConnectionState.UNKNOWN,
            address = "0.0.0.0",
            macAddress = "00:00:00:00:00:00"
        )
    )
    val networkDeviceUiState: StateFlow<NetworkDeviceUiState> = _networkDeviceUiState.asStateFlow()

    /**
     * UI events
     */
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    /**
     * Background work UUID
     */
    private var _workUuid: UUID? = null

    /**
     * Preferences
     */
    private val dataStore: DataStore<Preferences> = context.dataStore
    private val _settings = MutableStateFlow(
        listOf(
            PreferenceSetting(
                stringPreferencesKey("network_device_ip_address"),
                context.getString(R.string.prefs_network_device_ip_address),
                "0.0.0.0"
            ),
            PreferenceSetting(
                stringPreferencesKey("network_device_mac_address"),
                context.getString(R.string.prefs_network_device_mac_address),
                "00:00:00:00:00:00"
            ),
            PreferenceSetting(
                stringPreferencesKey("network_subnet_mask"),
                context.getString(R.string.prefs_network_subnet_mask),
                "0.0.0.0"
            )
        )
    )
    val settings = _settings.asStateFlow()

    init {
        Log.d(TAG, "AppViewModel init")

        handlePreferences()
        monitorWifiConnectivity()
        monitorNetworkDeviceConnectivity()
    }

    private fun handlePreferences() {
        viewModelScope.launch {
            // handle dataStore flow
            launch {
                dataStore.data.collect { preferences ->
                    preferences.asMap().forEach { entry ->
                        val key = entry.key.name
                        val value = entry.value.toString()
                        _settings.value = _settings.value.map { setting ->
                            if (setting.key.name == key) {
                                setting.copy(value = value)
                            } else {
                                setting
                            }
                        }
                    }
                }
            }
            // handle settings flow
            launch {
                // update internal status based on changed settings
                _settings.collect { settings ->
                    settings.forEach { setting ->
                        when (setting.key.name) {
                            "network_device_ip_address" -> {
                                _networkDeviceUiState.update { currentState ->
                                    currentState.copy(address = setting.value)
                                }
                            }

                            "network_device_mac_address" -> {
                                _networkDeviceUiState.update { currentState ->
                                    currentState.copy(macAddress = setting.value)
                                }
                            }

                            "network_subnet_mask" -> {
                                _wifiSubnetMask = setting.value
                                _wifiUiState.update { currentState ->
                                    currentState.copy(
                                        broadcastAddress = NetworkHelpers.getBroadcastAddress(
                                            currentState.address,
                                            setting.value
                                        )
                                    )
                                }
                            }
                        }
                    }
                    checkNetworkDeviceConnectivity()
                }
            }
        }
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
                            _wifiSubnetMask
                        )
                    )
                }
                /* in case the WiFi network is disconnected the UI shall show an unknown
                 * connection status */
                if (_wifiUiState.value.connectionState == ConnectionState.DISCONNECTED) {
                    Log.d(TAG, "monitorWifiConnectivity Wifi disconnected")
                    _networkDeviceUiState.update { currentState ->
                        currentState.copy(connectionState = ConnectionState.UNKNOWN)
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

    private fun monitorNetworkDeviceConnectivity() {
        viewModelScope.launch {
            Log.d(TAG, "monitorNetDeviceConnectivity launched")
            workManager.getWorkInfosByTagLiveData("networkDeviceConnectivity").asFlow()
                .collectLatest { workInfos ->
                    Log.d(TAG, "monitorNetDeviceConnectivity workInfo updated")
                    var connectionState: ConnectionState = ConnectionState.UNKNOWN
                    for (workInfo in workInfos) {
                        Log.d(TAG, "monitorNetDeviceConnectivity workInfo iteration")
                        if (workInfo.state.isFinished) {
                            if ((_workUuid != null) && (workInfo.id == _workUuid)) {
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
                            }
                        } else {
                            connectionState = ConnectionState.PENDING
                        }
                    }
                    _networkDeviceUiState.update { currentState ->
                        currentState.copy(connectionState = connectionState)
                    }
                }
        }
    }

    fun updateActivityState(isForeground: Boolean) {
        _isInForeground.value = isForeground
    }

    fun checkNetworkDeviceConnectivity() {
        Log.d(TAG, "checkNetworkDeviceConnectivity entry")
        _networkDeviceUiState.update { currentState ->
            currentState.copy(connectionState = ConnectionState.UNKNOWN)
        }

        if (_wifiUiState.value.connectionState == ConnectionState.CONNECTED) {
            if (NetworkHelpers.areInSameSubnet(
                    _wifiUiState.value.address,
                    _networkDeviceUiState.value.address,
                    _wifiSubnetMask
                )
            ) {
                // set state to pending before doing the network connectivity check
                _networkDeviceUiState.update { currentState ->
                    currentState.copy(connectionState = ConnectionState.PENDING)
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val inetAddress = InetAddress.getByName(_networkDeviceUiState.value.address)
                        val connectionState = if (inetAddress.isReachable(500)) {
                            ConnectionState.CONNECTED
                        } else {
                            ConnectionState.DISCONNECTED
                        }
                        _networkDeviceUiState.update { currentState ->
                            currentState.copy(connectionState = connectionState)
                        }
                    } catch (e: IOException) {
                        Log.d(TAG, "checkNetworkDeviceConnectivity IO exception")
                        _networkDeviceUiState.update { currentState ->
                            currentState.copy(connectionState = ConnectionState.UNKNOWN)
                        }
                    }
                }
            }
        }
        Log.d(TAG, "checkNetworkDeviceConnectivity exit")
    }

    fun wakeUpNetworkDevice() {
        viewModelScope.launch {
            if (_wifiUiState.value.connectionState == ConnectionState.CONNECTED) {
                if (NetworkHelpers.areInSameSubnet(
                        _wifiUiState.value.address,
                        _networkDeviceUiState.value.address,
                        _wifiSubnetMask
                    )
                ) {
                    val result = sendWakeUpMessage(
                        _networkDeviceUiState.value.macAddress,
                        _wifiUiState.value.broadcastAddress
                    )
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "wakeUpNetworkDevice success")
                            // update UI about sent wake up message
                            _uiEvent.emit(UiEvent.WakeUpNetworkDeviceResult(WakeUpResult.SUCCESS))

                            // start background worker to wait for network device to get
                            // available
                            val workUuid = UUID.randomUUID()
                            val inputData = Data.Builder()
                                .putString("host", _networkDeviceUiState.value.address)
                                .putInt("attempts", 5)
                                .build()
                            val workRequest = OneTimeWorkRequestBuilder<NetDeviceCheckWorker>()
                                .addTag("networkDeviceConnectivity")
                                .setId(workUuid)
                                .setInitialDelay(30, TimeUnit.SECONDS)
                                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                                .setInputData(inputData)
                                .build()

                            Log.d(TAG, "wakeUpNetworkDevice enqueue workRequest")
                            workManager.enqueueUniqueWork(
                                "networkDeviceConnectivity",
                                ExistingWorkPolicy.REPLACE,
                                workRequest
                            )
                            _workUuid = workUuid
                        },
                        onFailure = {
                            Log.d(TAG, "wakeUpNetworkDevice failure")
                            _uiEvent.emit(UiEvent.WakeUpNetworkDeviceResult(WakeUpResult.FAILURE))
                        }
                    )
                } else {
                    Log.d(TAG, "wakeUpNetworkDevice not in same subnet")
                    _uiEvent.emit(UiEvent.WakeUpNetworkDeviceResult(WakeUpResult.SUBNET_MISMATCH))
                }
            } else {
                Log.d(TAG, "wakeUpNetworkDevice not connected")
                _uiEvent.emit(UiEvent.WakeUpNetworkDeviceResult(WakeUpResult.WIFI_DISCONNECTED))
            }
        }
    }

    /**
     * preferences
     */
    fun updateSetting(key: Preferences.Key<String>, newValue: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[key] = newValue
            }
        }
    }

}
