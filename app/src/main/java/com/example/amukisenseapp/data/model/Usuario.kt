package com.example.amukisenseapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Modelo de datos para la colección "usuarios" en Firestore
 * Documento: usuarios/{uid}
 */
data class Usuario(
    val uid: String = "",
    val nombre_usuario: String = "",
    val correo: String = "",
    val provider: String = "", // "google" | "email"
    @ServerTimestamp
    val fecha_union: Timestamp? = null,
    val foto_perfil_url: String? = null,
    val numero_usuario: Int? = null,
    val config: Config = Config(),
    val stats: StatsUsuario = StatsUsuario()
)

/**
 * Configuración del usuario
 */
data class ConfigUsuario(
    val notificaciones: Boolean = true,
    val registro_automatico: Boolean = false,
    val distancia_metros: Int = 200, // 200, 500 o 1000
    val auto_registro_activado: Boolean = false
)

/**
 * Estadísticas del usuario
 */
data class StatsUsuario(
    val registros_manual: Int = 0,
    val registros_auto: Int = 0,
    val quejas: Int = 0
)

