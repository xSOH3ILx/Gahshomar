package com.soh3il.calendar

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithText
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.entity.ReminderEntity
import com.soh3il.core.model.Reminder
import com.soh3il.core.scheduler.NotificationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter.from
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = [35], application = Application::class, instrumentedPackages = ["com.soh3il"])
class EntryNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var controller: ActivityController<MainActivity>
    private val activity get() = controller.get()

    @Before fun launchActivity() { controller = Robolectric.buildActivity(MainActivity::class.java).setup().visible() }
    @After fun closeActivity() { controller.pause().stop().destroy() }

    private fun deliver(intent: Intent) = compose.runOnUiThread {
        ReflectionHelpers.callInstanceMethod<Void>(activity, "onNewIntent", from(Intent::class.java, intent))
    }

    @Test(timeout = 60_000) fun converterShortcutOpensTheConverterDialog() {
        deliver(Intent().putExtra("extra_action", "OPEN_CONVERTER"))
        assertConverterIsOpen()
        compose.onNodeWithText("بستن").performClick()
        controller.recreate()
        compose.onNode(isDialog()).assertDoesNotExist()
    }

    private fun assertConverterIsOpen() {
        compose.onNode(isDialog() and hasAnyDescendant(hasText("مبدل پیشرفته تاریخ"))).assertIsDisplayed()
    }

    @Test(timeout = 60_000) fun coldConverterShortcutOpensTheConverterDialog() {
        controller.pause().stop().destroy()
        controller = Robolectric.buildActivity(MainActivity::class.java,
            Intent(activity, MainActivity::class.java).putExtra("extra_action", "OPEN_CONVERTER")).setup().visible()
        assertConverterIsOpen()
    }

    @Test(timeout = 60_000) fun openConverterSurvivesActivityRecreation() {
        deliver(Intent().putExtra("extra_action", "OPEN_CONVERTER"))
        assertConverterIsOpen()
        controller.recreate()
        assertConverterIsOpen()
    }

    @Test(timeout = 60_000) fun tappingReminderNotificationOpensThatReminderForEditing() { openReminder(cold = false) }
    @Test(timeout = 60_000) fun coldNotificationTapOpensThatReminderForEditing() { openReminder(cold = true) }

    @Test(timeout = 60_000) fun missingNotificationTargetShowsAnExplanationWithoutOpeningABlankEditor() {
        deliver(Intent().putExtra(NotificationHelper.EXTRA_REMINDER_ID, Long.MAX_VALUE))
        compose.waitUntil(timeoutMillis = 5_000) {
            shadowOf(android.os.Looper.getMainLooper()).idle()
            ShadowToast.getTextOfLatestToast() == "این یادآور دیگر در برنامه نیست"
        }
        org.junit.Assert.assertEquals("این یادآور دیگر در برنامه نیست", ShadowToast.getTextOfLatestToast())
        compose.onNodeWithText("ویرایش یادآور").assertDoesNotExist()
    }

    @Config(qualifiers = "w411dp-h891dp-mdpi")
    @Test(timeout = 60_000) fun notificationNavigationLetsTheUserKeepAnUnsavedDraft() {
        compose.onNodeWithContentDescription("افزودن یادآور").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("عنوان یادآور").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithText("عنوان یادآور").performTextInput("پیش‌نویس محفوظ")
        val id = runBlocking { Soh3ilDatabase.getInstance(activity).reminderDao().insertReminder(ReminderEntity.fromDomain(
            Reminder(title = "target", jalaliYear = 1406, jalaliMonth = 1, jalaliDay = 2, hour = 9, minute = 0))) }
        deliver(Intent().putExtra(NotificationHelper.EXTRA_REMINDER_ID, id))
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("پیش‌نویس باز دارید").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithText("پیش‌نویس باز دارید").assertIsDisplayed()
        compose.onNodeWithText("ماندن در پیش‌نویس").performClick()
        compose.onNodeWithText("عنوان یادآور").assertTextContains("پیش‌نویس محفوظ")
    }

    private fun openReminder(cold: Boolean): Unit = runBlocking {
        val context = activity
        shadowOf(context.application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val id = Soh3ilDatabase.getInstance(context).reminderDao().insertReminder(ReminderEntity.fromDomain(
            Reminder(title = "notification navigation fixture", jalaliYear = 1406, jalaliMonth = 1,
                jalaliDay = 1, hour = 9, minute = 0)))
        NotificationHelper.showReminderNotification(context, id, "fixture", "")
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = shadowOf(manager.activeNotifications.single { it.tag != null }.notification.contentIntent).savedIntent
        if (cold) {
            controller.pause().stop().destroy()
            controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup().visible()
        } else deliver(intent)
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("ویرایش یادآور").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithText("ویرایش یادآور").assertIsDisplayed()
        compose.onNodeWithText("notification navigation fixture").assertIsDisplayed()
    }
}
