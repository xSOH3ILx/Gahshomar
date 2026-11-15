package com.soh3il.calendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun AppNavigationBar(currentTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    imageVector = if (currentTab == 0) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                    contentDescription = "تقویم"
                )
            },
            label = { Text("تقویم") }
        )
        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    imageVector = if (currentTab == 1) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "تنظیمات"
                )
            },
            label = { Text("تنظیمات") }
        )
    }
}
