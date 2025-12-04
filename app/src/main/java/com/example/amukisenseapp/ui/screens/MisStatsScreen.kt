package com.example.amukisenseapp.ui.screens

import android.util.Log
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * Vista principal de estadísticas del usuario mostrando:
 * - Nombre de usuario de Firebase
 * - Contador de Registros Manuales
 * - Contador de Registros Automáticos
 * - Contador de Quejas
 * - Barra de navegación inferior
 * 
 * REFACTORIZADO: Ahora usa RegistroRepository en lugar de acceso directo a Firestore
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisStatsScreen(
    onNavigateToConfig: () -> Unit,
    onNavigateToRegistrosM: () -> Unit,
    onNavigateToQuejas: () -> Unit,
    onNavigateToTodos: () -> Unit,
    onNavigateToMapa: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val repository = remember { RegistroRepository() }
    val currentUser = auth.currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var userName by remember { mutableStateOf("User Name") }
    var joinDate by remember { mutableStateOf("Oct 2025") }
    var registrosManual by remember { mutableIntStateOf(0) }
    var registrosFoto by remember { mutableIntStateOf(0) }
    var quejas by remember { mutableIntStateOf(0) }
    var registrosAuto by remember { mutableIntStateOf(0) }
    
    // Obtener nombre de usuario y estadísticas usando Repository
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            Log.d("MisStatsScreen", "Usuario autenticado: ${user.uid}")
            
            scope.launch {
                // Obtener información del usuario
                repository.getUsuarioInfo(user.uid).fold(
                    onSuccess = { usuarioInfo ->
                        usuarioInfo?.let {
                            userName = it.nombreUsuario
                            
                            // Formatear fecha de unión
                            it.fechaUnion?.toDate()?.let { fecha ->
                                val format = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                                joinDate = format.format(fecha)
                            } ?: run {
                                // Fallback a la fecha de Firebase Auth
                                user.metadata?.creationTimestamp?.let { timestamp ->
                                    val date = Date(timestamp)
                                    val format = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))
                                    joinDate = format.format(date)
                                }
                            }
                            
                            Log.d("MisStatsScreen", "Datos usuario obtenidos: $userName, $joinDate")
                        }
                    },
                    onFailure = { e ->
                        Log.e("MisStatsScreen", "Error obteniendo info usuario", e)
                    }
                )
                
                // Obtener estadísticas de registros
                repository.getEstadisticasUsuario(user.uid).fold(
                    onSuccess = { stats ->
                        registrosManual = stats.registrosManual
                        registrosFoto = stats.registrosFoto
                        quejas = stats.quejas
                        registrosAuto = stats.registrosAuto
                        Log.d("MisStatsScreen", "Estadísticas: Manual=$registrosManual, Foto=$registrosFoto, Quejas=$quejas, Auto=$registrosAuto")
                    },
                    onFailure = { e ->
                        Log.e("MisStatsScreen", "Error obteniendo estadísticas", e)
                    }
                )
            }
        } ?: run {
            Log.e("MisStatsScreen", "Usuario no autenticado")
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar personalizado - Misma altura que MapaMedidas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateToConfig) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = Color.Black
                        )
                    }
                    
                    Text(
                        text = "Mis Stats",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    
                    // Spacer para balancear
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                // Contenido principal
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Imagen de perfil usando ic_usuario_generico
                    val userIcon = remember {
                        BitmapPainter(
                            context.resources.getDrawable(R.mipmap.ic_usuario_generico, null)
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
                    
                    // Nombre de usuario (alias)
                    Text(
                        text = userName.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Fecha de unión
                    Text(
                        text = "Unido: $joinDate",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Título de estadísticas
                    Text(
                        text = "Mis Estadísticas :",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Fila 1: Registros M. | Registros F. | Quejas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Registros Manuales
                        StatCard(
                            iconRes = R.mipmap.ic_registros_round,
                            title = "Registros M.",
                            count = registrosManual
                        )
                        
                        // Registros con Foto
                        StatCard(
                            iconRes = R.mipmap.ic_registros_round,
                            title = "Registros F.",
                            count = registrosFoto
                        )
                        
                        // Quejas
                        StatCard(
                            iconRes = R.mipmap.ic_registros_round,
                            title = "Quejas",
                            count = quejas
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Fila 2: Registros Automáticos (centrado)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Registros Automáticos (futuro)
                        StatCard(
                            iconRes = R.mipmap.ic_registros_round,
                            title = "Registros A.",
                            count = registrosAuto
                        )
                    }
                }
            }
            
            // Barra de navegación inferior - Misma posición que MapaMedidas
            BottomNavigationBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                selectedItem = 2, // Usuario seleccionado
                onItemSelected = { index ->
                    when (index) {
                        0 -> onNavigateToTodos()
                        1 -> onNavigateToMapa()
                        2 -> { /* Ya estamos aquí */ }
                    }
                }
            )
        }
    }
}

@Composable
private fun StatCard(
    iconRes: Int,
    title: String,
    count: Int
) {
    val context = LocalContext.current
    val iconPainter = remember {
        BitmapPainter(
            context.resources.getDrawable(iconRes, null)
                .toBitmap()
                .asImageBitmap()
        )
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        // Icono
        Image(
            painter = iconPainter,
            contentDescription = title,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Título
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        // Contador
        Text(
            text = count.toString(),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
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
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp),
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
