package com.example.amukisenseapp.navigation

sealed class Screen(val route: String) {
    // VISTA 1: Inicio
    object Inicio : Screen("inicio")
    
    // VISTA 2: Login - correo
    object LoginCorreo : Screen("login_correo")
    
    // VISTA 3: Mapa - Medidas
    object MapaMedidas : Screen("mapa_medidas")
    
    // VISTA 4: Mapa - Explorar
    object MapaExplorar : Screen("mapa_explorar?lat={lat}&lng={lng}") {
        fun createRoute(lat: Double? = null, lng: Double? = null): String {
            return if (lat != null && lng != null) {
                "mapa_explorar?lat=$lat&lng=$lng"
            } else {
                "mapa_explorar"
            }
        }
    }
    
    // VISTA 5: Mapa - Explorar - Buscar_Lugar
    object MapaExplorarBuscar : Screen("mapa_explorar_buscar")
    
    // VISTA 6: Mapa - Explorar - Lugares
    object MapaExplorarLugares : Screen("mapa_explorar_lugares")
    
    // VISTA 7: Menu - Registro
    object MenuRegistro : Screen("menu_registro")
    
    // VISTA 8: Menu - Registro - Activo
    object RegistroActivo : Screen("registro_activo")
    
    // VISTA 9: Menu - Registro - Manual1
    object RegistroManual : Screen("registro_manual")
    
    // VISTA 10: Menu - Registro - Queja1
    object RegistroQueja : Screen("registro_queja")
    
    // VISTA 11: Menu - Registro - Captura
    object RegistroCaptura : Screen("registro_captura")
    
    // VISTA 12: Estad. - Mis_stats
    object EstadMisStats : Screen("estad_mis_stats")
    
    // VISTA 13: Estad. - Todos
    object EstadTodos : Screen("estad_todos")
    
    // VISTA 14: Estad. - Config.
    object EstadConfig : Screen("estad_config")
    
    // VISTA 15: Estad. - Mis_stats - Registros
    object EstadMisStatsRegistros : Screen("estad_mis_stats_registros")
    
    // VISTA 16: Estad. - Mis_stats - Quejas
    object EstadMisStatsQuejas : Screen("estad_mis_stats_quejas")
    
    // VISTA 17: Estad. - Todos - Subidos
    object EstadTodosSubidos : Screen("estad_todos_subidos")
    
    // VISTA 18: Estad. - Config. - Cuenta
    object EstadConfigCuenta : Screen("estad_config_cuenta")
    
    // VISTA 19: Estad. - Config. - Cuenta - Correo
    object EstadConfigCuentaCorreo : Screen("estad_config_cuenta_correo")
    
    // VISTA 20: Estad. - Mis_stats - Registros - Mod
    object EstadMisStatsRegistrosMod : Screen("estad_mis_stats_registros_mod/{registroId}") {
        fun createRoute(registroId: String) = "estad_mis_stats_registros_mod/$registroId"
    }
    
    // VISTA 21: Estad. - Mis_stats - Quejas - Mod
    object EstadMisStatsQuejasMod : Screen("estad_mis_stats_quejas_mod/{quejaId}") {
        fun createRoute(quejaId: String) = "estad_mis_stats_quejas_mod/$quejaId"
    }
    
    // VISTA 22: Estad. - Config. - Cuenta - Eliminar
    object EstadConfigCuentaEliminar : Screen("estad_config_cuenta_eliminar")
    
    // VISTA 23: Dashboard - Estadísticas de Ruido
    object Dashboard : Screen("dashboard")
    
    // VISTA 24: Comunidad - Miembros Unidos
    object MiembrosUnidos : Screen("miembros_unidos")
}
