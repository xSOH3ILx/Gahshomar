package com.soh3il.calendar

import android.app.Application
import com.soh3il.core.updater.UpdateChecker
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class UpdateCheckTest {
    @Test fun malformedVersionIsAnErrorInsteadOfAConfirmedNoUpdate() {
        assertThrows(IOException::class.java) {
            runBlocking { UpdateChecker.checkForUpdates("1.4.0") { request ->
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("fixture")
                    .body("{\"tag_name\":\"invalid\",\"html_url\":\"https://github.com/xSOH3ILx/Gahshomar/releases/latest\"}".toResponseBody()).build()
            } }
        }
    }
    @Test fun transportFailureMustNotReportTheInstalledVersionAsLatest() {
        assertThrows(IOException::class.java) {
            runBlocking { UpdateChecker.checkForUpdates("1.4.0") { throw IOException("offline fixture") } }
        }
    }
    @Test fun serverFailureMustNotReportTheInstalledVersionAsLatest() {
        assertThrows(IOException::class.java) {
            runBlocking { UpdateChecker.checkForUpdates("1.4.0") { request ->
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(503).message("fixture").body("".toResponseBody()).build()
            } }
        }
    }
    @Test fun validEqualVersionIsAConfirmedNoUpdateResult() = runBlocking {
        val result = UpdateChecker.checkForUpdates("1.4.0") { request ->
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("fixture")
                .body("{\"tag_name\":\"v1.4.0\",\"html_url\":\"https://github.com/xSOH3ILx/Gahshomar/releases/tag/v1.4.0\",\"body\":\"fixture\"}".toResponseBody()).build()
        }
        assertFalse(result.hasUpdate)
        assertEquals("1.4.0", result.latestVersionName)
    }
}
