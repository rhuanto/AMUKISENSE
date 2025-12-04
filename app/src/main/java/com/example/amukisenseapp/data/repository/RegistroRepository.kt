package com.example.amukisenseapp.data.repository

import android.net.Uri
import com.example.amukisenseapp.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import ch.hsr.geohash.GeoHash

/**
 * Repositorio principal para operaciones con Firebase Firestore
 * Maneja todas las operaciones CRUD para usuarios, registros, lugares y métricas globales
 */
class RegistroRepository {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    // Referencias a colecciones
    private val usuariosCollection = db.collection("usuarios")
    private val registrosCollection = db.collection("registros")
    private val lugaresCollection = db.collection("lugares")
    private val comunidadDoc = db.collection("comunidad").document("global")
    
    /**
     * ============================================
     * OPERACIONES DE USUARIOS
     * ============================================
     */
    
    /**
     * Crear nuevo usuario en Firestore después del registro
     */
    suspend fun createUsuario(
        uid: String,
        email: String,
        provider: String,
        nombreUsuario: String? = null
    ): Result<Unit> {
        return try {
            // Verificar si el usuario ya existe para evitar duplicados
            val existingUser = usuariosCollection.document(uid).get().await()
            if (existingUser.exists()) {
                // Usuario ya existe, no crear duplicado
                return Result.success(Unit)
            }
            
            // Generar nombre de usuario si no se proporciona
            val nombre = nombreUsuario ?: generarNombreUsuario(email)
            val numeroUsuario = obtenerSiguienteNumeroUsuario()
            
            val usuario = Usuario(
                uid = uid,
                nombre_usuario = "$nombre$numeroUsuario",
                correo = email,
                provider = provider,
                fecha_union = Timestamp.now(),
                numero_usuario = numeroUsuario
            )
            
            // Guardar usuario en Firestore
            usuariosCollection.document(uid).set(usuario).await()
            
            // Incrementar contador global de usuarios usando set con merge
            // Esto crea el documento si no existe
            comunidadDoc.set(
                mapOf("total_usuarios" to FieldValue.increment(1)),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Obtener datos del usuario actual
     */
    suspend fun getUsuario(uid: String): Result<Usuario?> {
        return try {
            val snapshot = usuariosCollection.document(uid).get().await()
            val usuario = snapshot.toObject(Usuario::class.java)?.copy(uid = uid)
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Actualizar configuración del usuario
     */
    suspend fun updateConfigUsuario(uid: String, config: Config): Result<Unit> {
        return try {
            usuariosCollection.document(uid)
                .set(mapOf("config" to config), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Actualizar estadísticas del usuario
     */
    private suspend fun incrementarStatsUsuario(uid: String, tipo: String) {
        val campo = when (tipo) {
            "manual" -> "stats.registros_manual"
            "auto", "automatico" -> "stats.registros_auto" // Soportar ambos nombres
            "queja" -> "stats.quejas"
            "captura" -> "stats.registros_manual" // Las capturas cuentan como manuales
            else -> return
        }
        
        usuariosCollection.document(uid)
            .set(mapOf(campo to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }
    
    /**
     * Eliminar usuario y todos sus datos
     */
    suspend fun deleteUsuario(uid: String): Result<Unit> {
        return try {
            // 1. Eliminar todos los registros del usuario
            val registros = registrosCollection
                .whereEqualTo("uid_usuario", uid)
                .get()
                .await()
            
            val batch = db.batch()
            registros.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            // 2. Eliminar imágenes de Storage
            val storageRef = storage.reference.child("imagenes_usuarios/$uid")
            try {
                storageRef.listAll().await().items.forEach { it.delete().await() }
            } catch (e: Exception) {
                // Si no hay imágenes, continuar
            }
            
            // 3. Eliminar documento de usuario
            usuariosCollection.document(uid).delete().await()
            
            // 4. Decrementar contador global usando set con merge
            comunidadDoc.set(
                mapOf("total_usuarios" to FieldValue.increment(-1)),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * ============================================
     * OPERACIONES DE REGISTROS
     * ============================================
     */
    
    /**
     * Crear registro manual de ruido
     */
    suspend fun createRegistroManual(
        db: Double,
        tipoLugar: String,
        sensacion: String,
        comentario: String,
        direccion: String,
        coordenadas: Coordenadas
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))
        
        return try {
            val registroId = UUID.randomUUID().toString()
            val geohash = calcularGeohash(coordenadas.lat, coordenadas.lng)
            
            val registro = Registro(
                id = registroId,
                uid_usuario = uid,
                tipo = "manual",
                db = db,
                coordenadas = coordenadas,
                geohash = geohash, //JGGPSSDNM-
                tipo_lugar = tipoLugar,
                sensacion = sensacion,
                comentario = comentario.takeIf { it.isNotBlank() },
                fecha = Timestamp.now(),
                direccion = direccion,
                auto_generado = false,
                visible_publico = true
            )
            
            registrosCollection.document(registroId).set(registro).await()
            
            // Incrementar contadores usando set con merge
            incrementarStatsUsuario(uid, "manual")
            comunidadDoc.set(
                mapOf("total_registros" to FieldValue.increment(1)),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            
            Result.success(registroId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Crear registro automático (para servicio de background)
     */
    suspend fun createRegistroAutomatico(
        db: Double,
        direccion: String,
        coordenadas: Coordenadas,
        geohash: String,
        distanciaMetros: Double
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))
        
        return try {
            val registroId = UUID.randomUUID().toString()
            
            // Registro automático solo con campos básicos: dB, fecha, hora, lugar
            val registro = Registro(
                id = registroId,
                uid_usuario = uid,
                tipo = "automatico",
                db = db,
                coordenadas = coordenadas,
                geohash = geohash,
                sensacion = null,  // Sin sensación en registros automáticos
                tipo_lugar = null, // Sin tipo de lugar
                comentario = null, // Sin comentario
                direccion = direccion,
                fecha = Timestamp.now(),
                imagen_url = null,
                auto_generado = true,
                distancia_m = distanciaMetros,
                visible_publico = true
            )
            
            registrosCollection.document(registroId).set(registro).await()
            
            // Incrementar contadores usando set con merge
            incrementarStatsUsuario(uid, "automatico")
            comunidadDoc.set(
                mapOf("total_registros" to FieldValue.increment(1)),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            
            Result.success(registroId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Crear registro de queja
     */
    suspend fun createRegistroQueja(
        db: Double,
        origenRuido: String,
        impacto: String,
        sensacion: String,
        comentario: String,
        direccion: String,
        coordenadas: Coordenadas
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))
        
        return try {
            val registroId = UUID.randomUUID().toString()
            val geohash = calcularGeohash(coordenadas.lat, coordenadas.lng)
            
            val registro = Registro(
                id = registroId,
                uid_usuario = uid,
                tipo = "queja",
                db = db,
                coordenadas = coordenadas,
                geohash = geohash,
                sensacion = sensacion,
                comentario = comentario.takeIf { it.isNotBlank() },
                origen_ruido = origenRuido,
                impacto = impacto,
                fecha = Timestamp.now(),
                direccion = direccion,
                auto_generado = false,
                visible_publico = true
            )
            
            registrosCollection.document(registroId).set(registro).await()
            
            // Incrementar contadores usando set con merge
            incrementarStatsUsuario(uid, "queja")
            comunidadDoc.set(
                mapOf(
                    "total_quejas" to FieldValue.increment(1),
                    "total_registros" to FieldValue.increment(1)
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            
            Result.success(registroId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Crear registro con captura de imagen
     */
    suspend fun createRegistroCaptura(
        db: Double,
        sensacion: String,
        direccion: String,
        coordenadas: Coordenadas,
        imageUri: Uri
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))
        
        return try {
            val registroId = UUID.randomUUID().toString()
            
            // 1. Subir imagen a Storage
            val imagenUrl = subirImagenRegistro(uid, registroId, imageUri)
            
            // 2. Crear registro con URL de imagen
            val geohash = calcularGeohash(coordenadas.lat, coordenadas.lng)
            
            val registro = Registro(
                id = registroId,
                uid_usuario = uid,
                tipo = "captura",
                db = db,
                coordenadas = coordenadas,
                geohash = geohash,
                sensacion = sensacion,
                fecha = Timestamp.now(),
                direccion = direccion,
                imagen_url = imagenUrl,
                auto_generado = false,
                visible_publico = true
            )
            
            registrosCollection.document(registroId).set(registro).await()
            
            // Incrementar contadores usando set con merge
            incrementarStatsUsuario(uid, "captura")
            comunidadDoc.set(
                mapOf("total_registros" to FieldValue.increment(1)),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            
            Result.success(registroId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtener registros del usuario actual
     */
    suspend fun getRegistrosUsuario(
        uid: String,
        tipo: String? = null
    ): Result<List<Registro>> {
        return try {
            var query: Query = registrosCollection
                .whereEqualTo("uid_usuario", uid)
            
            if (tipo != null) {
                query = query.whereEqualTo("tipo", tipo)
            }
            
            val snapshot = query
                .get()
                .await()
            
            val registros = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Registro::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.fecha?.toDate()?.time ?: 0L }
            
            Result.success(registros)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtener registro por ID
     */
    suspend fun getRegistro(registroId: String): Result<Registro?> {
        return try {
            val snapshot = registrosCollection.document(registroId).get().await()
            val registro = snapshot.toObject(Registro::class.java)?.copy(id = registroId)
            Result.success(registro)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Actualizar registro existente
     */
    suspend fun updateRegistro(
        registroId: String,
        campos: Map<String, Any>
    ): Result<Unit> {
        return try {
            registrosCollection.document(registroId)
                .update(campos)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Eliminar registro
     */
    suspend fun deleteRegistro(registroId: String): Result<Unit> {
        return try {
            val registro = getRegistro(registroId).getOrNull()
                ?: return Result.failure(Exception("Registro no encontrado"))
            
            // Eliminar imagen si existe
            registro.imagen_url?.let {
                try {
                    val storageRef = storage.getReferenceFromUrl(it)
                    storageRef.delete().await()
                } catch (e: Exception) {
                    // Si falla la eliminación de la imagen, continuar
                }
            }
            
            // Eliminar documento
            registrosCollection.document(registroId).delete().await()
            
            // Decrementar contadores
            val campo = when (registro.tipo) {
                "manual", "captura" -> "stats.registros_manual"
                "auto" -> "stats.registros_auto"
                "queja" -> "stats.quejas"
                else -> null
            }
            
            campo?.let {
                usuariosCollection.document(registro.uid_usuario)
                    .set(mapOf(it to FieldValue.increment(-1)), com.google.firebase.firestore.SetOptions.merge())
                    .await()
            }
            
            // Decrementar contadores globales usando set con merge
            if (registro.tipo == "queja") {
                comunidadDoc.set(
                    mapOf(
                        "total_registros" to FieldValue.increment(-1),
                        "total_quejas" to FieldValue.increment(-1)
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            } else {
                comunidadDoc.set(
                    mapOf("total_registros" to FieldValue.increment(-1)),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtener feed global de subidas (últimas)
     */
    suspend fun getSubidasGlobales(limite: Int = 30): Result<List<Registro>> {
        return try {
            val snapshot = registrosCollection
                .whereEqualTo("visible_publico", true)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(limite.toLong())
                .get()
                .await()
            
            val registros = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Registro::class.java)?.copy(id = doc.id)
            }
            
            Result.success(registros)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * ============================================
     * OPERACIONES DE MÉTRICAS GLOBALES
     * ============================================
     */
    
    /**
     * Obtener métricas globales de la comunidad
     */
    suspend fun getMetricasGlobales(): Result<MetricasGlobales> {
        return try {
            val snapshot = comunidadDoc.get().await()
            val metricas = snapshot.toObject(MetricasGlobales::class.java)
                ?: MetricasGlobales()
            Result.success(metricas)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * ============================================
     * FUNCIONES AUXILIARES
     * ============================================
     */
    
    /**
     * Subir imagen a Firebase Storage
     */
    private suspend fun subirImagenRegistro(
        uid: String,
        registroId: String,
        imageUri: Uri
    ): String {
        return try {
            val storageRef = storage.reference
                .child("imagenes_usuarios/$uid/registros/$registroId.jpg")
            
            // Subir archivo
            val uploadTask = storageRef.putFile(imageUri).await()
            
            // Obtener URL de descarga
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            downloadUrl
        } catch (e: Exception) {
            // Log del error específico para debugging
            android.util.Log.e("RegistroRepository", "Error subiendo imagen: ${e.message}", e)
            throw Exception("Error al subir imagen a Firebase Storage: ${e.message}")
        }
    }
    
    /**
     * Generar nombre de usuario a partir del email
     */
    private fun generarNombreUsuario(email: String): String {
        return email.substringBefore("@").replace("[^a-zA-Z]".toRegex(), "")
    }
    
    /**
     * Obtener siguiente número de usuario (secuencial)
     * Cuenta los usuarios reales en la colección
     */
    private suspend fun obtenerSiguienteNumeroUsuario(): Int {
        return try {
            // Contar usuarios reales en la colección
            val count = usuariosCollection.get().await().size()
            count + 1
        } catch (e: Exception) {
            e.printStackTrace()
            1
        }
    }
    
    /**
     * Calcular geohash estándar para coordenadas usando el algoritmo de intercalación de bits.
     * 
     * Utiliza la librería ch.hsr.geohash que implementa el algoritmo real de Z-order curve,
     * que intercala los bits de latitud y longitud para crear un índice único espacial.
     * 
     * Ventajas del Geohash estándar:
     * - Identificación de vecinos: Permite encontrar puntos cercanos calculando los geohashes vecinos
     * - Consultas por prefijo: Permite búsquedas de área truncando el geohash
     * - Optimización de búsqueda: Un solo índice para búsquedas espaciales eficientes
     * 
     * Precisión recomendada:
     * - Precisión 7: ~150 metros (área de ciudad)
     * - Precisión 8: ~40 metros (área de vecindario)
     * - Precisión 9: ~5 metros (área de edificio) - RECOMENDADO para esta app
     * - Precisión 10: ~1 metro (área de habitación)
     * 
     * @param lat Latitud de la coordenada (-90.0 a 90.0)
     * @param lng Longitud de la coordenada (-180.0 a 180.0)
     * @return String Geohash en formato base32 (ej: "6u3d2wgrj")
     */
    private fun calcularGeohash(lat: Double, lng: Double): String {
        // Usar precisión 9 para un balance entre resolución (~5m) y tamaño de índice
        // Ejemplo de resultado para (-12.0544, -77.0862): "6qj4xn2vk"
        val precision = 9
        return GeoHash.withCharacterPrecision(lat, lng, precision).toBase32()
    }
    
    /**
     * Buscar registros cercanos a una ubicación (consulta geoespacial)
     * Implementación simplificada usando filtrado en memoria
     * Para producción, considerar usar GeoFirestore o similar
     * @param lat Latitud del centro de búsqueda
     * @param lng Longitud del centro de búsqueda
     * @param radiusKm Radio de búsqueda en kilómetros
     */
    suspend fun getRegistrosCercanos(
        lat: Double, 
        lng: Double, 
        radiusKm: Double
    ): Result<List<Registro>> {
        return try {
            // Obtener todos los registros públicos
            val snapshot = registrosCollection
                .whereEqualTo("visible_publico", true)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            
            // Filtrar por distancia en memoria
            val registros = snapshot.documents.mapNotNull { doc ->
                val registro = doc.toObject(Registro::class.java)?.copy(id = doc.id)
                registro?.let {
                    val distance = calcularDistancia(lat, lng, it.coordenadas.lat, it.coordenadas.lng)
                    if (distance <= radiusKm) it else null
                }
            }
            
            Result.success(registros)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calcula la distancia entre dos coordenadas usando la fórmula de Haversine
     * @return Distancia en kilómetros
     */
    private fun calcularDistancia(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371.0 // Radio de la Tierra en km
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    // ============ MÉTODOS DE ESTADÍSTICAS Y CONSULTAS DE USUARIO ============
    
    /**
     * Obtiene las estadísticas de registros de un usuario
     * Cuenta los registros por tipo (manual, captura con foto, queja, auto)
     * 
     * @param uid ID del usuario
     * @return Result con los contadores de cada tipo de registro
     */
    suspend fun getEstadisticasUsuario(uid: String): Result<EstadisticasUsuario> {
        return try {
            // Contar registros manuales
            val registrosManual = registrosCollection
                .whereEqualTo("uid_usuario", uid)
                .whereEqualTo("tipo", "manual")
                .get()
                .await()
                .size()
            
            // Contar registros con foto (captura)
            val registrosFoto = registrosCollection
                .whereEqualTo("uid_usuario", uid)
                .whereEqualTo("tipo", "captura")
                .get()
                .await()
                .size()
            
            // Contar quejas
            val quejas = registrosCollection
                .whereEqualTo("uid_usuario", uid)
                .whereEqualTo("tipo", "queja")
                .get()
                .await()
                .size()
            
            // Contar registros automáticos (en futuro)
            // Por ahora en 0 porque no hay registros automáticos implementados
            val registrosAuto = 0
            
            Result.success(EstadisticasUsuario(registrosManual, registrosFoto, quejas, registrosAuto))
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error obteniendo estadísticas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene información básica del usuario (nombre, fecha de unión)
     * Usado en pantallas de perfil y configuración
     * 
     * @param uid ID del usuario
     * @return Result con datos del usuario o null si no existe
     */
    suspend fun getUsuarioInfo(uid: String): Result<UsuarioInfo?> {
        return try {
            val document = usuariosCollection.document(uid).get().await()
            if (document.exists()) {
                val nombreUsuario = document.getString("nombre_usuario") ?: "Usuario"
                val correo = document.getString("correo") ?: ""
                val fechaUnion = document.getTimestamp("fecha_union")
                val fotoPerfilUrl = document.getString("foto_perfil_url")
                
                Result.success(
                    UsuarioInfo(
                        uid = uid,
                        nombreUsuario = nombreUsuario,
                        correo = correo,
                        fechaUnion = fechaUnion,
                        fotoPerfilUrl = fotoPerfilUrl
                    )
                )
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error obteniendo info usuario: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Observa en tiempo real la información del usuario
     * Útil para pantallas que necesitan actualizaciones automáticas
     * 
     * @param uid ID del usuario
     * @return Flow que emite UsuarioInfo cuando hay cambios
     */
    fun observarUsuarioInfo(uid: String): Flow<UsuarioInfo?> = callbackFlow {
        val listener = usuariosCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("RegistroRepository", "Error en listener: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val nombreUsuario = snapshot.getString("nombre_usuario") ?: "Usuario"
                    val correo = snapshot.getString("correo") ?: ""
                    val fechaUnion = snapshot.getTimestamp("fecha_union")
                    val fotoPerfilUrl = snapshot.getString("foto_perfil_url")
                    
                    trySend(
                        UsuarioInfo(
                            uid = uid,
                            nombreUsuario = nombreUsuario,
                            correo = correo,
                            fechaUnion = fechaUnion,
                            fotoPerfilUrl = fotoPerfilUrl
                        )
                    )
                } else {
                    trySend(null)
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Obtiene la configuración del usuario
     * 
     * @param uid ID del usuario
     * @return Result con la configuración del usuario
     */
    suspend fun getConfig(uid: String): Result<Config> {
        return try {
            val document = usuariosCollection.document(uid).get().await()
            if (document.exists()) {
                val configMap = document.get("config") as? Map<*, *>
                val config = if (configMap != null) {
                    Config(
                        notificaciones = configMap["notificaciones"] as? Boolean ?: true,
                        registro_automatico = configMap["registro_automatico"] as? Boolean ?: false,
                        distancia_metros = (configMap["distancia_metros"] as? Long)?.toInt() ?: 200,
                        auto_registro_activado = configMap["auto_registro_activado"] as? Boolean ?: false
                    )
                } else {
                    Config()
                }
                Result.success(config)
            } else {
                Result.success(Config())
            }
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error obteniendo config: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Actualiza el estado de notificaciones del usuario
     * Usado en pantallas de configuración
     * 
     * @param uid ID del usuario
     * @param enabled True para activar, false para desactivar
     * @return Result indicando éxito o error
     */
    suspend fun updateNotificaciones(uid: String, enabled: Boolean): Result<Unit> {
        return try {
            usuariosCollection.document(uid)
                .update("config.notificaciones", enabled)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error actualizando notificaciones: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene registros globales para mostrar en el mapa
     * Limitado a 500 registros más recientes por defecto
     * 
     * @param limite Número máximo de registros a obtener
     * @return Result con lista de registros o error
     */
    suspend fun getRegistrosGlobales(limite: Int = 500): Result<List<Registro>> {
        return try {
            val snapshot = registrosCollection
                .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limite.toLong())
                .get()
                .await()
            
            val registros = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    
                    val coordenadasMap = data["coordenadas"] as? Map<*, *>
                    val coordenadas = if (coordenadasMap != null) {
                        Coordenadas(
                            lat = (coordenadasMap["lat"] as? Number)?.toDouble() ?: 0.0,
                            lng = (coordenadasMap["lng"] as? Number)?.toDouble() ?: 0.0
                        )
                    } else null
                    
                    if (coordenadas == null) return@mapNotNull null
                    
                    Registro(
                        id = doc.id,
                        uid_usuario = data["uid_usuario"] as? String ?: "",
                        tipo = data["tipo"] as? String ?: "manual",
                        db = (data["db"] as? Number)?.toDouble() ?: 0.0,
                        coordenadas = coordenadas,
                        geohash = data["geohash"] as? String,
                        tipo_lugar = data["tipo_lugar"] as? String,
                        sensacion = data["sensacion"] as? String,
                        comentario = data["comentario"] as? String,
                        origen_ruido = data["origen_ruido"] as? String,
                        impacto = data["impacto"] as? String,
                        direccion = data["direccion"] as? String,
                        fecha = data["fecha"] as? com.google.firebase.Timestamp,
                        imagen_url = data["imagen_url"] as? String,
                        auto_generado = data["auto_generado"] as? Boolean ?: false,
                        distancia_m = (data["distancia_m"] as? Number)?.toDouble(),
                        visible_publico = data["visible_publico"] as? Boolean ?: true
                    )
                } catch (e: Exception) {
                    android.util.Log.e("RegistroRepository", "Error parseando registro ${doc.id}: ${e.message}")
                    null
                }
            }
            
            Result.success(registros)
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error obteniendo registros globales: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene registros dentro de un área geográfica específica
     * Útil para cargar solo los registros visibles en el mapa
     * 
     * @param latMin Latitud mínima del área
     * @param latMax Latitud máxima del área
     * @param lngMin Longitud mínima del área
     * @param lngMax Longitud máxima del área
     * @return Result con lista de registros en el área o error
     */
    suspend fun getRegistrosEnArea(
        latMin: Double,
        latMax: Double,
        lngMin: Double,
        lngMax: Double
    ): Result<List<Registro>> {
        return try {
            // Firestore no soporta queries con múltiples rangos, así que hacemos filtrado en cliente
            val snapshot = registrosCollection
                .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1000)
                .get()
                .await()
            
            val registros = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    
                    val coordenadasMap = data["coordenadas"] as? Map<*, *>
                    val coordenadas = if (coordenadasMap != null) {
                        Coordenadas(
                            lat = (coordenadasMap["lat"] as? Number)?.toDouble() ?: 0.0,
                            lng = (coordenadasMap["lng"] as? Number)?.toDouble() ?: 0.0
                        )
                    } else null
                    
                    if (coordenadas == null) return@mapNotNull null
                    
                    // Filtrar por área
                    if (coordenadas.lat < latMin || coordenadas.lat > latMax ||
                        coordenadas.lng < lngMin || coordenadas.lng > lngMax) {
                        return@mapNotNull null
                    }
                    
                    Registro(
                        id = doc.id,
                        uid_usuario = data["uid_usuario"] as? String ?: "",
                        tipo = data["tipo"] as? String ?: "manual",
                        db = (data["db"] as? Number)?.toDouble() ?: 0.0,
                        coordenadas = coordenadas,
                        geohash = data["geohash"] as? String,
                        tipo_lugar = data["tipo_lugar"] as? String,
                        sensacion = data["sensacion"] as? String,
                        comentario = data["comentario"] as? String,
                        origen_ruido = data["origen_ruido"] as? String,
                        impacto = data["impacto"] as? String,
                        direccion = data["direccion"] as? String,
                        fecha = data["fecha"] as? com.google.firebase.Timestamp,
                        imagen_url = data["imagen_url"] as? String,
                        auto_generado = data["auto_generado"] as? Boolean ?: false,
                        distancia_m = (data["distancia_m"] as? Number)?.toDouble(),
                        visible_publico = data["visible_publico"] as? Boolean ?: true
                    )
                } catch (e: Exception) {
                    android.util.Log.e("RegistroRepository", "Error parseando registro ${doc.id}: ${e.message}")
                    null
                }
            }
            
            Result.success(registros)
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error obteniendo registros en área: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * ============================================
     * OPERACIONES DE COMUNIDAD
     * ============================================
     */
    
    /**
     * Obtener lista de todos los usuarios registrados
     * Para mostrar en la pantalla "Miembros Unidos"
     * 
     * @return Result con lista de usuarios ordenados por fecha de unión
     */
    suspend fun getAllUsuarios(): Result<List<Usuario>> {
        return try {
            val snapshot = usuariosCollection
                .orderBy("fecha_union", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val usuarios = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Usuario::class.java)?.copy(uid = doc.id)
                } catch (e: Exception) {
                    android.util.Log.e("RegistroRepository", "Error parseando usuario ${doc.id}: ${e.message}")
                    null
                }
            }
            
            Result.success(usuarios)
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error obteniendo usuarios", e)
            Result.failure(e)
        }
    }
    
    /**
     * ============================================
     * OPERACIONES DE DASHBOARD
     * ============================================
     */
    
    /**
     * Obtener quejas agrupadas por distrito para el dashboard
     * Extrae el distrito de la dirección y cuenta las quejas por cada uno
     * 
     * @param limite Número máximo de registros a consultar (default: 500)
     * @return Result con lista de QuejasPorDistrito o error
     */
    suspend fun getQuejasPorDistrito(limite: Int = 500): Result<List<QuejasPorDistrito>> {
        return try {
            // Obtener solo los registros de tipo "queja"
            val snapshot = registrosCollection
                .whereEqualTo("tipo", "queja")
                .whereEqualTo("visible_publico", true)
                .limit(limite.toLong())
                .get()
                .await()
            
            // Agrupar por distrito extraído de la dirección
            val quejasMap = mutableMapOf<String, Int>()
            
            snapshot.documents.forEach { doc ->
                try {
                    val direccion = doc.getString("direccion")
                    val distrito = extraerDistrito(direccion)
                    
                    // Solo contar si se pudo extraer un distrito válido
                    if (distrito.isNotEmpty()) {
                        quejasMap[distrito] = quejasMap.getOrDefault(distrito, 0) + 1
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RegistroRepository", "Error procesando queja ${doc.id}: ${e.message}")
                }
            }
            
            // Convertir el mapa a lista y ordenar por cantidad descendente
            val quejasList = quejasMap.map { (distrito, cantidad) ->
                QuejasPorDistrito(distrito = distrito, cantidad = cantidad)
            }.sortedByDescending { it.cantidad }
            
            Result.success(quejasList)
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error obteniendo quejas por distrito", e)
            Result.failure(e)
        }
    }
    
    /**
     * Extrae el nombre del distrito de una dirección completa
     * Busca patrones comunes en direcciones peruanas
     * 
     * Ejemplos:
     * - "Av. Lima 123, San Isidro, Lima" -> "San Isidro"
     * - "Jr. Tacna, Cercado de Lima" -> "Cercado de Lima"
     * - "Calle Los Sauces 456, Miraflores, Lima, Peru" -> "Miraflores"
     * 
     * @param direccion Dirección completa
     * @return Nombre del distrito o "Desconocido" si no se puede extraer
     */
    private fun extraerDistrito(direccion: String?): String {
        if (direccion.isNullOrBlank()) return "Desconocido"
        
        try {
            // Lista de palabras clave a ignorar (no son distritos)
            val palabrasClave = setOf(
                "lima", "peru", "perú", "región", "provincia", 
                "departamento", "calle", "avenida", "av", "jr", "jirón",
                "psje", "pasaje", "mz", "lt", "lote", "manzana"
            )
            
            // Dividir la dirección por comas
            val partes = direccion.split(",").map { it.trim() }
            
            // Buscar distrito en las partes intermedias (generalmente está entre la calle y la ciudad)
            for (i in 1 until partes.size.coerceAtMost(3)) {
                val parte = partes[i].trim()
                
                // Verificar que no sea una palabra clave ignorada
                val esPalabraClave = palabrasClave.any { 
                    parte.lowercase().contains(it) 
                }
                
                // Verificar que no sea solo números (como códigos postales)
                val esNumero = parte.all { it.isDigit() || it.isWhitespace() }
                
                // Si pasa las validaciones, probablemente es el distrito
                if (!esPalabraClave && !esNumero && parte.length > 3) {
                    return parte
                }
            }
            
            // Si no se encontró distrito en las partes intermedias, usar heurística
            // Si tiene más de 2 partes, la segunda suele ser el distrito
            if (partes.size >= 2) {
                val candidato = partes[1].trim()
                if (candidato.length > 3) {
                    return candidato
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error extrayendo distrito de: $direccion", e)
        }
        
        return "Desconocido"
    }
    
    /**
     * Extrae el nombre de la calle/avenida principal de una dirección
     * Ejemplo: "Av. Larco 456, Miraflores, Lima" → "Av. Larco"
     */
    private fun extraerCalle(direccion: String?): String {
        if (direccion.isNullOrBlank()) return "Desconocido"
        
        try {
            // Dividir por comas y tomar la primera parte (suele ser la calle)
            val partes = direccion.split(",")
            if (partes.isNotEmpty()) {
                val calle = partes[0].trim()
                
                // Limpiar números de dirección al final
                val calleNombre = calle.replace(Regex("""\d+.*$"""), "").trim()
                
                if (calleNombre.length > 3) {
                    return calleNombre
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error extrayendo calle de: $direccion", e)
        }
        
        return "Desconocido"
    }
    
    /**
     * Obtiene la evolución temporal del ruido (últimos 6 meses)
     */
    suspend fun getEvolucionTemporal(): Result<List<EvolucionTemporal>> {
        return try {
            // Calcular fecha hace 1 semana
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
            val fechaHaceSemana = Timestamp(calendar.time)
            
            val snapshot = registrosCollection
                .whereEqualTo("visible_publico", true)
                .whereGreaterThanOrEqualTo("fecha", fechaHaceSemana)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(1000)
                .get()
                .await()
            
            val registrosPorDia = snapshot.documents.mapNotNull { doc ->
                try {
                    val fecha = doc.getTimestamp("fecha") ?: return@mapNotNull null
                    val db = (doc.getDouble("db") ?: return@mapNotNull null)
                    
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = fecha.toDate()
                    val dia = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    val mes = calendar.get(java.util.Calendar.MONTH)
                    
                    val meses = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", 
                                       "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                    val diaNombre = "$dia ${meses[mes]}"
                    
                    diaNombre to db
                } catch (e: Exception) {
                    null
                }
            }
            
            val evolucion = registrosPorDia
                .groupBy { it.first }
                .map { (dia, registros) ->
                    EvolucionTemporal(
                        fecha = dia,
                        promedioDB = registros.map { it.second }.average(),
                        cantidadRegistros = registros.size
                    )
                }
                .sortedBy { 
                    // Ordenar por número de día
                    it.fecha.split(" ").firstOrNull()?.toIntOrNull() ?: 0
                }
                .takeLast(7)
            
            Result.success(evolucion)
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error obteniendo evolución temporal", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene las calles/avenidas más ruidosas
     */
    suspend fun getTopCallesRuidosas(limite: Int = 10): Result<List<CalleRuidosa>> {
        return try {
            val snapshot = registrosCollection
                .whereEqualTo("visible_publico", true)
                .get()
                .await()
            
            val callesData = snapshot.documents.mapNotNull { doc ->
                try {
                    val direccion = doc.getString("direccion") ?: return@mapNotNull null
                    val db = doc.getDouble("db") ?: return@mapNotNull null
                    val calle = extraerCalle(direccion)
                    val distrito = extraerDistrito(direccion)
                    
                    if (calle == "Desconocido") return@mapNotNull null
                    
                    Triple(calle, distrito, db)
                } catch (e: Exception) {
                    null
                }
            }
            
            val callesRuidosas = callesData
                .groupBy { it.first }
                .map { (calle, registros) ->
                    val distrito = registros.firstOrNull()?.second ?: "Desconocido"
                    CalleRuidosa(
                        calle = calle,
                        distrito = distrito,
                        promedioDB = registros.map { it.third }.average(),
                        cantidadQuejas = registros.size
                    )
                }
                .sortedByDescending { it.promedioDB }
                .take(limite)
            
            Result.success(callesRuidosas)
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error obteniendo calles ruidosas", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene el promedio de ruido por hora del día (0-23)
     */
    suspend fun getRuidoPorHorario(): Result<List<RuidoPorHorario>> {
        return try {
            val snapshot = registrosCollection
                .whereEqualTo("visible_publico", true)
                .limit(5000)
                .get()
                .await()
            
            val registrosPorHora = snapshot.documents.mapNotNull { doc ->
                try {
                    val fecha = doc.getTimestamp("fecha") ?: return@mapNotNull null
                    val db = doc.getDouble("db") ?: return@mapNotNull null
                    
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = fecha.toDate()
                    val hora = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                    
                    hora to db
                } catch (e: Exception) {
                    null
                }
            }
            
            val ruidoPorHora = registrosPorHora
                .groupBy { it.first }
                .map { (hora, registros) ->
                    RuidoPorHorario(
                        hora = hora,
                        promedioDB = registros.map { it.second }.average(),
                        cantidadRegistros = registros.size
                    )
                }
                .sortedBy { it.hora }
            
            Result.success(ruidoPorHora)
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error obteniendo ruido por horario", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene la evolución temporal filtrada por ubicación del usuario
     * @param lat Latitud del usuario
     * @param lng Longitud del usuario
     * @param radiusKm Radio de búsqueda en km (default 1 km)
     */
    suspend fun getEvolucionTemporalPorUbicacion(
        lat: Double,
        lng: Double,
        radiusKm: Double = 1.0
    ): Result<List<EvolucionTemporal>> {
        return try {
            // Calcular fecha hace 1 semana
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
            val fechaHaceSemana = Timestamp(calendar.time)
            
            val snapshot = registrosCollection
                .whereEqualTo("visible_publico", true)
                .whereGreaterThanOrEqualTo("fecha", fechaHaceSemana)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(1000)
                .get()
                .await()
            
            // Filtrar por distancia
            val registrosPorDia = snapshot.documents.mapNotNull { doc ->
                try {
                    val coordenadas = doc.get("coordenadas") as? Map<*, *> ?: return@mapNotNull null
                    val regLat = (coordenadas["lat"] as? Number)?.toDouble() ?: return@mapNotNull null
                    val regLng = (coordenadas["lng"] as? Number)?.toDouble() ?: return@mapNotNull null
                    
                    // Verificar si está dentro del radio
                    val distancia = calcularDistancia(lat, lng, regLat, regLng)
                    if (distancia > radiusKm) return@mapNotNull null
                    
                    val fecha = doc.getTimestamp("fecha") ?: return@mapNotNull null
                    val db = (doc.getDouble("db") ?: return@mapNotNull null)
                    
                    val cal = java.util.Calendar.getInstance()
                    cal.time = fecha.toDate()
                    val dia = cal.get(java.util.Calendar.DAY_OF_MONTH)
                    val mes = cal.get(java.util.Calendar.MONTH)
                    
                    val meses = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", 
                                       "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                    val diaNombre = "$dia ${meses[mes]}"
                    
                    diaNombre to db
                } catch (e: Exception) {
                    null
                }
            }
            
            val evolucion = registrosPorDia
                .groupBy { it.first }
                .map { (dia, registros) ->
                    EvolucionTemporal(
                        fecha = dia,
                        promedioDB = registros.map { it.second }.average(),
                        cantidadRegistros = registros.size
                    )
                }
                .sortedBy { 
                    it.fecha.split(" ").firstOrNull()?.toIntOrNull() ?: 0
                }
                .takeLast(7)
            
            Result.success(evolucion)
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error obteniendo evolución temporal por ubicación", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene el heatmap por horario filtrado por ubicación del usuario
     * @param lat Latitud del usuario
     * @param lng Longitud del usuario
     * @param radiusKm Radio de búsqueda en km (default 1 km)
     */
    suspend fun getRuidoPorHorarioPorUbicacion(
        lat: Double,
        lng: Double,
        radiusKm: Double = 1.0
    ): Result<List<RuidoPorHorario>> {
        return try {
            val snapshot = registrosCollection
                .whereEqualTo("visible_publico", true)
                .limit(5000)
                .get()
                .await()
            
            // Filtrar por distancia
            val registrosPorHora = snapshot.documents.mapNotNull { doc ->
                try {
                    val coordenadas = doc.get("coordenadas") as? Map<*, *> ?: return@mapNotNull null
                    val regLat = (coordenadas["lat"] as? Number)?.toDouble() ?: return@mapNotNull null
                    val regLng = (coordenadas["lng"] as? Number)?.toDouble() ?: return@mapNotNull null
                    
                    // Verificar si está dentro del radio
                    val distancia = calcularDistancia(lat, lng, regLat, regLng)
                    if (distancia > radiusKm) return@mapNotNull null
                    
                    val fecha = doc.getTimestamp("fecha") ?: return@mapNotNull null
                    val db = doc.getDouble("db") ?: return@mapNotNull null
                    
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = fecha.toDate()
                    val hora = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                    
                    hora to db
                } catch (e: Exception) {
                    null
                }
            }
            
            val ruidoPorHora = registrosPorHora
                .groupBy { it.first }
                .map { (hora, registros) ->
                    RuidoPorHorario(
                        hora = hora,
                        promedioDB = registros.map { it.second }.average(),
                        cantidadRegistros = registros.size
                    )
                }
                .sortedBy { it.hora }
            
            Result.success(ruidoPorHora)
        } catch (e: Exception) {
            android.util.Log.e("RegistroRepository", "Error obteniendo ruido por horario por ubicación", e)
            Result.failure(e)
        }
    }
}

/**
 * Modelo para métricas globales
 */
data class MetricasGlobales(
    val total_usuarios: Int = 0,
    val total_registros: Int = 0,
    val total_quejas: Int = 0,
    val ultima_actualizacion: Timestamp? = null
)

/**
 * Estadísticas de registros de un usuario
 */
data class EstadisticasUsuario(
    val registrosManual: Int = 0,
    val registrosFoto: Int = 0,
    val quejas: Int = 0,
    val registrosAuto: Int = 0
)

/**
 * Información básica del usuario para mostrar en UI
 */
data class UsuarioInfo(
    val uid: String,
    val nombreUsuario: String,
    val correo: String,
    val fechaUnion: Timestamp? = null,
    val fotoPerfilUrl: String? = null
)
