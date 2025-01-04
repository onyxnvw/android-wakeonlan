package de.onyxnvw.wakeonlan

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermission() {
    val notificationPermissionState =
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    var showDialog by remember { mutableStateOf(true) }

    if (!notificationPermissionState.status.isGranted && showDialog) {
        AlertDialog(
            icon = {},
            title = {
                Text(stringResource(R.string.perm_notif_dialog_title))
            },
            text = {
                Text(stringResource(R.string.perm_notif_rationale))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        notificationPermissionState.launchPermissionRequest()
                        showDialog = false
                    }
                ) {
                    Text(stringResource(R.string.perm_notif_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                    }
                ) {
                    Text(stringResource(R.string.perm_notif_dismiss))
                }
            },
            onDismissRequest = {
                showDialog = false
            }
        )
    }
}
