package com.soh3il.feature.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import com.soh3il.core.scheduler.NotificationHelper
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.model.AppTheme
import com.soh3il.core.scheduler.LiveIconHelper
import com.soh3il.core.scheduler.PersistentNotificationHelper
import com.soh3il.feature.settings.dialogs.AutoStartDialog
import com.soh3il.feature.settings.sections.AboutUpdateSection
import com.soh3il.feature.settings.sections.AppearanceSection
import com.soh3il.feature.settings.sections.BatterySection
import com.soh3il.feature.settings.sections.NotificationsSection
import com.soh3il.feature.settings.sections.IntegrationsSection
import com.soh3il.feature.settings.sections.ToolsSection
import com.soh3il.feature.settings.util.AutoStartHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppTheme = AppTheme.SYSTEM,
    onThemeSelected: (AppTheme) -> Unit = {},
    isAmoled: Boolean = false,
    onAmoledToggled: (Boolean) -> Unit = {},
    currentVersionName: String = "1.0.0",
    onPersistentNotificationToggled: ((Boolean) -> Unit)? = null,
    onExportIcsClick: (() -> Unit)? = null,
    onAutoBackupToggled: ((Boolean) -> Unit)? = null,
    onDefaultCalendarSelected: ((String) -> Unit)? = null,
    onImportIcsUri: ((Uri) -> Unit)? = null,
    onSyncCalendarClick: (() -> Unit)? = null,
    contactBirthdaysEnabled: Boolean = false,
    contactsPermissionGranted: Boolean = false,
    onContactBirthdaysToggled: ((Boolean) -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onWidgetsClick: (() -> Unit)? = null,
    openDateConverterRequested: Boolean = false,
    onDateConverterRequestHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Dialog state
    var showDateConverterDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openDateConverterRequested) {
        if (openDateConverterRequested) {
            showDateConverterDialog = true
            onDateConverterRequestHandled()
        }
    }
    var showAutoStartDialog by remember { mutableStateOf(false) }
    val deviceGuide = remember { AutoStartHelper.getDeviceGuide() }

    // User preferences state
    var isPersistentNotificationEnabled by remember {
        mutableStateOf(UserPreferences.isPersistentNotificationEnabled(context))
    }
    var isLockscreenCalendarEnabled by remember {
        mutableStateOf(UserPreferences.isLockscreenCalendarEnabled(context))
    }
    var isGregorianDateEnabled by remember {
        mutableStateOf(UserPreferences.isPersistentGregorianEnabled(context))
    }
    var isHijriDateEnabled by remember {
        mutableStateOf(UserPreferences.isPersistentHijriEnabled(context))
    }
    var isLiveIconEnabled by remember {
        mutableStateOf(UserPreferences.isLiveIconEnabled(context))
    }
    var isDynamicNotifIconEnabled by remember {
        mutableStateOf(UserPreferences.isDynamicNotificationIconEnabled(context))
    }
    var selectedSoundTitle by remember {
        mutableStateOf(UserPreferences.getNotificationSoundTitle(context))
    }

    fun syncAllWidgetsAndNotifications() {
        com.soh3il.core.scheduler.MidnightReceiver.requestWidgetUpdate(context)

        // 2. Refresh Persistent Notification
        scope.launch(Dispatchers.IO) {
            try {
                if (UserPreferences.isPersistentNotificationEnabled(context)) {
                    val db = Soh3ilDatabase.getInstance(context)
                    val active = db.reminderDao().getAllActiveRemindersSync().map { it.toDomain() }
                    PersistentNotificationHelper.updateNotification(context, active)
                }
            } catch (_: Exception) {}
        }

        // 3. Update Live Icon
        try {
            LiveIconHelper.updateLiveIcon(context)
        } catch (_: Exception) {}
    }

    val importIcsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportIcsUri?.invoke(uri)
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }

            val title = if (uri != null) {
                try {
                    val ringtone = RingtoneManager.getRingtone(context, uri)
                    ringtone?.getTitle(context) ?: "صدای انتخابی"
                } catch (_: Exception) {
                    "صدای انتخابی"
                }
            } else {
                "بی‌صدا"
            }

            UserPreferences.setNotificationSound(context, uri?.toString(), title)
            NotificationHelper.updateNotificationChannelSound(context, uri)
            selectedSoundTitle = title
            syncAllWidgetsAndNotifications()
        }
    }

    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBattery = remember(context) {
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تنظیمات برنامه",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "بازگشت"
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0.dp),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

        // 1. پوسته و ظاهر (Appearance & Theme)
        AppearanceSection(
            context = context,
            currentTheme = currentTheme,
            onThemeSelected = { newTheme ->
                onThemeSelected(newTheme)
                syncAllWidgetsAndNotifications()
            },
            isAmoled = isAmoled,
            onAmoledToggled = { newAmoled ->
                onAmoledToggled(newAmoled)
                syncAllWidgetsAndNotifications()
            },
            isLiveIconEnabled = isLiveIconEnabled,
            onLiveIconToggled = { newVal ->
                isLiveIconEnabled = newVal
                UserPreferences.setLiveIconEnabled(context, newVal)
                LiveIconHelper.updateLiveIcon(context)
                syncAllWidgetsAndNotifications()
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. اعلان‌ها و نوار وضعیت (Notifications & Status Bar)
        NotificationsSection(
            context = context,
            isPersistentNotificationEnabled = isPersistentNotificationEnabled,
            onPersistentNotificationToggled = { enabled ->
                isPersistentNotificationEnabled = enabled
                UserPreferences.setPersistentNotificationEnabled(context, enabled)
                onPersistentNotificationToggled?.invoke(enabled)
                syncAllWidgetsAndNotifications()
            },
            isLockscreenCalendarEnabled = isLockscreenCalendarEnabled,
            onLockscreenCalendarToggled = { enabled ->
                isLockscreenCalendarEnabled = enabled
                UserPreferences.setLockscreenCalendarEnabled(context, enabled)
                syncAllWidgetsAndNotifications()
            },
            isGregorianDateEnabled = isGregorianDateEnabled,
            onGregorianDateToggled = { enabled ->
                isGregorianDateEnabled = enabled
                UserPreferences.setPersistentGregorianEnabled(context, enabled)
                syncAllWidgetsAndNotifications()
            },
            isHijriDateEnabled = isHijriDateEnabled,
            onHijriDateToggled = { enabled ->
                isHijriDateEnabled = enabled
                UserPreferences.setPersistentHijriEnabled(context, enabled)
                syncAllWidgetsAndNotifications()
            },
            isDynamicNotifIconEnabled = isDynamicNotifIconEnabled,
            onDynamicNotifIconToggled = { newVal ->
                isDynamicNotifIconEnabled = newVal
                UserPreferences.setDynamicNotificationIconEnabled(context, newVal)
                onPersistentNotificationToggled?.invoke(isPersistentNotificationEnabled)
                syncAllWidgetsAndNotifications()
            },
            selectedSoundTitle = selectedSoundTitle,
            ringtonePickerLauncher = ringtonePickerLauncher
        )

        Spacer(modifier = Modifier.height(20.dp))

        IntegrationsSection(
            contactBirthdaysEnabled = contactBirthdaysEnabled,
            contactsPermissionGranted = contactsPermissionGranted,
            onContactBirthdaysToggled = { enabled -> onContactBirthdaysToggled?.invoke(enabled) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. ابزارها و گاه‌شماری (Tools & Calendar)
        ToolsSection(
            onOpenDateConverter = { showDateConverterDialog = true },
            onWidgetsClick = onWidgetsClick,
            onExportIcsClick = onExportIcsClick,
            onAutoBackupToggled = onAutoBackupToggled,
            onDefaultCalendarSelected = onDefaultCalendarSelected,
            importIcsLauncher = importIcsLauncher,
            onSyncCalendarClick = onSyncCalendarClick
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. پایداری سیستم و بهینه‌سازی باتری (System & Battery Optimization)
        BatterySection(
            context = context,
            isIgnoringBattery = isIgnoringBattery,
            onShowAutoStartDialog = { showAutoStartDialog = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 5. درباره برنامه و به‌روزرسانی (About & Release Metadata)
        AboutUpdateSection(
            context = context,
            currentVersionName = currentVersionName
        )
    }
    }

    // Dialogs
    if (showAutoStartDialog) {
        AutoStartDialog(
            context = context,
            deviceGuide = deviceGuide,
            onDismiss = { showAutoStartDialog = false }
        )
    }

    if (showDateConverterDialog) {
        DateConverterDialog(onDismiss = { showDateConverterDialog = false })
    }
}
