package com.example.amukisenseapp.data.model

data class Config(
    val notificaciones: Boolean = true,
    val registro_automatico: Boolean = false,
    val distancia_metros: Int = 200,
    val auto_registro_activado: Boolean = false
)
