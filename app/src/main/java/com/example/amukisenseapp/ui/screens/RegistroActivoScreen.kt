package com.example.amukisenseapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amukisenseapp.ui.viewmodel.ConfigViewModel
import androidx.compose.material.icons.filled.*
import com.example.amukisenseapp.service.LocationTrackingService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RegistroActivoScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConfigViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()
    val context = LocalContext.current
    
    var locationEnabled by remember { mutableStateOf(false) }
    var selectedDistance by remember { mutableStateOf(200) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Sincronizar con la configuración guardada al cargar
    LaunchedEffect(config) {
        locationEnabled = config.auto_registro_activado
        selectedDistance = config.distancia_metros
    }

    // Solicitar permisos necesarios para Android 13+
    val permissionsToRequest = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
    
    val permissionsState = rememberMultiplePermissionsState(permissionsToRequest)
    
    // Launcher para solicitar permiso de ubicación en segundo plano (Android 10+)
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("RegistroActivo", "Permiso de ubicación en segundo plano otorgado")
        } else {
            android.util.Log.d("RegistroActivo", "Permiso de ubicación en segundo plano denegado")
            locationEnabled = false
        }
    }
    
    // Función para iniciar el servicio
    fun startLocationService() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_SERVICE
            putExtra("distancia_metros", selectedDistance)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        android.util.Log.d("RegistroActivo", "Servicio de ubicación iniciado con distancia: $selectedDistance m")
    }
    
    // Función para detener el servicio
    fun stopLocationService() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
        android.util.Log.d("RegistroActivo", "Servicio de ubicación detenido")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitud de Permiso") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Icono
            Icon(
                Icons.Default.DirectionsWalk,
                contentDescription = null,
                tint = Color(0xFF2900CC),
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "Registro Activo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Al darnos acceso a tu ubicación, podemos vincular tus mediciones a un lugar específico, de forma anónima.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Activar permiso")
                        Switch(
                            checked = locationEnabled,
                            onCheckedChange = { newValue ->
                                if (newValue) {
                                    // Usuario quiere activar - solicitar permisos
                                    if (permissionsState.allPermissionsGranted) {
                                        locationEnabled = true
                                    } else {
                                        permissionsState.launchMultiplePermissionRequest()
                                        showPermissionDialog = true
                                    }
                                } else {
                                    // Usuario quiere desactivar
                                    locationEnabled = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2900CC)
                            )
                        )
                    }
                    
                    // Mostrar estado de permisos
                    if (locationEnabled && !permissionsState.allPermissionsGranted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ Se requieren permisos de ubicación para activar esta función",
                            fontSize = 12.sp,
                            color = Color(0xFFFF9800),
                            textAlign = TextAlign.Center
                        )
                    }
                
                }
            }

            // Selector de distancia
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Distancia para nuevo registro (metros):",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                var expanded by remember { mutableStateOf(false) }
                val distances = listOf(200, 500, 1000)

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedDistance.toString(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        distances.forEach { distance ->
                            DropdownMenuItem(
                                text = { Text("$distance m") },
                                onClick = {
                                    selectedDistance = distance
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón Confirmar
            Button(
                onClick = {
                    // Guardar configuración en Firebase
                    viewModel.updateConfig(
                        config.copy(
                            auto_registro_activado = locationEnabled,
                            distancia_metros = selectedDistance
                        )
                    )
                    
                    // Iniciar o detener servicio según estado
                    if (locationEnabled && permissionsState.allPermissionsGranted) {
                        startLocationService()
                    } else {
                        stopLocationService()
                    }
                    
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirmar", fontSize = 16.sp, color = Color.White)
            }
        }
    }
    
    // Diálogo explicativo de permisos
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF2900CC),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Permisos necesarios",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Para el registro automático necesitamos:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Ubicación en segundo plano")
                    Text("• Notificaciones")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Esto nos permite registrar ruido automáticamente mientras caminas.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    }
                ) {
                    Text("Entendido")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPermissionDialog = false
                        locationEnabled = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
