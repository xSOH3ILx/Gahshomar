package com.soh3il.core.scheduler

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.jalali.JalaliDate

object LiveIconHelper {

    private const val DEFAULT_ALIAS_NAME = "com.soh3il.calendar.DefaultLauncherAlias"
    private const val ALIAS_NAME_PREFIX = "com.soh3il.calendar.Day"

    /**
     * Updates the launcher app icon dynamically based on user preferences and current day.
     * Uses Android's native activity-alias mechanism to display the Jalali day number.
     * Keeps MainActivity permanently enabled to prevent ActivityNotFoundException on pending intents.
     */
    fun updateLiveIcon(context: Context) {
        try {
            val pm = context.packageManager
            val isEnabled = UserPreferences.isLiveIconEnabled(context)
            val today = JalaliDate.now()
            val currentDay = today.day.coerceIn(1, 31)

            val targetComponent = if (isEnabled) {
                ComponentName(context.packageName, "$ALIAS_NAME_PREFIX$currentDay")
            } else {
                ComponentName(context.packageName, DEFAULT_ALIAS_NAME)
            }

            val targetState = pm.getComponentEnabledSetting(targetComponent)
            val isTargetAlreadyEnabled = if (targetComponent.className == DEFAULT_ALIAS_NAME) {
                targetState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    targetState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            } else {
                targetState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }

            val allAliases = buildList {
                add(ComponentName(context.packageName, DEFAULT_ALIAS_NAME))
                for (day in 1..31) {
                    add(ComponentName(context.packageName, "$ALIAS_NAME_PREFIX$day"))
                }
            }

            // If target is enabled and no other alias is enabled, we are already in sync
            val otherEnabledAliases = allAliases.filter { component ->
                if (component == targetComponent) return@filter false
                val state = pm.getComponentEnabledSetting(component)
                if (component.className == DEFAULT_ALIAS_NAME) {
                    state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                } else {
                    state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                }
            }

            if (isTargetAlreadyEnabled && otherEnabledAliases.isEmpty()) {
                return
            }

            val flags = PackageManager.DONT_KILL_APP

            // 1. Enable target component FIRST so at least one launcher entry is guaranteed to exist
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                flags
            )

            // 2. Disable only components that are currently active
            for (component in allAliases) {
                if (component == targetComponent) continue
                val state = pm.getComponentEnabledSetting(component)
                val shouldDisable = if (component.className == DEFAULT_ALIAS_NAME) {
                    state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                } else {
                    state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                }

                if (shouldDisable) {
                    pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        flags
                    )
                }
            }
        } catch (e: Exception) {
            // Fail gracefully if launcher or package manager has transient issues
            android.util.Log.w("LiveIconHelper", "live icon switch failed", e)
        }
    }

    /**
     * Self-healing check: verifies that exactly one launcher alias is enabled.
     * If 0 or >1 are enabled, immediately heals and resets to valid state.
     */
    fun validateAndHealLauncherAliases(context: Context) {
        // An old day alias may be the only enabled alias, yet still be stale.
        // updateLiveIcon also checks the user's preference and changes aliases enable-first.
        updateLiveIcon(context)
    }
}
