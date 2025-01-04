package de.onyxnvw.wakeonlan.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.onyxnvw.wakeonlan.R
import de.onyxnvw.wakeonlan.ui.theme.WakeOnLanTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(/*viewModel: AppViewModel*/ appVersion: String) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    var ipAddress by remember { mutableStateOf("192.168.1.2")}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Setting(
            key = "IP-Adresse",
            value = ipAddress,
            icon = painterResource(R.drawable.devices_other_24px),
            isEditable = true,
            onEdit = { showBottomSheet = true })
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Color.Gray, thickness = 1.dp)
        Spacer(modifier = Modifier.height(4.dp))
        Setting(
            key = "Netzmaske",
            value = "255.255.255.0",
            icon = painterResource(R.drawable.devices_other_24px),
            isEditable = true,
            onEdit = { showBottomSheet = true })
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Color.Gray, thickness = 1.dp)
        Spacer(modifier = Modifier.height(4.dp))
        Setting(
            key = "MAC Adresse",
            value = "11:22:33:44:55:66",
            icon = painterResource(R.drawable.storage_24px),
            isEditable = true,
            onEdit = { showBottomSheet = true })
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Color.Gray, thickness = 1.dp)
        Spacer(modifier = Modifier.height(4.dp))
        /** version information */
        Setting(
            key = "App version",
            value = appVersion,
            icon = painterResource(R.drawable.info_24px),
            isEditable = false,
            onEdit = {})
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState,
        ) {
            var text by remember { mutableStateOf(ipAddress) }

            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Gib einen neuen Wert ein") },
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .fillMaxWidth()
            ) {
                Text("Abbrechen", modifier = Modifier.clickable {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                })
                Spacer(modifier = Modifier.width(32.dp))
                Text("Speichern", modifier = Modifier.clickable {
                    ipAddress = text
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                })
            }
        }
    }
}

@Composable
fun Setting(key: String, value: String, icon: Painter, isEditable: Boolean, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .then(if (isEditable) Modifier.clickable { onEdit() } else Modifier)
    ) {
        Icon(
            painter = icon,
            contentDescription = key,
            modifier = Modifier
                .fillMaxHeight()
                .width(64.dp)
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1.0f)
        ) {
            Text(
                text = key,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize
            )
            Text(
                text = value,
                fontSize = MaterialTheme.typography.bodySmall.fontSize
            )
        }
//        if (isEditable) {
//            Icon(
//                painter = painterResource(R.drawable.edit_24px),
//                contentDescription = "Edit",
//                modifier = Modifier
//                    .fillMaxHeight()
//                    .width(64.dp)
//                    .clickable { onEdit() }
//                    .padding(16.dp)
//            )
//        }
    }
}


@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun SettingsPreview() {
    WakeOnLanTheme {
        SettingsScreen(appVersion = "1.0")
    }
}

