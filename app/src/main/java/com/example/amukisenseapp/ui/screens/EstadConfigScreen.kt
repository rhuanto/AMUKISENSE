package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
 * VISTA 14: Estad. - Config.
 * 
 * Funcionalidad ACTIVA:
 * - Navegación hacia atrás
 * - Navegación a "Cuenta"
 * - Toggle de notificaciones (ACTIVO - guarda en Firebase)
 * - Botón Logout (ACTIVO - cierra sesión)
 * 
 * Funcionalidad INACTIVA:
 * - "Seguridad, Términos y Condiciones" (no funcional)
 * - Imagen de perfil real (placeholder)
 * 
 * REFACTORIZADO: Ahora usa RegistroRepository en lugar de acceso directo a Firestore
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadConfigScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCuenta: () -> Unit,
    onLogout: () -> Unit
) {
    android.util.Log.d("EstadConfigScreen", "🟢 Composable iniciado")
    
    val auth = FirebaseAuth.getInstance()
    val repository = remember { RegistroRepository() }
    val currentUser = auth.currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    android.util.Log.d("EstadConfigScreen", "Usuario actual: ${currentUser?.uid ?: "NULL"}")
    
    var userName by remember { mutableStateOf("Cargando...") }
    var joinDate by remember { mutableStateOf("Cargando...") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    
    // Obtener datos del usuario usando Repository
    LaunchedEffect(currentUser) {
        android.util.Log.d("EstadConfigScreen", "🔵 LaunchedEffect iniciado con usuario: ${currentUser?.uid}")
        currentUser?.let { user ->
            android.util.Log.d("EstadConfigScreen", "LaunchedEffect ejecutado para usuario: ${user.uid}")
            
            scope.launch {
                // Obtener información del usuario
                repository.getUsuarioInfo(user.uid).fold(
                    onSuccess = { usuarioInfo ->
                        usuarioInfo?.let {
                            userName = it.nombreUsuario
                            android.util.Log.d("EstadConfigScreen", "Alias obtenido: ${it.nombreUsuario}")
                            
                            // Formatear fecha de unión - formato "Oct 2025"
                            it.fechaUnion?.toDate()?.let { fecha ->
                                val format = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                                val formattedDate = "Unido: ${format.format(fecha)}"
                                joinDate = formattedDate
                                android.util.Log.d("EstadConfigScreen", "Fecha obtenida: $formattedDate")
                            } ?: run {
                                user.metadata?.creationTimestamp?.let { timestamp ->
                                    val date = Date(timestamp)
                                    val format = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                                    val formattedDate = "Unido: ${format.format(date)}"
                                    joinDate = formattedDate
                                }
                            }
                        }
                    },
                    onFailure = { e ->
                        android.util.Log.e("EstadConfigScreen", "Error obteniendo datos: ${e.message}", e)
                    }
                )
                
                // Obtener configuración de notificaciones
                repository.getConfig(user.uid).fold(
                    onSuccess = { config ->
                        notificationsEnabled = config.notificaciones
                        android.util.Log.d("EstadConfigScreen", "Config obtenida: notificaciones = ${config.notificaciones}")
                    },
                    onFailure = { e ->
                        android.util.Log.e("EstadConfigScreen", "Error obteniendo config", e)
                    }
                )
            }
        } ?: run {
            android.util.Log.e("EstadConfigScreen", "❌ currentUser es NULL en LaunchedEffect")
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
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Fila con logo a la izquierda y datos a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo de perfil a la izquierda (ic_usuario_generico_foreground)
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
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Columna con alias y fecha a la derecha
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = userName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = joinDate,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sección AJUSTES
            Text(
                text = "AJUSTES",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    // Cuenta (ACTIVO)
                    SettingOption(
                        title = "Cuenta",
                        onClick = onNavigateToCuenta
                    )

                    Divider()

                    // Seguridad, Términos y Condiciones (INACTIVO)
                    SettingOption(
                        title = "Seguridad, Términos y Condiciones",
                        onClick = { /* TODO: Implementar en Sprint 2+ */ },
                        enabled = false
                    )

                    Divider()

                    // Notificaciones (ACTIVO con toggle)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notificaciones",
                            fontSize = 16.sp
                        )
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { newValue ->
                                notificationsEnabled = newValue
                                // Guardar en Firebase usando Repository
                                currentUser?.let { user ->
                                    scope.launch {
                                        repository.updateNotificaciones(user.uid, newValue).fold(
                                            onSuccess = {
                                                android.util.Log.d("EstadConfigScreen", "Notificaciones actualizadas: $newValue")
                                            },
                                            onFailure = { e ->
                                                android.util.Log.e("EstadConfigScreen", "Error actualizando notificaciones", e)
                                                // Revertir el cambio en caso de error
                                                notificationsEnabled = !newValue
                                            }
                                        )
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2900CC)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón Logout (ACTIVO)
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Black
                )
            ) {
                Text("Logout", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SettingOption(
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = if (enabled) Color.Black else Color.Gray
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (enabled) Color.Gray else Color.LightGray
        )
    }
}
