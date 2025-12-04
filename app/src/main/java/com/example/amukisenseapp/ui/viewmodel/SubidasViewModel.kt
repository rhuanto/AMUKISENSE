package com.example.amukisenseapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amukisenseapp.data.model.Registro
import com.example.amukisenseapp.data.repository.RegistroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de UI para la pantalla de Subidas
 */
data class SubidasUiState(
    val registros: List<Registro> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel para gestionar las subidas de la comunidad
 */
class SubidasViewModel : ViewModel() {
    
    private val repository = RegistroRepository()
    
    private val _uiState = MutableStateFlow(SubidasUiState())
    val uiState: StateFlow<SubidasUiState> = _uiState.asStateFlow()
    
    init {
        cargarSubidas()
    }
    
    /**
     * Carga todos los registros públicos desde Firebase
     */
    fun cargarSubidas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = repository.getRegistrosGlobales(limite = 100)
            
            result.onSuccess { registros ->
                _uiState.value = _uiState.value.copy(
                    registros = registros,
                    isLoading = false,
                    error = null
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar subidas: ${exception.message}"
                )
            }
        }
    }
}
