package com.soh3il.calendar

import android.app.Application
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class LocalCrashReporterTest {

    @Test
    fun installArchivesPreviousCrashAndWritesFormattedReport() {
        val app = RuntimeEnvironment.getApplication()
        val crashFile = LocalCrashReporter.crashFile(app)
        val archiveFile = File(app.filesDir, "previous_crash.log")
        crashFile.delete()
        archiveFile.delete()

        crashFile.writeText("prior crash payload")
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        try {
            LocalCrashReporter.install(app)
            assertFalse(crashFile.exists())
            assertTrue(archiveFile.exists())
            assertTrue(archiveFile.readText().contains("prior crash payload"))

            val report = LocalCrashReporter.buildReport(
                app,
                Thread.currentThread(),
                IllegalStateException("synthetic crash")
            )
            assertTrue(report.contains("version="))
            assertTrue(report.contains("IllegalStateException: synthetic crash"))
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        }
    }
}
