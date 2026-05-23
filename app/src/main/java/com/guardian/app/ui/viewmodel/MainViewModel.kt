package com.guardian.app.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.app.data.ThreatEventRepository
import com.guardian.app.lock.LockEngine
import com.guardian.app.lock.LockState
import com.guardian.app.service.LocalDnsVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore("guardian_settings")

data class StatsUiState(
    val weeklyCounts: List<Int> = List(7) { 0 },
    val weekTotal: Int = 0,
    val cleanStreakDays: Int = 0
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lockEngine: LockEngine,
    private val threatEventRepository: ThreatEventRepository
) : ViewModel() {

    private companion object {
        val KEY_VPN_ACTIVE = booleanPreferencesKey("vpn_active")
        val KEY_ACCESS_ACTIVE = booleanPreferencesKey("access_active")
        val KEY_QURAN_PKG = stringPreferencesKey("quran_pkg")
        val KEY_IMG_ENABLED = booleanPreferencesKey("img_enabled")
        val KEY_LOCK_HOURS_MIN = intPreferencesKey("lock_h_min")
        val KEY_LOCK_HOURS_MAX = intPreferencesKey("lock_h_max")
    }

    val lockState: StateFlow<LockState> = lockEngine.lockStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LockState(false, 0, 0L, ""))

    private val settings = context.settingsStore.data

    val vpnActive: StateFlow<Boolean> = settings
        .map { it[KEY_VPN_ACTIVE] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val accessibilityActive: StateFlow<Boolean> = settings
        .map { it[KEY_ACCESS_ACTIVE] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val quranPackage: StateFlow<String> = settings
        .map { it[KEY_QURAN_PKG] ?: LockEngine.DEFAULT_QURAN_PACKAGE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LockEngine.DEFAULT_QURAN_PACKAGE)

    val imageClassificationEnabled: StateFlow<Boolean> = settings
        .map { it[KEY_IMG_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val lockHoursMin: StateFlow<Int> = settings
        .map { it[KEY_LOCK_HOURS_MIN] ?: 5 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    val lockHoursMax: StateFlow<Int> = settings
        .map { it[KEY_LOCK_HOURS_MAX] ?: 24 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 24)

    private val _statsUiState = MutableStateFlow(StatsUiState())
    val statsUiState: StateFlow<StatsUiState> = _statsUiState

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _statsUiState.value = StatsUiState(
                weeklyCounts = threatEventRepository.weeklyDailyCounts(),
                weekTotal = threatEventRepository.totalThisWeek(),
                cleanStreakDays = threatEventRepository.cleanDaysStreak()
            )
        }
    }

    fun startVpn(context: Context) {
        LocalDnsVpnService.start(context, LocalDnsVpnService.MODE_DNS_ONLY)
        viewModelScope.launch {
            context.settingsStore.edit { it[KEY_VPN_ACTIVE] = true }
        }
    }

    fun stopVpn(context: Context) {
        context.stopService(Intent(context, LocalDnsVpnService::class.java))
        viewModelScope.launch {
            context.settingsStore.edit { it[KEY_VPN_ACTIVE] = false }
        }
    }

    fun setQuranPackage(pkg: String) = viewModelScope.launch {
        context.settingsStore.edit { it[KEY_QURAN_PKG] = pkg }
    }

    fun setImageClassification(enabled: Boolean) = viewModelScope.launch {
        context.settingsStore.edit { it[KEY_IMG_ENABLED] = enabled }
    }

    fun setLockHoursRange(min: Int, max: Int) = viewModelScope.launch {
        context.settingsStore.edit {
            it[KEY_LOCK_HOURS_MIN] = min
            it[KEY_LOCK_HOURS_MAX] = maxOf(min + 1, max)
        }
    }

    fun markAccessibilityActive(active: Boolean) = viewModelScope.launch {
        context.settingsStore.edit { it[KEY_ACCESS_ACTIVE] = active }
    }
}
