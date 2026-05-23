package com.guardian.app.ui

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guardian.app.receiver.GuardianDeviceAdminReceiver
import com.guardian.app.ui.screens.HomeScreen
import com.guardian.app.ui.screens.PermissionStep
import com.guardian.app.ui.screens.SettingsScreen
import com.guardian.app.ui.screens.SetupScreen
import com.guardian.app.ui.screens.StatsScreen
import com.guardian.app.ui.theme.GuardianTheme
import com.guardian.app.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuardianTheme {
                val vm: MainViewModel = hiltViewModel()
                GuardianApp(viewModel = vm, activity = this)
            }
        }
    }
}

sealed class NavTab(val label: String, val icon: ImageVector) {
    object Home : NavTab("الرئيسية", Icons.Default.Home)
    object Stats : NavTab("الإحصائيات", Icons.Default.BarChart)
    object Settings : NavTab("الإعدادات", Icons.Default.Settings)
    object Setup : NavTab("الإعداد", Icons.Default.Build)
}

private val tabs = listOf(NavTab.Home, NavTab.Stats, NavTab.Settings, NavTab.Setup)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianApp(viewModel: MainViewModel, activity: Activity) {
    val lockState by viewModel.lockState.collectAsStateWithLifecycle()
    val vpnActive by viewModel.vpnActive.collectAsStateWithLifecycle()
    val accessActive by viewModel.accessibilityActive.collectAsStateWithLifecycle()
    val quranPkg by viewModel.quranPackage.collectAsStateWithLifecycle()
    val imgEnabled by viewModel.imageClassificationEnabled.collectAsStateWithLifecycle()
    val lockHoursMin by viewModel.lockHoursMin.collectAsStateWithLifecycle()
    val lockHoursMax by viewModel.lockHoursMax.collectAsStateWithLifecycle()
    val stats by viewModel.statsUiState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf<NavTab>(NavTab.Home) }

    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.startVpn(activity)
    }

    val adminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    val setupSteps = buildSetupSteps(activity, vpnActive, accessActive)

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            if (tab == NavTab.Stats) viewModel.refreshStats()
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                NavTab.Home -> HomeScreen(
                    lockState = lockState,
                    vpnActive = vpnActive,
                    accessibilityActive = accessActive,
                    weekDetections = stats.weekTotal,
                    onToggleVpn = {
                        if (!vpnActive) {
                            val intent = VpnService.prepare(activity)
                            if (intent != null) vpnLauncher.launch(intent)
                            else viewModel.startVpn(activity)
                        } else {
                            viewModel.stopVpn(activity)
                        }
                    },
                    onToggleAccessibility = {
                        activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
                NavTab.Stats -> StatsScreen(stats = stats)
                NavTab.Settings -> SettingsScreen(
                    quranPackage = quranPkg,
                    onQuranPackageChange = viewModel::setQuranPackage,
                    imageClassificationEnabled = imgEnabled,
                    onImageClassificationToggle = viewModel::setImageClassification,
                    level3LockHoursMin = lockHoursMin,
                    level3LockHoursMax = lockHoursMax,
                    onLevel3RangeChange = viewModel::setLockHoursRange
                )
                NavTab.Setup -> SetupScreen(
                    steps = setupSteps,
                    onStepAction = { index ->
                        handleSetupStep(index, activity, vpnLauncher, adminLauncher)
                    },
                    onFinish = { selectedTab = NavTab.Home }
                )
            }
        }
    }
}

private fun buildSetupSteps(
    context: Context,
    vpnActive: Boolean,
    accessibilityActive: Boolean
): List<PermissionStep> {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminCn = ComponentName(context, GuardianDeviceAdminReceiver::class.java)
    val photosGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    return listOf(
        PermissionStep(
            icon = Icons.Default.Accessibility,
            title = "إذن إمكانية الوصول",
            description = "يتيح للحارس مراقبة النصوص والكشف عن المحتوى المشبوه",
            buttonLabel = "تفعيل",
            isGranted = accessibilityActive
        ),
        PermissionStep(
            icon = Icons.Default.Wifi,
            title = "إذن VPN",
            description = "يُمكّن تصفية DNS لحجب المواقع المحظورة على مستوى الشبكة",
            buttonLabel = "تفعيل",
            isGranted = vpnActive
        ),
        PermissionStep(
            icon = Icons.Default.AdminPanelSettings,
            title = "مسؤول الجهاز",
            description = "يسمح بقفل الشاشة فوراً عند كشف المحتوى غير اللائق",
            buttonLabel = "منح",
            isGranted = dpm.isAdminActive(adminCn)
        ),
        PermissionStep(
            icon = Icons.Default.SystemUpdate,
            title = "عرض فوق التطبيقات",
            description = "يعرض رسائل التحذير الإسلامية فوق أي تطبيق",
            buttonLabel = "منح",
            isGranted = Settings.canDrawOverlays(context)
        ),
        PermissionStep(
            icon = Icons.Default.PhotoLibrary,
            title = "الوصول إلى الصور",
            description = "يحلّل الصور الجديدة تلقائياً ويحذف المحتوى غير اللائق",
            buttonLabel = "منح",
            isGranted = photosGranted
        )
    )
}

private fun handleSetupStep(
    index: Int,
    activity: Activity,
    vpnLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    adminLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    when (index) {
        0 -> activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        1 -> {
            val intent = VpnService.prepare(activity)
            if (intent != null) vpnLauncher.launch(intent)
        }
        2 -> {
            val adminCn = ComponentName(activity, GuardianDeviceAdminReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminCn)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "يحتاج الحارس هذا الإذن لقفل الشاشة فوراً"
                )
            }
            adminLauncher.launch(intent)
        }
        3 -> {
            val uri = android.net.Uri.parse("package:${activity.packageName}")
            activity.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
            )
        }
        4 -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.requestPermissions(
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                    1001
                )
            } else {
                activity.requestPermissions(
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    1001
                )
            }
        }
    }
}
