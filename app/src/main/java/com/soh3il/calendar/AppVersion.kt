package com.soh3il.calendar

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

internal fun installedVersion(context: Context): String = with(context) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        }
    }.getOrNull() ?: "1.0.0"
}
