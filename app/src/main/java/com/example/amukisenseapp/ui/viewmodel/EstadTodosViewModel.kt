package com.example.amukisenseapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amukisenseapp.data.repository.RegistroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de UI para la pantalla Todos
 */
data class EstadTodosUiState(
    val totalMiembros: Int = 0,
    val totalSubidas: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel para gestionar las métricas globales de la comunidad
 */
class EstadTodosViewModel : ViewModel() {
    
    private val repository = RegistroRepository()
    
    private val _uiState = MutableStateFlow(EstadTodosUiState())
    val uiState: StateFlow<EstadTodosUiState> = _uiState.asStateFlow()
    
    init {
        cargarMetricas()
    }
    
    /**
     * Carga las métricas globales desde Firebase
     * - total_usuarios: Cantidad de usuarios registrados
     * - total_registros: Cantidad de registros/subidas totales
     */
    fun cargarMetricas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = repository.getMetricasGlobales()
            
            result.onSuccess { metricas ->
                _uiState.value = _uiState.value.copy(
                    totalMiembros = metricas.total_usuarios,
                    totalSubidas = metricas.total_registros,
                    isLoading = false,
                    error = null
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar métricas: ${exception.message}"
                )
            }
        }
    }
}
