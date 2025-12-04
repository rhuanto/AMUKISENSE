package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * VISTA 20: Estad. - Mis_stats - Registros - Mod
 * 
 * Funcionalidad ACTIVA:
 * - Carga de datos del registro desde Firebase
 * - Edición de campos permitidos
 * - Guardado de cambios en Firebase
 * 
 * Funcionalidad INACTIVA:
 * - dB, fecha y hora (NO editables - históricos)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadMisStatsRegistrosModScreen(
    registroId: String,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    // TODO: Cargar datos del registro desde Firebase
    val dbHistorico by remember { mutableStateOf("70") }
    val fechaHistorica by remember { mutableStateOf("01:10 pm\n10/10/25") }
    
    var direccion by remember { mutableStateOf("Av. los clavelessd...") }
    var tipoLugar by remember { mutableStateOf("Parque") }
    var sensacion by remember { mutableStateOf("Muy molesto") }
    var comentario by remember { mutableStateOf("El ruido fue muy molesto.....") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro Ruido") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card principal con datos NO EDITABLES
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dbHistorico,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2900CC)
                    )
                    Text("dB", fontSize = 24.sp, color = Color(0xFF2900CC))
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = fechaHistorica,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dirección (EDITABLE)
            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección:") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tipo de Lugar (EDITABLE)
            var expandedTipo by remember { mutableStateOf(false) }
            val tiposLugar = listOf("Parque", "Estacionamiento", "Biblioteca", "Casa", "Oficina")

            ExposedDropdownMenuBox(
                expanded = expandedTipo,
                onExpandedChange = { expandedTipo = it }
            ) {
                OutlinedTextField(
                    value = tipoLugar,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de lugar:") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedTipo,
                    onDismissRequest = { expandedTipo = false }
                ) {
                    tiposLugar.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo) },
                            onClick = {
                                tipoLugar = tipo
                                expandedTipo = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nivel de Sensación (EDITABLE)
            var expandedSensacion by remember { mutableStateOf(false) }
            val sensaciones = listOf("Relajante", "Agradable", "Neutral", "Molesto", "Muy molesto")

            ExposedDropdownMenuBox(
                expanded = expandedSensacion,
                onExpandedChange = { expandedSensacion = it }
            ) {
                OutlinedTextField(
                    value = sensacion,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Nivel Sensación:") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSensacion) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedSensacion,
                    onDismissRequest = { expandedSensacion = false }
                ) {
                    sensaciones.forEach { sens ->
                        DropdownMenuItem(
                            text = { Text(sens) },
                            onClick = {
                                sensacion = sens
                                expandedSensacion = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Comentario (EDITABLE)
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

            // Botón Guardar
            Button(
                onClick = {
                    isLoading = true
                    // TODO: Actualizar en Firebase
                    onSaveSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Guardar", fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

/**
 * VISTA 21: Estad. - Mis_stats - Quejas - Mod
 * 
 * Similar a la vista de modificación de registros, pero para quejas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadMisStatsQuejasModScreen(
    quejaId: String,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    // TODO: Cargar datos de la queja desde Firebase
    val dbHistorico by remember { mutableStateOf("70") }
    val fechaHistorica by remember { mutableStateOf("01:10 pm\n10/10/25") }
    val ubicacionHistorica by remember { mutableStateOf("Av. los clavelessd...") }
    
    var origenRuido by remember { mutableStateOf("Tráfico") }
    var impacto by remember { mutableStateOf("Dificultad dormir") }
    var sensacion by remember { mutableStateOf("Muy Molesto") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro Queja") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card con datos NO EDITABLES
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(dbHistorico, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                    Text("dB", fontSize = 24.sp, color = Color(0xFFF44336))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(fechaHistorica, fontSize = 12.sp, color = Color.Gray)
                    Text(ubicacionHistorica, fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Origen del Ruido (EDITABLE)
            var expandedOrigen by remember { mutableStateOf(false) }
            val origenes = listOf("Obras/Construcción", "Tráfico", "Vecinos/Actividades domésticas", "Aparatos o fallas mecánicas", "Aeronaves")

            ExposedDropdownMenuBox(expanded = expandedOrigen, onExpandedChange = { expandedOrigen = it }) {
                OutlinedTextField(
                    value = origenRuido,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cual es origen del molesto ruido:") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOrigen) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedOrigen, onDismissRequest = { expandedOrigen = false }) {
                    origenes.forEach { origen ->
                        DropdownMenuItem(text = { Text(origen) }, onClick = { origenRuido = origen; expandedOrigen = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Impacto (EDITABLE)
            var expandedImpacto by remember { mutableStateOf(false) }
            val impactos = listOf("Irritabilidad/Mal humor", "Dificultad dormir", "Falta de concentración", "Estrés o Ansiedad", "Dolor de cabeza")

            ExposedDropdownMenuBox(expanded = expandedImpacto, onExpandedChange = { expandedImpacto = it }) {
                OutlinedTextField(
                    value = impacto,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Como te hizo sentir:") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedImpacto) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedImpacto, onDismissRequest = { expandedImpacto = false }) {
                    impactos.forEach { imp ->
                        DropdownMenuItem(text = { Text(imp) }, onClick = { impacto = imp; expandedImpacto = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nivel de Sensación (EDITABLE)
            var expandedSensacion by remember { mutableStateOf(false) }
            val sensaciones = listOf("Molestia menor (Ocasional)", "Molestia tolerable (Recurrente)", "Interrupción significativa (Informar)", "Interferencia constante (Acción inmediata)", "Situación insostenible (Queja formal)")

            ExposedDropdownMenuBox(expanded = expandedSensacion, onExpandedChange = { expandedSensacion = it }) {
                OutlinedTextField(
                    value = sensacion,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Nivel Sensación:") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSensacion) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedSensacion, onDismissRequest = { expandedSensacion = false }) {
                    sensaciones.forEach { sens ->
                        DropdownMenuItem(text = { Text(sens) }, onClick = { sensacion = sens; expandedSensacion = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isLoading = true
                    // TODO: Actualizar en Firebase
                    onSaveSuccess()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Guardar", fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}
