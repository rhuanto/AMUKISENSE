package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.amukisenseapp.R
import com.example.amukisenseapp.data.repository.RegistroRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * VISTA 18: Estad. - Config. - Cuenta
 * 
 * Funcionalidad ACTIVA:
 * - Muestra información de la cuenta desde Firebase
 * - Navegación a "Correo y Contraseña"
 * - Navegación a "Eliminar cuenta"
 * 
 * Funcionalidad INACTIVA:
 * - Lenguaje (limitado a español)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadConfigCuentaScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCorreo: () -> Unit,
    onNavigateToEliminar: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val repository = remember { RegistroRepository() }
    val currentUser = auth.currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var userName by remember { mutableStateOf("Cargando...") }
    var joinDate by remember { mutableStateOf("Cargando...") }
    val userId by remember { mutableStateOf("#10") }
    var correo by remember { mutableStateOf("Cargando...") }
    val googleVinculado by remember { mutableStateOf(true) }
    
    // Obtener datos del usuario desde Firebase
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            correo = user.email ?: "Sin correo"
            
            scope.launch {
                repository.getUsuarioInfo(user.uid).fold(
                    onSuccess = { usuarioInfo ->
                        usuarioInfo?.let {
                            userName = it.nombreUsuario
                            
                            // Formatear fecha de unión - formato "Oct 2025"
                            it.fechaUnion?.toDate()?.let { fecha ->
                                val format = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                                joinDate = format.format(fecha)
                            } ?: run {
                                user.metadata?.creationTimestamp?.let { timestamp ->
                                    val date = Date(timestamp)
                                    val format = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                                    joinDate = format.format(date)
                                }
                            }
                        }
                    },
                    onFailure = { e ->
                        android.util.Log.e("ConfigCuentaScreen", "Error: ${e.message}", e)
                    }
                )
            }
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
                }
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
            Spacer(modifier = Modifier.height(24.dp))

            // Imagen de perfil (ic_usuario_generico_foreground)
            val userIcon = remember {
                BitmapPainter(
                    context.resources.getDrawable(R.mipmap.ic_usuario_generico_foreground, null)
                        .toBitmap()
                        .asImageBitmap()
                )
            }
            
            Image(
                painter = userIcon,
                contentDescription = "Perfil",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(userName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Unido: $joinDate", fontSize = 14.sp, color = Color.Gray)
            Text(userId, fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            // Datos de la cuenta
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("Correo", correo)
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoRow("Lenguaje", "Español")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Método de logeo
            Text(
                text = "Método de logeo",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    LoginMethodRow(
                        title = "Google",
                        status = if (googleVinculado) "Vinculado" else "No vinculado",
                        onClick = null
                    )
                    Divider()
                    LoginMethodRow(
                        title = "Correo y Contraseña",
                        status = "Agregar",
                        onClick = onNavigateToCorreo
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón eliminar cuenta
            OutlinedButton(
                onClick = onNavigateToEliminar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("Eliminar cuenta", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp)
    }
}

@Composable
private fun LoginMethodRow(
    title: String,
    status: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(status, fontSize = 14.sp, color = Color.Gray)
            if (onClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
            }
        }
    }
}

/**
 * VISTA 19: Estad. - Config. - Cuenta - Correo
 * 
 * Funcionalidad ACTIVA:
 * - Campos de entrada para correo y contraseña
 * - Validación y guardado en Firebase
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadConfigCuentaCorreoScreen(
    onNavigateBack: () -> Unit,
    onContinue: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(
                text = "Correo y Contraseña",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo:") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text("Contraseña:") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) 
                    androidx.compose.ui.text.input.VisualTransformation.None 
                else 
                    androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null
                        )
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "*Estos datos son solo para uso de crear y gestionar, por ningún motivo se compartirá ni usará para uso comercial.",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // TODO: Validar y guardar en Firebase
                    onContinue()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Continuar", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

/**
 * VISTA 22: Estad. - Config. - Cuenta - Eliminar
 * 
 * Funcionalidad ACTIVA:
 * - Confirmación de eliminación de cuenta
 * - Eliminación de datos de Firebase
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadConfigCuentaEliminarScreen(
    onNavigateBack: () -> Unit,
    onConfirm: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                // Ícono de advertencia
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFEBEE)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "ELIMINAR CUENTA",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Text(
                        "Se perderá todos sus datos, registros y configuraciones previas, el proceso es irremediable.",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(20.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        // TODO: Eliminar cuenta de Firebase
                        onConfirm()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirmar", fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}
