package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SensorTelemetry
import com.example.ui.theme.BioAmber
import com.example.ui.theme.BioCyan
import com.example.ui.theme.BioGreen
import com.example.ui.theme.BioTextMuted
import com.example.ui.theme.BioTextPrimary
import com.example.ui.theme.BioVioletLight

@Composable
fun TelemetryStrip(
    telemetry: SensorTelemetry,
    isServiceActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ambient Light Card
            TelemetryItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Lightbulb,
                label = "Ambient Light",
                value = if (isServiceActive) "${telemetry.lux.toInt()} lx" else "Standby",
                badge = if (telemetry.isPocketDark && isServiceActive) "In Pocket" else "Exposed",
                badgeColor = if (telemetry.isPocketDark) BioVioletLight else BioCyan
            )

            // Kinetic Acceleration Card
            TelemetryItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Vibration,
                label = "Kinetic Delta",
                value = if (isServiceActive) String.format("%.1f m/s²", telemetry.accelerationDelta) else "Standby",
                badge = if (telemetry.accelerationDelta > 10f) "Movement" else "Still",
                badgeColor = if (telemetry.accelerationDelta > 10f) BioAmber else BioGreen
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Power Metabolism Card
            TelemetryItem(
                modifier = Modifier.weight(1f),
                icon = if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                label = "Power Core",
                value = "${telemetry.batteryLevel}%",
                badge = if (telemetry.isCharging) "Infusing" else "Discharging",
                badgeColor = if (telemetry.isCharging) BioGreen else BioCyan
            )

            // Neural Focus Session Card
            val mins = telemetry.screenSessionSeconds / 60
            val secs = telemetry.screenSessionSeconds % 60
            TelemetryItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timer,
                label = "Screen Session",
                value = if (isServiceActive) String.format("%02d:%02d", mins, secs) else "00:00",
                badge = if (telemetry.screenSessionSeconds > 1200) "Fatigued" else "Focused",
                badgeColor = if (telemetry.screenSessionSeconds > 1200) BioAmber else BioCyan
            )
        }
    }
}

@Composable
private fun TelemetryItem(
    icon: ImageVector,
    label: String,
    value: String,
    badge: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        borderWidth = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = badgeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = BioTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    color = BioTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}
