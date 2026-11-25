package com.soh3il.calendar

import android.app.Application
import android.content.Context
import java.io.File
import java.time.Instant

/**
 * گزارش کرش محلی: خطاهای کنترل‌نشده آخرین اجرا را در فایل متنی سبک ذخیره می‌کند
 * و در اجرای بعدی برای جلوگیری از تکرار هشدار، لاگ قبلی را بایگانی می‌کند.
 */
object LocalCrashReporter {

    private const val FILE_NAME = "last_crash.log"
    private const val ARCHIVE_FILE_NAME = "previous_crash.log"
    private const val MAX_STACK_CHARS = 8000

    fun install(application: Application) {
        val previous = crashFile(application)
        if (previous.exists()) {
            android.util.Log.w("LocalCrashReporter", "previous run crashed: ${previous.length()} bytes logged")
            val archive = File(application.filesDir, ARCHIVE_FILE_NAME)
            archive.delete()
            if (!previous.renameTo(archive)) {
                previous.delete()
            }
        }
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                crashFile(application).writeText(buildReport(application, thread, throwable))
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    internal fun crashFile(context: Context): File = File(context.filesDir, FILE_NAME)

    internal fun buildReport(context: Context, thread: Thread, throwable: Throwable): String = buildString {
        appendLine("time=${Instant.now()}")
        appendLine("version=${installedVersion(context)}")
        appendLine("thread=${thread.name}")
        appendLine(throwable.stackTraceToString().take(MAX_STACK_CHARS))
    }
}
