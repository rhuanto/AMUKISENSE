package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

/**
 * VISTA 19: Estad. - Config. - Cuenta - Correo
 * 
 * Vista para agregar o actualizar credenciales de correo y contraseña
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorreoPasswordScreen(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var isAlreadyLinked by remember { mutableStateOf(false) }
    
    // Verificar si ya tiene email/password vinculado al cargar
    LaunchedEffect(currentUser) {
        currentUser?.providerData?.forEach { profile ->
            if (profile.providerId == "password") {
                isAlreadyLinked = true
                email = profile.email ?: ""
            }
        }
        
        // Si no está vinculado, sugerir el correo actual de Google
        if (!isAlreadyLinked && currentUser?.email != null) {
            email = currentUser.email ?: ""
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Título
            Text(
                text = if (isAlreadyLinked) "Correo ya Vinculado" else "Vincular Correo y Contraseña",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            if (isAlreadyLinked) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Esta cuenta ya tiene correo y contraseña vinculados.",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Campo Correo
            Text(
                text = "Correo:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isAlreadyLinked,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2900CC),
                    unfocusedBorderColor = Color(0xFF2900CC),
                    disabledBorderColor = Color.Gray,
                    disabledTextColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Campo Contraseña
            Text(
                text = "Contraseña:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isAlreadyLinked,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    if (!isAlreadyLinked) {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2900CC),
                    unfocusedBorderColor = Color(0xFF2900CC),
                    disabledBorderColor = Color.Gray,
                    disabledTextColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Campo Confirmar Contraseña
            Text(
                text = "Confirmar Contraseña:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isAlreadyLinked,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    if (!isAlreadyLinked) {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2900CC),
                    unfocusedBorderColor = Color(0xFF2900CC),
                    disabledBorderColor = Color.Gray,
                    disabledTextColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Nota de privacidad
            Text(
                text = "*Estos datos son solo para uso de crear y gestionar, por ningún motivo se compartirá ni usará para uso comercial.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Mensaje de error
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Botón Vincular
            Button(
                onClick = {
                    // Si ya está vinculado, solo regresar
                    if (isAlreadyLinked) {
                        onNavigateBack()
                        return@Button
                    }
                    
                    // Validaciones
                    if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        errorMessage = "Por favor ingrese un correo válido"
                        return@Button
                    }
                    
                    if (password.length < 6) {
                        errorMessage = "La contraseña debe tener al menos 6 caracteres"
                        return@Button
                    }
                    
                    if (password != confirmPassword) {
                        errorMessage = "Las contraseñas no coinciden"
                        return@Button
                    }
                    
                    isLoading = true
                    errorMessage = null
                    
                    currentUser?.let { user ->
                        // Vincular credenciales de email/password
                        val credential = EmailAuthProvider.getCredential(email, password)
                        
                        user.linkWithCredential(credential)
                            .addOnSuccessListener {
                                isLoading = false
                                showSuccessDialog = true
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = when {
                                    e.message?.contains("email-already-in-use") == true || 
                                    e.message?.contains("already in use") == true -> 
                                        "Este correo ya está registrado en otra cuenta. Por favor use un correo diferente."
                                    e.message?.contains("invalid-email") == true -> 
                                        "El formato del correo no es válido"
                                    e.message?.contains("weak-password") == true -> 
                                        "La contraseña es muy débil. Use al menos 6 caracteres."
                                    e.message?.contains("provider-already-linked") == true ||
                                    e.message?.contains("already been linked") == true -> {
                                        isAlreadyLinked = true
                                        "Esta cuenta ya tiene correo y contraseña vinculados"
                                    }
                                    else -> "Error al vincular: ${e.localizedMessage ?: e.message}"
                                }
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAlreadyLinked) Color.Gray else Color.Black,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        if (isAlreadyLinked) "Regresar" else "Vincular",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "¡Vinculación exitosa!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Tu correo y contraseña han sido vinculados correctamente. Ahora puedes iniciar sesión con cualquiera de los dos métodos.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black
                    )
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}
