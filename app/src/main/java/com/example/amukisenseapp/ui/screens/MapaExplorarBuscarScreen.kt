package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amukisenseapp.data.model.Registro
import com.example.amukisenseapp.data.repository.RegistroRepository
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * VISTA 5: Mapa - Explorar - Buscar_Lugar
 * 
 * Funcionalidad ACTIVA:
 * - Navegación hacia atrás
 * - Campo de búsqueda de texto
 * - Búsqueda real en Firebase por dirección
 * - Listado de lugares registrados con filtro
 * - Click en elementos para ver ubicación en mapa
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaExplorarBuscarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMap: (lat: Double, lng: Double) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var registros by remember { mutableStateOf<List<Registro>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val repository = remember { RegistroRepository() }
    
    // Búsqueda con debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 3) {
            isLoading = true
            delay(500) // Debounce de 500ms
            
            repository.getRegistrosGlobales(limite = 100).fold(
                onSuccess = { todosRegistros ->
                    registros = todosRegistros.filter { registro ->
                        registro.direccion?.contains(searchQuery, ignoreCase = true) == true
                    }
                    isLoading = false
                },
                onFailure = {
                    isLoading = false
                }
            )
        } else {
            registros = emptyList()
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.KeyboardArrowDown, "Cerrar búsqueda")
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
            // Caja de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar lugar") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Estado de búsqueda
            when {
                searchQuery.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Ingresa al menos 3 caracteres para buscar",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                searchQuery.length < 3 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sigue escribiendo...",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
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
                        Text(
                            "No se encontraron lugares con \"$searchQuery\"",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    Text(
                        "${registros.size} resultado(s)",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(registros) { registro ->
                            RegistroItem(
                                registro = registro,
                                onClick = {
                                    onNavigateToMap(
                                        registro.coordenadas.lat,
                                        registro.coordenadas.lng
                                    )
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
private fun RegistroItem(
    registro: Registro,
    onClick: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale("es", "ES")) }
    val fechaStr = registro.fecha?.toDate()?.let { dateFormat.format(it) } ?: "Sin fecha"
    
    // Color según nivel de dB
    val dbColor = when {
        registro.db < 65 -> Color(0xFF4CAF50)   // Verde
        registro.db < 80 -> Color(0xFFFFC107)   // Amarillo
        else -> Color(0xFFFF5722)                // Naranja/Rojo
    }
    
    val tipoTexto = when (registro.tipo) {
        "queja" -> "Queja"
        "manual" -> "Manual"
        "auto" -> "Automático"
        "captura" -> "Captura"
        else -> registro.tipo
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nivel dB con color
            Surface(
                modifier = Modifier.size(60.dp),
                color = dbColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${registro.db.toInt()}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = dbColor
                        )
                        Text(
                            "dB",
                            fontSize = 12.sp,
                            color = dbColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    registro.direccion ?: "Ubicación sin nombre",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        fechaStr,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        tipoTexto,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                if (!registro.sensacion.isNullOrBlank()) {
                    Text(
                        registro.sensacion!!,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                if (!registro.comentario.isNullOrBlank()) {
                    Text(
                        registro.comentario!!,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

