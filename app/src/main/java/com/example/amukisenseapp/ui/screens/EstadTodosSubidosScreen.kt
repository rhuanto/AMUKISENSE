package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amukisenseapp.data.model.Registro
import com.example.amukisenseapp.ui.viewmodel.SubidasViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * VISTA 17: Estad. - Todos - Subidos
 * 
 * Funcionalidad ACTIVA:
 * - Consulta de últimas subidas de la comunidad desde Firebase (ACTIVA)
 * - Listado cronológico
 * 
 * Funcionalidad INACTIVA:
 * - Banderas de países (placeholder)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadTodosSubidosScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubidasViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Icono transparente para equilibrar el centrado
                    IconButton(onClick = { }, enabled = false) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.Transparent
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header: Icono + Título + Descripción
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF44336))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Subidas",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Mediciones únicas de todos los miembros de la red",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Últimos",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Error desconocido",
                            color = Color.Red
                        )
                    }
                }
                
                uiState.registros.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay registros disponibles",
                            color = Color.Gray
                        )
                    }
                }
                
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.registros) { registro ->
                            RegistroItem(registro)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Determina el color según el nivel de decibelios y si es queja
 * - Queja: Rojo
 * - 0-70: Verde
 * - 71-85: Amarillo
 * - 86+: Naranja
 */
private fun obtenerColorDecibelios(db: Double, esQueja: Boolean): Color {
    return when {
        esQueja -> Color(0xFFE53935) // Rojo para quejas
        db <= 70 -> Color(0xFF43A047) // Verde
        db <= 85 -> Color(0xFFFDD835) // Amarillo
        else -> Color(0xFFFF6F00) // Naranja
    }
}

/**
 * Formatea la fecha de timestamp a formato completo
 */
private fun formatearFechaCompleta(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return "Fecha desconocida"
    
    val date = timestamp.toDate()
    val formatter = SimpleDateFormat("MMM. d, - HH:mm", Locale("es", "ES"))
    return formatter.format(date)
}

/**
 * Componente de item individual de registro
 */
@Composable
private fun RegistroItem(registro: Registro) {
    val dbValue = registro.db ?: 0.0
    val esQueja = registro.tipo?.lowercase() == "queja"
    val colorDb = obtenerColorDecibelios(dbValue, esQueja)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Decibelios grandes a la izquierda
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Text(
                    text = "${dbValue.toInt()}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorDb
                )
                Text(
                    text = "dB",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorDb
                )
                
                // Barra de nivel debajo de los dB
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .background(
                            color = colorDb,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }

            // Información del registro a la derecha
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Dirección/ubicación
                Text(
                    text = registro.direccion ?: "Ubicación no disponible",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Fecha y hora
                Text(
                    text = formatearFechaCompleta(registro.fecha),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Información adicional en fila
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tipo de medición
                    Text(
                        text = when(registro.tipo?.lowercase()) {
                            "queja" -> "Muy molesto"
                            "manual" -> "Manual"
                            "auto" -> "Automático"
                            else -> "Manual"
                        },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    // Comentario
                    Text(
                        text = "Comentario: ${if (!registro.comentario.isNullOrEmpty()) "Sí" else "No"}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
