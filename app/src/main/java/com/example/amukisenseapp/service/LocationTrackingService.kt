package com.example.amukisenseapp.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.amukisenseapp.MainActivity
import com.example.amukisenseapp.R
import com.example.amukisenseapp.data.model.Coordenadas
import com.example.amukisenseapp.data.model.Registro
import com.example.amukisenseapp.data.repository.RegistroRepository
import com.example.amukisenseapp.util.AudioRecorder
import com.example.amukisenseapp.util.LocationManager
import com.google.android.gms.location.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Date

import ch.hsr.geohash.GeoHash

/**
 * Servicio en segundo plano para registro automático de ruido
 * 
 * Funcionalidad:
 * - Monitorea la ubicación del usuario en tiempo real
 * - Calcula distancia recorrida desde último registro
 * - Registra automáticamente cuando se supera la distancia configurada
 * - Captura nivel de decibelios en el momento del registro
 * - Obtiene dirección legible mediante geocoding
 */
class LocationTrackingService : Service() {

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val NOTIFICATION_CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1001
        
        // Intervalo de actualización de ubicación (cada 30 segundos)
        private const val LOCATION_UPDATE_INTERVAL = 30000L // 30 segundos
        private const val LOCATION_UPDATE_FASTEST_INTERVAL = 15000L // 15 segundos
        
        // Actions para control del servicio
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        
        // Variable para rastrear estado del servicio
        var isRunning = false
            private set
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: RegistroRepository
    private lateinit var locationManager: LocationManager
    private lateinit var audioRecorder: AudioRecorder
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Última ubicación registrada
    private var lastRegisteredLocation: Location? = null
    
    // Distancia configurada (metros)
    private var configuredDistance: Int = 200
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servicio creado")
        
        repository = RegistroRepository()
        locationManager = LocationManager(this)
        audioRecorder = AudioRecorder()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        setupLocationCallback()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                Log.d(TAG, "Iniciando servicio de rastreo")
                // Leer distancia configurada del intent
                configuredDistance = intent.getIntExtra("distancia_metros", 200)
                Log.d(TAG, "Distancia configurada: $configuredDistance metros")
                startForegroundService()
                startLocationUpdates()
                isRunning = true
            }
            ACTION_STOP_SERVICE -> {
                Log.d(TAG, "Deteniendo servicio de rastreo")
                stopLocationUpdates()
                stopSelf()
                isRunning = false
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }
    }

    private fun startForegroundService() {
        val notification = createNotification(
            "Registro activo habilitado",
            "Monitoreando tu ubicación para registros automáticos"
        )
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Registro Activo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones de registro automático de ruido"
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_registros_round)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun startLocationUpdates() {
        // Verificar permisos
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Permiso de ubicación no otorgado")
            stopSelf()
            return
        }

        // Obtener configuración del usuario
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "Usuario no autenticado")
            stopSelf()
            return
        }

        serviceScope.launch {
            try {
                // Recargar configuración desde Firebase por si cambió
                repository.getConfig(currentUser.uid).fold(
                    onSuccess = { config ->
                        // Solo actualizar si no se pasó explícitamente en el intent
                        if (configuredDistance == 200 && config.distancia_metros != 200) {
                            configuredDistance = config.distancia_metros
                        }
                        Log.d(TAG, "Configuración verificada: distancia = $configuredDistance metros")
                    },
                    onFailure = { e ->
                        Log.w(TAG, "No se pudo cargar config, usando valor actual: ${e.message}")
                    }
                )
                
                // Configurar solicitud de ubicación
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    LOCATION_UPDATE_INTERVAL
                ).apply {
                    setMinUpdateIntervalMillis(LOCATION_UPDATE_FASTEST_INTERVAL)
                    setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL * 2)
                }.build()

                // Iniciar actualizaciones de ubicación
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                
                Log.d(TAG, "Actualizaciones de ubicación iniciadas con distancia $configuredDistance m")
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar actualizaciones: ${e.message}", e)
                stopSelf()
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "Actualizaciones de ubicación detenidas")
    }

    private fun handleLocationUpdate(location: Location) {
        Log.d(TAG, "Nueva ubicación: ${location.latitude}, ${location.longitude}")

        // Si es la primera ubicación, solo guardarla
        if (lastRegisteredLocation == null) {
            lastRegisteredLocation = location
            Log.d(TAG, "Primera ubicación registrada")
            return
        }

        // Calcular distancia desde último registro
        val distance = lastRegisteredLocation!!.distanceTo(location)
        Log.d(TAG, "Distancia desde último registro: $distance metros")

        // Actualizar notificación con distancia actual
        updateNotification(
            "Registro activo habilitado",
            "Distancia recorrida: ${distance.toInt()}m / $configuredDistance m"
        )

        // Si se superó la distancia configurada, crear registro automático
        if (distance >= configuredDistance) {
            Log.d(TAG, "Distancia superada! Creando registro automático...")
            createAutomaticRegistro(location)
            lastRegisteredLocation = location
        }
    }

    private fun createAutomaticRegistro(location: Location) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        serviceScope.launch {
            try {
                // 1. Capturar nivel de decibelios
                var decibelios = 0.0
                val dbJob = launch {
                    decibelios = audioRecorder.startRecording().first()
                    audioRecorder.stopRecording()
                }
                dbJob.join()

                // 2. Obtener dirección legible
                val direccion = locationManager.getAddressFromCoordinates(
                    location.latitude,
                    location.longitude
                )

                // 3. Calcular geohash para indexación espacial
                val geohash = GeoHash.withCharacterPrecision(
                    location.latitude,
                    location.longitude,
                    8
                ).toBase32()

                // 4. Crear registro automático usando el repositorio
                // Solo con campos básicos: dB, fecha/hora, lugar (sin sensación)
                repository.createRegistroAutomatico(
                    db = decibelios,
                    direccion = direccion,
                    coordenadas = Coordenadas(location.latitude, location.longitude),
                    geohash = geohash,
                    distanciaMetros = configuredDistance.toDouble()
                ).fold(
                    onSuccess = {
                        Log.d(TAG, "✅ Registro automático creado exitosamente")
                        
                        // Actualizar notificación
                        updateNotification(
                            "Registro creado automáticamente",
                            "Nivel: ${decibelios.toInt()} dB - $direccion"
                        )
                        
                        // Restaurar notificación original después de 3 segundos
                        serviceScope.launch {
                            delay(3000)
                            updateNotification(
                                "Registro activo habilitado",
                                "Monitoreando tu ubicación..."
                            )
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "❌ Error creando registro automático: ${e.message}", e)
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error en registro automático: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
        audioRecorder.stopRecording()
        isRunning = false
        Log.d(TAG, "Servicio destruido")
    }
}
