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
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.amukisenseapp.ui.viewmodel.MapaViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.text.SimpleDateFormat
import java.util.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import android.Manifest

/**
 * VISTA 4: Mapa - Explorar
 * 
 * Funcionalidad ACTIVA:
 * - Mapa interactivo con registros de ruido
 * - Navegación entre tabs "Medidas" y "Explorar"
 * - Botón "Lugares" con menú desplegable de filtros:
 *   1. Niveles de Ruido en calle (registros manuales y automáticos)
 *   2. Quejas por ruido (solo quejas, marcadores rojos)
 * - Barra de búsqueda inferior
 * - Marcadores coloreados según nivel de dB y tipo
 * - Navegación a ubicación específica desde búsqueda
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapaExplorarScreen(
    onNavigateToMedidas: () -> Unit,
    onNavigateToLugares: () -> Unit = {},
    onNavigateToBuscar: () -> Unit = {},
    targetLat: Double? = null,
    targetLng: Double? = null,
    viewModel: MapaViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(1) } // 1 = Explorar
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Estado de permisos de ubicación
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    // Determinar si se puede habilitar "My Location"
    val hasLocationPermission = locationPermissionsState.permissions.any { it.status.isGranted }
    
    // Solicitar permisos al inicio si no están concedidos
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }
    
    // Estado desde ViewModel
    val uiState by viewModel.uiState.collectAsState()
    
    // Estado para los registros del mapa
    val registros = when (val state = uiState) {
        is MapaViewModel.MapaUiState.Success -> state.registros
        else -> emptyList()
    }
    val isLoading = uiState is MapaViewModel.MapaUiState.Loading
    val errorMessage = (uiState as? MapaViewModel.MapaUiState.Error)?.message
    
    // Estado del filtro: "niveles" o "quejas"
    var filtroActivo by remember { mutableStateOf("niveles") } // "niveles" o "quejas"
    var showLugaresMenu by remember { mutableStateOf(false) }
    
    // Ubicación del usuario - Guardada para persistir entre navegaciones
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasObtainedLocation by remember { mutableStateOf(false) }
    
    // Posición inicial de la cámara (se actualizará con ubicación real)
    val defaultPosition = LatLng(19.4326, -99.1332)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation ?: defaultPosition, 12f)
    }
    
    // Obtener ubicación actual del usuario SOLO la primera vez Y solo si hay permisos
    LaunchedEffect(hasObtainedLocation, hasLocationPermission) {
        if (!hasObtainedLocation && hasLocationPermission) {
            scope.launch {
                try {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    val cancellationTokenSource = CancellationTokenSource()
                    
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location ->
                        location?.let {
                            val newPosition = LatLng(it.latitude, it.longitude)
                            userLocation = newPosition
                            hasObtainedLocation = true
                            // Mover cámara a la ubicación del usuario solo la primera vez
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(newPosition, 14f),
                                    durationMs = 1000
                                )
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("MapaExplorarScreen", "Error obteniendo ubicación: ${e.message}")
                    hasObtainedLocation = true // Marcar como intentado aunque falle
                }
            }
        }
    }
    
    // Mover cámara a ubicación objetivo si se proporciona
    LaunchedEffect(targetLat, targetLng) {
        if (targetLat != null && targetLng != null) {
            val targetPosition = LatLng(targetLat, targetLng)
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(targetPosition, 16f),
                durationMs = 1000
            )
        }
    }
    
    // Filtrar registros según el filtro activo
    val registrosFiltrados = remember(registros, filtroActivo) {
        when (filtroActivo) {
            "niveles" -> registros.filter { it.tipo != "queja" }
            "quejas" -> registros.filter { it.tipo == "queja" }
            else -> registros
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ExplorarTabButton(
                        text = "Medidas",
                        selected = selectedTab == 0,
                        onClick = { 
                            selectedTab = 0
                            onNavigateToMedidas()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ExplorarTabButton(
                        text = "Explorar",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                }

                // Mapa de Google con registros filtrados
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapType = MapType.TERRAIN,
                        isMyLocationEnabled = hasLocationPermission // Solo habilitar si hay permisos
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = hasLocationPermission, // Solo mostrar botón si hay permisos
                        mapToolbarEnabled = false
                    )
                ) {
                    // Marcadores de registros con colores según tipo y nivel de dB
                    registrosFiltrados.forEach { registro ->
                        val position = LatLng(registro.coordenadas.lat, registro.coordenadas.lng)
                        val markerColor = if (registro.tipo == "queja") {
                            BitmapDescriptorFactory.HUE_RED // Rojo para quejas
                        } else {
                            explorarGetMarkerColorForDb(registro.db) // Verde/Amarillo/Naranja para niveles
                        }
                        
                        val dbEntero = registro.db.toInt()
                        val sensacion = registro.sensacion?.takeIf { it.isNotBlank() } 
                            ?: explorarGetSensacionForDb(registro.db)
                        val fechaCorta = registro.fecha?.toDate()?.let { explorarFormatDate(it) } ?: "Sin fecha"
                        val direccionCorta = registro.direccion?.take(40) ?: "Sin ubicación"
                        
                        Marker(
                            state = MarkerState(position = position),
                            title = if (registro.tipo == "queja") {
                                "Queja - ${registro.origen_ruido ?: "Ruido"}"
                            } else {
                                "$dbEntero dB - $sensacion"
                            },
                            snippet = "$fechaCorta\n$direccionCorta",
                            icon = BitmapDescriptorFactory.defaultMarker(markerColor)
                        )
                    }
                }
                
                // Indicador de carga
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                // Botón "Lugares" flotante con menú desplegable
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 120.dp)
                ) {
                    Button(
                        onClick = { showLugaresMenu = !showLugaresMenu },
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = "Lugares",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lugares", color = Color.Black)
                        Icon(
                            if (showLugaresMenu) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.Black
                        )
                    }
                    
                    // Menú desplegable de filtros
                    DropdownMenu(
                        expanded = showLugaresMenu,
                        onDismissRequest = { showLugaresMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = if (filtroActivo == "niveles") Color(0xFF2900CC) else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Niveles de Ruido en calle",
                                        color = if (filtroActivo == "niveles") Color(0xFF2900CC) else Color.Black
                                    )
                                }
                            },
                            onClick = {
                                filtroActivo = "niveles"
                                showLugaresMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (filtroActivo == "quejas") Color.Red else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Quejas por ruido",
                                        color = if (filtroActivo == "quejas") Color.Red else Color.Black
                                    )
                                }
                            },
                            onClick = {
                                filtroActivo = "quejas"
                                showLugaresMenu = false
                            }
                        )
                    }
                }
                }
                
                // Mensaje de error
                errorMessage?.let { message ->
                    Snackbar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(message)
                    }
                }
            }
            
            // Barra de búsqueda inferior
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                onClick = onNavigateToBuscar
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Buscar lugar",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
internal fun ExplorarTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color.Black else Color.LightGray,
            contentColor = if (selected) Color.White else Color.DarkGray
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Text(text)
    }
}

/**
 * Determina el color del marcador según el nivel de dB
 * Verde: 0-70 dB
 * Amarillo: 71-85 dB
 * Naranja: >85 dB
 */
internal fun explorarGetMarkerColorForDb(db: Double): Float {
    return when {
        db <= 70.0 -> BitmapDescriptorFactory.HUE_GREEN    // 0-70 dB: Verde
        db <= 85.0 -> BitmapDescriptorFactory.HUE_YELLOW   // 71-85 dB: Amarillo
        else -> BitmapDescriptorFactory.HUE_ORANGE         // >85 dB: Naranja
    }
}

/**
 * Determina la sensación según el nivel de dB
 */
internal fun explorarGetSensacionForDb(db: Double): String {
    return when {
        db < 45.0 -> "Muy tranquilo"
        db < 55.0 -> "Tranquilo"
        db < 65.0 -> "Tolerable"
        db < 75.0 -> "Molesto"
        db < 85.0 -> "Muy molesto"
        else -> "Insoportable"
    }
}

/**
 * Formatea la fecha para mostrar en el info window (formato corto)
 */
internal fun explorarFormatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    return formatter.format(date)
}
