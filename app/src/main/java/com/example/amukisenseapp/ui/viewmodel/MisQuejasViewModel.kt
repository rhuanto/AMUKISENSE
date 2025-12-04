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

class MisQuejasViewModel : ViewModel() {
    private val repository = RegistroRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _quejas = MutableStateFlow<List<Registro>>(emptyList())
    val quejas: StateFlow<List<Registro>> = _quejas.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var quejasOriginales = listOf<Registro>()

    init {
        cargarQuejas()
    }

    private fun cargarQuejas() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = auth.currentUser?.uid
            
            if (userId != null) {
                val result = repository.getRegistrosUsuario(userId, "queja")
                result.onSuccess { listaQuejas ->
                    quejasOriginales = listaQuejas
                    _quejas.value = listaQuejas
                }.onFailure { exception ->
                    _error.value = exception.message
                }
            }
            
            _isLoading.value = false
        }
    }

    fun ordenarPorFecha(ascendente: Boolean) {
        _quejas.value = if (ascendente) {
            _quejas.value.sortedBy { it.fecha?.toDate()?.time ?: 0L }
        } else {
            _quejas.value.sortedByDescending { it.fecha?.toDate()?.time ?: 0L }
        }
    }

    fun ordenarPorDb(ascendente: Boolean) {
        _quejas.value = if (ascendente) {
            _quejas.value.sortedBy { it.db }
        } else {
            _quejas.value.sortedByDescending { it.db }
        }
    }

    fun eliminarQueja(quejaId: String) {
        viewModelScope.launch {
            val result = repository.deleteRegistro(quejaId)
            result.onSuccess {
                // Recargar quejas después de eliminar
                cargarQuejas()
            }.onFailure { exception ->
                _error.value = exception.message
            }
        }
    }
}
