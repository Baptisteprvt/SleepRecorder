package com.example.sleeprecorder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SleepRecordingService : Service() {

    private val CHANNEL_ID = "SleepRecorderChannel"
    private var wakeLock: PowerManager.WakeLock? = null

    private var audioAnalyzer: AudioAnalyzer? = null
    private var mediaRecorder: MediaRecorder? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var recordingJob: Job? = null

    private var delayJob: Job? = null //30 minutes d'endormissement

    override fun onCreate() {
        super.onCreate()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SleepRecorder::MicWakeLock")
        wakeLock?.acquire()

        audioAnalyzer = AudioAnalyzer(
            threshold = 15000,
            onNoiseDetected = { startRecording10Seconds() }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()

        delayJob = serviceScope.launch {
            Log.d("SleepService", "Début de la phase d'endormissement (30 minutes)...")

            // Délai de 30 minutes pour passer l'endormissement
            delay(30 * 60 * 1000L)

            if (isActive) {
                Log.d("SleepService", "Fin de la phase d'endormissement. Début de l'écoute.")
                audioAnalyzer?.startListening()
            }
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Service d'enregistrement",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Enregistreur de Sommeil")
            .setContentText("Phase d'endormissement... L'écoute démarrera dans 30 min.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun startRecording10Seconds() {
        if (recordingJob?.isActive == true) return

        recordingJob = serviceScope.launch {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val outputFile = File(filesDir, "SleepRecord_$timestamp.m4a")

                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this@SleepRecordingService)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(outputFile.absolutePath)
                    prepare()
                    start()
                }

                delay(10000)

                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null

                audioAnalyzer?.startListening()

            } catch (e: Exception) {
                Log.e("SleepService", "Erreur d'enregistrement", e)
                audioAnalyzer?.startListening()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.takeIf { it.isHeld }?.release()

        delayJob?.cancel()

        audioAnalyzer?.stopListening()
        recordingJob?.cancel()
        mediaRecorder?.release()
        mediaRecorder = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}