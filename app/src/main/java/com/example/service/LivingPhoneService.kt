package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.audio.AudioEngine
import com.example.data.LivingPhonePreferences
import com.example.model.AudioPriority
import com.example.model.LivingEvent
import com.example.model.ModuleType
import com.example.model.SensorTelemetry
import com.example.model.SoundCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.sqrt
import kotlin.random.Random

class LivingPhoneService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "LivingPhoneService"
        const val CHANNEL_ID = "living_phone_consciousness"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_TEST_TRIGGER = "com.example.service.ACTION_TEST_TRIGGER"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_MODULE = "extra_module"

        // Cooldown constants (milliseconds)
        private const val COOLDOWN_SHAKE_MS = 15_000L           // 15 seconds
        private const val COOLDOWN_POCKET_DARK_MS = 30_000L     // 30 seconds
        private const val COOLDOWN_POCKET_LIGHT_MS = 30_000L    // 30 seconds
        private const val COOLDOWN_BATTERY_LOW_MS = 300_000L    // 5 minutes
        private const val COOLDOWN_CHARGING_MS = 20_000L        // 20 seconds
        private const val COOLDOWN_EXERCISE_MS = 1_800_000L     // 30 minutes
        private const val COOLDOWN_LATE_NIGHT_MS = 1_800_000L   // 30 minutes
        private const val COOLDOWN_SLEEP_REMINDER_MS = 3_600_000L // 1 hour
        private const val COOLDOWN_GENERAL_UNLOCK_MS = 1_200_000L // 20 minutes
        private const val COOLDOWN_USAGE_MS = 1_200_000L        // 20 minutes
        private const val USAGE_TRIGGER_THRESHOLD_SECONDS = 1200L // 20 mins screen on

        // Shared reactive state for UI
        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

        private val _telemetryFlow = MutableStateFlow(SensorTelemetry())
        val telemetryFlow: StateFlow<SensorTelemetry> = _telemetryFlow.asStateFlow()

        private val _activityLogFlow = MutableStateFlow<List<LivingEvent>>(emptyList())
        val activityLogFlow: StateFlow<List<LivingEvent>> = _activityLogFlow.asStateFlow()

        private val _activeAudioEngineFlow = MutableStateFlow<AudioEngine?>(null)
        val activeAudioEngineFlow: StateFlow<AudioEngine?> = _activeAudioEngineFlow.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, LivingPhoneService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LivingPhoneService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun testTrigger(context: Context, category: SoundCategory, module: ModuleType) {
            val intent = Intent(context, LivingPhoneService::class.java).apply {
                action = ACTION_TEST_TRIGGER
                putExtra(EXTRA_CATEGORY, category.name)
                putExtra(EXTRA_MODULE, module.name)
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var preferences: LivingPhonePreferences
    private lateinit var audioEngine: AudioEngine

    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Pocket state tracking
    private var isCurrentlyInPocket = false
    private var darkDebounceJob: Job? = null
    private var lastPocketDarkTime = 0L
    private var lastPocketLightTime = 0L

    // Accelerometer tracking
    private var lastShakeTime = 0L
    private var gravityValues = FloatArray(3)
    private var hasGravity = false

    // Battery & Charging tracking
    private var previousBatteryLevel = -1
    private var wasCharging = false
    private var lastBatteryLowTime = 0L
    private var lastChargingTriggerTime = 0L

    // Time & Screen activity tracking
    private var isScreenOn = true
    private var screenSessionSeconds = 0L
    private var lastScreenTurnedOnTime = System.currentTimeMillis()
    private var lastExercisePromptTime = 0L
    private var lastLateNightTime = 0L
    private var lastSleepReminderTime = 0L
    private var lastGeneralUnlockTime = 0L
    private var lastUsagePromptTime = 0L

    private var timeMonitorJob: Job? = null
    private var screenTimerJob: Job? = null

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent)
                Intent.ACTION_POWER_CONNECTED -> handlePowerConnected()
                Intent.ACTION_POWER_DISCONNECTED -> handlePowerDisconnected()
                Intent.ACTION_SCREEN_ON -> handleScreenOn()
                Intent.ACTION_SCREEN_OFF -> handleScreenOff()
                Intent.ACTION_USER_PRESENT -> handleUserUnlock()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LivingPhoneService onCreate")
        preferences = LivingPhonePreferences(this)
        audioEngine = AudioEngine(this)
        _activeAudioEngineFlow.value = audioEngine

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LivingPhone::SensoryCore")

        createNotificationChannel()
        registerSensors()
        registerSystemReceivers()
        startTimeAndSessionMonitors()

        _isRunningFlow.value = true
        preferences.setServiceEnabled(true)
        logEvent(ModuleType.GENERAL, "Core Awakened", "Living sensory consciousness initialized and active.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_STOP -> {
                    Log.d(TAG, "Stopping service via ACTION_STOP")
                    stopSelf()
                    return START_NOT_STICKY
                }
                ACTION_TEST_TRIGGER -> {
                    val catName = intent.getStringExtra(EXTRA_CATEGORY)
                    val modName = intent.getStringExtra(EXTRA_MODULE)
                    if (catName != null && modName != null) {
                        try {
                            val cat = SoundCategory.valueOf(catName)
                            val mod = ModuleType.valueOf(modName)
                            triggerManualSound(cat, mod)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling test trigger", e)
                        }
                    }
                }
            }
        }

        startForegroundNotification()
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notification = buildForegroundNotification("Living Phone Sensory Core Active")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, LivingPhoneService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.living_phone_icon_1791671160262)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Deactivate", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildForegroundNotification(statusText))
    }

    // ==========================================
    // Sensor Listeners (Light & Accelerometer)
    // ==========================================
    private fun registerSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor != null) {
            sensorManager?.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                handleLightSensor(lux)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                handleAccelerometer(event.values)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Pocket State Detection:
     * Transitions between Dark (~0 lux) and Light (> 25 lux)
     */
    private fun handleLightSensor(lux: Float) {
        _telemetryFlow.value = _telemetryFlow.value.copy(
            lux = lux,
            isPocketDark = isCurrentlyInPocket
        )

        if (!preferences.isModuleEnabled(ModuleType.POCKET)) return

        val now = System.currentTimeMillis()

        // Pocket Dark Transition (~0 to 3 lux)
        if (lux < 3.0f) {
            if (!isCurrentlyInPocket && darkDebounceJob == null) {
                darkDebounceJob = serviceScope.launch {
                    delay(1200L)
                    if (_telemetryFlow.value.lux < 3.0f) {
                        isCurrentlyInPocket = true
                        _telemetryFlow.value = _telemetryFlow.value.copy(isPocketDark = true)
                        if (now - lastPocketDarkTime > COOLDOWN_POCKET_DARK_MS) {
                            lastPocketDarkTime = now
                            audioEngine.playCategory(SoundCategory.POCKET_DARK, priority = AudioPriority.NORMAL)
                            logEvent(
                                ModuleType.POCKET,
                                "Pocket In (Dark Hush)",
                                "Ambient light dropped to ${lux.toInt()} lx. Resting in pocket."
                            )
                        }
                    }
                    darkDebounceJob = null
                }
            }
        } else if (lux > 25.0f) {
            darkDebounceJob?.cancel()
            darkDebounceJob = null

            // Transition from Pocket to Light
            if (isCurrentlyInPocket) {
                isCurrentlyInPocket = false
                _telemetryFlow.value = _telemetryFlow.value.copy(isPocketDark = false)
                if (now - lastPocketLightTime > COOLDOWN_POCKET_LIGHT_MS) {
                    lastPocketLightTime = now
                    audioEngine.playCategory(SoundCategory.POCKET_LIGHT, priority = AudioPriority.NORMAL)
                    logEvent(
                        ModuleType.POCKET,
                        "Pocket Out (Greeting)",
                        "Emerging into light (${lux.toInt()} lx). Greeting world."
                    )
                }
            }
        }
    }

    /**
     * Accelerometer Motion & Shaking Detection
     */
    private fun handleAccelerometer(values: FloatArray) {
        val alpha = 0.8f
        if (!hasGravity) {
            gravityValues[0] = values[0]
            gravityValues[1] = values[1]
            gravityValues[2] = values[2]
            hasGravity = true
        } else {
            gravityValues[0] = alpha * gravityValues[0] + (1 - alpha) * values[0]
            gravityValues[1] = alpha * gravityValues[1] + (1 - alpha) * values[1]
            gravityValues[2] = alpha * gravityValues[2] + (1 - alpha) * values[2]
        }

        val linearX = values[0] - gravityValues[0]
        val linearY = values[1] - gravityValues[1]
        val linearZ = values[2] - gravityValues[2]
        val netForce = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)

        _telemetryFlow.value = _telemetryFlow.value.copy(accelerationDelta = netForce)

        if (!preferences.isModuleEnabled(ModuleType.MOTION)) return

        if (netForce > 13.0f) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > COOLDOWN_SHAKE_MS) {
                lastShakeTime = now
                audioEngine.playCategory(SoundCategory.MOTION, priority = AudioPriority.NORMAL)
                logEvent(
                    ModuleType.MOTION,
                    "Sudden Shake / G-Force",
                    "Kinetic acceleration surged to ${String.format("%.1f", netForce)} m/s²."
                )
            }
        }
    }

    // ==========================================
    // Battery & Charging Handlers
    // ==========================================
    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else level
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        _telemetryFlow.value = _telemetryFlow.value.copy(
            batteryLevel = batteryPct,
            isCharging = isCharging
        )

        val now = System.currentTimeMillis()

        // Battery Module Logic (~15% low, 100% full)
        if (preferences.isModuleEnabled(ModuleType.BATTERY)) {
            if (previousBatteryLevel > 15 && batteryPct in 1..15 && !isCharging) {
                if (now - lastBatteryLowTime > COOLDOWN_BATTERY_LOW_MS) {
                    lastBatteryLowTime = now
                    audioEngine.playCategory(SoundCategory.BATTERY_15, priority = AudioPriority.HIGH)
                    logEvent(
                        ModuleType.BATTERY,
                        "Low Power Alert",
                        "Battery level critically depleted to $batteryPct%. Yearning for energy."
                    )
                }
            } else if (previousBatteryLevel < 100 && batteryPct == 100 && isCharging) {
                audioEngine.playCategory(SoundCategory.BATTERY_100, priority = AudioPriority.HIGH)
                logEvent(
                    ModuleType.BATTERY,
                    "Full Vitality Saturation",
                    "Battery reached 100% maximum capacity. Glowing with energy!"
                )
            }
        }

        previousBatteryLevel = batteryPct
        wasCharging = isCharging
    }

    private fun handlePowerConnected() {
        _telemetryFlow.value = _telemetryFlow.value.copy(isCharging = true)
        if (!preferences.isModuleEnabled(ModuleType.CHARGING)) return

        val now = System.currentTimeMillis()
        if (now - lastChargingTriggerTime > COOLDOWN_CHARGING_MS) {
            lastChargingTriggerTime = now
            audioEngine.playCategory(SoundCategory.CHARGING_CONNECT, priority = AudioPriority.HIGH)
            logEvent(
                ModuleType.CHARGING,
                "Energy Infusion Engaged",
                "Power source connected. Energy flow initiated."
            )
        }
    }

    private fun handlePowerDisconnected() {
        _telemetryFlow.value = _telemetryFlow.value.copy(isCharging = false)
        if (!preferences.isModuleEnabled(ModuleType.CHARGING)) return

        val now = System.currentTimeMillis()
        if (now - lastChargingTriggerTime > COOLDOWN_CHARGING_MS) {
            lastChargingTriggerTime = now
            audioEngine.playCategory(SoundCategory.CHARGING_DISCONNECT, priority = AudioPriority.NORMAL)
            logEvent(
                ModuleType.CHARGING,
                "Energy Flow Detached",
                "Disconnected from power source."
            )
        }
    }

    // ==========================================
    // Screen & Time Activity Handlers
    // ==========================================
    private fun handleScreenOn() {
        isScreenOn = true
        lastScreenTurnedOnTime = System.currentTimeMillis()
        _telemetryFlow.value = _telemetryFlow.value.copy(isScreenOn = true)
    }

    private fun handleScreenOff() {
        isScreenOn = false
        screenSessionSeconds = 0L
        _telemetryFlow.value = _telemetryFlow.value.copy(
            isScreenOn = false,
            screenSessionSeconds = 0L
        )
    }

    /**
     * Screen unlock trigger (General module: ~5% RNG, long cooldown)
     * Also checks Late Night sleep module trigger (12 AM - 4 AM) with exact hour voice!
     */
    private fun handleUserUnlock() {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // 1. Sleep Module Late Night Check (12 AM - 4 AM / 00:00 - 04:00)
        if (preferences.isModuleEnabled(ModuleType.SLEEP) && hour in 0..4) {
            if (now - lastLateNightTime > COOLDOWN_LATE_NIGHT_MS) {
                lastLateNightTime = now
                val specificHourFile = when (hour) {
                    0 -> "hour_12_midnight.mp3"
                    1 -> "hour_01_am.mp3"
                    2 -> "hour_02_am.mp3"
                    3 -> "hour_03_am.mp3"
                    4 -> "hour_04_am.mp3"
                    else -> "hour_12_midnight.mp3"
                }
                audioEngine.playCategory(
                    category = SoundCategory.SLEEP_LATE_NIGHT,
                    specificFilename = specificHourFile,
                    priority = AudioPriority.NORMAL
                )
                logEvent(
                    ModuleType.SLEEP,
                    "Late Night Whisper ($hour:00 AM)",
                    "Active during late night hours. Encouraging rest."
                )
                return
            }
        }

        // 2. General Module: Unlock trigger with low probability (~5%) and long cooldown
        if (preferences.isModuleEnabled(ModuleType.GENERAL)) {
            if (now - lastGeneralUnlockTime > COOLDOWN_GENERAL_UNLOCK_MS) {
                val roll = Random.nextInt(100)
                if (roll < 5) {
                    lastGeneralUnlockTime = now
                    audioEngine.playCategory(SoundCategory.GENERAL, priority = AudioPriority.LOW)
                    logEvent(
                        ModuleType.GENERAL,
                        "Spontaneous Greeting",
                        "Spontaneous conscious curiosity upon unlock."
                    )
                }
            }
        }
    }

    /**
     * Continuous monitoring loops for Exercise, Usage, and Sleep Reminder
     */
    private fun startTimeAndSessionMonitors() {
        // Continuous screen session timer (Usage module)
        screenTimerJob?.cancel()
        screenTimerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                if (isScreenOn) {
                    screenSessionSeconds++
                    _telemetryFlow.value = _telemetryFlow.value.copy(
                        screenSessionSeconds = screenSessionSeconds
                    )

                    if (preferences.isModuleEnabled(ModuleType.USAGE) &&
                        screenSessionSeconds >= USAGE_TRIGGER_THRESHOLD_SECONDS
                    ) {
                        val now = System.currentTimeMillis()
                        if (now - lastUsagePromptTime > COOLDOWN_USAGE_MS) {
                            lastUsagePromptTime = now
                            audioEngine.playCategory(SoundCategory.USAGE, priority = AudioPriority.NORMAL)
                            logEvent(
                                ModuleType.USAGE,
                                "Neural Fatigue Warning",
                                "Continuous screen exposure for ${(screenSessionSeconds / 60)} minutes. Suggesting eye break."
                            )
                        }
                    }
                }
            }
        }

        // Time-gated periodic checks (Exercise & Sleep Reminders)
        timeMonitorJob?.cancel()
        timeMonitorJob = serviceScope.launch {
            while (isActive) {
                delay(30_000L)
                val now = System.currentTimeMillis()
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)

                // 1. Exercise Module: Strictly time-gated between 8 AM and 8 PM (08:00 - 20:00)
                if (preferences.isModuleEnabled(ModuleType.EXERCISE) && hour in 8..19) {
                    if (now - lastExercisePromptTime > COOLDOWN_EXERCISE_MS) {
                        if (Random.nextFloat() < 0.15f) {
                            lastExercisePromptTime = now
                            audioEngine.playCategory(SoundCategory.EXERCISE, priority = AudioPriority.LOW)
                            logEvent(
                                ModuleType.EXERCISE,
                                "Sedentary Stretch Nudge",
                                "Kinetic stillness detected during daytime ($hour:00). Encouraging movement."
                            )
                        }
                    }
                }

                // 2. Sleep Module: Evening Bedtime Reminder (10 PM - 11:59 PM)
                if (preferences.isModuleEnabled(ModuleType.SLEEP) && hour in 22..23) {
                    if (now - lastSleepReminderTime > COOLDOWN_SLEEP_REMINDER_MS) {
                        lastSleepReminderTime = now
                        audioEngine.playCategory(SoundCategory.SLEEP_REMINDER, priority = AudioPriority.LOW)
                        logEvent(
                            ModuleType.SLEEP,
                            "Circadian Bedtime Bell",
                            "Evening rest window ($hour:00). Playing soothing bedtime reminder."
                        )
                    }
                }
            }
        }
    }

    private fun registerSystemReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(systemReceiver, filter)
    }

    private fun triggerManualSound(category: SoundCategory, module: ModuleType) {
        audioEngine.playCategory(category, priority = AudioPriority.CRITICAL)
        logEvent(module, "Manual Acoustic Probe", "Triggered test sound from ${category.folderName}.")
    }

    private fun logEvent(module: ModuleType, title: String, detail: String) {
        val event = LivingEvent(
            timestamp = System.currentTimeMillis(),
            module = module,
            title = title,
            detail = detail,
            soundName = audioEngine.currentSoundFlow.value
        )
        val currentList = _activityLogFlow.value.toMutableList()
        currentList.add(0, event)
        if (currentList.size > 40) {
            currentList.removeAt(currentList.lastIndex)
        }
        _activityLogFlow.value = currentList

        _telemetryFlow.value = _telemetryFlow.value.copy(
            lastTriggerModule = module,
            lastTriggerDescription = title,
            lastTriggerTimeMillis = event.timestamp
        )
        updateNotification("$title: $detail")
    }

    override fun onDestroy() {
        Log.d(TAG, "LivingPhoneService onDestroy")
        try {
            unregisterReceiver(systemReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Receiver already unregistered", e)
        }
        sensorManager?.unregisterListener(this)
        screenTimerJob?.cancel()
        timeMonitorJob?.cancel()
        serviceScope.cancel()

        audioEngine.release()
        _activeAudioEngineFlow.value = null
        _isRunningFlow.value = false
        preferences.setServiceEnabled(false)

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
