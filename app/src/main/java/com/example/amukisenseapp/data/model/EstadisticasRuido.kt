package com.example.amukisenseapp.data.model

/**
 * Modelo para la evolución temporal de ruido
 */
data class EvolucionTemporal(
    val fecha: String,          // "Nov 2025", "Dic 2025"
    val promedioDB: Double,
    val cantidadRegistros: Int
)

/**
 * Modelo para las calles más ruidosas
 */
data class CalleRuidosa(
    val calle: String,          // Nombre de la calle/avenida
    val distrito: String,
    val promedioDB: Double,
    val cantidadQuejas: Int
)

/**
 * Modelo para heatmap por horario
 */
data class RuidoPorHorario(
    val hora: Int,              // 0-23
    val promedioDB: Double,
    val cantidadRegistros: Int
)
