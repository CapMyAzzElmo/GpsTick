package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpeedPreset
import com.example.model.SpeedUnit
import com.example.service.JoyStickController
import com.example.service.MockLocationService
import com.example.ui.components.ComposeJoyStick
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyanGlow
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberIndigoSecondary
import com.example.ui.theme.CyberNavyBg
import com.example.ui.theme.CyberNavySurface
import com.example.ui.theme.CyberNavySurfaceVariant
import com.example.ui.theme.CyberRose
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
fun ControllerScreen(
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationState by JoyStickController.locationState.collectAsState()
    val speedConfig by JoyStickController.speedConfig.collectAsState()

    val canDrawOverlays = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberNavyBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Status Bar Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mock Status Badge
            val statusColor by animateColorAsState(
                targetValue = when {
                    !locationState.isMockingActive -> CyberTextSecondary
                    locationState.isPaused -> CyberAmber
                    else -> CyberEmerald
                },
                label = "status_color"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberNavySurfaceVariant)
                    .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        !locationState.isMockingActive -> "MOCK: OFF"
                        locationState.isPaused -> "MOCK: PAUSED"
                        else -> "MOCK: ACTIVE"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            // Overlay Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberNavySurfaceVariant)
                    .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (locationState.isOverlayActive) Icons.Default.Layers else Icons.Default.LayersClear,
                    contentDescription = null,
                    tint = if (locationState.isOverlayActive) CyberCyanPrimary else CyberTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (locationState.isOverlayActive) "OVERLAY: ON" else "OVERLAY: OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (locationState.isOverlayActive) CyberCyanPrimary else CyberTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Mock permission warning banner if needed
        if (!locationState.isMockLocationAuthorized && locationState.lastError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberRose.copy(alpha = 0.15f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyberRose, CyberAmber))),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CyberRose,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Developer Setup Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberRose,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Set GPS JoyStick as mock location app in Developer Options.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberTextPrimary
                        )
                    }
                    Button(
                        onClick = onNavigateToSetup,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberRose),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("fix_setup_button")
                    ) {
                        Text("Setup", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 2. Telemetry Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavySurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyberBorder, CyberCyanPrimary.copy(alpha = 0.3f)))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS TELEMETRY",
                        style = MaterialTheme.typography.titleLarge,
                        color = CyberCyanPrimary,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(
                                "GPS Coordinates",
                                "%.6f, %.6f".format(locationState.latitude, locationState.longitude)
                            )
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Coordinates copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Coordinates",
                            tint = CyberTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Coordinates readout
                Text(
                    text = "%.6f,  %.6f".format(locationState.latitude, locationState.longitude),
                    style = MaterialTheme.typography.displayMedium,
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stats grid: Speed, Bearing, Altitude, Accuracy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Speed
                    Column {
                        Text("CURRENT SPEED", style = MaterialTheme.typography.labelSmall, color = CyberTextSecondary)
                        Text(
                            "%.1f km/h".format(locationState.speedKmh),
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberCyanGlow,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Bearing with rotating compass icon
                    Column {
                        Text("HEADING", style = MaterialTheme.typography.labelSmall, color = CyberTextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = CyberIndigoSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(locationState.bearing)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "%.0f°".format(locationState.bearing),
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Altitude
                    Column {
                        Text("ALTITUDE", style = MaterialTheme.typography.labelSmall, color = CyberTextSecondary)
                        Text(
                            "%.1f m".format(locationState.altitude),
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Accuracy
                    Column {
                        Text("ACCURACY", style = MaterialTheme.typography.labelSmall, color = CyberTextSecondary)
                        Text(
                            "±3.0 m",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Floating Overlay Launch Card (Addresses overlay button prompt requirement)
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavySurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyberCyanPrimary.copy(alpha = 0.5f), CyberIndigoSecondary))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = CyberCyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "FLOATING OVERLAY JOYSTICK",
                                style = MaterialTheme.typography.titleMedium,
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Float on top of mobile gaming apps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyberTextSecondary
                            )
                        }
                    }
                }

                if (!canDrawOverlays) {
                    Text(
                        text = "Permission 'Display over other apps' is needed for the floating joystick to appear on top of games.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberAmber
                    )
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberAmber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("grant_overlay_permission_button")
                    ) {
                        Text("Grant Overlay Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (locationState.isOverlayActive) {
                                    val intent = Intent(context, MockLocationService::class.java).apply {
                                        action = MockLocationService.ACTION_HIDE_OVERLAY
                                    }
                                    context.startService(intent)
                                } else {
                                    // Also start mocking if not active
                                    if (!locationState.isMockingActive) {
                                        JoyStickController.startMocking(context)
                                    }
                                    val intent = Intent(context, MockLocationService::class.java).apply {
                                        action = MockLocationService.ACTION_SHOW_OVERLAY
                                    }
                                    context.startService(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (locationState.isOverlayActive) CyberRose else CyberCyanPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("toggle_overlay_button")
                        ) {
                            Icon(
                                imageVector = if (locationState.isOverlayActive) Icons.Default.LayersClear else Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (locationState.isOverlayActive) Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (locationState.isOverlayActive) "Hide Floating Overlay" else "Launch Floating Overlay",
                                color = if (locationState.isOverlayActive) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Configurable Movement Speeds (Addresses speed prompt requirement)
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavySurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyberBorder, CyberBorder))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = CyberCyanPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MOVEMENT SPEED",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Unit selector pills
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SpeedUnit.values().forEach { unit ->
                            FilterChip(
                                selected = speedConfig.unit == unit,
                                onClick = { JoyStickController.setSpeedUnit(unit) },
                                label = { Text(unit.symbol, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyanPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                // Preset Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpeedPreset.values().forEach { preset ->
                        val isSelected = speedConfig.selectedPreset == preset
                        Button(
                            onClick = { JoyStickController.setSpeedPreset(preset) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) CyberCyanPrimary else CyberNavySurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("preset_${preset.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = when (preset) {
                                    SpeedPreset.WALK -> Icons.Default.DirectionsWalk
                                    SpeedPreset.JOG -> Icons.Default.DirectionsRun
                                    SpeedPreset.BIKE -> Icons.Default.DirectionsBike
                                    SpeedPreset.DRIVE -> Icons.Default.DirectionsCar
                                },
                                contentDescription = null,
                                tint = if (isSelected) Color.Black else CyberTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.Black else CyberTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Granular Speed Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fine Tune: ${speedConfig.displaySpeed()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberTextPrimary
                        )
                        Text(
                            text = "(0.5 - 80 km/h)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberTextSecondary
                        )
                    }

                    Slider(
                        value = speedConfig.speedKmh,
                        onValueChange = { JoyStickController.setCustomSpeedKmh(it) },
                        valueRange = 0.5f..80.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyanGlow,
                            activeTrackColor = CyberCyanPrimary,
                            inactiveTrackColor = CyberBorder
                        ),
                        modifier = Modifier.testTag("speed_slider")
                    )
                }

                // Realistic Anti-Cheat Human Speed Variance Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Anti-Cheat Jitter (±5%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Simulates natural human walking speed variations",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberTextSecondary
                        )
                    }
                    Switch(
                        checked = speedConfig.randomVariance,
                        onCheckedChange = { JoyStickController.toggleRandomVariance(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyberCyanPrimary
                        )
                    )
                }

                // Lock-Walk Continuous Direction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Walk (Lock Direction)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Keeps traveling in current heading without holding screen",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberTextSecondary
                        )
                    }
                    Switch(
                        checked = speedConfig.lockWalk,
                        onCheckedChange = { JoyStickController.toggleLockWalk() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyberEmerald
                        )
                    )
                }
            }
        }

        // 5. In-App Analog Joystick Pad
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavySurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyberBorder, CyberBorder))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VIRTUAL JOYSTICK CONTROLLER",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Drag knob to walk • Push further to sprint",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                ComposeJoyStick(
                    size = 210.dp,
                    onDeflectionChanged = { dx, dy ->
                        JoyStickController.updateJoystick(dx, dy)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Direction Indicator
                Text(
                    text = if (speedConfig.lockWalk) "AUTO-WALK ACTIVE (${"%.0f°".format(locationState.bearing)})"
                    else "RELEASE TO STOP",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (speedConfig.lockWalk) CyberEmerald else CyberTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 6. Master Control Actions (Start/Stop/Pause)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!locationState.isMockingActive) {
                Button(
                    onClick = { JoyStickController.startMocking(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_mocking_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START SPOOFING GPS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black
                    )
                }
            } else {
                Button(
                    onClick = { JoyStickController.togglePause() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (locationState.isPaused) CyberEmerald else CyberAmber
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("pause_mocking_button")
                ) {
                    Icon(
                        imageVector = if (locationState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (locationState.isPaused) "RESUME" else "PAUSE",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black
                    )
                }

                Button(
                    onClick = { JoyStickController.stopMocking(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRose),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("stop_mocking_button")
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STOP",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}
