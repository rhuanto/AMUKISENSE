package com.example.amukisenseapp.data.model

/**
 * Modelo de datos para representar estadísticas de quejas agrupadas por distrito
 */
data class QuejasPorDistrito(
    val distrito: String,
    val cantidad: Int
)
