package com.soh3il.calendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.theme.Soh3ilTheme
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.feature.calendar.CalendarScreen
import com.soh3il.feature.reminder.AddReminderSheet
import android.content.Intent
import com.soh3il.feature.settings.SettingsScreen
import com.soh3il.calendar.integrations.rememberCalendarSyncAction
import com.soh3il.calendar.integrations.rememberContactBirthdays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var widgetDestination by mutableStateOf<com.soh3il.calendar.widget.WidgetDestination?>(null)
    private var notificationReminderId by mutableStateOf<Long?>(null)

    private var integrationRefreshVersion by mutableStateOf(0)

    private val requestContactsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                UserPreferences.setContactBirthdaysEnabled(this, true)
                integrationRefreshVersion++
            }
        }

    private val requestCalendarPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            integrationRefreshVersion++
        }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                android.widget.Toast.makeText(
                    this,
                    "بدون مجوز اعلان، یادآورها نمایش داده نمی‌شوند و انجام‌شده حساب نمی‌شوند.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else {
                recoverMissedAlarms()
            }
        }

    private var openAddReminderTrigger by mutableStateOf(false)
    private var openConverterTrigger by mutableStateOf(false)
    private var openTodayTrigger by mutableStateOf(false)

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        com.soh3il.calendar.widget.WidgetNavigation.parse(intent)?.let { widgetDestination = it }
        notificationReminderId = intent.getLongExtra(com.soh3il.core.scheduler.NotificationHelper.EXTRA_REMINDER_ID, 0L).takeIf { it > 0 }
        if (notificationReminderId != null) widgetDestination = null
        if (
            intent.action == com.soh3il.core.scheduler.PersistentNotificationHelper.ACTION_OPEN_ADD_REMINDER ||
            intent.getBooleanExtra(com.soh3il.core.scheduler.PersistentNotificationHelper.EXTRA_OPEN_ADD_REMINDER, false)
        ) {
            openAddReminderTrigger = true
        }
        when (intent.getStringExtra("extra_action")) {
            "ADD_REMINDER" -> openAddReminderTrigger = true
            "OPEN_TODAY" -> openTodayTrigger = true
            "OPEN_CONVERTER" -> openConverterTrigger = true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val db = Soh3ilDatabase.getInstance(this)
        if (savedInstanceState == null) handleIncomingIntent(intent)
        else {
            @Suppress("DEPRECATION")
            val pending = savedInstanceState.getParcelable<Intent>("pending_widget_navigation")
            widgetDestination = com.soh3il.calendar.widget.WidgetNavigation.parse(pending)
            notificationReminderId = savedInstanceState.getLong("pending_notification_reminder", 0L).takeIf { it > 0 }
            openConverterTrigger = savedInstanceState.getBoolean("pending_converter")
        }

        setContent {
            val context = LocalContext.current
            var currentTheme by remember { mutableStateOf(UserPreferences.getTheme(context)) }
            var isAmoled by remember { mutableStateOf(UserPreferences.isAmoled(context)) }

            Soh3ilTheme(themePreference = currentTheme, isAmoled = isAmoled) {
                val coroutineScope = rememberCoroutineScope()
                var currentTab by rememberSaveable { mutableStateOf(0) } // 0=Calendar, 1=Settings
                var showAddSheet by rememberSaveable { mutableStateOf(false) }
                var savingReminder by remember { mutableStateOf(false) }
                val syncCalendar = rememberCalendarSyncAction(context, db) {
                    requestCalendarPermissions.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                }
                var showWidgetPicker by rememberSaveable { mutableStateOf(false) }
                var editingReminderId by rememberSaveable { mutableStateOf<Long?>(null) }
                var selectedCalendarJdn by rememberSaveable { mutableStateOf(com.soh3il.core.jalali.JalaliDate.now().toJdn()) }
                var navigationDateJdn by rememberSaveable { mutableStateOf<Long?>(null) }
                var reminderSession by rememberSaveable { mutableStateOf(0) }
                val selectedCalendarDate = com.soh3il.core.jalali.JalaliDate.fromJdn(selectedCalendarJdn)
                val tabState = rememberSaveableStateHolder()

                val remindersState by db.reminderDao().getAllReminders().collectAsState(initial = null)
                val reminders = remember(remindersState) { remindersState.orEmpty().map { it.toDomain() } }
                val editingReminder = reminders.firstOrNull { it.id == editingReminderId }
                ResolveReminderEntry(notificationReminderId, reminders.takeIf { remindersState != null }) {
                    widgetDestination = it
                    notificationReminderId = null
                }
                com.soh3il.calendar.widget.WidgetNavigationPrompt(
                    destination = widgetDestination, hasDraft = showAddSheet,
                    ready = widgetDestination?.reminderId == null || remindersState != null,
                    onCancel = { widgetDestination = null },
                    onProceed = { destination ->
                        reminderSession++
                        currentTab = 0
                        selectedCalendarJdn = destination.date.toJdn()
                        navigationDateJdn = destination.date.toJdn()
                        val target = reminders.firstOrNull { it.id == destination.reminderId }
                        editingReminderId = target?.id
                        showAddSheet = destination.add || target != null
                        if (destination.reminderId != null && target == null) {
                            android.widget.Toast.makeText(context, "این یادآور دیگر در برنامه نیست", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        widgetDestination = null
                    }
                )
                var birthdaySolarYear by rememberSaveable { mutableStateOf(com.soh3il.core.jalali.JalaliDate.now().year) }
                val contactsPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
                val contactBirthdaysEnabled = UserPreferences.isContactBirthdaysEnabled(context)
                val contactBirthdays = rememberContactBirthdays(
                    context, birthdaySolarYear, contactBirthdaysEnabled,
                    contactsPermissionGranted, integrationRefreshVersion
                )

                val currentVersion = remember { installedVersion(context) }

                // Update persistent notification, Home Screen Widget, and Live Launcher Icon
                androidx.compose.runtime.LaunchedEffect(remindersState) {
                    if (remindersState == null) com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(context)
                    else com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(context, reminders)
                    com.soh3il.core.scheduler.LiveIconHelper.updateLiveIcon(context)
                    com.soh3il.core.scheduler.MidnightScheduler.scheduleMidnightAlarm(context)
                }

                // Check if opened from notification or app shortcut
                androidx.compose.runtime.LaunchedEffect(openAddReminderTrigger, openTodayTrigger, openConverterTrigger) {
                    if (openAddReminderTrigger) {
                        currentTab = 0
                        editingReminderId = null
                        selectedCalendarJdn = com.soh3il.core.jalali.JalaliDate.now().toJdn()
                        showAddSheet = true
                        openAddReminderTrigger = false
                    } else if (openTodayTrigger) {
                        currentTab = 0
                        selectedCalendarJdn = com.soh3il.core.jalali.JalaliDate.now().toJdn()
                        navigationDateJdn = selectedCalendarJdn
                        openTodayTrigger = false
                    } else if (openConverterTrigger) {
                        currentTab = 1
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        AppNavigationBar(currentTab, onTabSelected = { currentTab = it })
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
                        tabState.SaveableStateProvider(currentTab) {
                            when (currentTab) {
                                0 -> CalendarScreen(
                                    requestedJdn = navigationDateJdn,
                                    onNavigationHandled = { navigationDateJdn = null },
                                    onDisplayedYearChanged = { birthdaySolarYear = it },
                                    reminders = reminders,
                                    additionalOccasions = contactBirthdays,
                                    onAddReminderClick = { date ->
                                        selectedCalendarJdn = date.toJdn()
                                        editingReminderId = null
                                        showAddSheet = true
                                    },
                                    onReminderClick = {
                                        editingReminderId = it.id
                                        showAddSheet = true
                                    }
                                )
                                1 -> SettingsScreen(
                                    openDateConverterRequested = openConverterTrigger,
                                    onDateConverterRequestHandled = { openConverterTrigger = false },
                                    onWidgetsClick = { showWidgetPicker = true },
                                    currentTheme = currentTheme,
                                    onThemeSelected = {
                                        currentTheme = it
                                        UserPreferences.setTheme(context, it)
                                        com.soh3il.calendar.widget.CalendarAppWidgetProvider.requestUpdate(this@MainActivity)
                                    },
                                    isAmoled = isAmoled,
                                    onAmoledToggled = {
                                        isAmoled = it
                                        UserPreferences.setAmoled(context, it)
                                        com.soh3il.calendar.widget.CalendarAppWidgetProvider.requestUpdate(this@MainActivity)
                                    },
                                    currentVersionName = currentVersion,
                                    onPersistentNotificationToggled = { enabled ->
                                        if (enabled) {
                                            com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(this@MainActivity, reminders)
                                        } else {
                                            com.soh3il.core.scheduler.PersistentNotificationHelper.cancelNotification(this@MainActivity)
                                        }
                                        com.soh3il.calendar.widget.CalendarAppWidgetProvider.requestUpdate(this@MainActivity)
                                    },
                                    onExportIcsClick = {
                                        try {
                                            com.soh3il.core.scheduler.IcsHelper.shareIcsFile(this@MainActivity, reminders)
                                        } catch (error: Exception) {
                                            android.widget.Toast.makeText(this@MainActivity, "فایل خروجی ساخته نشد؛ اطلاعات یادآورها را بررسی کنید.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    onImportIcsUri = { uri ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val result = try { ReminderMutations.importIcs(context, db, uri) }
                                            catch (error: CancellationException) { throw error }
                                            catch (error: Exception) { Result.failure(error) }
                                            withContext(Dispatchers.Main) {
                                                val message = result.fold(
                                                    onSuccess = { (total, added) ->
                                                        if (total > added) "${added.toPersianDigits()} یادآور جدید وارد شد (${(total - added).toPersianDigits()} مورد تکراری نادیده گرفته شد)"
                                                        else "${added.toPersianDigits()} یادآور وارد شد"
                                                    },
                                                    onFailure = { error ->
                                                        if (error is IllegalArgumentException) error.message ?: "فایل تقویم معتبر نیست"
                                                        else "ورود کامل نشد؛ ممکن است بعضی موارد قبلاً ثبت شده باشند. فایل و یادآورها را بررسی کنید."
                                                    }
                                                )
                                                android.widget.Toast.makeText(this@MainActivity, message, android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    onSyncCalendarClick = syncCalendar,
                                    contactBirthdaysEnabled = contactBirthdaysEnabled,
                                    contactsPermissionGranted = contactsPermissionGranted,
                                    onContactBirthdaysToggled = { enabled ->
                                        if (enabled && !contactsPermissionGranted) {
                                            requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
                                        } else {
                                            UserPreferences.setContactBirthdaysEnabled(context, enabled)
                                            integrationRefreshVersion++
                                        }
                                    }
                                )
                            }
                        }

                        if (showWidgetPicker) {
                            com.soh3il.calendar.widget.WidgetPinDialog { showWidgetPicker = false }
                        }

                        if (showAddSheet && (editingReminderId == null || editingReminder != null)) {
                            val initialDate = editingReminder?.let {
                                com.soh3il.core.jalali.JalaliDate(it.jalaliYear, it.jalaliMonth, it.jalaliDay)
                            } ?: selectedCalendarDate

                            androidx.compose.runtime.key(reminderSession) { AddReminderSheet(
                                initialDate = initialDate,
                                existingReminder = editingReminder,
                                onDismiss = {
                                    showAddSheet = false
                                    editingReminderId = null
                                },
                                onSaveReminder = { reminder ->
                                    if (!savingReminder) {
                                        savingReminder = true
                                        coroutineScope.launch {
                                            try {
                                                withContext(Dispatchers.IO) { ReminderMutations.save(context, db, reminder) }
                                                showAddSheet = false
                                                editingReminderId = null
                                            } catch (error: CancellationException) {
                                                throw error
                                            } catch (error: Exception) {
                                                android.widget.Toast.makeText(context, "ذخیره یا زمان‌بندی انجام نشد؛ دوباره تلاش کنید.", android.widget.Toast.LENGTH_LONG).show()
                                            } finally { savingReminder = false }
                                        }
                                    }
                                },
                                isSaving = savingReminder,
                                onDeleteReminder = { reminder ->
                                    if (!savingReminder) {
                                        savingReminder = true
                                        coroutineScope.launch {
                                            try {
                                                withContext(Dispatchers.IO) { ReminderMutations.delete(context, db, reminder) }
                                                showAddSheet = false
                                                editingReminderId = null
                                            } catch (error: CancellationException) { throw error }
                                            catch (error: Exception) {
                                                android.widget.Toast.makeText(context, "حذف کامل نشد؛ یادآور را بررسی کنید.", android.widget.Toast.LENGTH_LONG).show()
                                            } finally { savingReminder = false }
                                        }
                                    }
                                }
                            ) }
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        notificationReminderId?.let { outState.putLong("pending_notification_reminder", it) }
        outState.putBoolean("pending_converter", openConverterTrigger)
        widgetDestination?.let {
            outState.putParcelable("pending_widget_navigation", com.soh3il.calendar.widget.WidgetNavigation.intent(this, 0, it))
        }
        super.onSaveInstanceState(outState)
    }

    private fun recoverMissedAlarms() {
        lifecycleScope.launch(Dispatchers.IO) {
            try { com.soh3il.core.scheduler.AlarmScheduler.recoverMissed(applicationContext) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { android.util.Log.e("AlarmRecovery", "Alarm recovery failed: ${error.javaClass.simpleName}") }
        }
    }

    override fun onResume() {
        super.onResume()
        integrationRefreshVersion++
        recoverMissedAlarms()
        com.soh3il.core.scheduler.LiveIconHelper.updateLiveIcon(this)
        com.soh3il.core.scheduler.MidnightScheduler.scheduleMidnightAlarm(this)
        com.soh3il.calendar.widget.CalendarAppWidgetProvider.requestUpdate(this)
        if (com.soh3il.core.database.preferences.UserPreferences.isPersistentNotificationEnabled(this)) {
            com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(this)
        }
    }
}
