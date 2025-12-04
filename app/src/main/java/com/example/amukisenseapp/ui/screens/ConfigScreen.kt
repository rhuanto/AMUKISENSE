package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Vista de configuración del perfil de usuario con:
 * - Información de perfil (nombre, fecha de unión)
 * - Opciones de ajustes (Cuenta, Seguridad, Notificaciones)
 * - Botón de logout
 * 
 * REFACTORIZADO: Ahora usa RegistroRepository en lugar de acceso directo a Firestore
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCuenta: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val repository = remember { RegistroRepository() }
    val currentUser = auth.currentUser
    val scope = rememberCoroutineScope()
    
    var userName by remember { mutableStateOf("User Name") }
    var joinDate by remember { mutableStateOf("Sep 2025") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    
    // Obtener datos del usuario usando Repository
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            scope.launch {
                repository.getUsuarioInfo(user.uid).fold(
                    onSuccess = { usuarioInfo ->
                        usuarioInfo?.let {
                            userName = it.nombreUsuario
                            
                            // Formatear fecha de creación de cuenta
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
                        android.util.Log.e("ConfigScreen", "Error obteniendo info usuario", e)
                    }
                )
                
                // Obtener configuración de notificaciones
                repository.getConfig(user.uid).fold(
                    onSuccess = { config ->
                        notificationsEnabled = config.notificaciones
                    },
                    onFailure = { e ->
                        android.util.Log.e("ConfigScreen", "Error obteniendo config", e)
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
            Spacer(modifier = Modifier.height(16.dp))
            
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Nombre de usuario
            Text(
                text = userName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Fecha de unión
            Text(
                text = "Unido: $joinDate",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Sección AJUSTES
            Text(
                text = "AJUSTES",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    // Cuenta
                    SettingItem(
                        icon = Icons.Default.AccountCircle,
                        title = "Cuenta",
                        onClick = onNavigateToCuenta,
                        showArrow = true
                    )
                    Divider()
                    
                    // Seguridad, Términos y Condiciones (no funcional aún)
                    SettingItem(
                        icon = Icons.Default.Security,
                        title = "Seguridad, Términos y Condiciones",
                        onClick = { /* No funcional */ },
                        showArrow = true
                    )
                    Divider()
                    
                    // Notificaciones con toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF2900CC),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Notificaciones",
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                notificationsEnabled = enabled
                                currentUser?.let { user ->
                                    scope.launch {
                                        repository.updateNotificaciones(user.uid, enabled).fold(
                                            onSuccess = {
                                                android.util.Log.d("ConfigScreen", "Notificaciones actualizadas: $enabled")
                                            },
                                            onFailure = { e ->
                                                android.util.Log.e("ConfigScreen", "Error actualizando notificaciones", e)
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
            
            // Botón Logout
            Button(
                onClick = {
                    auth.signOut()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.buttonElevation(2.dp)
            ) {
                Text(
                    "Logout",
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    showArrow: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF2900CC),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (showArrow) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
