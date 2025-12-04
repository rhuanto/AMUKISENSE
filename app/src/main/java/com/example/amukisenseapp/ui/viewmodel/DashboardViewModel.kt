package com.example.amukisenseapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amukisenseapp.data.model.*
import com.example.amukisenseapp.data.repository.RegistroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de UI para la pantalla de Dashboard
 */
data class DashboardUiState(
    val quejasPorDistrito: List<QuejasPorDistrito> = emptyList(),
    val evolucionTemporal: List<EvolucionTemporal> = emptyList(),
    val topCallesRuidosas: List<CalleRuidosa> = emptyList(),
    val ruidoPorHorario: List<RuidoPorHorario> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel para gestionar el dashboard de estadísticas de ruido
 * Obtiene datos de Firebase y los prepara para visualización en gráficos
 */
class DashboardViewModel : ViewModel() {
    
    private val repository = RegistroRepository()
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    /**
     * Carga todos los datos del dashboard desde Firebase (datos globales)
     */
    fun cargarDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Cargar quejas por distrito (global)
                val quejasResult = repository.getQuejasPorDistrito(limite = 1000)
                
                // Cargar evolución temporal (global - se actualizará con ubicación)
                val evolucionResult = repository.getEvolucionTemporal()
                
                // Cargar top calles ruidosas (global)
                val callesResult = repository.getTopCallesRuidosas(limite = 10)
                
                // Cargar ruido por horario (global - se actualizará con ubicación)
                val horarioResult = repository.getRuidoPorHorario()
                
                _uiState.value = _uiState.value.copy(
                    quejasPorDistrito = quejasResult.getOrNull() ?: emptyList(),
                    evolucionTemporal = evolucionResult.getOrNull() ?: emptyList(),
                    topCallesRuidosas = callesResult.getOrNull() ?: emptyList(),
                    ruidoPorHorario = horarioResult.getOrNull() ?: emptyList(),
                    isLoading = false,
                    error = null
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar datos: ${exception.message}"
                )
            }
        }
    }
    
    /**
     * Carga datos filtrados por la ubicación del usuario (radio de 1 km)
     * @param lat Latitud del usuario
     * @param lng Longitud del usuario
     */
    fun cargarDashboardPorUbicacion(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                // Cargar evolución temporal filtrada por ubicación
                val evolucionResult = repository.getEvolucionTemporalPorUbicacion(lat, lng, 1.0)
                
                // Cargar ruido por horario filtrado por ubicación
                val horarioResult = repository.getRuidoPorHorarioPorUbicacion(lat, lng, 1.0)
                
                _uiState.value = _uiState.value.copy(
                    evolucionTemporal = evolucionResult.getOrNull() ?: emptyList(),
                    ruidoPorHorario = horarioResult.getOrNull() ?: emptyList()
                )
            } catch (exception: Exception) {
                android.util.Log.e("DashboardViewModel", "Error cargando datos por ubicación", exception)
            }
        }
    }
    
    /**
     * Resetea los errores del estado
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
