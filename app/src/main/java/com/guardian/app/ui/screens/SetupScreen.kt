package com.guardian.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardian.app.ui.theme.OffWhite
import com.guardian.app.ui.theme.OliveGreen
import com.guardian.app.ui.theme.OliveGreenDark

data class PermissionStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val buttonLabel: String,
    val isGranted: Boolean
)

@Composable
fun SetupScreen(
    steps: List<PermissionStep>,
    onStepAction: (Int) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allGranted = steps.all { it.isGranted }
    val granted = steps.count { it.isGranted }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OffWhite)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(OliveGreenDark, OliveGreen)))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🛡", fontSize = 52.sp)
                Spacer(Modifier.height(12.dp))
                Text("إعداد الحارس", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "نحتاج بعض الأذونات لتفعيل الحماية الكاملة",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("التقدم", fontSize = 12.sp, color = Color.Gray)
                Text("$granted / ${steps.size}", fontSize = 12.sp, color = OliveGreen, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { granted.toFloat() / steps.size },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = OliveGreen,
                trackColor = Color(0xFFD0E8B8)
            )
        }

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            steps.forEachIndexed { index, step ->
                PermissionStepCard(step, index + 1) { onStepAction(index) }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onFinish,
            enabled = allGranted,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OliveGreen,
                disabledContainerColor = Color(0xFFCCCCCC)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (allGranted) "تفعيل الحارس" else "أكمل الأذونات أولاً",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun PermissionStepCard(step: PermissionStep, stepNum: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (step.isGranted) Color(0xFFEFF7E8) else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (step.isGranted) 0.dp else 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (step.isGranted) OliveGreen else Color(0xFFE8F0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (step.isGranted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                } else {
                    Text("$stepNum", color = OliveGreenDark, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(step.icon, contentDescription = null, tint = if (step.isGranted) OliveGreen else Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(step.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(step.description, fontSize = 12.sp, color = Color.Gray, lineHeight = 17.sp)
            }
            if (!step.isGranted) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = OliveGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(step.buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text("مفعّل", color = OliveGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
