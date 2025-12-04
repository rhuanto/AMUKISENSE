package com.example.amukisenseapp.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
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
 * VISTA 12: Estad. - Mis_stats
 * 
 * Funcionalidad ACTIVA:
 * - Navegación a configuración
 * - Navegación entre secciones (Medidas, Stats, Todos)
 * - Navegación a lista de Registros y Quejas
 * - Consulta de contadores desde Firebase (ACTIVA)
 */

 
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadMisStatsScreen(
    onNavigateToConfig: () -> Unit,
    onNavigateToMedidas: () -> Unit,
    onNavigateToTodos: () -> Unit,
    onNavigateToRegistros: () -> Unit,
    onNavigateToQuejas: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val repository = remember { RegistroRepository() }
    val currentUser = auth.currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var userName by remember { mutableStateOf("User Name") }
    var joinDate by remember { mutableStateOf("Unido: oct 2025") }
    var registrosManuales by remember { mutableIntStateOf(0) }
    var registrosCapturas by remember { mutableIntStateOf(0) }
    var quejas by remember { mutableIntStateOf(0) }
    var registrosAutomaticos by remember { mutableIntStateOf(0) }
    
    // Obtener nombre de usuario y estadísticas usando Repository
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            Log.d("EstadMisStatsScreen", "Usuario autenticado: ${user.uid}")
            
            scope.launch {
                // Obtener información del usuario
                repository.getUsuarioInfo(user.uid).fold(
                    onSuccess = { usuarioInfo ->
                        usuarioInfo?.let {
                            userName = it.nombreUsuario
                            
                            // Formatear fecha de unión
                            it.fechaUnion?.toDate()?.let { fecha ->
                                val format = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                                joinDate = "Unido: ${format.format(fecha)}"
                            } ?: run {
                                // Fallback a la fecha de Firebase Auth
                                user.metadata?.creationTimestamp?.let { timestamp ->
                                    val date = Date(timestamp)
                                    val format = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                                    joinDate = "Unido: ${format.format(date)}"
                                }
                            }
                            
                            Log.d("EstadMisStatsScreen", "Datos usuario obtenidos: $userName, $joinDate")
                        }
                    },
                    onFailure = { e ->
                        Log.e("EstadMisStatsScreen", "Error obteniendo info usuario", e)
                    }
                )
                
                // Obtener estadísticas de registros
                repository.getEstadisticasUsuario(user.uid).fold(
                    onSuccess = { stats ->
                        registrosManuales = stats.registrosManual
                        registrosCapturas = stats.registrosFoto
                        quejas = stats.quejas
                        registrosAutomaticos = stats.registrosAuto
                        Log.d("EstadMisStatsScreen", "Estadísticas: Manual=$registrosManuales, Foto=$registrosCapturas, Quejas=$quejas, Auto=$registrosAutomaticos")
                    },
                    onFailure = { e ->
                        Log.e("EstadMisStatsScreen", "Error obteniendo estadísticas", e)
                    }
                )
            }
        } ?: run {
            Log.e("EstadMisStatsScreen", "Usuario no autenticado")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Título enmarcado con esquinas redondeadas (centrado)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F5F5)
                        ) {
                            Text(
                                "Mis Stats",
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                navigationIcon = {
                    // Botón de configuración enmarcado
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF5F5F5)
                    ) {
                        IconButton(onClick = onNavigateToConfig) {
                            Icon(Icons.Default.Settings, "Configuración")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Imagen de perfil usando ic_usuario_generico_foreground
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

                // Nombre de usuario
                Text(
                    text = userName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Fecha de unión
                Text(
                    text = joinDate,
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Mis Estadísticas :",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Métricas de estadísticas - Tarjetas cuadradas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Registros Manuales (ACTIVO - seleccionable)
                    StatCard(
                        icon = Icons.Default.Edit,
                        title = "Registros\nManuales",
                        count = registrosManuales,
                        iconColor = Color(0xFF9C27B0),
                        onClick = onNavigateToRegistros,
                        enabled = true
                    )

                    // Registros Capturas (ACTIVO - no seleccionable por ahora)
                    StatCard(
                        icon = Icons.Default.Camera,
                        title = "Registro\nCapturas",
                        count = registrosCapturas,
                        iconColor = Color(0xFFFFA726),
                        onClick = { },
                        enabled = false
                    )
                    
                    // Quejas (ACTIVO - seleccionable)
                    StatCard(
                        icon = Icons.Default.Notifications,
                        title = "Registro\nQuejas",
                        count = quejas,
                        iconColor = Color(0xFFFFA726),
                        onClick = onNavigateToQuejas,
                        enabled = true
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Registros Automáticos (INACTIVO - no seleccionable) - Centrado solo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    StatCard(
                        icon = Icons.Default.AutoAwesome,
                        title = "Registro\nAutomáticos",
                        count = registrosAutomaticos,
                        iconColor = Color(0xFFFFA726),
                        onClick = { },
                        enabled = false
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            // Barra de navegación inferior
            BottomNavigationBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                selectedItem = 2, // Stats selected
                onItemSelected = { index ->
                    when (index) {
                        0 -> onNavigateToTodos()
                        1 -> onNavigateToMedidas()
                        2 -> { /* Already here */ }
                    }
                }
            )
        }
    }
}

@Composable
private fun StatCardFull(
    icon: ImageVector,
    title: String,
    count: Int,
    iconColor: Color,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color.White else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(if (enabled) 4.dp else 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ícono
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = if (enabled) iconColor.copy(alpha = 0.1f) else Color(0xFFE0E0E0)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (enabled) iconColor else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Título
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) Color.Black else Color.Gray
                )
            }
            
            // Contador
            Text(
                text = count.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) iconColor else Color.Gray
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    title: String,
    count: Int,
    iconColor: Color,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(width = 100.dp, height = 120.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícono circular
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Título en dos líneas
            Text(
                text = title,
                fontSize = 9.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Normal
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Contador
            Text(
                text = count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 40.dp, vertical = 12.dp)
            .wrapContentWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Public,
                selected = selectedItem == 0,
                onClick = { onItemSelected(0) }
            )
            Spacer(modifier = Modifier.width(24.dp))
            BottomNavItem(
                icon = Icons.Default.GraphicEq,
                selected = selectedItem == 1,
                onClick = { onItemSelected(1) }
            )
            Spacer(modifier = Modifier.width(24.dp))
            BottomNavItem(
                icon = Icons.Default.Person,
                selected = selectedItem == 2,
                onClick = { onItemSelected(2) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) Color.Black else Color.LightGray,
            modifier = Modifier.size(32.dp)
        )
    }
}
