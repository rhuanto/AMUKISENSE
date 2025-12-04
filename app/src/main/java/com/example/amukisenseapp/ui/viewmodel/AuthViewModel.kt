package com.example.amukisenseapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amukisenseapp.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        if (authRepository.isUserLoggedIn) {
            _authState.value = AuthState.Authenticated(authRepository.currentUser!!)
        }
    }

    /**
     * Verifica si un correo ya está registrado en Firebase
     * Este es el primer paso del flujo de login/registro
     */
    fun checkEmailExists(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            // Validar formato del correo
            if (!authRepository.isValidEmailFormat(email)) {
                _authState.value = AuthState.Error("Formato de correo inválido")
                return@launch
            }
            
            // Verificar si el correo existe en Firebase Auth
            val result = authRepository.checkIfEmailExists(email)
            _authState.value = if (result.isSuccess) {
                val (exists, methods) = result.getOrNull() ?: (false to emptyList())
                
                if (exists) {
                    // El correo existe - verificar métodos de autenticación
                    if (methods.contains("password")) {
                        // Tiene email/password - mostrar login
                        AuthState.EmailExists(email)
                    } else if (methods.isNotEmpty()) {
                        // Existe pero solo con otro proveedor (Google, etc.)
                        val provider = when {
                            methods.contains("google.com") -> "Google"
                            methods.contains("facebook.com") -> "Facebook"
                            else -> "otro proveedor"
                        }
                        AuthState.Error("Esta cuenta fue creada con $provider. Por favor, inicia sesión usando ese método.")
                    } else {
                        // Existe en Firestore pero sin métodos claros - permitir login
                        AuthState.EmailExists(email)
                    }
                } else {
                    // El correo no existe - mostrar registro
                    AuthState.EmailNotExists(email)
                }
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Error al verificar correo")
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.loginWithEmail(email, password)
            _authState.value = if (result.isSuccess) {
                AuthState.Authenticated(result.getOrNull()!!)
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun registerWithEmail(email: String, password: String, nombreUsuario: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.registerWithEmail(email, password, nombreUsuario)
            _authState.value = if (result.isSuccess) {
                AuthState.Authenticated(result.getOrNull()!!)
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun startGoogleSignIn() {
        // Este método será llamado desde la Activity/Fragment con el intent de Google Sign-In
        _authState.value = AuthState.Loading
    }

    fun handleGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.loginWithGoogle(idToken)
            _authState.value = if (result.isSuccess) {
                AuthState.Authenticated(result.getOrNull()!!)
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            _authState.value = if (result.isSuccess) {
                AuthState.PasswordResetSent
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Error al enviar email")
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _authState.value = AuthState.Initial
    }

    fun clearError() {
        _authState.value = AuthState.Initial
    }

    sealed class AuthState {
        object Initial : AuthState()
        object Loading : AuthState()
        data class Authenticated(val user: FirebaseUser) : AuthState()
        data class Error(val message: String) : AuthState()
        object PasswordResetSent : AuthState()
        data class EmailExists(val email: String) : AuthState()
        data class EmailNotExists(val email: String) : AuthState()
    }
}
