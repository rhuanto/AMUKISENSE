package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.layout.*
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

/**
 * VISTA 6: Mapa - Explorar - Lugares
 * 
 * Funcionalidad ACTIVA:
 * - Botón X para cerrar ventana modal
 * - Navegación a vistas de filtros (INACTIVAS por ahora)
 * 
 * Funcionalidad INACTIVA (Sprint 2+):
 * - Filtro "Nivel de Ruido en Calle"
 * - Filtro "Quejas por Ruido"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaExplorarLugaresScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNivelRuido: () -> Unit,
    onNavigateToQuejas: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, "Cerrar")
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
            Text(
                text = "Selecciona una opción",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    // Botón: Nivel de Ruido en Calle (INACTIVO)
                    OptionButton(
                        icon = Icons.Default.GraphicEq,
                        title = "Nivel de Ruido en Calle",
                        onClick = onNavigateToNivelRuido,
                        enabled = false // INACTIVO (Sprint 2+)
                    )
                    
                    Divider()
                    
                    // Botón: Quejas por Ruido (INACTIVO)
                    OptionButton(
                        icon = Icons.Default.ReportProblem,
                        title = "Quejas por Ruido",
                        onClick = onNavigateToQuejas,
                        enabled = false // INACTIVO (Sprint 2+)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nota informativa
            Text(
                "Estas opciones estarán disponibles en Sprint 2+",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun OptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        color = if (enabled) Color.White else Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) Color(0xFF2900CC) else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
                color = if (enabled) Color.Black else Color.Gray
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (enabled) Color.Gray else Color.LightGray
            )
        }
    }
}
