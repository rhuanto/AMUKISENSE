package com.example.amukisenseapp.ui.viewmodel

import android.location.Location
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amukisenseapp.data.model.Coordenadas
import com.example.amukisenseapp.data.repository.RegistroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegistroViewModel(
    private val repository: RegistroRepository = RegistroRepository()
) : ViewModel() {

    private val _registroState = MutableStateFlow<RegistroState>(RegistroState.Initial)
    val registroState: StateFlow<RegistroState> = _registroState

    private val _currentDb = MutableStateFlow(0.0)
    val currentDb: StateFlow<Double> = _currentDb

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _registroToEdit = MutableStateFlow<com.example.amukisenseapp.data.model.Registro?>(null)
    val registroToEdit: StateFlow<com.example.amukisenseapp.data.model.Registro?> = _registroToEdit

    fun updateDb(db: Double) {
        _currentDb.value = db
    }

    fun loadRegistro(registroId: String) {
        viewModelScope.launch {
            _registroState.value = RegistroState.Loading
            val result = repository.getRegistro(registroId)
            result.onSuccess { registro ->
                _registroToEdit.value = registro
                registro?.let {
                    _currentDb.value = it.db
                }
                _registroState.value = RegistroState.Initial
            }.onFailure { exception ->
                _registroState.value = RegistroState.Error(exception.message ?: "Error al cargar registro")
            }
        }
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = location
    }

    fun createRegistroManual(
        db: Double,
        tipoLugar: String,
        sensacion: String,
        comentario: String,
        direccion: String,
        coordenadas: Coordenadas
    ) {
        viewModelScope.launch {
            _registroState.value = RegistroState.Loading
            val result = repository.createRegistroManual(
                db = db,
                tipoLugar = tipoLugar,
                sensacion = sensacion,
                comentario = comentario,
                direccion = direccion,
                coordenadas = coordenadas
            )
            _registroState.value = if (result.isSuccess) {
                RegistroState.Success
            } else {
                RegistroState.Error(result.exceptionOrNull()?.message ?: "Error al guardar")
            }
        }
    }

    fun updateRegistroManual(
        registroId: String,
        db: Double,
        tipoLugar: String,
        sensacion: String,
        comentario: String,
        direccion: String,
        coordenadas: Coordenadas
    ) {
        viewModelScope.launch {
            _registroState.value = RegistroState.Loading
            val campos = mutableMapOf<String, Any>(
                "db" to db,
                "tipo_lugar" to tipoLugar,
                "sensacion" to sensacion,
                "comentario" to comentario,
                "direccion" to direccion,
                "coordenadas" to mapOf<String, Double>(
                    "lat" to coordenadas.lat,
                    "lng" to coordenadas.lng
                )
            )
            val result = repository.updateRegistro(registroId, campos)
            _registroState.value = if (result.isSuccess) {
                RegistroState.Success
            } else {
                RegistroState.Error(result.exceptionOrNull()?.message ?: "Error al actualizar")
            }
        }
    }

    fun createRegistroQueja(
        db: Double,
        origenRuido: String,
        impacto: String,
        sensacion: String,
        comentario: String,
        direccion: String,
        coordenadas: Coordenadas
    ) {
        viewModelScope.launch {
            _registroState.value = RegistroState.Loading
            val result = repository.createRegistroQueja(
                db = db,
                origenRuido = origenRuido,
                impacto = impacto,
                sensacion = sensacion,
                comentario = comentario,
                direccion = direccion,
                coordenadas = coordenadas
            )
            _registroState.value = if (result.isSuccess) {
                RegistroState.Success
            } else {
                RegistroState.Error(result.exceptionOrNull()?.message ?: "Error al guardar")
            }
        }
    }

    fun updateRegistroQueja(
        registroId: String,
        db: Double,
        origenRuido: String,
        impacto: String,
        sensacion: String,
        comentario: String,
        direccion: String,
        coordenadas: Coordenadas
    ) {
        viewModelScope.launch {
            _registroState.value = RegistroState.Loading
            val campos = mutableMapOf<String, Any>(
                "db" to db,
                "origen_ruido" to origenRuido,
                "impacto" to impacto,
                "sensacion" to sensacion,
                "comentario" to comentario,
                "direccion" to direccion,
                "coordenadas" to mapOf<String, Double>(
                    "lat" to coordenadas.lat,
                    "lng" to coordenadas.lng
                )
            )
            val result = repository.updateRegistro(registroId, campos)
            _registroState.value = if (result.isSuccess) {
                RegistroState.Success
            } else {
                RegistroState.Error(result.exceptionOrNull()?.message ?: "Error al actualizar")
            }
        }
    }

    fun createRegistroCaptura(
        db: Double,
        sensacion: String,
        direccion: String,
        coordenadas: Coordenadas,
        imageUri: Uri
    ) {
        viewModelScope.launch {
            _registroState.value = RegistroState.Loading
            val result = repository.createRegistroCaptura(
                db = db,
                sensacion = sensacion,
                direccion = direccion,
                coordenadas = coordenadas,
                imageUri = imageUri
            )
            _registroState.value = if (result.isSuccess) {
                RegistroState.Success
            } else {
                RegistroState.Error(result.exceptionOrNull()?.message ?: "Error al guardar")
            }
        }
    }

    fun resetState() {
        _registroState.value = RegistroState.Initial
    }

    sealed class RegistroState {
        object Initial : RegistroState()
        object Loading : RegistroState()
        object Success : RegistroState()
        data class Error(val message: String) : RegistroState()
    }
}
