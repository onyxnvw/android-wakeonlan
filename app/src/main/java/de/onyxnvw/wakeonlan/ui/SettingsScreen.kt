package de.onyxnvw.wakeonlan.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.onyxnvw.wakeonlan.AppViewModel
import de.onyxnvw.wakeonlan.R
import de.onyxnvw.wakeonlan.data.PreferenceSetting
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel, appVersion: String) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    val settings by viewModel.settings.collectAsState()
    var selectedSetting by remember { mutableStateOf<PreferenceSetting?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        settings.forEach { setting ->
            ListItem(
                headlineContent = { Text(setting.name) },
                supportingContent = { Text(setting.value) },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier
                            .clickable {
                                selectedSetting = setting
                                showBottomSheet = true
                            }
                            .padding(16.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
            )
            HorizontalDivider(color = Color.Gray, thickness = 1.dp)
        }
        /** version information */
        ListItem(
            headlineContent = { Text(stringResource(id = R.string.prefs_app_version)) },
            supportingContent = { Text(appVersion) },
            trailingContent = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Edit",
                    modifier = Modifier
                        .width(64.dp)
                        .padding(16.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
        )
    }

    if (showBottomSheet) {
        if (selectedSetting != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState,
            ) {
                var text by remember { mutableStateOf(selectedSetting!!.value) }

                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(id = R.string.prefs_edit_action_title)) },
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
                    Text(
                        stringResource(id = R.string.prefs_edit_action_dismiss),
                        modifier = Modifier.clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(32.dp))
                    Text(
                        stringResource(id = R.string.prefs_edit_action_confirm),
                        modifier = Modifier.clickable {
                            viewModel.updateSetting(selectedSetting!!.key, text)
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

//@Preview(
//    showBackground = true,
//    showSystemUi = true
//)
//@Composable
//fun SettingsPreview() {
//    WakeOnLanTheme {
//        SettingsScreen(appVersion = "1.0")
//    }
//}
