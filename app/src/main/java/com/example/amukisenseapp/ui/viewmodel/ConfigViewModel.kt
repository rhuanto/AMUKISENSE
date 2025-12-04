package com.example.amukisenseapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amukisenseapp.data.model.Config
import com.example.amukisenseapp.data.repository.RegistroRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar la configuración del usuario
 * Para usar RegistroRepository con conexión real a Firebase
 */
class ConfigViewModel(
    private val repository: RegistroRepository = RegistroRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _config = MutableStateFlow(Config())
    val config: StateFlow<Config> = _config

    init {
        loadConfig()
    }

    /**
     * Cargar configuración del usuario desde Firebase
     */
    private fun loadConfig() {
        val uid = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            val result = repository.getUsuario(uid)
            
            if (result.isSuccess) {
                result.getOrNull()?.let { usuario ->
                    _config.value = usuario.config
                }
            }
        }
    }

    /**
     * Actualizar configuración del usuario en Firebase
     */
    fun updateConfig(newConfig: Config) {
        val uid = auth.currentUser?.uid ?: return
        _config.value = newConfig
        
        viewModelScope.launch {
            repository.updateConfigUsuario(uid, newConfig)
        }
    }
}
