with open("app/src/main/java/com/example/core/audio/AudioEngine.kt", "r") as f:
    code = f.read()

target_start = "    private fun executePlayback(request: AudioRequest) {"
target_end = "    private fun copyAssetToCache(assetPath: String): File? {"

new_method = """    private fun executePlayback(request: AudioRequest) {
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

            val tempFile = copyAssetToCache(assetPath)
            if (tempFile == null || !tempFile.exists()) {
                Log.e(TAG, "Failed to cache asset: $assetPath")
                onPlaybackCompleted()
                return
            }

            mediaPlayer?.apply {
                reset()
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    isCurrentlyPlaying = true
                    currentPlayingAsset = assetPath
                    _isPlayingFlow.value = true
                    _currentSoundFlow.value = assetPath.substringAfterLast('/')
                    startAmplitudeSimulation()
                    Log.d(TAG, "Successfully playing sound: $assetPath")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra for $assetPath")
                    onPlaybackCompleted()
                    true
                }
                setOnCompletionListener {
                    onPlaybackCompleted()
                }
                prepareAsync()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during executePlayback for $assetPath", e)
            onPlaybackCompleted()
        }
    }

    private fun copyAssetToCache(assetPath: String): File? {"""

if target_start in code and target_end in code:
    parts = code.split(target_start)
    prefix = parts[0]
    rest = parts[1].split(target_end)[1]
    
    updated_code = prefix + new_method + rest
    with open("app/src/main/java/com/example/core/audio/AudioEngine.kt", "w") as f:
        f.write(updated_code)
    print("Successfully updated AudioEngine.kt with robust async playback!")
else:
    print("Error: Could not find exact boundaries for replacement.")
