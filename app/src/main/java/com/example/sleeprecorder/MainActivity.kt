package com.example.sleeprecorder

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0A)
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

enum class Screen { RECORD, LIST }

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.RECORD) }
    when (currentScreen) {
        Screen.RECORD -> SleepRecorderScreen(onNavigateToList = { currentScreen = Screen.LIST })
        Screen.LIST -> RecordingsListScreen(onNavigateBack = { currentScreen = Screen.RECORD })
    }
}

@Composable
fun NeonSleepButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val neonColor = if (isRecording) Color(0xFFFF073A) else Color(0xFF00F7FF)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "scaleAnim")

    //Effet de halo
    Box(
        modifier = modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to neonColor.copy(alpha = 0.5f),
                            0.66f to neonColor.copy(alpha = 0.25f),
                            1.0f to Color.Transparent // Fondu
                        )
                    )
                )
        )

        //Bouton cliquable
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2A2A2A), Color.Black)
                    )
                )
                .border(width = 2.dp, color = neonColor.copy(alpha = 0.8f), shape = CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isRecording) "ARRÊTER" else "COMMENCER\nLA NUIT",
                color = neonColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    LocalTextStyle.current.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = neonColor,
                            blurRadius = 15f
                        )
                    )
                } else LocalTextStyle.current
            )
        }
    }
}

@Composable
fun SleepRecorderScreen(onNavigateToList: () -> Unit) {
    val context = LocalContext.current
    var isRecordingState by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else { true }

        if (audioGranted && notifGranted) {
            cleanUpOldRecordings(context)
            val intent = Intent(context, SleepRecordingService::class.java)
            ContextCompat.startForegroundService(context, intent)
            isRecordingState = true
        } else {
            Toast.makeText(context, "Permissions requises", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enregistreur de Sommeil",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            style = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                LocalTextStyle.current.copy(shadow = androidx.compose.ui.graphics.Shadow(color = Color(0xFF00F7FF), blurRadius = 20f))
            } else LocalTextStyle.current
        )

        Spacer(modifier = Modifier.height(70.dp))

        NeonSleepButton(
            isRecording = isRecordingState,
            onClick = {
                if (!isRecordingState) {
                    val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else { true }

                    if (hasAudio && hasNotif) {
                        cleanUpOldRecordings(context)
                        val intent = Intent(context, SleepRecordingService::class.java)
                        ContextCompat.startForegroundService(context, intent)
                        isRecordingState = true
                    } else {
                        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        permissionLauncher.launch(perms.toTypedArray())
                    }
                } else {
                    context.stopService(Intent(context, SleepRecordingService::class.java))
                    isRecordingState = false
                }
            }
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = if (isRecordingState) "Endormissement en cours (30min)..." else "Prêt à enregistrer",
            color = if(isRecordingState) Color(0xFF00F7FF).copy(alpha=0.7f) else Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(60.dp))

        OutlinedButton(
            onClick = onNavigateToList,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
        ) {
            Icon(Icons.Default.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Voir les enregistrements")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun RecordingsListScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<File>>(emptyList()) }

    val mediaPlayer = remember { MediaPlayer() }
    var playingFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        val directory = context.filesDir
        val audioFiles = directory.listFiles { file ->
            file.name.startsWith("SleepRecord_") && file.name.endsWith(".m4a")
        }
        files = audioFiles?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("← Retour", color = Color.White)
            }

            if (files.isNotEmpty()) {
                Button(
                    onClick = {
                        if (playingFile != null) {
                            mediaPlayer.stop()
                            mediaPlayer.reset()
                            playingFile = null
                        }
                        files.forEach { it.delete() }
                        files = emptyList()
                        Toast.makeText(context, "Tous les enregistrements ont été supprimés", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                        contentDescription = "Tout supprimer",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tout supprimer", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Vos nuits", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (files.isEmpty()) {
            Text("Aucun enregistrement pour le moment.", color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files) { file ->
                    val isPlaying = playingFile == file

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E1E))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (isPlaying) {
                                        mediaPlayer.stop()
                                        mediaPlayer.reset()
                                        playingFile = null
                                    } else {
                                        mediaPlayer.reset()
                                        mediaPlayer.setDataSource(file.absolutePath)
                                        mediaPlayer.prepare()
                                        mediaPlayer.start()
                                        playingFile = file
                                        mediaPlayer.setOnCompletionListener { playingFile = null }
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = if (isPlaying) Color.Green else Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = formatFileName(file.name), color = Color.White, fontSize = 14.sp)
                        }

                        IconButton(onClick = { saveToDownloads(context, file) }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Share,
                                contentDescription = "Sauvegarder",
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatFileName(fileName: String): String {
    return try {
        val dateString = fileName.removePrefix("SleepRecord_").removeSuffix(".m4a")
        val inputFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy à HH:mm:ss", Locale.getDefault())
        val date: Date = inputFormat.parse(dateString) ?: return fileName
        outputFormat.format(date)
    } catch (e: Exception) {
        fileName
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun saveToDownloads(context: android.content.Context, file: File) {
    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/SleepRecorder")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri).use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream!!)
                }
            }
            Toast.makeText(context, "Sauvegardé dans Téléchargements/SleepRecorder", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Erreur lors de la sauvegarde : ${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun cleanUpOldRecordings(context: android.content.Context, daysToKeep: Int = 1) {
    val directory = context.filesDir
    val files = directory.listFiles { file ->
        file.name.startsWith("SleepRecord_") && file.name.endsWith(".m4a")
    }

    if (files == null) return

    val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)

    var deletedCount = 0
    for (file in files) {
        if (file.lastModified() < cutoffTime) {
            if (file.delete()) {
                deletedCount++
            }
        }
    }
    Log.d("SleepRecorder", "Nettoyage terminé : $deletedCount anciens fichiers supprimés.")
}