package com.guardian.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.guardian.app.ui.viewmodel.StatsUiState

@Composable
fun StatsScreen(
    stats: StatsUiState,
    modifier: Modifier = Modifier
) {
    val maxBar = (stats.weeklyCounts.maxOrNull() ?: 1).coerceAtLeast(1)
    val dayLabels = listOf("أحد", "إثن", "ثلا", "أرب", "خمي", "جمع", "سبت")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OffWhite)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "الإحصائيات",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = OliveGreenDark
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigStat(
                value = stats.weekTotal.toString(),
                label = "كشف الأسبوع",
                icon = Icons.Default.Search,
                modifier = Modifier.weight(1f)
            )
            BigStat(
                value = stats.cleanStreakDays.toString(),
                label = "أيام بلا انتهاك",
                icon = Icons.Default.Favorite,
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, null, tint = OliveGreen)
                    Spacer(Modifier.padding(4.dp))
                    Text("نشاط الأسبوع", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    stats.weeklyCounts.forEachIndexed { index, count ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val barHeight = ((count.toFloat() / maxBar) * 100).dp.coerceAtLeast(4.dp)
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(barHeight)
                                    .background(
                                        if (count > 0) OliveGreen else Color(0xFFE0E0E0),
                                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(count.toString(), fontSize = 10.sp, color = Color.Gray)
                            Text(dayLabels.getOrElse(index) { "" }, fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = OliveGreen)
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OliveGreenDark)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
