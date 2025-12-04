package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.text.SimpleDateFormat
import java.util.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import android.Manifest

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapaMedidasScreen(
    onNavigateToRegistro: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToTodos: () -> Unit,
    onNavigateToExplorar: () -> Unit = {},
    viewModel: MapaViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
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
                    android.util.Log.e("MapaMedidasScreen", "Error obteniendo ubicación: ${e.message}")
                    hasObtainedLocation = true // Marcar como intentado aunque falle
                }
            }
        }
    }
    
    Scaffold(
        floatingActionButton = {
            // FAB para nuevo registro - Redondeado y elevado
            FloatingActionButton(
                onClick = onNavigateToRegistro,
                containerColor = Color(0xFF2900CC),
                modifier = Modifier.offset(y = (-80).dp),
                shape = CircleShape // Totalmente redondeado
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Registrar",
                    tint = Color.White
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
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
                    TabButton(
                        text = "Medidas",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TabButton(
                        text = "Explorar",
                        selected = selectedTab == 1,
                        onClick = { 
                            selectedTab = 1
                            onNavigateToExplorar()
                        }
                    )
                }

                // Mapa de Google con registros
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
                        // Marcadores de registros con colores según nivel de dB y tipo
                        registros.forEach { registro ->
                            val position = LatLng(registro.coordenadas.lat, registro.coordenadas.lng)
                            val markerColor = getMarkerColor(registro.db, registro.tipo)
                            val dbEntero = registro.db.toInt()
                            val sensacion = registro.sensacion?.takeIf { it.isNotBlank() } 
                                ?: getSensacionForDb(registro.db)
                            val fechaHora = registro.fecha?.toDate()?.let { formatDateAndTime(it) } ?: Pair("Sin fecha", "")
                            val origenRuido = registro.origen_ruido?.takeIf { it.isNotBlank() } ?: "Origen no especificado"
                            
                            Marker(
                                state = MarkerState(position = position),
                                title = if (registro.tipo == "queja") {
                                    "Queja - $origenRuido"
                                } else {
                                    "$dbEntero dB - $sensacion"
                                },
                                snippet = "${fechaHora.first} | ${fechaHora.second}\n$origenRuido",
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
                    
                    // Mensaje de error
                    errorMessage?.let { message ->
                        Snackbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Text(message)
                        }
                    }
                }
            }

            // Bottom navigation bar - Rectángulo más compacto
            BottomNavigationBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                selectedItem = 1, // Medidas selected
                onItemSelected = { index ->
                    when (index) {
                        0 -> onNavigateToTodos()
                        1 -> { /* Already here */ }
                        2 -> onNavigateToStats()
                    }
                }
            )
        }
    }
}

/**
 * Determina el color del marcador según el nivel de dB y tipo de registro
 * Verde: 0-70 dB
 * Amarillo: 71-85 dB
 * Naranja: >85 dB
 * Rojo: Quejas (tipo = "queja")
 */
private fun getMarkerColor(db: Double, tipo: String): Float {
    // Rojo reservado para quejas
    if (tipo == "queja") {
        return BitmapDescriptorFactory.HUE_RED
    }
    
    // Para registros normales según nivel de dB
    return when {
        db <= 70.0 -> BitmapDescriptorFactory.HUE_GREEN    // 0-70 dB: Verde
        db <= 85.0 -> BitmapDescriptorFactory.HUE_YELLOW   // 71-85 dB: Amarillo
        else -> BitmapDescriptorFactory.HUE_ORANGE         // >85 dB: Naranja
    }
}

/**
 * Determina la sensación según el nivel de dB
 */
private fun getSensacionForDb(db: Double): String {
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
 * Formatea la fecha y hora por separado para mostrar en el info window
 */
private fun formatDateAndTime(date: Date): Pair<String, String> {
    val dateFormatter = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return Pair(dateFormatter.format(date), timeFormatter.format(date))
}

@Composable
private fun TabButton(
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

@Composable
private fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 40.dp, vertical = 12.dp) // Más padding horizontal para reducir ancho
            .wrapContentWidth(), // No llenar todo el ancho
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 4.dp), // Padding interno para controlar tamaño
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Public,
                selected = selectedItem == 0,
                onClick = { onItemSelected(0) }
            )
            Spacer(modifier = Modifier.width(24.dp)) // Espaciado entre iconos
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
