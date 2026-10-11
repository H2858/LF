package com.example.model

/**
 * The 8 core sensory modules of Living Phone
 */
enum class ModuleType(
    val title: String,
    val subtitle: String,
    val description: String,
    val defaultEnabled: Boolean = true
) {
    BATTERY(
        title = "Power Metabolism",
        subtitle = "Battery State Awareness",
        description = "Reactions to low energy starvation (~15%) and full vitality saturation (100%)."
    ),
    CHARGING(
        title = "Energy Infusion",
        subtitle = "Power Cable & Wireless Flux",
        description = "Organic harmonic surges when connected to power, gentle detachment tone when disconnected."
    ),
    EXERCISE(
        title = "Kinetic Motivation",
        subtitle = "Sedentary Behavior Guard",
        description = "Time-gated prompts (8 AM - 8 PM) with intelligent cooldown & low RNG to avoid disturbance."
    ),
    GENERAL(
        title = "Living Consciousness",
        subtitle = "Spontaneous Curiosity",
        description = "Subtle, low-probability (~5%) unlock reactions and rare curious acoustic blips."
    ),
    MOTION(
        title = "Vestibular Balance",
        subtitle = "Kinetic & Shaking Telemetry",
        description = "Responds playfully to sudden accelerations, turbulence, and vigorous shakes."
    ),
    POCKET(
        title = "Ambient Proximity",
        subtitle = "Light Sensor & Pocket State",
        description = "Snuggles into restful hush in dark pockets (~0 lux), greets the world when exposed to light."
    ),
    SLEEP(
        title = "Circadian Rhythm",
        subtitle = "Night Owl & Rest Reminders",
        description = "Deep hour-specific whispers during late night (12 AM - 4 AM) and calming evening bedtime reminders."
    ),
    USAGE(
        title = "Neural Fatigue",
        subtitle = "Screen Session Monitoring",
        description = "Monitors continuous uninterrupted screen focus and gently reminds to rest tired eyes."
    )
}

/**
 * Priority levels for audio engine concurrency control
 */
enum class AudioPriority(val value: Int) {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4)
}

/**
 * Sound categories matching exact asset directory structure:
 * AR_EG/BATTERY/100, AR_EG/BATTERY/15,
 * AR_EG/CHARGING/Connect, AR_EG/CHARGING/Disconnect,
 * AR_EG/EXERCISE, AR_EG/GENERAL, AR_EG/MOTION,
 * AR_EG/POCKET/Dark, AR_EG/POCKET/Light,
 * AR_EG/SLEEP/LATE_NIGHT, AR_EG/SLEEP/SLEEP_REMINDER,
 * AR_EG/USAGE.
 */
enum class SoundCategory(
    val folderName: String,
    val defaultPriority: AudioPriority
) {
    BATTERY("BATTERY/15", AudioPriority.HIGH),
    BATTERY_100("BATTERY/100", AudioPriority.HIGH),
    BATTERY_15("BATTERY/15", AudioPriority.HIGH),
    CHARGING("CHARGING/Connect", AudioPriority.HIGH),
    CHARGING_CONNECT("CHARGING/Connect", AudioPriority.HIGH),
    CHARGING_DISCONNECT("CHARGING/Disconnect", AudioPriority.HIGH),
    EXERCISE("EXERCISE", AudioPriority.LOW),
    GENERAL("GENERAL", AudioPriority.LOW),
    MOTION("MOTION", AudioPriority.NORMAL),
    POCKET_DARK("POCKET/Dark", AudioPriority.NORMAL),
    POCKET_LIGHT("POCKET/Light", AudioPriority.NORMAL),
    SLEEP_LATE_NIGHT("SLEEP/LATE_NIGHT", AudioPriority.NORMAL),
    SLEEP_REMINDER("SLEEP/SLEEP_REMINDER", AudioPriority.LOW),
    USAGE("USAGE", AudioPriority.NORMAL);

    val parentCategory: String
        get() = folderName.substringBefore('/')

    val subFolder: String?
        get() = if (folderName.contains('/')) folderName.substringAfter('/') else null
}

/**
 * Real-time sensory readings passed to UI
 */
data class SensorTelemetry(
    val lux: Float = 0f,
    val isPocketDark: Boolean = false,
    val accelerationDelta: Float = 0f,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val screenSessionSeconds: Long = 0L,
    val isScreenOn: Boolean = true,
    val lastTriggerModule: ModuleType? = null,
    val lastTriggerDescription: String = "Engine initialized",
    val lastTriggerTimeMillis: Long = System.currentTimeMillis()
)

/**
 * Historical reaction log for sensory telemetry stream
 */
data class LivingEvent(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val module: ModuleType,
    val title: String,
    val detail: String,
    val soundName: String? = null
)
