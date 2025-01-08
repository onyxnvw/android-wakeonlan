package de.onyxnvw.wakeonlan.ui.components

import android.provider.Settings.Global.getString
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.onyxnvw.wakeonlan.R
import de.onyxnvw.wakeonlan.data.ConnectionState
import de.onyxnvw.wakeonlan.data.WifiUiState

@Composable
fun WifiStatusCard(wifiUiState: WifiUiState) {
    val painter: Painter = if (wifiUiState.connectionState != ConnectionState.DISCONNECTED) {
        painterResource(id = R.drawable.wifi_48px)
    } else {
        painterResource(id = R.drawable.wifi_off_48px)
    }
    val color: Color = colorResource(id = wifiUiState.connectionState.color)
    val info: List<String> = listOf(
        stringResource(R.string.connection_ip_address, wifiUiState.address),
        stringResource(R.string.connection_broadcast_address, wifiUiState.broadcastAddress),
        stringResource(R.string.connection_status, stringResource(id = wifiUiState.connectionState.desc))
    )

    val modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(128.dp)

    StatusCard(painter, color, info, onClick = {}, modifier = modifier)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WifiStatusCardPreview() {
    WifiStatusCard(WifiUiState(ConnectionState.CONNECTED, "192.10.128.30"))
}