package com.guardian.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardian.app.ui.theme.Gold
import com.guardian.app.ui.theme.OffWhite
import com.guardian.app.ui.theme.OliveGreen
import com.guardian.app.ui.theme.OliveGreenDark

@Composable
fun SettingsScreen(
    quranPackage: String,
    onQuranPackageChange: (String) -> Unit,
    imageClassificationEnabled: Boolean,
    onImageClassificationToggle: (Boolean) -> Unit,
    level3LockHoursMin: Int,
    level3LockHoursMax: Int,
    onLevel3RangeChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var quranInput by remember(quranPackage) { mutableStateOf(quranPackage) }
    var showQuranDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OffWhite)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("الإعدادات", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OliveGreenDark)

        SettingsSection("التطبيقات المسموح بها دائماً") {
            SettingsItemText(Icons.Default.MenuBook, "تطبيق القرآن الكريم", quranPackage) {
                showQuranDialog = true
            }
            Text(
                "المكالمات والرسائل القصيرة مسموح بها دائماً.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        SettingsSection("تحليل الصور") {
            SettingsItemToggle(
                Icons.Default.Image,
                "تفعيل كشف الصور",
                "يحلّل الصور ويحذف المحتوى غير اللائق",
                imageClassificationEnabled,
                onImageClassificationToggle
            )
        }

        SettingsSection("مدة القفل — المستوى 3") {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "النطاق: $level3LockHoursMin – $level3LockHoursMax ساعة",
                    fontSize = 13.sp,
                    color = OliveGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text("الحد الأدنى", fontSize = 12.sp, color = Color.Gray)
                Slider(
                    value = level3LockHoursMin.toFloat(),
                    onValueChange = { onLevel3RangeChange(it.toInt(), level3LockHoursMax) },
                    valueRange = 1f..12f,
                    steps = 10,
                    colors = SliderDefaults.colors(thumbColor = OliveGreen, activeTrackColor = OliveGreen)
                )
                Text("الحد الأقصى", fontSize = 12.sp, color = Color.Gray)
                Slider(
                    value = level3LockHoursMax.toFloat(),
                    onValueChange = { onLevel3RangeChange(level3LockHoursMin, it.toInt()) },
                    valueRange = 12f..24f,
                    steps = 11,
                    colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold)
                )
            }
        }

        SettingsSection("حول التطبيق") {
            SettingsItemInfo(Icons.Default.Info, "الإصدار", "1.0.0")
            SettingsItemInfo(Icons.Default.Shield, "نموذج الكشف", "ML Kit + TFLite")
            SettingsItemInfo(Icons.Default.Security, "الحماية", "نشطة")
        }
    }

    if (showQuranDialog) {
        AlertDialog(
            onDismissRequest = { showQuranDialog = false },
            title = { Text("حزمة تطبيق القرآن") },
            text = {
                OutlinedTextField(
                    value = quranInput,
                    onValueChange = { quranInput = it },
                    singleLine = true,
                    label = { Text("com.quran.labs.androidquran") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onQuranPackageChange(quranInput.trim())
                    showQuranDialog = false
                }) { Text("حفظ", color = OliveGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showQuranDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OliveGreen, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp), content = content)
        }
    }
}

@Composable
private fun SettingsItemToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = OliveGreen, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = OliveGreen, checkedTrackColor = Color(0xFFD0E8B8))
        )
    }
}

@Composable
private fun SettingsItemText(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = OliveGreen, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
private fun SettingsItemInfo(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text(value, fontSize = 13.sp, color = Color.Gray)
    }
}
