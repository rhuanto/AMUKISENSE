package com.example.amukisenseapp.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.SignInMethodQueryResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val registroRepository = RegistroRepository()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = currentUser != null

    /**
     * Verifica si un correo electrónico ya está registrado en Firebase Authentication
     * @return Result con Pair(existe: Boolean, métodos: List<String>)
     */
    suspend fun checkIfEmailExists(email: String): Result<Pair<Boolean, List<String>>> {
        return try {
            val normalizedEmail = email.trim().lowercase()
            
            Log.d("AuthRepository", "Checking email: original='$email', normalized='$normalizedEmail'")
            
            // Verificar con fetchSignInMethodsForEmail
            val methods = try {
                auth.fetchSignInMethodsForEmail(normalizedEmail).await()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error fetching sign-in methods", e)
                null
            }
            
            val signInMethods = methods?.signInMethods ?: emptyList()
            val hasAuthMethods = signInMethods.isNotEmpty()
            
            Log.d("AuthRepository", "Auth methods for $normalizedEmail: $signInMethods")
            
            // Si existe en Auth, devolver true con los métodos
            if (hasAuthMethods) {
                Log.d("AuthRepository", "Email exists with methods: $signInMethods")
                return Result.success(Pair(true, signInMethods))
            }
            
            // Verificar en Firestore como respaldo
            val firestoreDoc = try {
                firestore.collection("usuarios")
                    .whereEqualTo("correo", normalizedEmail)
                    .limit(1)
                    .get()
                    .await()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error checking Firestore by email", e)
                null
            }
            
            val existsInFirestore = firestoreDoc != null && !firestoreDoc.isEmpty
            Log.d("AuthRepository", "Firestore check: $existsInFirestore")
            
            // Si existe en Firestore pero no en Auth, devolver true sin métodos específicos
            if (existsInFirestore) {
                return Result.success(Pair(true, emptyList()))
            }
            
            // No existe en ninguno
            Result.success(Pair(false, emptyList()))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error checking email existence", e)
            Result.failure(e)
        }
    }

    /**
     * Valida que el formato del correo sea correcto
     * @return true si el formato es válido
     */
    fun isValidEmailFormat(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        return email.matches(emailRegex)
    }

    // Login con Email y Contraseña
    suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login error: ${e.message}", e)
            
            // Verificar si el error es por método de autenticación incorrecto
            if (e.message?.contains("no user record", ignoreCase = true) == true ||
                e.message?.contains("wrong-password", ignoreCase = true) == true ||
                e.message?.contains("invalid-credential", ignoreCase = true) == true) {
                
                // Verificar qué métodos de autenticación tiene este correo
                try {
                    val methods = auth.fetchSignInMethodsForEmail(email).await()
                    val signInMethods = methods.signInMethods ?: emptyList()
                    
                    if (signInMethods.isNotEmpty() && !signInMethods.contains("password")) {
                        // El correo existe pero con otro proveedor
                        val provider = when {
                            signInMethods.contains("google.com") -> "Google"
                            signInMethods.contains("facebook.com") -> "Facebook"
                            else -> signInMethods.firstOrNull() ?: "otro proveedor"
                        }
                        return Result.failure(Exception("Esta cuenta fue creada con $provider. Por favor, inicia sesión usando ese método."))
                    }
                } catch (methodsError: Exception) {
                    Log.e("AuthRepository", "Error checking sign-in methods", methodsError)
                }
            }
            
            Result.failure(e)
        }
    }

    // Registro con Email y Contraseña
    suspend fun registerWithEmail(
        email: String,
        password: String,
        nombreUsuario: String
    ): Result<FirebaseUser> {
        return try {
            // Primero verificar si el correo ya está registrado
            val existingMethods = auth.fetchSignInMethodsForEmail(email).await()
            if (!existingMethods.signInMethods.isNullOrEmpty()) {
                // El correo ya existe, intentar login
                return loginWithEmail(email, password)
            }
            
            // Crear usuario en Firebase Authentication
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!
            
            // Crear documento de usuario en Firestore usando RegistroRepository
            val createResult = registroRepository.createUsuario(
                uid = user.uid,
                email = email,
                provider = "email",
                nombreUsuario = nombreUsuario
            )
            
            if (createResult.isFailure) {
                // Log del error para debugging
                createResult.exceptionOrNull()?.printStackTrace()
                
                // NO eliminar el usuario de Auth - permitir que se complete el registro
                // El documento de Firestore se creará cuando se reintente
                // return Result.failure(createResult.exceptionOrNull() ?: Exception("Error al crear usuario en Firestore"))
            }
            
            Result.success(user)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // Login con Google
    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!
            
            // Verificar si el usuario ya existe en Firestore
            val usuarioExistente = registroRepository.getUsuario(user.uid)
            
            // Si no existe, crear documento de usuario
            if (usuarioExistente.getOrNull() == null) {
                registroRepository.createUsuario(
                    uid = user.uid,
                    email = user.email ?: "",
                    provider = "google",
                    nombreUsuario = user.displayName ?: "Usuario"
                )
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Recuperar contraseña
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Logout
    fun logout() {
        auth.signOut()
    }

    // Eliminar cuenta
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = currentUser ?: return Result.failure(Exception("No user logged in"))
            
            // Eliminar usuario de Firestore (incluye registros en cascada)
            registroRepository.deleteUsuario(user.uid)
            
            // Eliminar cuenta de Auth
            user.delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
