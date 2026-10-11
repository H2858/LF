package com.example.ui.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LivingEvent
import com.example.model.ModuleType
import com.example.model.SoundCategory
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.SoundWaveVisualizer
import com.example.ui.components.TelemetryStrip
import com.example.ui.theme.BioAmber
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BioCyanMuted
import com.example.ui.theme.BioGreen
import com.example.ui.theme.BioTextMuted
import com.example.ui.theme.BioTextPrimary
import com.example.ui.theme.BioTextSecondary
import com.example.ui.theme.BioViolet
import com.example.ui.theme.BioVioletLight
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardBorderActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Handled */ }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CharcoalBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top App Bar Header
            item {
                DashboardHeader(isServiceRunning = uiState.isServiceRunning)
            }

            // 2. Hero Glass Card: Power Consciousness & Sound Wave
            item {
                HeroConsciousnessCard(
                    isServiceRunning = uiState.isServiceRunning,
                    isPlayingAudio = uiState.isPlayingAudio,
                    currentSoundName = uiState.currentSoundName,
                    amplitude = uiState.audioAmplitude,
                    onToggleService = { viewModel.toggleService() },
                    onOpenSoundSandbox = { viewModel.setSandboxVisible(true) }
                )
            }

            // 3. Live Telemetry Strip
            item {
                Text(
                    text = "BIOMETRIC SENSORY STREAM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = BioCyan
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                TelemetryStrip(
                    telemetry = uiState.telemetry,
                    isServiceActive = uiState.isServiceRunning
                )
            }

            // 4. Modules Section Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AUTONOMOUS MODULES (8)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                            color = BioVioletLight
                        )
                    )

                    Text(
                        text = "${uiState.moduleSettings.values.count { it }}/8 Active",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BioTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // 5. 8 Sensory Module Cards
            items(ModuleType.values()) { module ->
                val isEnabled = uiState.moduleSettings[module] ?: module.defaultEnabled
                ModuleCard(
                    module = module,
                    isEnabled = isEnabled,
                    isServiceActive = uiState.isServiceRunning,
                    onToggle = { viewModel.toggleModule(module) },
                    onTestSound = { viewModel.testModuleSound(module) }
                )
            }

            // 6. Sensory Event Stream (Activity Log)
            item {
                ActivityLogSection(events = uiState.activityLog)
            }
        }
    }

    // Sound Sandbox Bottom Sheet
    if (uiState.isSoundSandboxVisible) {
        SoundSandboxSheet(
            onDismiss = { viewModel.setSandboxVisible(false) },
            onPlaySound = { category, filename ->
                viewModel.playSpecificSound(category, filename)
            }
        )
    }
}

@Composable
private fun DashboardHeader(isServiceRunning: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "LIVING PHONE",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = BioTextPrimary
                )
            )
            Text(
                text = "Autonomous Sensory Organism",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BioTextMuted,
                    fontSize = 12.sp
                )
            )
        }

        // Live Conscious Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isServiceRunning) Color(0x2200F5D4) else Color(0x2264748B),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isServiceRunning) BioCyan.copy(alpha = 0.6f) else Color(0x4464748B)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isServiceRunning) BioGreen else Color(0xFF94A3B8))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isServiceRunning) "CONSCIOUS" else "DORMANT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isServiceRunning) BioGreen else BioTextMuted,
                        letterSpacing = 0.8.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun HeroConsciousnessCard(
    isServiceRunning: Boolean,
    isPlayingAudio: Boolean,
    currentSoundName: String?,
    amplitude: Float,
    onToggleService: () -> Unit,
    onOpenSoundSandbox: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isServiceRunning) BioCyan.copy(alpha = 0.5f) else GlassCardBorder,
        label = "hero_border"
    )

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        isActive = isServiceRunning,
        activeBorderColor = borderColor,
        borderWidth = 1.2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isServiceRunning) Color(0x3300F5D4) else Color(0x22444455)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Core Engine",
                            tint = if (isServiceRunning) BioCyan else BioTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isServiceRunning) "Core Engine Active" else "Core Inactive",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BioTextPrimary
                            )
                        )
                        Text(
                            text = if (isServiceRunning) "Sensors and listeners active" else "Companion dormant in standby",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isServiceRunning) BioCyanMuted else BioTextMuted,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Master Toggle Switch
                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = { onToggleService() },
                    modifier = Modifier.testTag("master_service_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = BioCyan,
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF262626)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sound Wave Canvas
            SoundWaveVisualizer(
                isServiceActive = isServiceRunning,
                isPlayingAudio = isPlayingAudio,
                amplitude = amplitude,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Playback Status Indicator or Acoustic Sandbox Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPlayingAudio && currentSoundName != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x3300F5D4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BioCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Waves,
                                contentDescription = null,
                                tint = BioCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Echoing: $currentSoundName",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BioCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (isServiceRunning) "Listening to ambient stimuli..." else "Tap switch to awaken core",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BioTextMuted,
                            fontSize = 12.sp
                        )
                    )
                }

                FilledTonalButton(
                    onClick = onOpenSoundSandbox,
                    modifier = Modifier.testTag("btn_acoustic_sandbox"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0x2E9D4EDD),
                        contentColor = BioVioletLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Test Audio",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Audio Sandbox",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(
    module: ModuleType,
    isEnabled: Boolean,
    isServiceActive: Boolean,
    onToggle: () -> Unit,
    onTestSound: () -> Unit
) {
    val moduleIcon = getModuleIcon(module)
    val moduleColor = getModuleColor(module)

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        isActive = isEnabled && isServiceActive,
        activeBorderColor = moduleColor.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isEnabled) moduleColor.copy(alpha = 0.18f) else Color(0x1A444455)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = moduleIcon,
                            contentDescription = module.title,
                            tint = if (isEnabled) moduleColor else BioTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = module.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) BioTextPrimary else BioTextMuted
                            )
                        )
                        Text(
                            text = module.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isEnabled) moduleColor.copy(alpha = 0.9f) else BioTextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("toggle_${module.name.lowercase()}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = moduleColor,
                        uncheckedThumbColor = Color(0xFF64748B),
                        uncheckedTrackColor = Color(0xFF262626)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = module.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BioTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onTestSound,
                    modifier = Modifier.testTag("test_${module.name.lowercase()}"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = moduleColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        moduleColor.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Probe Sound",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Probe Reaction",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityLogSection(events: List<LivingEvent>) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = BioCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SENSORY TIMELINE",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BioTextPrimary,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Text(
                    text = "${events.size} Events",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BioTextMuted,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Awaiting sensory stimuli... Activate Core to record live events.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BioTextMuted,
                            fontSize = 12.sp
                        )
                    )
                }
            } else {
                val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                events.take(6).forEachIndexed { index, event ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            val modColor = getModuleColor(event.module)
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(modColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BioTextPrimary
                                        )
                                    )
                                    Text(
                                        text = timeFormat.format(Date(event.timestamp)),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = BioTextMuted,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                                Text(
                                    text = event.detail,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BioTextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                        if (index < 5 && index < events.lastIndex) {
                            HorizontalDivider(
                                color = Color(0x1A444455),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SoundSandboxSheet(
    onDismiss: () -> Unit,
    onPlaySound: (SoundCategory, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val soundItems = listOf(
        SoundCategory.BATTERY_100 to listOf("battery_100%_1.mp3", "battery_100%_2.mp3", "battery_100%_3.mp3"),
        SoundCategory.BATTERY_15 to listOf("battery_15%_1.mp3", "battery_15%_2.mp3", "battery_15%_3.mp3", "battery_15%_4.mp3"),
        SoundCategory.CHARGING_CONNECT to listOf(
            "charging_connect_1.mp3", "charging_connect_2.mp3", "charging_connect_3.mp3",
            "charging_connect_4.mp3", "charging_connect_5.mp3", "charging_connect_6.mp3", "charging_connect_7.mp3"
        ),
        SoundCategory.CHARGING_DISCONNECT to listOf(
            "charging_disconnect_1.mp3", "charging_disconnect_2.mp3", "charging_disconnect_3.mp3",
            "charging_disconnect_4.mp3", "charging_disconnect_5.mp3", "charging_disconnect_6.mp3", "charging_disconnect_7.mp3"
        ),
        SoundCategory.EXERCISE to listOf("exercise_1.mp3", "exercise_2.mp3", "exercise_3.mp3", "exercise_4.mp3", "exercise_5.mp3"),
        SoundCategory.GENERAL to listOf(
            "general_1.mp3", "general_2.mp3", "general_3.mp3", "general_4.mp3", "general_5.mp3",
            "general_6.mp3", "general_7.mp3", "general_8.mp3", "general_9.mp3", "general_10.mp3",
            "general_11.mp3", "general_12.mp3", "general_13.mp3"
        ),
        SoundCategory.MOTION to listOf("motion_1.mp3", "motion_2.mp3", "motion_3.mp3", "motion_4.mp3"),
        SoundCategory.POCKET_DARK to listOf("pocket_dark_1.mp3", "pocket_dark_2.mp3", "pocket_dark_3.mp3"),
        SoundCategory.POCKET_LIGHT to listOf("pocket_light_1.mp3", "pocket_light_2.mp3", "pocket_light_3.mp3"),
        SoundCategory.SLEEP_LATE_NIGHT to listOf("hour_12_midnight.mp3", "hour_01_am.mp3", "hour_02_am.mp3", "hour_03_am.mp3", "hour_04_am.mp3"),
        SoundCategory.SLEEP_REMINDER to listOf("sleep_sleep_reminder_1.mp3", "sleep_sleep_reminder_2.mp3", "sleep_sleep_reminder_3.mp3"),
        SoundCategory.USAGE to listOf("usage_1.mp3", "usage_2.mp3", "usage_3.mp3")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111114),
        contentColor = BioTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "ACOUSTIC SANDBOX",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = BioCyan
                )
            )
            Text(
                text = "Synthesized audio assets & synthesized assets across all modules.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BioTextMuted,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(soundItems) { (category, files) ->
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        borderWidth = 0.8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Path: ${category.folderName}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BioVioletLight
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                files.forEach { filename ->
                                    FilledTonalButton(
                                        onClick = { onPlaySound(category, filename) },
                                        modifier = Modifier.testTag("play_${filename.substringBefore('.')}"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0x3300F5D4),
                                            contentColor = BioCyan
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = filename.substringBefore('.'),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun getModuleIcon(module: ModuleType): ImageVector = when (module) {
    ModuleType.BATTERY -> Icons.Default.BatteryAlert
    ModuleType.CHARGING -> Icons.Default.Bolt
    ModuleType.EXERCISE -> Icons.Default.FitnessCenter
    ModuleType.GENERAL -> Icons.Default.Psychology
    ModuleType.MOTION -> Icons.Default.Vibration
    ModuleType.POCKET -> Icons.Default.Lightbulb
    ModuleType.SLEEP -> Icons.Default.Bedtime
    ModuleType.USAGE -> Icons.Default.HourglassBottom
}

private fun getModuleColor(module: ModuleType): Color = when (module) {
    ModuleType.BATTERY -> BioAmber
    ModuleType.CHARGING -> BioGreen
    ModuleType.EXERCISE -> BioCyan
    ModuleType.GENERAL -> BioVioletLight
    ModuleType.MOTION -> Color(0xFFFF5252)
    ModuleType.POCKET -> Color(0xFF48CAE4)
    ModuleType.SLEEP -> Color(0xFFB5179E)
    ModuleType.USAGE -> Color(0xFFFF9E00)
}
