package com.example.amukisenseapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amukisenseapp.data.model.Usuario
import com.example.amukisenseapp.data.repository.RegistroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de UI para la pantalla Miembros Unidos
 */
data class MiembrosUnidosUiState(
    val usuarios: List<Usuario> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel para gestionar la lista de miembros de la comunidad
 */
class MiembrosUnidosViewModel : ViewModel() {
    
    private val repository = RegistroRepository()
    
    private val _uiState = MutableStateFlow(MiembrosUnidosUiState())
    val uiState: StateFlow<MiembrosUnidosUiState> = _uiState.asStateFlow()
    
    init {
        cargarMiembros()
    }
    
    /**
     * Carga todos los usuarios registrados desde Firebase
     */
    fun cargarMiembros() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = repository.getAllUsuarios()
            
            result.onSuccess { usuarios ->
                _uiState.value = _uiState.value.copy(
                    usuarios = usuarios,
                    isLoading = false,
                    error = null
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar miembros: ${exception.message}"
                )
            }
        }
    }
}
