package com.example.amukisenseapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amukisenseapp.data.model.Registro
import com.example.amukisenseapp.data.repository.RegistroRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MisRegistrosViewModel : ViewModel() {
    private val repository = RegistroRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _registros = MutableStateFlow<List<Registro>>(emptyList())
    val registros: StateFlow<List<Registro>> = _registros.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _tipoActual = MutableStateFlow("manual")
    val tipoActual: StateFlow<String> = _tipoActual.asStateFlow()
    
    private var registrosOriginales = listOf<Registro>()

    init {
        cargarRegistros("manual")
    }

    fun cargarRegistrosPorTipo(tipo: String) {
        _tipoActual.value = tipo
        cargarRegistros(tipo)
    }

    private fun cargarRegistros(tipo: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = auth.currentUser?.uid
            
            if (userId != null) {
                val result = repository.getRegistrosUsuario(userId, tipo)
                result.onSuccess { listaRegistros ->
                    registrosOriginales = listaRegistros
                    _registros.value = listaRegistros
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            }
            
            _isLoading.value = false
        }
    }

    fun ordenarPorFecha(ascendente: Boolean) {
        _registros.value = if (ascendente) {
            _registros.value.sortedBy { it.fecha?.toDate()?.time ?: 0L }
        } else {
            _registros.value.sortedByDescending { it.fecha?.toDate()?.time ?: 0L }
        }
    }

    fun ordenarPorDb(ascendente: Boolean) {
        _registros.value = if (ascendente) {
            _registros.value.sortedBy { it.db }
        } else {
            _registros.value.sortedByDescending { it.db }
        }
    }

    fun eliminarRegistro(registroId: String) {
        viewModelScope.launch {
            val result = repository.deleteRegistro(registroId)
            result.onSuccess {
                // Recargar registros después de eliminar
                cargarRegistros(_tipoActual.value)
            }.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }
}
