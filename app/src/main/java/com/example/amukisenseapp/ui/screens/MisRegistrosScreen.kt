package com.example.amukisenseapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amukisenseapp.data.model.Registro
import com.example.amukisenseapp.ui.viewmodel.MisRegistrosViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * VISTA 15: Estad. - Mis_stats - Registros
 * 
 * Funcionalidad:
 * - Muestra lista de registros manuales del usuario
 * - Filtros por fecha y nivel de dB
 * - Tarjetas deslizables con opciones de editar y eliminar
 * - Navegación a vista de modificación
 */
 
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisRegistrosScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: MisRegistrosViewModel = viewModel()
) {
    val registros by viewModel.registros.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val tipoActual by viewModel.tipoActual.collectAsState()
    
    var filtroFechaAscendente by remember { mutableStateOf(true) }
    var filtroDbAscendente by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var registroToDelete by remember { mutableStateOf<Registro?>(null) }

    if (showDeleteDialog && registroToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Registro") },
            text = { Text("¿Estás seguro de que deseas eliminar este registro?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        registroToDelete?.let { viewModel.eliminarRegistro(it.id) }
                        showDeleteDialog = false
                        registroToDelete = null
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Registros",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Mediciones registradas por ti:",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color(0xFFF5F5F5)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.Black
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Header con ícono
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color(0xFF9C27B0)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Filtros de tipo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tipo:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 12.dp)
                )
                
                // R. Manuales
                Surface(
                    modifier = Modifier
                        .clickable { viewModel.cargarRegistrosPorTipo("manual") }
                        .padding(end = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (tipoActual == "manual") Color(0xFF9C27B0) else Color(0xFFF5F5F5)
                ) {
                    Text(
                        "R. Manuales",
                        fontSize = 12.sp,
                        color = if (tipoActual == "manual") Color.White else Color.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                // R. Automáticos
                Surface(
                    modifier = Modifier
                        .clickable { viewModel.cargarRegistrosPorTipo("automatico") }
                        .padding(end = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (tipoActual == "automatico") Color(0xFF9C27B0) else Color(0xFFF5F5F5)
                ) {
                    Text(
                        "R. Automáticos",
                        fontSize = 12.sp,
                        color = if (tipoActual == "automatico") Color.White else Color.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                // R. Capturas
                Surface(
                    modifier = Modifier.clickable { viewModel.cargarRegistrosPorTipo("captura") },
                    shape = RoundedCornerShape(16.dp),
                    color = if (tipoActual == "captura") Color(0xFF9C27B0) else Color(0xFFF5F5F5)
                ) {
                    Text(
                        "R. Capturas",
                        fontSize = 12.sp,
                        color = if (tipoActual == "captura") Color.White else Color.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            // Filtros de ordenamiento
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filtros:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 12.dp)
                )
                
                // Filtro Fecha
                Surface(
                    modifier = Modifier
                        .clickable {
                            filtroFechaAscendente = !filtroFechaAscendente
                            viewModel.ordenarPorFecha(filtroFechaAscendente)
                        }
                        .padding(end = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF5F5F5)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Fecha",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (filtroFechaAscendente) Color.Black else Color.LightGray
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).offset(y = (-8).dp),
                                tint = if (!filtroFechaAscendente) Color.Black else Color.LightGray
                            )
                        }
                    }
                }
                
                // Filtro Nivel dB
                Surface(
                    modifier = Modifier
                        .clickable {
                            filtroDbAscendente = !filtroDbAscendente
                            viewModel.ordenarPorDb(filtroDbAscendente)
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF5F5F5)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Nivel dB",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (filtroDbAscendente) Color.Black else Color.LightGray
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).offset(y = (-8).dp),
                                tint = if (!filtroDbAscendente) Color.Black else Color.LightGray
                            )
                        }
                    }
                }
            }

            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            // Separador "Últimos"
            Text(
                text = "Últimos",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Lista de registros
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                registros.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No hay registros",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(registros) { registro ->
                            RegistroCard(
                                registro = registro,
                                onEdit = { onNavigateToEdit(registro.id) },
                                onDelete = {
                                    registroToDelete = registro
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegistroCard(
    registro: Registro,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, - HH:mm", Locale("es", "ES"))
    val fecha = registro.fecha?.toDate()
    
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Tarjeta principal con datos
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 120.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Columna izquierda: dB con barra de color
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .width(70.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${registro.db.toInt()}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9C27B0)
                        )
                        Text(
                            text = "dB",
                            fontSize = 14.sp,
                            color = Color(0xFF9C27B0),
                            modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Barra de color según nivel
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(3.dp)
                            .background(
                                color = when {
                                    registro.db < 70 -> Color(0xFF4CAF50)
                                    registro.db < 85 -> Color(0xFFFFC107)
                                    else -> Color(0xFFFF5722)
                                },
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
                
                // Columna central: Datos del registro
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Ubicación
                    Text(
                        text = registro.direccion?.take(40) ?: "Sin ubicación",
                        fontSize = 11.sp,
                        color = Color.Black,
                        maxLines = 1,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 10.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Fecha y hora | Tipo de lugar
                    Text(
                        text = "${fecha?.let { dateFormat.format(it) } ?: "Sin fecha"} | ${registro.origen_ruido ?: "Parque"}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    
                    // Nivel de molestia | Captura | Comentario
                    Text(
                        text = "Muy molesto | Captura: No",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        // Tarjetas flotantes en la esquina derecha
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tarjeta de editar
            Card(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onEdit() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE)),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color(0xFF616161),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Tarjeta de eliminar
            Card(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onDelete() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDADADA)),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFF616161),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
