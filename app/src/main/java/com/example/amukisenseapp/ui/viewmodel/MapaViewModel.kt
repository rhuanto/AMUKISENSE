package com.example.amukisenseapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amukisenseapp.data.model.Registro
import com.example.amukisenseapp.data.repository.RegistroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapaViewModel(
    private val repository: RegistroRepository = RegistroRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapaUiState>(MapaUiState.Loading)
    val uiState: StateFlow<MapaUiState> = _uiState

    init {
        cargarRegistrosGlobales()
    }

    fun cargarRegistrosGlobales(limite: Int = 500) {
        viewModelScope.launch {
            _uiState.value = MapaUiState.Loading
            
            val result = repository.getRegistrosGlobales(limite)
            _uiState.value = if (result.isSuccess) {
                val registros = result.getOrNull() ?: emptyList()
                MapaUiState.Success(registros)
            } else {
                MapaUiState.Error(result.exceptionOrNull()?.message ?: "Error al cargar registros")
            }
        }
    }

    sealed class MapaUiState {
        object Loading : MapaUiState()
        data class Success(val registros: List<Registro>) : MapaUiState()
        data class Error(val message: String) : MapaUiState()
    }
}
