package com.example.amukisenseapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.amukisenseapp.ui.screens.*
import com.example.amukisenseapp.ui.viewmodel.AuthViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    onGoogleSignInClick: () -> Unit,
    authViewModel: AuthViewModel
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Inicio.route) {
            InicioScreen(
                onNavigateToLoginCorreo = {
                    navController.navigate(Screen.LoginCorreo.route)
                },
                onGoogleSignInClick = onGoogleSignInClick,
                viewModel = authViewModel
            )
        }

        composable(Screen.LoginCorreo.route) {
            LoginCorreoScreen(
                onNavigateBack = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.navigate(Screen.MapaMedidas.route) {
                        popUpTo(Screen.Inicio.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.MapaMedidas.route) {
            MapaMedidasScreen(
                onNavigateToRegistro = {
                    navController.navigate(Screen.MenuRegistro.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.EstadMisStats.route)
                },
                onNavigateToTodos = {
                    navController.navigate(Screen.EstadTodos.route)
                },
                onNavigateToExplorar = {
                    navController.navigate(Screen.MapaExplorar.route)
                }
            )
        }

        composable(
            route = Screen.MapaExplorar.route,
            arguments = listOf(
                navArgument("lat") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lng") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val latStr = backStackEntry.arguments?.getString("lat")
            val lngStr = backStackEntry.arguments?.getString("lng")
            val targetLat = latStr?.toDoubleOrNull()
            val targetLng = lngStr?.toDoubleOrNull()
            
            MapaExplorarScreen(
                onNavigateToMedidas = {
                    navController.popBackStack()
                },
                onNavigateToLugares = {
                    navController.navigate(Screen.MapaExplorarLugares.route)
                },
                onNavigateToBuscar = {
                    navController.navigate(Screen.MapaExplorarBuscar.route)
                },
                targetLat = targetLat,
                targetLng = targetLng
            )
        }

        composable(Screen.MapaExplorarBuscar.route) {
            MapaExplorarBuscarScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMap = { lat, lng ->
                    navController.navigate(Screen.MapaExplorar.createRoute(lat, lng)) {
                        popUpTo(Screen.MapaExplorar.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.MenuRegistro.route) {
            MenuRegistroScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToActivo = {
                    navController.navigate(Screen.RegistroActivo.route)
                },
                onNavigateToManual = {
                    navController.navigate(Screen.RegistroManual.route)
                },
                onNavigateToQueja = {
                    navController.navigate(Screen.RegistroQueja.route)
                },
                onNavigateToCaptura = {
                    navController.navigate(Screen.RegistroCaptura.route)
                }
            )
        }

        composable(Screen.RegistroActivo.route) {
            RegistroActivoScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RegistroManual.route) {
            RegistroManualScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(Screen.RegistroQueja.route) {
            RegistroQuejaScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(Screen.RegistroCaptura.route) {
            RegistroCapturaScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        // Placeholders Sprint 2+
        composable(Screen.EstadMisStats.route) {
            EstadMisStatsScreen(
                onNavigateToConfig = {
                    navController.navigate(Screen.EstadConfig.route)
                },
                onNavigateToRegistros = {
                    navController.navigate(Screen.EstadMisStatsRegistros.route)
                },
                onNavigateToQuejas = {
                    navController.navigate(Screen.EstadMisStatsQuejas.route)
                },
                onNavigateToTodos = {
                    navController.navigate(Screen.EstadTodos.route)
                },
                onNavigateToMedidas = {
                    navController.navigate(Screen.MapaMedidas.route)
                }
            )
        }

        composable(Screen.EstadTodos.route) {
            EstadTodosScreen(
                onNavigateToConfig = {
                    navController.navigate(Screen.EstadConfig.route)
                },
                onNavigateToMedidas = {
                    navController.navigate(Screen.MapaMedidas.route) {
                        popUpTo(Screen.MapaMedidas.route) { inclusive = true }
                    }
                },
                onNavigateToStats = {
                    navController.navigate(Screen.EstadMisStats.route)
                },
                onNavigateToSubidas = {
                    navController.navigate(Screen.EstadTodosSubidos.route)
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route)
                },
                onNavigateToMiembros = {
                    navController.navigate(Screen.MiembrosUnidos.route)
                }
            )
        }
        
        composable(Screen.EstadTodosSubidos.route) {
            EstadTodosSubidosScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.EstadConfig.route) {
            ConfigScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCuenta = {
                    navController.navigate(Screen.EstadConfigCuenta.route)
                },
                onLogout = {
                    navController.navigate(Screen.Inicio.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.EstadConfigCuenta.route) {
            CuentaScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCorreo = {
                    navController.navigate(Screen.EstadConfigCuentaCorreo.route)
                },
                onNavigateToEliminar = {
                    navController.navigate(Screen.EstadConfigCuentaEliminar.route)
                }
            )
        }
        
        composable(Screen.EstadConfigCuentaCorreo.route) {
            CorreoPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
        
        composable(Screen.EstadConfigCuentaEliminar.route) {
            EliminarCuentaScreen(
                onNavigateBack = { navController.popBackStack() },
                onAccountDeleted = {
                    navController.navigate(Screen.Inicio.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Placeholders para vistas pendientes
        composable(Screen.EstadMisStatsRegistros.route) {
            MisRegistrosScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { registroId ->
                    navController.navigate(Screen.EstadMisStatsRegistrosMod.createRoute(registroId))
                }
            )
        }
        
        composable(
            route = Screen.EstadMisStatsRegistrosMod.route,
            arguments = listOf(
                navArgument("registroId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val registroId = backStackEntry.arguments?.getString("registroId") ?: ""
            RegistroManualScreen(
                registroId = registroId,
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }
        
        composable(Screen.EstadMisStatsQuejas.route) {
            MisQuejasScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { quejaId ->
                    navController.navigate(Screen.EstadMisStatsQuejasMod.createRoute(quejaId))
                }
            )
        }
        
        composable(
            route = Screen.EstadMisStatsQuejasMod.route,
            arguments = listOf(
                navArgument("quejaId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val quejaId = backStackEntry.arguments?.getString("quejaId") ?: ""
            RegistroQuejaScreen(
                registroId = quejaId,
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.MiembrosUnidos.route) {
            MiembrosUnidosScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
