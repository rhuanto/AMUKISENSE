package com.example.amukisenseapp.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amukisenseapp.data.model.Coordenadas
import com.example.amukisenseapp.ui.viewmodel.RegistroViewModel
import com.example.amukisenseapp.util.AudioRecorder
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.ui.platform.LocalContext
import com.example.amukisenseapp.util.LocationManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Pantalla de Registro Manual de Ruido (VISTA 9)
 * 
 * Esta pantalla permite al usuario registrar manualmente el nivel de ruido ambiental.
 * Utiliza el micrófono del dispositivo para medir los decibelios en tiempo real y
 * el GPS para obtener la ubicación exacta del registro.
 * 
 * Funcionalidades principales:
 * - Medición en tiempo real de decibelios usando AudioRecorder
 * - Captura de ubicación GPS
 * - Formulario para tipo de lugar, nivel de sensación y comentarios
 * - Almacenamiento en Firestore
 * 
 * @param onNavigateBack Callback para volver a la pantalla anterior
 * @param onSaveSuccess Callback ejecutado cuando el registro se guarda exitosamente
 * @param viewModel ViewModel que maneja la lógica de negocio y estado
 * 
 * @author Tu nombre
 * @date Octubre 2025
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RegistroManualScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    registroId: String? = null,
    viewModel: RegistroViewModel = viewModel()
) {
    // Observamos los estados del ViewModel usando collectAsState para recomposición automática
    val registroState by viewModel.registroState.collectAsState()
    val currentDb by viewModel.currentDb.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val registroToEdit by viewModel.registroToEdit.collectAsState()

    // Modo edición si registroId no es nulo
    val isEditMode = registroId != null

    // Creamos una instancia del AudioRecorder para capturar audio del micrófono
    // remember asegura que se mantiene la misma instancia durante recomposiciones
    val audioRecorder = remember { AudioRecorder() }

    // Solicitamos los permisos necesarios usando Accompanist Permissions
    // RECORD_AUDIO: Para acceder al micrófono y medir decibelios
    // ACCESS_FINE_LOCATION: Para obtener las coordenadas GPS exactas
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // Variables de estado del formulario usando remember para persistir entre recomposiciones
    var tipoLugar by remember { mutableStateOf("Parque") }
    var sensacion by remember { mutableStateOf("Molesto") }
    var comentario by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("Obteniendo ubicación...") }
    
    // Estado para mostrar diálogo de confirmación
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // LocationManager para obtener GPS y dirección
    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }
    val scope = rememberCoroutineScope()

    // Cargar datos del registro si estamos en modo edición
    LaunchedEffect(registroId) {
        if (registroId != null) {
            viewModel.loadRegistro(registroId)
        }
    }

    // Actualizar campos del formulario cuando se carga el registro
    LaunchedEffect(registroToEdit) {
        registroToEdit?.let { registro ->
            tipoLugar = registro.tipo_lugar ?: "Parque"
            sensacion = registro.sensacion ?: "Molesto"
            comentario = registro.comentario ?: ""
            direccion = registro.direccion ?: "Ubicación desconocida"
        }
    }

    // OBTENER UBICACIÓN GPS Y DIRECCIÓN
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted && !isEditMode) {
            scope.launch {
                // Obtener ubicación GPS
                val location = locationManager.getCurrentLocation()
                location?.let {
                    viewModel.updateLocation(it)
                    // Obtener dirección legible
                    val address = locationManager.getAddressFromCoordinates(
                        it.latitude,
                        it.longitude
                    )
                    direccion = address
                }
            }
        }
    }

    // IMPLEMENTACIÓN REAL DE CAPTURA DE AUDIO CON EL MICRÓFONO
    // Solo en modo creación, no en modo edición
    LaunchedEffect(permissionsState.allPermissionsGranted, isEditMode) {
        if (permissionsState.allPermissionsGranted && !isEditMode) {
            audioRecorder.startRecording().collect { dbValue ->
                viewModel.updateDb(dbValue)
            }
        }
    }

    // DisposableEffect se ejecuta cuando la pantalla se desmonta
    // Es CRÍTICO liberar el recurso del micrófono cuando salimos de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            // Detenemos la grabación y liberamos el AudioRecord solo si no estamos en modo edición
            if (!isEditMode) {
                audioRecorder.stopRecording()
            }
        }
    }

    // Observamos el estado del registro para mostrar confirmación o error
    LaunchedEffect(registroState) {
        when (registroState) {
            is RegistroViewModel.RegistroState.Success -> {
                showSuccessDialog = true
            }
            is RegistroViewModel.RegistroState.Error -> {
                errorMessage = (registroState as RegistroViewModel.RegistroState.Error).message
                showErrorDialog = true
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Si no tenemos todos los permisos, mostramos la pantalla de solicitud
    if (!permissionsState.allPermissionsGranted) {
        PermissionRequestScreen(
            onRequestPermissions = { permissionsState.launchMultiplePermissionRequest() },
            onNavigateBack = onNavigateBack
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Modificar Registro Manual" else "Registro Ruido") },
                navigationIcon = {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color(0xFFF5F5F5)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Volver")
                        }
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // CARD PRINCIPAL: Muestra el nivel de decibelios capturado en tiempo real
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mostramos el valor de dB capturado del micrófono
                    // toInt() convierte el Double a entero para mejor legibilidad
                    Text(
                        text = "${currentDb.toInt()}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = getDbColor(currentDb) // Color dinámico según nivel de ruido
                    )
                    Text(
                        text = "dB",
                        fontSize = 24.sp,
                        color = getDbColor(currentDb)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fila con información de fecha/hora y ubicación GPS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Mostramos la hora actual formateada
                        Text(
                            text = java.text.SimpleDateFormat("hh:mm a\nMM/dd/yy", java.util.Locale.getDefault())
                                .format(java.util.Date()),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        // Mostramos la dirección en lugar de coordenadas
                        Text(
                            text = direccion,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DROPDOWN 1: Tipo de Lugar
            // Permite al usuario categorizar dónde se está haciendo la medición
            DropdownField(
                label = "Tipo de Lugar:",
                selectedValue = tipoLugar,
                options = listOf("Parque", "Estacionamiento", "Biblioteca", "Casa", "Oficina"),
                onValueChange = { tipoLugar = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // DROPDOWN 2: Nivel de Sensación
            // Captura la percepción subjetiva del usuario sobre el ruido
            DropdownField(
                label = "Nivel de Sensación:",
                selectedValue = sensacion,
                options = listOf("Relajante", "Agradable", "Neutral", "Molesto", "Muy molesto"),
                onValueChange = { sensacion = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CAMPO DE COMENTARIO
            // TextField multilinea con límite de 100 caracteres
            OutlinedTextField(
                value = comentario,
                onValueChange = { if (it.length <= 100) comentario = it }, // Validación de longitud
                label = { Text("Comentario:") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                supportingText = { Text("${comentario.length}/100") } // Contador de caracteres
            )

            Spacer(modifier = Modifier.height(32.dp))

            // BOTÓN GUARDAR/ACTUALIZAR
            Button(
                onClick = {
                    // Preparamos las coordenadas GPS (o valores por defecto si no hay ubicación)
                    val coords = currentLocation?.let {
                        Coordenadas(it.latitude, it.longitude)
                    } ?: registroToEdit?.coordenadas ?: Coordenadas()

                    if (isEditMode && registroId != null) {
                        // Modo edición: actualizar registro existente
                        viewModel.updateRegistroManual(
                            registroId = registroId,
                            db = currentDb,
                            tipoLugar = tipoLugar,
                            sensacion = sensacion,
                            comentario = comentario,
                            direccion = direccion,
                            coordenadas = coords
                        )
                    } else {
                        // Modo creación: crear nuevo registro
                        viewModel.createRegistroManual(
                            db = currentDb,
                            tipoLugar = tipoLugar,
                            sensacion = sensacion,
                            comentario = comentario,
                            direccion = direccion,
                            coordenadas = coords
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = registroState !is RegistroViewModel.RegistroState.Loading
            ) {
                if (registroState is RegistroViewModel.RegistroState.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        if (isEditMode) "Actualizar" else "Guardar",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }

            // Mostramos mensajes de error si algo falla
            if (registroState is RegistroViewModel.RegistroState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (registroState as RegistroViewModel.RegistroState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Éxito",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(64.dp)
                )
            },
            title = {
                Text(
                    text = "¡Registro exitoso!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Gracias por registrar el nivel de ruido. Tu aporte ayuda a mejorar nuestra comunidad.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.resetState()
                        onSaveSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Diálogo de error
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
            },
            title = {
                Text(
                    text = "Error al guardar",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("No se pudo guardar el registro: $errorMessage")
            },
            confirmButton = {
                Button(
                    onClick = { showErrorDialog = false }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}

/**
 * Componente de Dropdown reutilizable para selección de opciones.
 * 
 * Este composable crea un menú desplegable (ExposedDropdownMenu) que permite
 * al usuario seleccionar una opción de una lista predefinida.
 * 
 * @param label Etiqueta que se muestra encima del dropdown
 * @param selectedValue Valor actualmente seleccionado
 * @param options Lista de opciones disponibles para seleccionar
 * @param onValueChange Callback que se ejecuta cuando el usuario selecciona una opción
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    // Estado para controlar si el menú está expandido o colapsado
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Etiqueta del campo
        Text(
            text = label,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // ExposedDropdownMenuBox es un contenedor de Material3 para dropdowns
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it } // Toggle del estado expandido
        ) {
            // TextField que actúa como trigger del dropdown
            OutlinedTextField(
                value = selectedValue,
                onValueChange = {}, // ReadOnly, no permite escritura manual
                readOnly = true,
                trailingIcon = { 
                    // Icono de flecha que rota según el estado
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor() // Vincula el TextField con el menú
            )

            // Menú desplegable que muestra las opciones
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false } // Cierra al tocar fuera
            ) {
                // Iteramos sobre todas las opciones disponibles
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option) // Actualizamos el valor seleccionado
                            expanded = false      // Cerramos el menú
                        }
                    )
                }
            }
        }
    }
}

/**
 * Pantalla de solicitud de permisos.
 * 
 * Se muestra cuando el usuario no ha otorgado los permisos necesarios
 * (RECORD_AUDIO y ACCESS_FINE_LOCATION). Explica por qué se necesitan
 * y proporciona botones para solicitarlos o cancelar.
 * 
 * @param onRequestPermissions Callback para lanzar el diálogo de permisos del sistema
 * @param onNavigateBack Callback para volver atrás si el usuario cancela
 */
@Composable
private fun PermissionRequestScreen(
    onRequestPermissions: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permisos necesarios",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Esta función requiere acceso al micrófono y ubicación.",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(onClick = onRequestPermissions) {
            Text("Solicitar permisos")
        }
        TextButton(onClick = onNavigateBack) {
            Text("Cancelar")
        }
    }
}

/**
 * Función helper para determinar el color del texto de dB según el nivel de ruido.
 * 
 * Escala de colores basada en estándares de contaminación acústica:
 * - Verde (< 50 dB): Nivel tranquilo y cómodo
 * - Amarillo (50-70 dB): Nivel moderado
 * - Naranja (70-85 dB): Nivel elevado, puede causar molestias
 * - Rojo (> 85 dB): Nivel peligroso, puede causar daño auditivo
 * 
 * @param db Nivel de decibelios medido
 * @return Color Color correspondiente al nivel de ruido
 */
private fun getDbColor(db: Double): Color {
    return when {
        db < 50 -> Color(0xFF4CAF50)  // Verde - Tranquilo
        db < 70 -> Color(0xFFFFC107)  // Amarillo - Moderado
        db < 85 -> Color(0xFFFF9800)  // Naranja - Elevado
        else -> Color(0xFFF44336)     // Rojo - Peligroso
    }
}
