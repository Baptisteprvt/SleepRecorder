package com.example.sleeprecorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class AudioAnalyzer(
    // Le seuil de déclenchement à 10 000 (à tester en conditions réelles).
    private val threshold: Int = 10000,
    private val onNoiseDetected: () -> Unit
) {

    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var analyzerJob: Job? = null
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isListening) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            audioRecord?.startRecording()
            isListening = true

            // On lance l'écoute dans un thread d'arrière-plan pour ne pas bloquer l'application
            analyzerJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ShortArray(bufferSize)

                while (isActive && isListening) {
                    val readResult = audioRecord?.read(buffer, 0, bufferSize) ?: 0

                    if (readResult > 0) {
                        val maxAmplitude = buffer.maxOfOrNull { abs(it.toInt()) } ?: 0

                        // Si le son dépasse notre seuil, on déclenche l'enregistrement
                        if (maxAmplitude > threshold) {
                            Log.d("AudioAnalyzer", "Bruit détecté ! Amplitude : $maxAmplitude")

                            onNoiseDetected()

                            stopListening() //On attend la fin de l'enregistrement (10secs)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioAnalyzer", "Erreur lors du démarrage de l'écoute", e)
        }
    }

    fun stopListening() {
        isListening = false
        analyzerJob?.cancel()
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }
}