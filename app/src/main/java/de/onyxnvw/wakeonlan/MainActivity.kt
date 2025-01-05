package de.onyxnvw.wakeonlan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.onyxnvw.wakeonlan.ui.SettingsScreen
import de.onyxnvw.wakeonlan.ui.StartScreen
import de.onyxnvw.wakeonlan.ui.events.UiEvent
import de.onyxnvw.wakeonlan.ui.theme.WakeOnLanTheme
import kotlinx.coroutines.launch


const val TAG = "WakeOnLAN"

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(applicationContext) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageManager = applicationContext.packageManager
        val packageName = applicationContext.packageName
        val packageInfo = packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
        val lifecycleObserver = AppLifecycleObserver(
            onForeground = { appViewModel.updateActivityState(true) },
            onBackground = { appViewModel.updateActivityState(false) }
        )
        lifecycle.addObserver(lifecycleObserver)

        setContent {
            WakeOnLanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    WakeOnLanApp(
                        viewModel = appViewModel,
                        appVersion = packageInfo.versionName
                    )
                }
            }
        }
    }
}


enum class WakeOnLanScreens(@StringRes val title: Int) {
    Start(title = R.string.start_screen),
    Settings(title = R.string.settings_screen)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeOnLanApp(
    viewModel: AppViewModel,
    navController: NavHostController = rememberNavController(),
    appVersion: String
) {
    /* notification permission handling */
    NotificationPermission()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val wifiUiState by viewModel.wifiUiState.collectAsState()
    val networkDeviceUiState by viewModel.networkDeviceUiState.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = WakeOnLanScreens.valueOf(
        backStackEntry?.destination?.route ?: WakeOnLanScreens.Start.name
    )
    val canNavigateBack = navController.previousBackStackEntry != null

    // check for UI events
    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.WakeUpNetworkDeviceResult -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(getString(context, event.result.desc))
                    }
                }

                is UiEvent.NetworkDeviceStatus -> {
                    coroutineScope.launch {
                        if (event.isAvailable) {
                            sendDeviceAvailableNotification(context)
                        } else {
                            sendDeviceUnavailableNotification(context)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(stringResource(currentScreen.title))
                },
                modifier = Modifier,
                actions = {
                    if (!canNavigateBack) {
                        IconButton(onClick = { navController.navigate(WakeOnLanScreens.Settings.name) }) {
                            Icon(
                                painter = painterResource(R.drawable.settings_24px),
                                contentDescription = stringResource(R.string.settings_action)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!canNavigateBack) {
                LargeFloatingActionButton(
                    onClick = {
                        viewModel.wakeUpNetworkDevice()
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.power_settings_new_48px),
                        contentDescription = stringResource(R.string.fab_action),
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = WakeOnLanScreens.Start.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = WakeOnLanScreens.Start.name) {
                StartScreen(
                    wifiUiState = wifiUiState,
                    networkDeviceUiState = networkDeviceUiState,
                    refresh = { viewModel.checkNetworkDeviceConnectivity() })
            }

            composable(route = WakeOnLanScreens.Settings.name) {
                SettingsScreen(appVersion = appVersion)
            }
        }
    }
}

fun sendDeviceAvailableNotification(context: Context) {
    val notification =
        NotificationCompat.Builder(context, "de.onyxnvw.wakeonlan.status_notification_channel_id")
            .setContentTitle(context.getString(R.string.notification_content_title))
            .setContentText(context.getString(R.string.notification_content_text_connected))
            .setSmallIcon(R.drawable.power_settings_new_48px)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

    with(NotificationManagerCompat.from(context)) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notify(1, notification)
    }
}

fun sendDeviceUnavailableNotification(context: Context) {
    val notification =
        NotificationCompat.Builder(context, "de.onyxnvw.wakeonlan.status_notification_channel_id")
            .setContentTitle(context.getString(R.string.notification_content_title))
            .setContentText(context.getString(R.string.notification_content_text_disconnected))
            .setSmallIcon(R.drawable.power_settings_new_48px)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

    with(NotificationManagerCompat.from(context)) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notify(2, notification)
    }
}


//@Preview(
//    showBackground = true,
//    showSystemUi = true
//)
//@Composable
//fun HomeScreenPreview() {
//    PwrOnNasTheme {
//        HomeScreenContent(
//            wifiConnState = ConnectionState.CONNECTED,
//            wifiAddress = "192.168.1.2",
//            nasConnState = ConnectionState.CONNECTED,
//            nasAddress = "192.168.1.250",
//            nasMacAddress = "11:22:33:00:AA:CC",
//            onFabClick = {},
//            refresh = {}
//        )
//    }
//}
