package com.example.amukisenseapp.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gestor de ubicación GPS para obtener coordenadas y direcciones.
 * 
 * Esta clase proporciona métodos para:
 * - Obtener la ubicación actual del dispositivo
 * - Convertir coordenadas a dirección legible (Geocoding reverso)
 * 
 * Usa Google Play Services FusedLocationProvider para precisión óptima.
 */
class LocationManager(context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())
    
    /**
     * Obtiene la ubicación actual del dispositivo.
     * 
     * Requiere permisos ACCESS_FINE_LOCATION o ACCESS_COARSE_LOCATION.
     * 
     * @return Location objeto con latitud, longitud, precisión, etc.
     * @throws SecurityException si no se tienen los permisos necesarios
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            // Usar última ubicación conocida primero (más rápido)
            val lastLocation = fusedLocationClient.lastLocation.await()
            
            if (lastLocation != null && isLocationRecent(lastLocation)) {
                lastLocation
            } else {
                // Si no hay última ubicación o es muy antigua, solicitar nueva
                getCurrentLocationFresh()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Solicita una nueva ubicación GPS actualizada.
     * Puede tardar varios segundos dependiendo de la señal GPS.
     */
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocationFresh(): Location? {
        return try {
            val cancellationTokenSource = CancellationTokenSource()
            
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Verifica si una ubicación es reciente (menos de 2 minutos).
     */
    private fun isLocationRecent(location: Location): Boolean {
        val twoMinutesInMillis = 2 * 60 * 1000
        return (System.currentTimeMillis() - location.time) < twoMinutesInMillis
    }
    
    /**
     * Convierte coordenadas GPS a dirección legible (Geocoding reverso).
     * 
     * Ejemplos de direcciones obtenidas:
     * - "Calle 123, Colonia Centro, Ciudad"
     * - "Av. Principal 456, Zona Norte"
     * 
     * @param latitude Latitud de la coordenada
     * @param longitude Longitud de la coordenada
     * @return String dirección legible o "Ubicación desconocida" si falla
     */
    suspend fun getAddressFromCoordinates(
        latitude: Double,
        longitude: Double
    ): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // API 33+ usa callback asíncrono
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: List<Address>) {
                                val address = addresses.firstOrNull()?.let { formatAddress(it) }
                                    ?: "Ubicación desconocida"
                                continuation.resume(address)
                            }
                            
                            override fun onError(errorMessage: String?) {
                                continuation.resume("Ubicación desconocida")
                            }
                        }
                    )
                } else {
                    // API < 33 usa método síncrono (deprecated pero necesario)
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val address = addresses?.firstOrNull()?.let { formatAddress(it) }
                        ?: "Ubicación desconocida"
                    continuation.resume(address)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume("Ubicación desconocida")
            }
        }
    }
    
    /**
     * Formatea un objeto Address a String legible.
     * 
     * Prioridad de componentes:
     * 1. thoroughfare (nombre de calle)
     * 2. featureName (número o nombre específico)
     * 3. subLocality (colonia/barrio)
     * 4. locality (ciudad)
     */
    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()
        
        // Nombre de calle y número
        address.thoroughfare?.let { parts.add(it) }
        address.subThoroughfare?.let { parts.add(it) }
        
        // Colonia/Barrio
        address.subLocality?.let { parts.add(it) }
        
        // Ciudad
        address.locality?.let { parts.add(it) }
        
        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            // Fallback a getAddressLine si no hay componentes específicos
            address.getAddressLine(0) ?: "Ubicación desconocida"
        }
    }
}
