package de.onyxnvw.wakeonlan.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.onyxnvw.wakeonlan.R

@Composable
fun StatusCard(
    icon: Painter,
    iconColor: Color,
    headline: String,
    info: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.then(
            Modifier
                .padding(8.dp)
                .clickable { onClick() }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight()
            ) {
                Icon(
                    painter = icon,
                    contentDescription = "",
                    tint = iconColor,
                    modifier = Modifier
                        .padding(16.dp)
                        .requiredSize(64.dp)
                )
            }
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight()
            ) {
                Text(
                    headline,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(horizontal = 2.dp, vertical = 8.dp)
                )
                info.forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StatusCardPreview() {
    val icon: Painter = painterResource(R.drawable.wifi_off_48px)
    val iconColor: Color = Color.Red
    val headline: String = "WiFi status"
    val info = listOf(
        stringResource(R.string.connection_ip_address, "192.168.1.1"),
        //stringResource(R.string.connection_broadcast_address, "192.168.1.255"),
        stringResource(R.string.connection_status, "connected")
    )
    val modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(144.dp)
    StatusCard(
        icon,
        iconColor,
        headline,
        info,
        onClick = {},
        modifier = modifier
    )
}