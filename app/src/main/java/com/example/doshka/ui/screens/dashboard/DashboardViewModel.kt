package com.example.doshka.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doshka.data.local.DataStoreManager
import com.example.doshka.data.remote.api.CycleTimeDto
import com.example.doshka.data.remote.api.DashboardStatsDto
import com.example.doshka.data.remote.api.DoshkaApi
import com.example.doshka.data.remote.api.EfficiencyDto
import com.example.doshka.data.remote.api.VelocityDto
import com.example.doshka.data.remote.api.WorkloadDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val stats: DashboardStatsDto? = null,
    val velocityData: List<VelocityDto> = emptyList(),
    val cycleTimeData: List<CycleTimeDto> = emptyList(),
    val workloadData: List<WorkloadDto> = emptyList(),
    val efficiencyData: EfficiencyDto? = null,
    val error: String? = null,
    val hasTeam: Boolean = false,
    val isExporting: Boolean = false,
    val exportedCsvPath: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val api: DoshkaApi,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Отримуємо team_id користувача
            val teamId = dataStoreManager.teamId.first()

            if (teamId.isNullOrBlank()) {
                // Користувач не має команди
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasTeam = false,
                        stats = DashboardStatsDto(
                            totalTasks = 0,
                            completedTasks = 0,
                            inProgressTasks = 0,
                            overdueTasks = 0,
                            completionRate = 0f,
                            averageCycleTime = null
                        )
                    )
                }
                return@launch
            }

            try {
                // Завантажуємо статистику паралельно
                val statsResponse = api.getDashboardStats(teamId)
                val velocityResponse = api.getVelocity(teamId)
                val cycleTimeResponse = api.getCycleTime(teamId)
                val workloadResponse = api.getWorkload(teamId)
                val efficiencyResponse = api.getEfficiency(teamId)

                if (statsResponse.isSuccessful) {
                    val stats = statsResponse.body()
                    val velocity = if (velocityResponse.isSuccessful) {
                        velocityResponse.body() ?: emptyList()
                    } else {
                        emptyList()
                    }
                    val cycleTime = if (cycleTimeResponse.isSuccessful) {
                        cycleTimeResponse.body() ?: emptyList()
                    } else {
                        emptyList()
                    }
                    val workload = if (workloadResponse.isSuccessful) {
                        workloadResponse.body() ?: emptyList()
                    } else {
                        emptyList()
                    }
                    val efficiency = if (efficiencyResponse.isSuccessful) {
                        efficiencyResponse.body()
                    } else {
                        null
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasTeam = true,
                            stats = stats,
                            velocityData = velocity,
                            cycleTimeData = cycleTime,
                            workloadData = workload,
                            efficiencyData = efficiency
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Помилка завантаження: ${statsResponse.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Помилка з'єднання: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadDashboard()
    }

    /**
     * Експорт задач у CSV формат
     */
    fun exportToCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            val teamId = dataStoreManager.teamId.first()
            if (teamId.isNullOrBlank()) {
                _uiState.update { it.copy(isExporting = false) }
                return@launch
            }

            try {
                val response = api.exportTasksCsv(teamId)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportedCsvPath = response.body()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            error = "Помилка експорту: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        error = "Помилка: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearExport() {
        _uiState.update { it.copy(exportedCsvPath = null) }
    }
}
