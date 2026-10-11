package com.example.core.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.model.AudioPriority
import com.example.model.SoundCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.PriorityQueue

/**
 * Represents a queued audio playback request
 */
data class AudioRequest(
    val category: SoundCategory,
    val specificFilename: String? = null,
    val priority: AudioPriority = category.defaultPriority,
    val requestTimeMillis: Long = System.currentTimeMillis()
) : Comparable<AudioRequest> {
    override fun compareTo(other: AudioRequest): Int {
        val pComp = other.priority.value.compareTo(this.priority.value)
        return if (pComp != 0) pComp else this.requestTimeMillis.compareTo(other.requestTimeMillis)
    }
}

/**
 * Core AudioEngine handling non-overlapping audio playback across the 8 folders.
 * Features strict priority queueing, concurrency control, and amplitude simulation for UI visualizers.
 * Supports AR_EG Arabic voice assets with fallback path resolution.
 */
class AudioEngine(private val context: Context) {

    companion object {
        private const val TAG = "AudioEngine"
        private const val MAX_QUEUE_CAPACITY = 3
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val playbackMutex = Mutex()

    private var mediaPlayer: MediaPlayer? = null
    private var isCurrentlyPlaying = false
    private var currentPlayingAsset: String? = null

    // State flows for UI and Service telemetry
    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow.asStateFlow()

    private val _currentSoundFlow = MutableStateFlow<String?>(null)
    val currentSoundFlow: StateFlow<String?> = _currentSoundFlow.asStateFlow()

    private val _amplitudeFlow = MutableStateFlow(0f)
    val amplitudeFlow: StateFlow<Float> = _amplitudeFlow.asStateFlow()

    private var amplitudeJob: Job? = null

    // Priority Queue for incoming requests while engine is busy
    private val audioQueue = PriorityQueue<AudioRequest>()
    private val queueLock = Any()

    init {
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener {
                    onPlaybackCompleted()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    mp.reset()
                    onPlaybackCompleted()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPlayer", e)
        }
    }

    /**
     * Public entrypoint to play audio from one of the categories or specific sound file.
     */
    fun playCategory(
        category: SoundCategory,
        specificFilename: String? = null,
        priority: AudioPriority = category.defaultPriority
    ) {
        val request = AudioRequest(category, specificFilename, priority)
        scope.launch {
            handleAudioRequest(request)
        }
    }

    private suspend fun handleAudioRequest(request: AudioRequest) {
        playbackMutex.withLock {
            if (!isCurrentlyPlaying) {
                executePlayback(request)
            } else {
                val currentPlayingPriority = getCurrentPlayingPriority()
                if (request.priority == AudioPriority.CRITICAL && currentPlayingPriority <= AudioPriority.NORMAL) {
                    Log.d(TAG, "Critical priority request arrived, interrupting current sound")
                    stopCurrentPlayback()
                    executePlayback(request)
                    return@withLock
                }

                synchronized(queueLock) {
                    if (audioQueue.size < MAX_QUEUE_CAPACITY) {
                        audioQueue.offer(request)
                        Log.d(TAG, "AudioEngine busy. Queued request: ${request.category} (Queue size: ${audioQueue.size})")
                    } else {
                        val peek = audioQueue.peek()
                        if (peek != null && request.priority.value > peek.priority.value) {
                            audioQueue.poll()
                            audioQueue.offer(request)
                            Log.d(TAG, "Queue full. Replaced lower priority item with ${request.category}")
                        } else {
                            Log.d(TAG, "AudioEngine busy and queue full. Dropped request: ${request.category}")
                        }
                    }
                }
            }
        }
    }

    private fun getCurrentPlayingPriority(): AudioPriority {
        val asset = currentPlayingAsset ?: return AudioPriority.LOW
        return when {
            asset.contains("BATTERY") || asset.contains("CHARGING") -> AudioPriority.HIGH
            asset.contains("MOTION") || asset.contains("POCKET") || asset.contains("USAGE") -> AudioPriority.NORMAL
            else -> AudioPriority.LOW
        }
    }

    private fun executePlayback(request: AudioRequest) {
        val assetPath = resolveAssetPath(request)
        if (assetPath == null) {
            Log.w(TAG, "No sound asset found for category: ${request.category.folderName}")
            checkNextInQueue()
            return
        }

        try {
            if (mediaPlayer == null) {
                initMediaPlayer()
            }

            var played = false
            try {
                val afd: AssetFileDescriptor = context.assets.openFd(assetPath)
                mediaPlayer?.apply {
                    reset()
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    prepare()
                    start()
                }
                afd.close()
                played = true
            } catch (e: Exception) {
                // If openFd throws because asset is compressed, extract to temp file and play
                Log.w(TAG, "openFd failed for $assetPath, falling back to stream cache: ${e.message}")
                val tempFile = copyAssetToCache(assetPath)
                if (tempFile != null) {
                    mediaPlayer?.apply {
                        reset()
                        setDataSource(tempFile.absolutePath)
                        prepare()
                        start()
                    }
                    played = true
                }
            }

            if (played) {
                isCurrentlyPlaying = true
                currentPlayingAsset = assetPath
                _isPlayingFlow.value = true
                _currentSoundFlow.value = assetPath.substringAfterLast('/')

                startAmplitudeSimulation()
                Log.d(TAG, "Playing sound: $assetPath with priority ${request.priority}")
            } else {
                onPlaybackCompleted()
            }

        } catch (e: IOException) {
            Log.e(TAG, "IOException reading asset: $assetPath", e)
            onPlaybackCompleted()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error playing asset: $assetPath", e)
            onPlaybackCompleted()
        }
    }

    private fun copyAssetToCache(assetPath: String): File? {
        return try {
            val cacheFile = File(context.cacheDir, "temp_sound_" + assetPath.hashCode() + ".mp3")
            context.assets.open(assetPath).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy asset to cache: $assetPath", e)
            null
        }
    }

    private fun onPlaybackCompleted() {
        stopCurrentPlayback()
        checkNextInQueue()
    }

    private fun stopCurrentPlayback() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception stopping MediaPlayer", e)
        }
        isCurrentlyPlaying = false
        currentPlayingAsset = null
        _isPlayingFlow.value = false
        _currentSoundFlow.value = null
        stopAmplitudeSimulation()
    }

    private fun checkNextInQueue() {
        scope.launch {
            playbackMutex.withLock {
                val nextRequest: AudioRequest? = synchronized(queueLock) {
                    if (audioQueue.isNotEmpty()) audioQueue.poll() else null
                }
                if (nextRequest != null) {
                    executePlayback(nextRequest)
                }
            }
        }
    }

    /**
     * Resolves asset filename from category folder or specific requested file.
     * Searches both under AR_EG/ and root assets directory, with case-insensitive subfolder matching.
     */
    private fun resolveAssetPath(request: AudioRequest): String? {
        val folder = request.category.folderName

        // Candidate directory prefixes to search
        val candidateFolders = listOf(
            "$folder",
            folder,
            folder.replace("/Dark", "/dark").replace("/Light", "/light").replace("/LATE_NIGHT", "/late_night").replace("/SLEEP_REMINDER", "/sleep_reminder").replace("/Connect", "/connect").replace("/Disconnect", "/disconnect"),
            folder.replace("/Dark", "/dark").replace("/Light", "/light").replace("/LATE_NIGHT", "/late_night").replace("/SLEEP_REMINDER", "/sleep_reminder").replace("/Connect", "/connect").replace("/Disconnect", "/disconnect")
        )

        for (dir in candidateFolders) {
            if (request.specificFilename != null) {
                val path = "$dir/${request.specificFilename}"
                if (assetExists(path)) {
                    return path
                }
            } else {
                val files = getSoundFilesInFolder(dir)
                if (files.isNotEmpty()) {
                    val picked = files.random()
                    return "$dir/$picked"
                }
            }
        }

        return null
    }

    private fun assetExists(path: String): Boolean {
        return try {
            context.assets.open(path).use { }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getSoundFilesInFolder(folder: String): List<String> {
        return try {
            val list = context.assets.list(folder) ?: return emptyList()
            list.filter { it.endsWith(".mp3") || it.endsWith(".wav") || it.endsWith(".ogg") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Returns all available sound filenames in the specified folder
     */
    fun getAvailableSounds(category: SoundCategory): List<String> {
        val folder = category.folderName
        val candidateFolders = listOf(
            "$folder",
            folder,
            folder.replace("/Dark", "/dark").replace("/Light", "/light"),
            folder.replace("/Dark", "/dark").replace("/Light", "/light")
        )
        for (dir in candidateFolders) {
            val files = getSoundFilesInFolder(dir)
            if (files.isNotEmpty()) {
                return files
            }
        }
        return emptyList()
    }

    /**
     * Simulates dynamic RMS amplitude levels while audio is playing to drive the UI soundwave visualizer
     */
    private fun startAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            var step = 0
            while (isCurrentlyPlaying) {
                val wave = (Math.sin(step * 0.45) * 0.3 + Math.cos(step * 0.2) * 0.2 + 0.5).toFloat()
                _amplitudeFlow.value = wave.coerceIn(0.2f, 1.0f)
                step++
                delay(40L)
            }
            _amplitudeFlow.value = 0f
        }
    }

    private fun stopAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _amplitudeFlow.value = 0f
    }

    fun stopAll() {
        scope.launch {
            playbackMutex.withLock {
                synchronized(queueLock) {
                    audioQueue.clear()
                }
                stopCurrentPlayback()
            }
        }
    }

    fun release() {
        stopAll()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        }
    }
}
