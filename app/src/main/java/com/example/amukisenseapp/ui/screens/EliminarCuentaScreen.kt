package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amukisenseapp.data.repository.RegistroRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

/**
 * VISTA 22: Estad. - Config. - Cuenta - Eliminar
 * 
 * Vista de advertencia y confirmación final para eliminar cuenta
 * 
 * REFACTORIZADO: Ahora usa RegistroRepository en lugar de acceso directo a Firestore
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EliminarCuentaScreen(
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val repository = remember { RegistroRepository() }
    val currentUser = auth.currentUser
    val scope = rememberCoroutineScope()
    
    var isDeleting by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Card de advertencia
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icono de advertencia (X roja)
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = Color(0xFFF44336)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Eliminar",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Título
                    Text(
                        text = "ELIMINAR CUENTA",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Mensaje de advertencia
                    Text(
                        text = "Se perderá todos sus datos, registros y configuraciones previas, el proceso es irremediable.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Mensaje de error si falla
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Botón Confirmar - Abre diálogo
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = !isDeleting
            ) {
                Text(
                    "Confirmar",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Diálogo de confirmación final
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "¿Está seguro?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Esta acción no se puede deshacer. Se eliminará permanentemente:\n\n" +
                    "• Su cuenta de usuario\n" +
                    "• Todos sus registros manuales\n" +
                    "• Todos sus registros con captura\n" +
                    "• Todas sus quejas\n" +
                    "• Toda su información personal"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isDeleting = true
                        errorMessage = null
                        
                        scope.launch {
                            try {
                                currentUser?.let { user ->
                                    // 1. Eliminar todos los datos del usuario usando Repository
                                    repository.deleteUsuario(user.uid).fold(
                                        onSuccess = {
                                            android.util.Log.d("EliminarCuentaScreen", "Datos de Firestore eliminados correctamente")
                                        },
                                        onFailure = { e ->
                                            throw e
                                        }
                                    )
                                    
                                    // 2. Eliminar cuenta de Firebase Authentication
                                    user.delete().await()
                                    
                                    // Éxito
                                    isDeleting = false
                                    showSuccessDialog = true
                                }
                            } catch (e: Exception) {
                                isDeleting = false
                                errorMessage = when {
                                    e.message?.contains("requires-recent-login") == true ->
                                        "Por seguridad, debe iniciar sesión nuevamente antes de eliminar su cuenta"
                                    else ->
                                        "Error al eliminar la cuenta: ${e.localizedMessage}"
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Sí, eliminar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
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
                    text = "Cuenta eliminada",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Su cuenta y todos sus datos han sido eliminados exitosamente.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onAccountDeleted()
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
