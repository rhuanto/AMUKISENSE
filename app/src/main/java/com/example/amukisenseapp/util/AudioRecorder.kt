package com.example.amukisenseapp.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Clase para capturar audio del micrófono y calcular el nivel de decibelios en tiempo real.
 * 
 * Esta clase utiliza AudioRecord para obtener muestras de audio del micrófono del dispositivo
 * y calcula el nivel de presión sonora (SPL) en decibelios.
 * 
 * @author Tu nombre
 * @date Octubre 2025
 */
class AudioRecorder {
    
    // Configuración de audio - Usa la frecuencia estándar de 44.1kHz
    private val sampleRate = 44100
    
    // Configuración de canal - MONO es suficiente para medir nivel de ruido
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    
    // Formato de audio - 16 bits es el estándar para la mayoría de dispositivos
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    // Calculamos el tamaño del buffer basado en la configuración de audio
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    // Instancia de AudioRecord para capturar audio del micrófono
    private var audioRecord: AudioRecord? = null
    
    // Flag para controlar el estado de grabación
    private var isRecording = false

    /**
     * Inicia la captura de audio y emite lecturas de decibelios cada 500ms.
     * 
     * Esta función utiliza Flow para emitir valores de dB de manera reactiva.
     * El cálculo se basa en el RMS (Root Mean Square) de la amplitud de la señal de audio.
     * 
     * Fórmula aplicada: dB = 20 * log10(RMS / referencia)
     * Donde la referencia es 32767 (máximo valor de 16 bits)
     * 
     * @return Flow<Double> - Emite valores de decibelios calculados
     */
    fun startRecording(): Flow<Double> = flow {
        try {
            // Creamos una nueva instancia de AudioRecord con la fuente del micrófono
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            // Verificamos que AudioRecord se inicializó correctamente
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Si falla, emitimos un valor por defecto y terminamos
                emit(45.0)
                return@flow
            }
            
            // Iniciamos la grabación de audio
            audioRecord?.startRecording()
            isRecording = true
            
            // Buffer para almacenar las muestras de audio capturadas
            val audioBuffer = ShortArray(bufferSize)
            
            // Bucle continuo para leer audio mientras isRecording sea true
            while (isRecording) {
                // Leemos muestras de audio del micrófono y las almacenamos en el buffer
                val readResult = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                
                if (readResult > 0) {
                    // Calculamos el RMS (Root Mean Square) de las muestras
                    // RMS es una medida de la amplitud efectiva de la señal
                    val rms = calculateRMS(audioBuffer, readResult)
                    
                    // Convertimos el RMS a decibelios usando la fórmula logarítmica
                    val db = if (rms > 0) {
                        // 20 * log10(rms / referencia) + offset para ajustar a escala real
                        // Referencia: 32767 es el valor máximo de un Short (16 bits)
                        // Offset: 90 es para ajustar la escala a valores de dB reales (aproximación)
                        val rawDb = 20 * log10(rms / 32767.0)
                        val calibratedDb = rawDb + 90 // Calibración para acercar a valores reales
                        
                        // Limitamos el rango entre 30 y 120 dB (rango típico de medición)
                        calibratedDb.coerceIn(30.0, 120.0)
                    } else {
                        // Si no hay señal, retornamos un valor de silencio
                        35.0
                    }
                    
                    // Emitimos el valor calculado a través del Flow
                    emit(db)
                    
                    // Esperamos 500ms antes de la siguiente lectura
                    // Esto evita sobrecargar el sistema y proporciona actualizaciones suaves
                    kotlinx.coroutines.delay(500)
                }
            }
        } catch (e: SecurityException) {
            // Capturamos excepción de permiso denegado
            // Esto ocurre si el usuario no otorgó el permiso RECORD_AUDIO
            emit(0.0) // Emitimos 0 para indicar error
        } catch (e: Exception) {
            // Capturamos cualquier otro error inesperado
            emit(35.0) // Emitimos un valor por defecto de silencio
        }
    }.flowOn(Dispatchers.IO) // Ejecutamos en el hilo de I/O para no bloquear la UI

    /**
     * Calcula el RMS (Root Mean Square) de un array de muestras de audio.
     * 
     * El RMS es una medida estadística de la magnitud de una señal variante.
     * Es útil para calcular la amplitud "efectiva" de una señal de audio.
     * 
     * Fórmula: RMS = sqrt( (sum(x^2)) / n )
     * 
     * @param buffer Array de muestras de audio (valores Short de -32768 a 32767)
     * @param readSize Número de muestras válidas en el buffer
     * @return Double - Valor RMS calculado
     */
    private fun calculateRMS(buffer: ShortArray, readSize: Int): Double {
        var sum = 0.0
        
        // Iteramos sobre todas las muestras válidas
        for (i in 0 until readSize) {
            // Convertimos cada muestra a Double y la elevamos al cuadrado
            val sample = buffer[i].toDouble()
            sum += sample * sample
        }
        
        // Calculamos el promedio de los cuadrados
        val mean = sum / readSize
        
        // Retornamos la raíz cuadrada del promedio (esto es el RMS)
        return sqrt(mean)
    }

    /**
     * Detiene la captura de audio y libera los recursos.
     * 
     * Es MUY IMPORTANTE llamar a esta función cuando ya no necesitamos
     * el micrófono para liberar el recurso del sistema.
     */
    fun stopRecording() {
        isRecording = false
        
        try {
            // Detenemos la grabación
            audioRecord?.stop()
            
            // Liberamos los recursos nativos del AudioRecord
            audioRecord?.release()
            
            // Limpiamos la referencia
            audioRecord = null
        } catch (e: Exception) {
            // Manejamos silenciosamente cualquier error al detener
            // (por ejemplo, si ya estaba detenido)
            e.printStackTrace()
        }
    }
}
