package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amukisenseapp.data.model.Usuario
import com.example.amukisenseapp.ui.viewmodel.MiembrosUnidosViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla que muestra la lista de todos los miembros registrados en la comunidad
 * Similar a "Subidas" pero muestra usuarios con su alias y fecha de unión
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiembrosUnidosScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: MiembrosUnidosViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Miembros Unidos",
                            textAlign = TextAlign.Center
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Espaciador para centrar el título
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = Color.Transparent
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.cargarMiembros() }) {
                            Text("Reintentar")
                        }
                    }
                }
                
                uiState.usuarios.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay miembros registrados",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                    }
                }
                
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header con ícono e información
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Ícono circular (igual que en la tarjeta)
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        color = Color(0xFF2900CC).copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = Color(0xFF2900CC)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Miembros Unidos",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Miembros que han registrado en todos los miembros de la red",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        // Separador "Últimos"
                        Text(
                            text = "Últimos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 8.dp)
                        )
                        
                        // Lista scrolleable de miembros
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(uiState.usuarios) { usuario ->
                                MiembroItem(usuario = usuario)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Item individual de miembro en la lista
 * Muestra: ícono circular, nombre de usuario y fecha de unión
 */
@Composable
private fun MiembroItem(usuario: Usuario) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono circular naranja (similar a la imagen)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(0xFFFF5722),
                        shape = CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Nombre de usuario (alias)
                Text(
                    text = usuario.nombre_usuario.ifEmpty { "Usuario" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                // Fecha de unión
                Text(
                    text = formatearFechaUnion(usuario.fecha_union),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Formatea la fecha de unión en formato legible
 * Ejemplo: "Miér, 01 Nov 24"
 */
private fun formatearFechaUnion(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return "Hace poco"
    
    return try {
        val date = timestamp.toDate()
        val formatter = SimpleDateFormat("EEE, dd MMM yy", Locale("es", "ES"))
        formatter.format(date)
    } catch (e: Exception) {
        "Fecha no disponible"
    }
}
