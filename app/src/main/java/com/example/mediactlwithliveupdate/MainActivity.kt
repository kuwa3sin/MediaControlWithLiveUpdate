package com.kuwa3sin.mediactlwithliveupdate

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.kuwa3sin.mediactlwithliveupdate.ui.theme.MediaCtlWithLiveUpdateTheme
import com.kuwa3sin.mediactlwithliveupdate.config.ChipIndicatorMode
import com.kuwa3sin.mediactlwithliveupdate.config.ChipIndicatorPreferences
import com.kuwa3sin.mediactlwithliveupdate.config.ChipTextMode
import com.kuwa3sin.mediactlwithliveupdate.ui.component.ExpressiveSurface
import com.kuwa3sin.mediactlwithliveupdate.ui.component.ElevatedListItem
import com.kuwa3sin.mediactlwithliveupdate.ui.component.ExpressiveChip
import kotlinx.coroutines.isActive
import android.content.Context

/**
 * Live Media Control - メインアクティビティ
 *
 * このアクティビティは、必要な権限のチェックとユーザーへの案内を提供する。
 *
 * 必要な権限:
 * 1. 通知リスナー権限 (BIND_NOTIFICATION_LISTENER_SERVICE)
 *    - 他アプリのメディア通知を傍受するために必要
 *    - Settings.ACTION_NOTIFICATION_LISTENER_SETTINGSで手動有効化
 *
 * 2. プロモート通知権限 (POST_PROMOTED_NOTIFICATIONS)
 *    - Live Update通知をステータスバーチップに表示するために必要
 *    - Android 16で導入された権限
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaCtlWithLiveUpdateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PermissionScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun PermissionScreen(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val sharedPrefs = remember(context) {
            context.getSharedPreferences(ChipIndicatorPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        }

    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableStateOf(SettingsTab.PERMISSIONS) }

        var hasNotificationListenerPermission by remember {
            mutableStateOf(isNotificationListenerEnabled())
        }
        var hasPromotedNotificationPermission by remember {
            mutableStateOf(canPostPromotedNotifications())
        }
        var hasNotificationPermission by remember {
            mutableStateOf(isNotificationPermissionGranted())
        }
        var chipIndicatorMode by remember {
            mutableStateOf(ChipIndicatorPreferences.readIndicatorMode(sharedPrefs))
        }
        var chipTextMode by remember {
            mutableStateOf(ChipIndicatorPreferences.readTextMode(sharedPrefs))
        }
        var liveUpdateEnabled by remember {
            mutableStateOf(ChipIndicatorPreferences.isLiveUpdateEnabled(sharedPrefs))
        }

        DisposableEffect(sharedPrefs) {
            val listener = ChipIndicatorPreferences.observe(sharedPrefs) { snapshot ->
                chipIndicatorMode = snapshot.indicatorMode
                chipTextMode = snapshot.textMode
                liveUpdateEnabled = snapshot.liveUpdateEnabled
            }
            onDispose {
                sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }

        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasNotificationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        }

        // 画面が表示されるたびに権限状態を更新
        LaunchedEffect(Unit) {
            while (isActive) {
                hasNotificationListenerPermission = isNotificationListenerEnabled()
                hasPromotedNotificationPermission = canPostPromotedNotifications()
                hasNotificationPermission = isNotificationPermissionGranted()
                kotlinx.coroutines.delay(1000)
            }
        }

        val allPermissionsGranted = hasNotificationListenerPermission &&
            hasPromotedNotificationPermission &&
            hasNotificationPermission

        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = getString(R.string.settings_title),
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = getString(R.string.settings_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LiveUpdateToggleCard(
                        enabled = liveUpdateEnabled,
                        onToggle = {
                            liveUpdateEnabled = it
                            ChipIndicatorPreferences.setLiveUpdateEnabled(sharedPrefs, it)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ExpressiveChipBar(
                        tabs = listOf(SettingsTab.PERMISSIONS, SettingsTab.CUSTOMIZE),
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedTab) {
                        SettingsTab.PERMISSIONS -> {
                            PermissionsSection(
                                allPermissionsGranted = allPermissionsGranted,
                                hasNotificationListenerPermission = hasNotificationListenerPermission,
                                hasNotificationPermission = hasNotificationPermission,
                                hasPromotedNotificationPermission = hasPromotedNotificationPermission,
                                requestNotifications = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        openAppNotificationSettings()
                                    }
                                },
                                openListenerSettings = { openNotificationListenerSettings() },
                                openPromotedSettings = { openPromotedNotificationSettings() }
                            )
                        }
                        SettingsTab.CUSTOMIZE -> {
                            CustomizationSection(
                                selectedMode = chipIndicatorMode,
                                selectedTextMode = chipTextMode,
                                onModeSelected = { mode ->
                                    chipIndicatorMode = mode
                                    ChipIndicatorPreferences.writeIndicatorMode(sharedPrefs, mode)
                                },
                                onTextModeSelected = { mode ->
                                    chipTextMode = mode
                                    ChipIndicatorPreferences.writeTextMode(sharedPrefs, mode)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    @Composable
    private fun LiveUpdateToggleCard(
        enabled: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        ExpressiveSurface(modifier = Modifier.fillMaxWidth(), tonalElevation = 6.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getString(R.string.live_update_toggle_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = getString(R.string.live_update_toggle_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }

    @Composable
    private fun PermissionsSection(
        allPermissionsGranted: Boolean,
        hasNotificationListenerPermission: Boolean,
        hasNotificationPermission: Boolean,
        hasPromotedNotificationPermission: Boolean,
        requestNotifications: () -> Unit,
        openListenerSettings: () -> Unit,
        openPromotedSettings: () -> Unit
    ) {
        if (allPermissionsGranted) {
            ExpressiveSurface(modifier = Modifier.fillMaxWidth(), tonalElevation = 4.dp) {
                Text(
                    text = "✓ " + getString(R.string.permissions_granted),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getString(R.string.service_running),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        PermissionCard(
            title = getString(R.string.permission_notification_listener_title),
            message = getString(R.string.permission_notification_listener_message),
            isGranted = hasNotificationListenerPermission,
            onRequestPermission = openListenerSettings
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionCard(
            title = getString(R.string.permission_post_notifications_title),
            message = getString(R.string.permission_post_notifications_message),
            isGranted = hasNotificationPermission,
            buttonText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                getString(R.string.request_permission)
            } else {
                getString(R.string.open_settings)
            },
            onRequestPermission = requestNotifications
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionCard(
            title = getString(R.string.permission_promoted_notifications_title),
            message = getString(R.string.permission_promoted_notifications_message),
            isGranted = hasPromotedNotificationPermission,
            onRequestPermission = openPromotedSettings
        )
    }

    @Composable
    private fun CustomizationSection(
        selectedMode: ChipIndicatorMode,
        selectedTextMode: ChipTextMode,
        onModeSelected: (ChipIndicatorMode) -> Unit,
        onTextModeSelected: (ChipTextMode) -> Unit
    ) {
        ExpressiveSurface(modifier = Modifier.fillMaxWidth(), tonalElevation = 4.dp) {
            Text(
                text = getString(R.string.chip_indicator_mode_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = getString(R.string.chip_indicator_mode_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            val options = listOf(
                ChipIndicatorMode.APP_ICON_ONLY to getString(R.string.chip_indicator_option_app_icon),
                ChipIndicatorMode.PLAYBACK_ICON_ONLY to getString(R.string.chip_indicator_option_playback_icon)
            )

            options.forEach { (mode, label) ->
                ElevatedListItem(
                    label = label,
                    selected = mode == selectedMode,
                    onClick = { onModeSelected(mode) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = getString(R.string.chip_text_mode_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = getString(R.string.chip_text_mode_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            val textOptions = listOf(
                ChipTextMode.NONE to getString(R.string.chip_text_option_none),
                ChipTextMode.APP_NAME_SHORT to getString(R.string.chip_text_option_app_name),
                ChipTextMode.PLAYBACK_STATE to getString(R.string.chip_text_option_playback_state)
            )

            textOptions.forEach { (mode, label) ->
                ElevatedListItem(
                    label = label,
                    selected = mode == selectedTextMode,
                    onClick = { onTextModeSelected(mode) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    @Composable
    private fun ExpressiveChipBar(
        tabs: List<SettingsTab>,
        selectedTab: SettingsTab,
        onTabSelected: (SettingsTab) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tabs.forEach { tab ->
                ExpressiveChip(
                    label = getString(tab.labelRes),
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    enum class SettingsTab(val labelRes: Int) {
        PERMISSIONS(R.string.permission_section_title),
        CUSTOMIZE(R.string.customization_section_title)
    }

    @Composable
    private fun PermissionCard(
        title: String,
        message: String,
        isGranted: Boolean,
        onRequestPermission: () -> Unit,
        buttonText: String = getString(R.string.open_settings)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ExpressiveSurface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = if (isGranted) 4.dp else 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chipColor = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = chipColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = if (isGranted) "✓" else "✗",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = chipColor
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isGranted) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(buttonText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    /**
     * 通知リスナー権限が有効かチェック
     */
    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        if (flat.isNullOrEmpty()) {
            return false
        }
        val names = flat.split(":")
        for (name in names) {
            val componentName = ComponentName.unflattenFromString(name)
            if (componentName != null) {
                if (packageName == componentName.packageName) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * プロモート通知を投稿できるかチェック
     */
    private fun canPostPromotedNotifications(): Boolean {
        val notificationManager = getSystemService(NotificationManager::class.java)
        return try {
            // Android 16でcanPostPromotedNotifications()が利用可能
            notificationManager.canPostPromotedNotifications()
        } catch (e: Exception) {
            // メソッドが存在しない場合はfalseを返す
            false
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (!notificationManager.areNotificationsEnabled()) {
            return false
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 通知リスナー設定画面を開く
     */
    private fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    /**
     * プロモート通知設定画面を開く
     */
    private fun openPromotedNotificationSettings() {
        try {
            // Android 16のACTION_MANAGE_APP_PROMOTED_NOTIFICATIONSを文字列で指定
            val intent = Intent("android.settings.MANAGE_APP_PROMOTED_NOTIFICATIONS")
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        } catch (e: Exception) {
            // フォールバック: アプリ通知設定画面を開く
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            } catch (e2: Exception) {
                // さらにフォールバック: 通知設定画面を開く
                val intent = Intent("android.settings.NOTIFICATION_SETTINGS")
                startActivity(intent)
            }
        }
    }

    private fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // 権限が両方揃っているか再チェック
        // これにより画面が自動更新される
    }
}