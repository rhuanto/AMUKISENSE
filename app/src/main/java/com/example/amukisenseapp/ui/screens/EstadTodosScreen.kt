package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amukisenseapp.R
import com.example.amukisenseapp.ui.viewmodel.EstadTodosViewModel

/**
 * VISTA 13: Estad. - Todos
 * 
 * Funcionalidad ACTIVA:
 * - Navegación a configuración
 * - Navegación entre secciones
 * - Navegación a "Subidas" desde card de Subidas
 * - Consulta de métricas desde Firebase (total_usuarios, total_registros)
 * - Navegación a Dashboard de quejas por distrito
 * 
 * Funcionalidad INACTIVA:
 * - Métrica dinámica "Alguien en X está midiendo Y dB" (placeholder)
 * - Gráfico de onda de sonido animado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadTodosScreen(
    onNavigateToConfig: () -> Unit,
    onNavigateToMedidas: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSubidas: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToMiembros: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: EstadTodosViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F5F5)
                        ) {
                            Text(
                                "Comunidad",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF5F5F5)
                    ) {
                        IconButton(onClick = onNavigateToConfig) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Configuración",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Espaciador para centrar el título
                    Spacer(modifier = Modifier.size(40.dp))
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Comentado: Métrica principal dinámica (INACTIVO - placeholder)
                /*
                item {
                    Text(
                        text = "Alguien en $lugarActual está midiendo ${dbActual}dB",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
                */

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    // Ícono de onda de sonido usando ic_audio_wave_foreground
                    val audioWaveIcon = remember {
                        BitmapPainter(
                            context.resources.getDrawable(R.mipmap.ic_audio_wave_foreground, null)
                                .toBitmap()
                                .asImageBitmap()
                        )
                    }
                    
                    Image(
                        painter = audioWaveIcon,
                        contentDescription = "Audio Wave",
                        modifier = Modifier.size(160.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Cards de métricas (ahora como items individuales en LazyColumn)
                item {
                    // Miembros (ACTIVO - clickeable)
                    CommunityMetricCard(
                        icon = Icons.Default.People,
                        iconColor = Color(0xFF2900CC),
                        title = "Miembros",
                        count = uiState.totalMiembros,
                        description = "Total de usuarios registrados en la comunidad",
                        onClick = onNavigateToMiembros,
                        enabled = true
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    // Subidas (ACTIVO - seleccionable)
                    CommunityMetricCard(
                        icon = Icons.Default.CloudUpload,
                        iconColor = Color(0xFFF44336),
                        title = "Subidas",
                        count = uiState.totalSubidas,
                        description = "Total de registros compartidos por todos",
                        onClick = onNavigateToSubidas,
                        enabled = true
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // COMENTADO: Puntos Recorridos (no funcional aún)
                /*
                item {
                    CommunityMetricCard(
                        icon = Icons.Default.DirectionsWalk,
                        iconColor = Color(0xFF4CAF50),
                        title = "Puntos Recorridos",
                        count = 0,
                        description = "Funcionalidad disponible próximamente",
                        onClick = null,
                        enabled = false
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                */
                item {
                    // Dashboard de Quejas (ACTIVO)
                    CommunityMetricCard(
                        icon = Icons.Default.Dashboard,
                        iconColor = Color(0xFFFF9800),
                        title = "Dashboard",
                        count = null, // No mostrar número aquí
                        description = "Visualiza estadísticas de quejas por distrito",
                        onClick = onNavigateToDashboard,
                        enabled = true
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // Barra de navegación inferior
            BottomNavigationBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                selectedItem = 0, // Todos selected
                onItemSelected = { index ->
                    when (index) {
                        0 -> { /* Already here */ }
                        1 -> onNavigateToMedidas()
                        2 -> onNavigateToStats()
                    }
                }
            )
        }
    }
}

@Composable
private fun CommunityMetricCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    count: Int?,
    description: String,
    onClick: (() -> Unit)?,
    enabled: Boolean
) {
    Card(
        onClick = onClick ?: {},
        enabled = enabled && onClick != null,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color.White else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(if (enabled) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
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
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Mostrar número solo si count no es null
                count?.let {
                    Text(
                        text = it.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Color.Black else Color.Gray
                    )
                }
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) Color.Black else Color.Gray
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (onClick != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (enabled) Color.Gray else Color.LightGray
                )
            }
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
