package com.example.amukisenseapp.ui.screens

import android.Manifest
import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amukisenseapp.data.model.*
import com.example.amukisenseapp.ui.viewmodel.DashboardViewModel
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData
import com.github.tehras.charts.bar.renderer.label.SimpleValueDrawer
import com.github.tehras.charts.piechart.PieChart
import com.github.tehras.charts.piechart.PieChartData
import com.github.tehras.charts.piechart.renderer.SimpleSliceDrawer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

/**
 * Pantalla de Estadísticas de Ruido
 * Muestra 4 indicadores: Quejas por Distrito, Top Calles, Evolución Temporal, Heatmap Horario
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: DashboardViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Estado para ubicación
    var ubicacionUsuario by remember { mutableStateOf<String?>(null) }
    var distritoUsuario by remember { mutableStateOf<String?>(null) }
    var latitudUsuario by remember { mutableStateOf<Double?>(null) }
    var longitudUsuario by remember { mutableStateOf<Double?>(null) }
    
    // Permisos de ubicación
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    // Solicitar permisos al iniciar
    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }
    
    // Obtener ubicación del usuario
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    location?.let {
                        // Guardar coordenadas
                        latitudUsuario = it.latitude
                        longitudUsuario = it.longitude
                        
                        // Cargar datos filtrados por ubicación
                        viewModel.cargarDashboardPorUbicacion(it.latitude, it.longitude)
                        
                        // Obtener dirección
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                distritoUsuario = address.subLocality ?: address.locality ?: "Lima"
                                ubicacionUsuario = listOfNotNull(
                                    address.thoroughfare,
                                    address.subLocality,
                                    address.locality
                                ).take(2).joinToString(", ")
                            }
                        } catch (e: Exception) {
                            distritoUsuario = "Lima"
                            ubicacionUsuario = "Ubicación actual"
                        }
                    }
                }
            } catch (e: SecurityException) {
                distritoUsuario = "Lima"
                ubicacionUsuario = "Ubicación no disponible"
            }
        }
    }
    
    // Cargar datos al iniciar
    LaunchedEffect(Unit) {
        viewModel.cargarDashboard()
    }
    
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
                        Button(onClick = { viewModel.cargarDashboard() }) {
                            Text("Reintentar")
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Título principal centrado
                        item {
                            Text(
                                text = "Estadísticas de Ruido",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        
                        // 1. Quejas por Distrito
                        item {
                            val totalQuejas = uiState.quejasPorDistrito.sumOf { it.cantidad }
                            IndicadorCard(
                                titulo = "Quejas por Distrito",
                                descripcion = "Distribución de quejas por zonas",
                                zona = "Lima",
                                totalRegistros = totalQuejas,
                                esGeneral = false
                            ) {
                                if (uiState.quejasPorDistrito.isNotEmpty()) {
                                    GraficoPieQuejas(uiState.quejasPorDistrito)
                                } else {
                                    Text("No hay datos disponibles", color = Color.Gray)
                                }
                            }
                        }
                        
                        // 2. Top Calles Ruidosas
                        item {
                            val totalCalles = uiState.topCallesRuidosas.sumOf { it.cantidadQuejas }
                            IndicadorCard(
                                titulo = "Top Calles Ruidosas",
                                descripcion = "Avenidas con mayor contaminación acústica",
                                zona = "Lima",
                                totalRegistros = totalCalles,
                                esGeneral = false
                            ) {
                                if (uiState.topCallesRuidosas.isNotEmpty()) {
                                    ListaCallesRuidosas(uiState.topCallesRuidosas)
                                } else {
                                    Text("No hay datos disponibles", color = Color.Gray)
                                }
                            }
                        }
                        
                        // 3. Evolución Temporal - COMENTADO
                        // item {
                        //     val totalEvolucion = uiState.evolucionTemporal.sumOf { it.cantidadRegistros }
                        //     val zonaEvolucion = if (ubicacionUsuario != null) {
                        //         "Radio 1 km - $ubicacionUsuario"
                        //     } else {
                        //         distritoUsuario ?: "Lima"
                        //     }
                        //     IndicadorCard(
                        //         titulo = "Evolución Temporal",
                        //         descripcion = "Promedio de ruido última semana",
                        //         zona = zonaEvolucion,
                        //         totalRegistros = totalEvolucion,
                        //         esGeneral = false
                        //     ) {
                        //         if (uiState.evolucionTemporal.isNotEmpty()) {
                        //             GraficoEvolucionTemporal(uiState.evolucionTemporal)
                        //         } else {
                        //             Text("No hay datos disponibles", color = Color.Gray)
                        //         }
                        //     }
                        // }
                        
                        // 4. Heatmap por Horario
                        item {
                            val totalHorario = uiState.ruidoPorHorario.sumOf { it.cantidadRegistros }
                            val zonaHorario = if (ubicacionUsuario != null) {
                                "Radio 1 km - $ubicacionUsuario"
                            } else {
                                distritoUsuario ?: "Lima"
                            }
                            IndicadorCard(
                                titulo = "Heatmap por Horario",
                                descripcion = "Promedio de ruido por hora del día",
                                zona = zonaHorario,
                                totalRegistros = totalHorario,
                                esGeneral = false
                            ) {
                                // Siempre mostrar el heatmap (con "Sin registro" en horas vacías)
                                GraficoHorario(uiState.ruidoPorHorario)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card contenedor para cada indicador con información de zona y registros
 */
@Composable
fun IndicadorCard(
    titulo: String,
    descripcion: String,
    zona: String? = null,
    totalRegistros: Int? = null,
    esGeneral: Boolean = true,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = titulo,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = descripcion,
                fontSize = 12.sp,
                color = Color.Gray
            )
            
            // Indicador de zona y tipo de análisis
            if (zona != null || totalRegistros != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (zona != null) {
                        Surface(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (esGeneral) "📍 General" else "📍 $zona",
                                fontSize = 10.sp,
                                color = Color(0xFF1976D2),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (totalRegistros != null && totalRegistros > 0) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$totalRegistros registros",
                                fontSize = 10.sp,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            content()
        }
    }
}

/**
 * Gráfico circular de quejas por distrito con leyenda
 */
@Composable
fun GraficoPieQuejas(datos: List<QuejasPorDistrito>) {
    if (datos.isEmpty()) return
    
    val total = datos.sumOf { it.cantidad }.toFloat()
    val topDistritos = datos.take(8)
    val otros = datos.drop(8).sumOf { it.cantidad }
    
    val colores = topDistritos.mapIndexed { index, _ ->
        generarColorPorIndice(index)
    }.toMutableList()
    
    // Agregar "Otros" si existen más distritos
    val datosParaGrafico = if (otros > 0) {
        topDistritos + QuejasPorDistrito("Otros", otros)
    } else {
        topDistritos
    }.also {
        if (otros > 0) colores.add(Color.Gray)
    }
    
    val slices = datosParaGrafico.mapIndexed { index, queja ->
        PieChartData.Slice(
            value = queja.cantidad.toFloat(),
            color = colores[index]
        )
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Gráfico
        Box(
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp)
        ) {
            PieChart(
                pieChartData = PieChartData(slices = slices),
                sliceDrawer = SimpleSliceDrawer(sliceThickness = 40f)
            )
        }
        
        // Leyenda
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            datosParaGrafico.forEachIndexed { index, queja ->
                val porcentaje = ((queja.cantidad / total) * 100).toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(colores[index])
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${queja.distrito}: ${queja.cantidad} ($porcentaje%)",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Gráfico de barras para evolución temporal
 */
@Composable
fun GraficoEvolucionTemporal(datos: List<EvolucionTemporal>) {
    if (datos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No hay datos disponibles para la última semana",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        datos.forEach { evolucion ->
            val maxDB = datos.maxOfOrNull { it.promedioDB } ?: 100.0
            val porcentaje = if (maxDB > 0) (evolucion.promedioDB / maxDB).coerceIn(0.0, 1.0) else 0.5
            val color = obtenerColorDecibelios(evolucion.promedioDB, false)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Fecha
                Text(
                    text = evolucion.fecha,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(65.dp)
                )
                
                // Barra de progreso
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .padding(horizontal = 8.dp)
                        .background(
                            color = Color(0xFFF0F0F0),
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    // Barra de color
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(porcentaje.toFloat())
                            .background(
                                color = color,
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                    
                    // Texto sobre la barra
                    Text(
                        text = "${evolucion.promedioDB.toInt()} dB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (porcentaje > 0.5) Color.White else Color.Black,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                    )
                }
                
                // Registros
                Text(
                    text = "${evolucion.cantidadRegistros}",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.width(30.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

/**
 * Lista de calles ruidosas
 */
@Composable
fun ListaCallesRuidosas(datos: List<CalleRuidosa>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        datos.take(10).forEachIndexed { index, calle ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${index + 1}. ${calle.calle}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = calle.distrito,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${calle.promedioDB.toInt()} dB",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = obtenerColorDecibelios(calle.promedioDB, false)
                        )
                        Text(
                            text = "${calle.cantidadQuejas} registros",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

/**
 * Heatmap simplificado por horario (0-23 horas)
 */
@Composable
fun GraficoHorario(datos: List<RuidoPorHorario>) {
    // Crear mapa de horas con datos
    val datosPorHora = datos.associateBy { it.hora }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Mostrar todas las 24 horas
        (0..23).forEach { hora ->
            val horario = datosPorHora[hora]
            val tieneRegistros = horario != null && horario.cantidadRegistros > 0
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${hora}:00",
                    fontSize = 12.sp,
                    modifier = Modifier.width(50.dp)
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (tieneRegistros && horario != null) {
                        // Hay datos - pintar según nivel de dB
                        val color = obtenerColorDecibelios(horario.promedioDB, false)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = color,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    } else {
                        // No hay datos - pintar gris con marca de agua
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sin registro",
                                fontSize = 9.sp,
                                color = Color(0xFFBDBDBD),
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                }
                
                Text(
                    text = if (tieneRegistros && horario != null) {
                        "${horario.promedioDB.toInt()} dB"
                    } else {
                        "--"
                    },
                    fontSize = 11.sp,
                    fontWeight = if (tieneRegistros) FontWeight.Bold else FontWeight.Normal,
                    color = if (tieneRegistros) Color.Black else Color.Gray,
                    modifier = Modifier.width(60.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

/**
 * Determina el color según el nivel de decibelios
 */
private fun obtenerColorDecibelios(db: Double, esQueja: Boolean): Color {
    return when {
        esQueja -> Color(0xFFE53935) // Rojo
        db <= 70 -> Color(0xFF43A047) // Verde
        db <= 85 -> Color(0xFFFDD835) // Amarillo
        else -> Color(0xFFFF6F00) // Naranja
    }
}

/**
 * Genera un color consistente basado en el índice
 */
private fun generarColorPorIndice(index: Int): Color {
    val colores = listOf(
        Color(0xFFE53935), // Rojo
        Color(0xFF1E88E5), // Azul
        Color(0xFF43A047), // Verde
        Color(0xFFFB8C00), // Naranja
        Color(0xFF8E24AA), // Púrpura
        Color(0xFF00ACC1), // Cian
        Color(0xFFFDD835), // Amarillo
        Color(0xFF6D4C41), // Marrón
        Color(0xFF546E7A), // Gris Azulado
        Color(0xFFD81B60)  // Rosa
    )
    return colores[index % colores.size]
}
