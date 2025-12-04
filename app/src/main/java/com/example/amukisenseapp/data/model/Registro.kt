package com.example.amukisenseapp.data.model

import com.google.firebase.Timestamp

data class Registro(
    val id: String = "",
    val uid_usuario: String = "",
    val tipo: String = "", // "manual" | "auto" | "queja" | "captura"
    val db: Double = 0.0,
    val coordenadas: Coordenadas = Coordenadas(),
    val geohash: String? = null,
    val tipo_lugar: String? = null,
    val sensacion: String? = null,
    val comentario: String? = null,
    val origen_ruido: String? = null, // solo para quejas
    val impacto: String? = null, // solo para quejas
    val fecha: Timestamp? = null,
    val imagen_url: String? = null,
    val direccion: String? = null,
    val auto_generado: Boolean = false,
    val distancia_m: Double? = null,
    val visible_publico: Boolean = true
)
