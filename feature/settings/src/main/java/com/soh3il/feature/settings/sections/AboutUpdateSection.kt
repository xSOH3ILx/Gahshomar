package com.soh3il.feature.settings.sections

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.updater.UpdateChecker
import com.soh3il.core.updater.UpdateInfo
import com.soh3il.feature.settings.components.SettingsSectionHeader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val OFFICIAL_REPO_URL = "https://github.com/xSOH3ILx/Gahshomar"

@Composable
fun AboutUpdateSection(
    context: Context,
    currentVersionName: String
) {
    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateCheckMessage by remember { mutableStateOf<String?>(null) }

    SettingsSectionHeader(
        title = "درباره برنامه",
        subtitle = "معرفی گاه‌شمار خورشیدی، توسعه‌دهنده و بررسی به‌روزرسانی",
        icon = Icons.Default.Info
    )
    Spacer(modifier = Modifier.height(6.dp))

    FarsiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "گاه‌شمار خورشیدی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "نسخه ${currentVersionName.toPersianDigits()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "گاه‌شمار خورشیدی یک تقویم و دستیار یادآوری دقیق و تمام‌بومی برای اندروید است که با موتور زمان‌بندی جلالی بدون خطای ماه‌های ۲۹ تا ۳۱ روزه، اعلان دائمی تاریخ در نوار وضعیت و صفحه‌قفل، شمارش معکوس رویدادها، ویجت‌های صفحه اصلی و مبدل سه‌گانه تاریخ طراحی شده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "توسعه‌دهنده",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "سهیل وطن‌دوست (Soheil VatanDoust)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "تمامی حقوق این برنامه محفوظ است. کپی یا استفاده مستقل از کدها مجاز نیست؛ علاقه‌مندان می‌توانند برای بهبود و مشارکت، پیشنهادها و Pull Request خود را مستقیماً در مخزن رسمی پروژه ثبت کنند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isCheckingUpdate = true
                        updateCheckMessage = null
                        updateInfo = null
                        try {
                            val result = UpdateChecker.checkForUpdates(currentVersionName)
                            updateInfo = result
                            updateCheckMessage = if (result.hasUpdate) {
                                "نسخه جدید ${result.latestVersionName.toPersianDigits()} در صفحه انتشار رسمی موجود است."
                            } else {
                                "شما از آخرین نسخه گاه‌شمار خورشیدی استفاده می‌کنید."
                            }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            updateCheckMessage = "بررسی نسخه ممکن نشد. اتصال اینترنت را بررسی کنید و دوباره تلاش کنید."
                        } finally {
                            isCheckingUpdate = false
                        }
                    }
                },
                enabled = !isCheckingUpdate,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("در حال بررسی نسخه...", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("بررسی نسخه جدید", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { UpdateChecker.openReleasePage(context, OFFICIAL_REPO_URL) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("مشارکت و مخزن رسمی گیتهاب", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }

            updateCheckMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (updateInfo?.hasUpdate == true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            updateInfo?.takeIf { it.hasUpdate }?.let { info ->
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "تغییرات نسخه ${info.latestVersionName.toPersianDigits()}:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    Text(
                        text = info.changelog,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { UpdateChecker.openReleasePage(context, info.releaseHtmlUrl) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاهده صفحه انتشار رسمی", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
