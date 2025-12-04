package com.example.amukisenseapp.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
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
 * Pantalla de Registro con Captura de Foto (VISTA 11)
 * 
 * Esta pantalla simula una interfaz de cámara para registrar ruido con fotografía asociada.
 * 
 * Características según especificación:
 * - Fondo oscuro (negro/gris muy oscuro) simulando vista de cámara en vivo
 * - Datos superpuestos en tiempo real (color blanco):
 *   1. Nivel de Decibelios: Valor grande actualizado en vivo del micrófono
 *   2. Nivel de Sensación: Asignado automáticamente según rango de dB
 *   3. Dirección: Geolocalización automática (no editable)
 * 
 * Botones:
 * - Superior izquierda: Flecha atrás en círculo (retroceder y cerrar cámara)
 * - Superior derecha: Dos flechas circulares (volver a tomar foto) - solo visible post-captura
 * - Inferior central: Círculo blanco "Foto" para capturar (modo en vivo)
 * - Inferior: Botón negro rectangular "Guardar" (post-captura)
 * 
 * Post-Captura:
 * - La imagen se vuelve estática mostrando la foto tomada
 * - Aparece botón para retomar
 * - Aparece botón Guardar que registra en Firebase con compresión de imagen
 * 
 * @param onNavigateBack Callback para volver a Menu - Registro
 * @param onSaveSuccess Callback ejecutado tras guardar exitosamente
 * @param viewModel ViewModel que maneja la lógica de registro
 * 
 * @author Tu nombre
 * @date Octubre 2025
 */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RegistroCapturaScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: RegistroViewModel = viewModel()
) {
    // Observamos los estados del ViewModel
    val registroState by viewModel.registroState.collectAsState()
    val currentDb by viewModel.currentDb.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    // Estado para la URI de la imagen capturada
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Dirección obtenida por geolocalización REAL
    var direccion by remember { mutableStateOf("Obteniendo ubicación...") }

    // Estado para mostrar diálogo de confirmación
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Creamos el AudioRecorder para captura real del micrófono
    val audioRecorder = remember { AudioRecorder() }
    
    // LocationManager para obtener GPS y dirección
    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }
    val scope = rememberCoroutineScope()

    // Solicitamos TRES permisos necesarios para esta pantalla
    // CAMERA: Para capturar la foto
    // RECORD_AUDIO: Para medir decibelios en tiempo real
    // ACCESS_FINE_LOCATION: Para coordenadas GPS y dirección
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // Creamos un archivo temporal en el cache del contexto donde guardar la foto
    val tempPhotoFile = remember {
        val file = java.io.File(
            context.cacheDir,
            "registro_${System.currentTimeMillis()}.jpg"
        )
        file
    }
    
    // Creamos la URI del archivo temporal usando FileProvider
    val tempPhotoUri = remember {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempPhotoFile
        )
    }

    // Launcher para capturar foto - usa TakePicture que abre la cámara directamente
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // La foto se guardó exitosamente en tempPhotoUri
            capturedImageUri = tempPhotoUri
        } else {
            // El usuario canceló o hubo error
            capturedImageUri = null
        }
    }

    // OBTENER UBICACIÓN GPS Y DIRECCIÓN
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
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

    // CAPTURA REAL DE AUDIO DEL MICRÓFONO
    // Iniciamos la captura cuando se monta la pantalla
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            // Recibimos lecturas continuas de dB del micrófono
            audioRecorder.startRecording().collect { dbValue ->
                viewModel.updateDb(dbValue)
            }
        }
    }

    // Liberamos el recurso del micrófono al salir
    DisposableEffect(Unit) {
        onDispose {
            audioRecorder.stopRecording()
        }
    }

    // Navegamos al éxito cuando se completa el registro
    LaunchedEffect(registroState) {
        when (registroState) {
            is RegistroViewModel.RegistroState.Success -> {
                audioRecorder.stopRecording() // Detener micrófono antes de mostrar diálogo
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

    // Pantalla de solicitud de permisos
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
            Text("Esta función requiere acceso a cámara, micrófono y ubicación.")
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

    // INTERFAZ PRINCIPAL: Box con fondo oscuro simulando vista de cámara
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Fondo negro como especifica la descripción
    ) {
        // MODO EN VIVO vs POST-CAPTURA
        // Si no hay imagen capturada: fondo gris muy oscuro simulando vista de cámara en vivo
        // Si hay imagen: se muestra la foto estática
        if (capturedImageUri != null) {
            // POST-CAPTURA: Mostramos la imagen estática tomada
            Image(
                painter = rememberAsyncImagePainter(capturedImageUri),
                contentDescription = "Imagen capturada",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // MODO EN VIVO: Simulación de vista de cámara (fondo gris muy oscuro)
            // En una implementación completa con CameraX, aquí iría el Preview de la cámara
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A1A)), // Gris muy oscuro
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Vista de cámara en vivo\n(Simulación)",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // OVERLAY: Datos superpuestos sobre la vista de cámara
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 24.dp) // Más espacio desde arriba
        ) {
            // === PARTE SUPERIOR: BOTONES DE NAVEGACIÓN ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // BOTÓN SUPERIOR IZQUIERDA: Flecha atrás en círculo
                // Función: Retroceder a Menu - Registro y cerrar interfaz de cámara
                IconButton(
                    onClick = {
                        audioRecorder.stopRecording() // Detener micrófono antes de salir
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f), 
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowBack, 
                        contentDescription = "Volver y cerrar cámara",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // BOTÓN SUPERIOR DERECHA: Dos flechas circulares (Refresh)
                // Solo visible POST-CAPTURA para volver a tomar la foto
                if (capturedImageUri != null) {
                    IconButton(
                        onClick = { 
                            // Limpiamos la imagen capturada para volver al modo en vivo
                            capturedImageUri = null 
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color.White.copy(alpha = 0.2f), 
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Volver a tomar foto",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Espaciador para empujar contenido hacia abajo
            Spacer(modifier = Modifier.weight(1f))

            // === DATOS SUPERPUESTOS EN TIEMPO REAL ===
            // Todos los textos en color blanco como indica la descripción
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. NIVEL DE DECIBELIOS
                // Fuente de tamaño grande, se actualiza en vivo según ruido captado
                Text(
                    text = "${currentDb.toInt()}",
                    fontSize = 72.sp, // Tamaño grande
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "dB", 
                    fontSize = 36.sp, 
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. NIVEL DE SENSACIÓN
                // NO es campo desplegable, se asigna automáticamente según rango de dB
                // Lista: "relajante", "agradable", "neutral", "molesto", "muy molesto"
                Text(
                    text = getSensacionFromDb(currentDb),
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. DIRECCIÓN
                // Ubicación geolocalizada automática (no editable)
                // Se rellena en base a la geolocalización del smartphone
                Text(
                    text = direccion,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // === BOTONES INFERIORES: CAPTURA Y GUARDADO ===
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (capturedImageUri == null) {
                    // MODO EN VIVO: Botón de Captura
                    // Círculo blanco (o claro) etiquetado como "Foto" debajo
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FloatingActionButton(
                            onClick = {
                                // Lanzamos la cámara directamente con la URI temporal
                                takePictureLauncher.launch(tempPhotoUri)
                            },
                            containerColor = Color.White,
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Tomar foto",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Foto",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    // POST-CAPTURA: Botón de Guardar
                    // Botón grande, alargado, rectangular con esquinas redondeadas
                    // Coloración negra con texto blanco "Guardar"
                    Button(
                        onClick = {
                            // Preparamos coordenadas GPS
                            val coords = currentLocation?.let {
                                Coordenadas(it.latitude, it.longitude)
                            } ?: Coordenadas()

                            // Obtenemos sensación automática según dB actual
                            val sensacion = getSensacionFromDb(currentDb)

                            // Realizamos validación y registro de todos los datos:
                            // - decibelios
                            // - sensación
                            // - ubicación
                            // - foto (con compresión para optimizar almacenamiento)
                            viewModel.createRegistroCaptura(
                                db = currentDb,
                                sensacion = sensacion,
                                direccion = direccion,
                                coordenadas = coords,
                                imageUri = capturedImageUri!!
                            )
                            // Tras registro exitoso, redirige a Menu - Registro
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
                                "Guardar", 
                                fontSize = 18.sp, 
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Mostrar errores si los hay
                if (registroState is RegistroViewModel.RegistroState.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = (registroState as RegistroViewModel.RegistroState.Error).message,
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
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
                    text = "¡Captura registrada!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Gracias por registrar. La foto y los datos de ruido se han guardado exitosamente.")
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
                    tint = Color(0xFFF44336),
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
 * Función auxiliar que asigna automáticamente el nivel de sensación
 * según el rango de decibelios actual.
 * 
 * Lista de sensaciones (según especificación):
 * - "Relajante": 0-30 dB
 * - "Agradable": 31-50 dB
 * - "Neutral": 51-70 dB
 * - "Molesto": 71-90 dB
 * - "Muy molesto": 91 dB o más
 * 
 * @param db Nivel de decibelios actual capturado del micrófono
 * @return String Nivel de sensación correspondiente (capitalizado)
 */
private fun getSensacionFromDb(db: Double): String {
    return when {
        db <= 30 -> "Relajante"
        db <= 50 -> "Agradable"
        db <= 70 -> "Neutral"
        db <= 90 -> "Molesto"
        else -> "Muy molesto"
    }
}
