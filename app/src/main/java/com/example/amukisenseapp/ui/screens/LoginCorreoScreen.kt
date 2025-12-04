package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amukisenseapp.ui.viewmodel.AuthViewModel
import androidx.compose.material.icons.filled.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginCorreoScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nombreUsuario by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    // Estados del flujo
    var showPasswordField by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    
    val authState by viewModel.authState.collectAsState()

    // Manejar los estados de autenticación
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthViewModel.AuthState.Authenticated -> {
                // Resetear estado antes de navegar para evitar bucles
                viewModel.clearError()
                onLoginSuccess()
            }
            is AuthViewModel.AuthState.EmailExists -> {
                // El correo existe - mostrar campo para login
                email = state.email
                showPasswordField = true
                isRegistering = false
            }
            is AuthViewModel.AuthState.EmailNotExists -> {
                // El correo no existe - mostrar campos para registro
                email = state.email
                showPasswordField = true
                isRegistering = true
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showPasswordField) {
                            // Volver al paso anterior
                            showPasswordField = false
                            isRegistering = false
                            password = ""
                            nombreUsuario = ""
                            viewModel.clearError()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Título dinámico
            Text(
                text = when {
                    isRegistering -> "Registre su cuenta"
                    showPasswordField -> "Iniciar Sesión"
                    else -> "Autenticación"
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Campo Email (siempre visible)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !showPasswordField // Deshabilitado después de verificar
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Nombre de Usuario (solo para registro)
            if (isRegistering && showPasswordField) {
                OutlinedTextField(
                    value = nombreUsuario,
                    onValueChange = { nombreUsuario = it },
                    label = { Text("Nombre de usuario") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Campo Contraseña (mostrar después de validar email)
            if (showPasswordField) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                "Toggle password"
                            )
                        }
                    },
                    singleLine = true
                )

                // Link Olvidé contraseña (solo en login)
                if (!isRegistering) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "¿Olvidé mi contraseña?",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón principal (Continuar / Login / Registrar)
            Button(
                onClick = {
                    if (!showPasswordField) {
                        // Paso 1: Verificar si el correo existe
                        if (email.isNotBlank()) {
                            viewModel.checkEmailExists(email)
                        }
                    } else {
                        // Paso 2: Login o Registro
                        if (isRegistering) {
                            // Registro
                            if (email.isNotBlank() && password.isNotBlank() && nombreUsuario.isNotBlank()) {
                                viewModel.registerWithEmail(email, password, nombreUsuario)
                            }
                        } else {
                            // Login
                            if (email.isNotBlank() && password.isNotBlank()) {
                                viewModel.loginWithEmail(email, password)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = authState !is AuthViewModel.AuthState.Loading && when {
                    !showPasswordField -> email.isNotBlank()
                    isRegistering -> email.isNotBlank() && password.isNotBlank() && nombreUsuario.isNotBlank()
                    else -> email.isNotBlank() && password.isNotBlank()
                }
            ) {
                if (authState is AuthViewModel.AuthState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = when {
                            !showPasswordField -> "Continuar"
                            isRegistering -> "Registrar"
                            else -> "Iniciar Sesión"
                        },
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Mensaje de error
            if (authState is AuthViewModel.AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (authState as AuthViewModel.AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
            
            // Mensaje informativo
            if (isRegistering && showPasswordField) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "El correo no está registrado. Complete los datos para crear su cuenta.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    // Dialog para recuperar contraseña
    if (showResetDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Recuperar contraseña") },
            text = {
                Column {
                    Text("Ingresa tu correo electrónico:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank()) {
                            viewModel.sendPasswordResetEmail(resetEmail)
                            showResetDialog = false
                        }
                    }
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
