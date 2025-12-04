package com.example.amukisenseapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amukisenseapp.ui.viewmodel.ConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuRegistroScreen(
    onNavigateBack: () -> Unit,
    onNavigateToActivo: () -> Unit,
    onNavigateToManual: () -> Unit,
    onNavigateToQueja: () -> Unit,
    onNavigateToCaptura: () -> Unit,
    viewModel: ConfigViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()

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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Seleccione una de las opciones",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Sección: Tipo de Registro
            Text(
                text = "Tipo Registro",
                fontSize = 14.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                MenuOption(
                    icon = Icons.Default.AutoMode,
                    title = "Registro Activo",
                    onClick = onNavigateToActivo
                )
                Divider()
                MenuOption(
                    icon = Icons.Default.Edit,
                    title = "Registro Manual",
                    onClick = onNavigateToManual
                )
                Divider()
                MenuOption(
                    icon = Icons.Default.ReportProblem,
                    title = "Registro Queja",
                    onClick = onNavigateToQueja
                )
                Divider()
                MenuOption(
                    icon = Icons.Default.CameraAlt,
                    title = "Captura",
                    onClick = onNavigateToCaptura
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sección: Otros Ajustes
            Text(
                text = "Otros ajustes",
                fontSize = 14.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                MenuToggleOption(
                    icon = Icons.Default.LocationOn,
                    title = "Localización",
                    checked = config.registro_automatico,
                    onCheckedChange = {
                        viewModel.updateConfig(config.copy(registro_automatico = it))
                    }
                )
                Divider()
                MenuToggleOption(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones",
                    checked = config.notificaciones,
                    onCheckedChange = {
                        viewModel.updateConfig(config.copy(notificaciones = it))
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuOption(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF2900CC),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

@Composable
private fun MenuToggleOption(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
            tint = Color(0xFF2900CC),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2900CC)
            )
        )
    }
}
