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
 * Pantalla de Registro de Queja de Ruido (VISTA 10)
 * 
 * Esta pantalla es específica para registrar quejas formales sobre ruido excesivo.
 * A diferencia del registro manual, incluye campos adicionales para:
 * - Identificar el origen específico del ruido molesto
 * - Documentar el impacto en la salud/bienestar del usuario
 * - Niveles de sensación más detallados orientados a quejas formales
 * 
 * La medición de dB se hace en tiempo real usando el micrófono del dispositivo,
 * generalmente esperando niveles más altos (60-100 dB) que justifiquen una queja.
 * 
 * Los datos se guardan en Firestore con tipo="queja" para diferenciarlos
 * de registros manuales normales.
 * 
 * @param onNavigateBack Callback para volver a la pantalla anterior
 * @param onSaveSuccess Callback ejecutado cuando la queja se registra exitosamente
 * @param viewModel ViewModel compartido que maneja la lógica de registro
 * 
 * @author Tu nombre
 * @date Octubre 2025
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RegistroQuejaScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    registroId: String? = null,
    viewModel: RegistroViewModel = viewModel()
) {
    // Observamos los estados del ViewModel
    val registroState by viewModel.registroState.collectAsState()
    val currentDb by viewModel.currentDb.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val registroToEdit by viewModel.registroToEdit.collectAsState()

    // Modo edición si registroId no es nulo
    val isEditMode = registroId != null

    // Creamos el AudioRecorder para captura real del micrófono
    val audioRecorder = remember { AudioRecorder() }

    // Solicitamos permisos necesarios
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // Estados del formulario de queja con valores por defecto relevantes
    var origenRuido by remember { mutableStateOf("Tráfico") }
    var impacto by remember { mutableStateOf("Dificultad dormir") }
    var sensacion by remember { mutableStateOf("Muy Molesto") }
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
            origenRuido = registro.origen_ruido ?: "Tráfico"
            impacto = registro.impacto ?: "Dificultad dormir"
            sensacion = registro.sensacion ?: "Muy Molesto"
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

    // CAPTURA REAL DE AUDIO DEL MICRÓFONO
    // Solo en modo creación, no en modo edición
    LaunchedEffect(permissionsState.allPermissionsGranted, isEditMode) {
        if (permissionsState.allPermissionsGranted && !isEditMode) {
            audioRecorder.startRecording().collect { dbValue ->
                viewModel.updateDb(dbValue)
            }
        }
    }

    // Liberamos el micrófono cuando salimos de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            if (!isEditMode) {
                audioRecorder.stopRecording()
            }
        }
    }

    // Navegamos al éxito cuando se completa el registro
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

    // Si no tenemos permisos, mostramos pantalla de solicitud
    if (!permissionsState.allPermissionsGranted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Permisos necesarios", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Esta función requiere acceso al micrófono y ubicación.")
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Solicitar permisos")
            }
            TextButton(onClick = onNavigateBack) {
                Text("Cancelar")
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Modificar Queja" else "Registro Queja") },
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
            // CARD PRINCIPAL: Muestra el nivel de decibelios en tiempo real
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Valor de dB capturado en tiempo real del micrófono
                    Text(
                        text = "${currentDb.toInt()}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336) // Rojo fijo para quejas (>60dB esperado)
                    )
                    Text(text = "dB", fontSize = 24.sp, color = Color(0xFFF44336))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Información de hora y ubicación
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
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

            // DROPDOWN 1: Origen del Ruido
            // Permite al usuario identificar la fuente específica del ruido molesto
            // Opciones basadas en las fuentes más comunes de quejas de ruido urbano
            DropdownQuejaField(
                label = "Cual es origen del molesto ruido:",
                selectedValue = origenRuido,
                options = listOf(
                    "Obras/Construcción",                  // Maquinaria pesada, martillos
                    "Tráfico",                              // Vehículos, bocinas, motores
                    "Vecinos/Actividades domésticas",       // Música, fiestas, electrodomésticos
                    "Aparatos o fallas mecánicas",          // Equipos industriales, extractores
                    "Aeronaves"                             // Aviones, helicópteros
                ),
                onValueChange = { origenRuido = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // DROPDOWN 2: Impacto en la Salud/Bienestar
            // Documenta cómo el ruido está afectando al usuario
            // Útil para justificar la queja y medir severidad
            DropdownQuejaField(
                label = "Como te hizo sentir:",
                selectedValue = impacto,
                options = listOf(
                    "Irritabilidad/Mal humor",    // Efecto psicológico leve
                    "Dificultad dormir",           // Interferencia con el descanso
                    "Falta de concentración",      // Afecta productividad/estudios
                    "Estrés o Ansiedad",           // Impacto en salud mental
                    "Dolor de cabeza"              // Síntoma físico directo
                ),
                onValueChange = { impacto = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // DROPDOWN 3: Nivel de Sensación
            // Escala más detallada y formal que en registro manual
            // Orientada a documentar severidad para quejas formales
            DropdownQuejaField(
                label = "Nivel Sensación:",
                selectedValue = sensacion,
                options = listOf(
                    "Molestia menor (Ocasional)",                      // Poco frecuente
                    "Molestia tolerable (Recurrente)",                 // Varias veces
                    "Interrupción significativa (Informar)",           // Afecta actividades
                    "Interferencia constante (Acción inmediata)",      // Impide vida normal
                    "Situación insostenible (Queja formal)"            // Requiere intervención
                ),
                onValueChange = { sensacion = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CAMPO DE COMENTARIO
            // Espacio para detalles adicionales que el usuario quiera documentar
            OutlinedTextField(
                value = comentario,
                onValueChange = { if (it.length <= 100) comentario = it },
                label = { Text("Comentario:") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                supportingText = { Text("${comentario.length}/100") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // BOTÓN GUARDAR/ACTUALIZAR QUEJA
            Button(
                onClick = {
                    val coords = currentLocation?.let {
                        Coordenadas(it.latitude, it.longitude)
                    } ?: registroToEdit?.coordenadas ?: Coordenadas()

                    if (isEditMode && registroId != null) {
                        // Modo edición: actualizar queja existente
                        viewModel.updateRegistroQueja(
                            registroId = registroId,
                            db = currentDb,
                            origenRuido = origenRuido,
                            impacto = impacto,
                            sensacion = sensacion,
                            comentario = comentario,
                            direccion = direccion,
                            coordenadas = coords
                        )
                    } else {
                        // Modo creación: crear nueva queja
                        viewModel.createRegistroQueja(
                            db = currentDb,
                            origenRuido = origenRuido,
                            impacto = impacto,
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
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

            // Muestra mensajes de error si algo falla al guardar
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
                    text = "¡Queja registrada!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Gracias por reportar. Tu queja ha sido registrada y ayudará a identificar fuentes de contaminación acústica.")
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
                Text("No se pudo guardar la queja: $errorMessage")
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
 * Componente Dropdown especializado para la pantalla de quejas.
 * 
 * Similar al DropdownField del registro manual, pero con nombre específico
 * para mantener separación de concerns entre las pantallas.
 * 
 * @param label Etiqueta descriptiva del campo
 * @param selectedValue Valor actualmente seleccionado
 * @param options Lista de opciones disponibles
 * @param onValueChange Callback que se ejecuta al seleccionar una opción
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownQuejaField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedValue,
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
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
