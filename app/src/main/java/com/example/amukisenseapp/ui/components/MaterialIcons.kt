package com.example.amukisenseapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Objeto que centraliza todos los íconos de Material Icons usados en la aplicación
 * Si un ícono no está disponible en Material Icons, se usa un ícono alternativo similar
 */
object AppIcons {
    // Navegación
    val ChevronRight: ImageVector
        @Composable get() = Icons.Default.KeyboardArrowRight
    
    // Visibilidad
    val Visibility: ImageVector
        @Composable get() = Icons.Default.Visibility
    
    val VisibilityOff: ImageVector
        @Composable get() = Icons.Default.VisibilityOff
    
    // Audio y Sonido
    val GraphicEq: ImageVector
        @Composable get() = Icons.Default.GraphicEq
    
    val Whatshot: ImageVector
        @Composable get() = Icons.Default.Whatshot
    
    // Mapas y Ubicación
    val Public: ImageVector
        @Composable get() = Icons.Default.Public
    
    val DirectionsWalk: ImageVector
        @Composable get() = Icons.Default.DirectionsWalk
    
    // Reportes y Problemas
    val ReportProblem: ImageVector
        @Composable get() = Icons.Default.ReportProblem
    
    // Cámara
    val CameraAlt: ImageVector
        @Composable get() = Icons.Default.CameraAlt
    
    // Nube y Upload
    val CloudUpload: ImageVector
        @Composable get() = Icons.Default.CloudUpload
    
    // Ordenamiento
    val SwapVert: ImageVector
        @Composable get() = Icons.Default.SwapVert
    
    // Grupos
    val Group: ImageVector
        @Composable get() = Icons.Default.Group
    
    // Automático
    val AutoMode: ImageVector
        @Composable get() = Icons.Default.AutoMode
    
    // Hexágono (usando alternativa)
    val HexagonOutlined: ImageVector
        @Composable get() = Icons.Outlined.Hexagon
}
