package com.example.amukisenseapp.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amukisenseapp.data.model.Registro
import com.example.amukisenseapp.ui.viewmodel.MisQuejasViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * VISTA 16: Estad. - Mis_stats - Quejas
 * 
 * Funcionalidad:
 * - Muestra lista de quejas del usuario
 * - Filtros por fecha y nivel de dB
 * - Tarjetas deslizables con opciones de editar y eliminar
 * - Navegación a vista de modificación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisQuejasScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: MisQuejasViewModel = viewModel()
) {
    val quejas by viewModel.quejas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var filtroFechaAscendente by remember { mutableStateOf(true) }
    var filtroDbAscendente by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var quejaToDelete by remember { mutableStateOf<Registro?>(null) }

    if (showDeleteDialog && quejaToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Queja") },
            text = { Text("¿Estás seguro de que deseas eliminar esta queja?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        quejaToDelete?.let { viewModel.eliminarQueja(it.id) }
                        showDeleteDialog = false
                        quejaToDelete = null
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
                            "Quejas",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Quejas registradas por ti:",
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
                    color = Color(0xFFE53935)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Filtros
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

            // Lista de quejas
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                quejas.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No hay quejas",
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
                        items(quejas) { queja ->
                            QuejaCard(
                                queja = queja,
                                onEdit = { onNavigateToEdit(queja.id) },
                                onDelete = {
                                    quejaToDelete = queja
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
private fun QuejaCard(
    queja: Registro,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, - HH:mm", Locale("es", "ES"))
    val fecha = queja.fecha?.toDate()
    
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
                            text = "${queja.db.toInt()}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                        Text(
                            text = "dB",
                            fontSize = 14.sp,
                            color = Color(0xFFE53935),
                            modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Barra roja para quejas
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(3.dp)
                            .background(
                                color = Color(0xFFE53935),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
                
                // Columna central: Datos de la queja
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Ubicación
                    Text(
                        text = queja.direccion?.take(40) ?: "Sin ubicación",
                        fontSize = 11.sp,
                        color = Color.Black,
                        maxLines = 1,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 10.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Fecha y hora | Tipo
                    Text(
                        text = "${fecha?.let { dateFormat.format(it) } ?: "Sin fecha"} | Queja ciudadana",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    
                    // Nivel de molestia | Captura
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableQuejaItem(
    queja: Registro,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        // Tarjeta de eliminar (más atrás, izquierda)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(start = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE53935)),
            shape = RoundedCornerShape(12.dp),
            onClick = onDelete
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .size(32.dp)
                )
            }
        }
        
        // Tarjeta de editar (medio, derecha)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(end = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(12.dp),
            onClick = onEdit
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .size(32.dp)
                )
            }
        }
        
        // Tarjeta principal (adelante)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Número de dB con color (siempre rojo para quejas)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(0.3f)
                ) {
                    Text(
                        text = "${queja.db.toInt()}",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "dB",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    // Línea de color rojo (siempre rojo para quejas)
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .background(
                                color = Color(0xFFE53935),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Información de la queja
                Column(
                    modifier = Modifier.weight(0.7f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Dirección
                    Text(
                        text = queja.direccion ?: "Ubicación desconocida",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 2
                    )
                    
                    // Fecha
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatearFechaCompleta(queja.fecha),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    // Tipo
                    Text(
                        text = "Parque",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    // Comentario
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Comentario: ",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = if (queja.comentario?.isNotBlank() == true) "Sí" else "No",
                            fontSize = 12.sp,
                            color = if (queja.comentario?.isNotBlank() == true) Color(0xFF43A047) else Color(0xFFE53935),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatearFechaCompleta(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return "Fecha desconocida"
    val date = timestamp.toDate()
    val format = SimpleDateFormat("MMM. d, - HH:mm", Locale("es"))
    return format.format(date)
}
