package de.onyxnvw.wakeonlan.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.onyxnvw.wakeonlan.data.ConnectionState
import de.onyxnvw.wakeonlan.R
import de.onyxnvw.wakeonlan.data.NetworkDeviceUiState

@Composable
fun StorageStatusCard(netDeviceUiState: NetworkDeviceUiState, refresh: () -> Unit) {
    val painter: Painter
    val color: Color
    val info: MutableList<String> = mutableListOf()

    info += stringResource(R.string.connection_status, stringResource(id = netDeviceUiState.connectionState.desc))

    when (netDeviceUiState.connectionState) {
        ConnectionState.UNKNOWN -> {
            painter = painterResource(R.drawable.cloud_sync_48px)
            color = Color.Gray
        }

        ConnectionState.PENDING -> {
            painter = painterResource(R.drawable.cloud_sync_48px)
            color = Color.Yellow
        }

        ConnectionState.DISCONNECTED -> {
            painter = painterResource(R.drawable.cloud_off_48px)
            color = Color.Red
        }

        ConnectionState.CONNECTED -> {
            painter = painterResource(R.drawable.cloud_done_48px)
            color = Color.Green
            info += "IP: " + netDeviceUiState.address
            info += "MAC: " + netDeviceUiState.macAddress
        }
    }
    val modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(128.dp)

    StatusCard(
        painter,
        color,
        info,
        onClick = { refresh() },
        modifier = modifier
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StorageStatusCardPreview() {
    StorageStatusCard(
        NetworkDeviceUiState(
            ConnectionState.CONNECTED,
            "192.10.128.30",
            "00:11:22:33:44:55"
        ),
        refresh = {}
    )
}
