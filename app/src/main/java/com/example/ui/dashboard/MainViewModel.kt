package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.audio.AudioEngine
import com.example.data.LivingPhonePreferences
import com.example.model.AudioPriority
import com.example.model.LivingEvent
import com.example.model.ModuleType
import com.example.model.SensorTelemetry
import com.example.model.SoundCategory
import com.example.service.LivingPhoneService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isServiceRunning: Boolean = false,
    val moduleSettings: Map<ModuleType, Boolean> = emptyMap(),
    val telemetry: SensorTelemetry = SensorTelemetry(),
    val activityLog: List<LivingEvent> = emptyList(),
    val isPlayingAudio: Boolean = false,
    val currentSoundName: String? = null,
    val audioAmplitude: Float = 0f,
    val isSoundSandboxVisible: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = LivingPhonePreferences(application)
    private var localAudioEngine: AudioEngine? = null

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var activeEngineJob: Job? = null

    init {
        observeState()
    }

    private fun observeState() {
        viewModelScope.launch {
            LivingPhoneService.isRunningFlow.collectLatest { isRunning ->
                _uiState.value = _uiState.value.copy(isServiceRunning = isRunning)
            }
        }

        viewModelScope.launch {
            preferences.modulesStateFlow.collectLatest { modules ->
                _uiState.value = _uiState.value.copy(moduleSettings = modules)
            }
        }

        viewModelScope.launch {
            LivingPhoneService.telemetryFlow.collectLatest { telemetry ->
                _uiState.value = _uiState.value.copy(telemetry = telemetry)
            }
        }

        viewModelScope.launch {
            LivingPhoneService.activityLogFlow.collectLatest { log ->
                _uiState.value = _uiState.value.copy(activityLog = log)
            }
        }

        viewModelScope.launch {
            LivingPhoneService.activeAudioEngineFlow.collectLatest { engine ->
                activeEngineJob?.cancel()
                if (engine != null) {
                    activeEngineJob = launch {
                        launch {
                            engine.isPlayingFlow.collectLatest { playing ->
                                _uiState.value = _uiState.value.copy(isPlayingAudio = playing)
                            }
                        }
                        launch {
                            engine.currentSoundFlow.collectLatest { sound ->
                                _uiState.value = _uiState.value.copy(currentSoundName = sound)
                            }
                        }
                        launch {
                            engine.amplitudeFlow.collectLatest { amp ->
                                _uiState.value = _uiState.value.copy(audioAmplitude = amp)
                            }
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPlayingAudio = false,
                        currentSoundName = null,
                        audioAmplitude = 0f
                    )
                }
            }
        }
    }

    fun getOrCreateLocalAudioEngine(): AudioEngine {
        if (localAudioEngine == null) {
            localAudioEngine = AudioEngine(getApplication())
        }
        return localAudioEngine!!
    }

    fun toggleService() {
        val currentlyRunning = LivingPhoneService.isRunningFlow.value
        if (currentlyRunning) {
            LivingPhoneService.stop(getApplication())
        } else {
            LivingPhoneService.start(getApplication())
        }
    }

    fun toggleModule(module: ModuleType) {
        val currentEnabled = preferences.isModuleEnabled(module)
        preferences.setModuleEnabled(module, !currentEnabled)
    }

    fun testModuleSound(module: ModuleType) {
        val category = when (module) {
            ModuleType.BATTERY -> SoundCategory.BATTERY_100
            ModuleType.CHARGING -> SoundCategory.CHARGING_CONNECT
            ModuleType.EXERCISE -> SoundCategory.EXERCISE
            ModuleType.GENERAL -> SoundCategory.GENERAL
            ModuleType.MOTION -> SoundCategory.MOTION
            ModuleType.POCKET -> SoundCategory.POCKET_LIGHT
            ModuleType.SLEEP -> SoundCategory.SLEEP_LATE_NIGHT
            ModuleType.USAGE -> SoundCategory.USAGE
        }

        if (LivingPhoneService.isRunningFlow.value) {
            LivingPhoneService.testTrigger(getApplication(), category, module)
        } else {
            val engine = getOrCreateLocalAudioEngine()
            engine.playCategory(category, priority = AudioPriority.CRITICAL)
            _uiState.value = _uiState.value.copy(
                isPlayingAudio = true,
                currentSoundName = category.folderName
            )
        }
    }

    fun playSpecificSound(category: SoundCategory, filename: String) {
        if (LivingPhoneService.isRunningFlow.value) {
            LivingPhoneService.activeAudioEngineFlow.value?.playCategory(
                category = category,
                specificFilename = filename,
                priority = AudioPriority.CRITICAL
            )
        } else {
            val engine = getOrCreateLocalAudioEngine()
            engine.playCategory(
                category = category,
                specificFilename = filename,
                priority = AudioPriority.CRITICAL
            )
            _uiState.value = _uiState.value.copy(
                isPlayingAudio = true,
                currentSoundName = filename
            )
        }
    }

    fun setSandboxVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isSoundSandboxVisible = visible)
    }

    override fun onCleared() {
        super.onCleared()
        localAudioEngine?.release()
        localAudioEngine = null
    }
}
