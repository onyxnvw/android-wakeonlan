package de.onyxnvw.wakeonlan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.onyxnvw.wakeonlan.data.ConnectionState
import de.onyxnvw.wakeonlan.data.NetworkDeviceUiState
import de.onyxnvw.wakeonlan.data.WifiUiState
import de.onyxnvw.wakeonlan.ui.components.StorageStatusCard
import de.onyxnvw.wakeonlan.ui.components.WifiStatusCard

@Composable
fun StartScreen(
    wifiUiState: WifiUiState,
    networkDeviceUiState: NetworkDeviceUiState,
    refresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
            //.padding(innerPadding)
        ) {
            WifiStatusCard(wifiUiState)
            StorageStatusCard(networkDeviceUiState, refresh)
        }
        if (networkDeviceUiState.connectionState == ConnectionState.PENDING) {
            CircularProgressIndicator(
                modifier = Modifier
                    .width(64.dp)
                    .align(Alignment.Center),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}