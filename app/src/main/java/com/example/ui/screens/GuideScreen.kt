package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.JoyStickController
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
fun GuideScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationState by JoyStickController.locationState.collectAsState()

    var isLocationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var isNotificationPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isLocationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isNotificationPermissionGranted = isGranted
    }

    val canDrawOverlays = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberNavyBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "SETUP & GAMING GUIDE",
            style = MaterialTheme.typography.displayMedium,
            color = CyberCyanPrimary,
            letterSpacing = 1.sp
        )
        Text(
            text = "Follow these essential steps to enable GPS spoofing and overlay controls for mobile gaming.",
            style = MaterialTheme.typography.bodyLarge,
            color = CyberTextSecondary
        )

        // 1. Checklist Cards
        Text(
            text = "PERMISSIONS & OS AUTHORIZATION",
            style = MaterialTheme.typography.titleMedium,
            color = CyberTextPrimary,
            fontWeight = FontWeight.Bold
        )

        // Mock Location App Card
        SetupItemCard(
            title = "1. Select Mock Location App",
            description = "Android requires granting mock location rights to GPS JoyStick in Developer Options.",
            isCompleted = locationState.isMockLocationAuthorized,
            actionLabel = "Open Dev Settings",
            onAction = {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    context.startActivity(intent)
                }
            }
        )

        // Display Over Other Apps Card
        SetupItemCard(
            title = "2. Display Over Other Apps (Overlay)",
            description = "Required to show the floating joystick controller over your mobile games.",
            isCompleted = canDrawOverlays,
            actionLabel = "Enable Overlay",
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            }
        )

        // Device Location Card
        SetupItemCard(
            title = "3. Location Services Permission",
            description = "Enables calculating realistic relative coordinates and initial location lock.",
            isCompleted = isLocationPermissionGranted,
            actionLabel = "Grant Location",
            onAction = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        )

        // Notifications Card
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SetupItemCard(
                title = "4. Background Service Notification",
                description = "Keeps the spoofing engine and joystick alive while switching between apps.",
                isCompleted = isNotificationPermissionGranted,
                actionLabel = "Allow Alerts",
                onAction = {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Anti-Cheat & Softban Cooldown Guide
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavySurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyberBorder, CyberIndigoSecondary))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyberCyanGlow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GAMING SAFETY & COOLDOWN TABLE",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "In mobile games (like Pokémon GO and Monster Hunter Now), teleporting instantly over large distances triggers automatic anti-cheat softbans unless you wait for the server cooldown window:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextSecondary
                )

                HorizontalDivider(color = CyberBorder, thickness = 1.dp)

                // Cooldown Table Rows
                CooldownRow("1 km", "30 seconds")
                CooldownRow("5 km", "2 minutes")
                CooldownRow("10 km", "7 minutes")
                CooldownRow("25 km", "15 minutes")
                CooldownRow("50 km", "22 minutes")
                CooldownRow("100 km", "35 minutes")
                CooldownRow("250 km", "45 minutes")
                CooldownRow("500 km", "65 minutes")
                CooldownRow("1,000 km+", "2 Hours (MAX)")

                HorizontalDivider(color = CyberBorder, thickness = 1.dp)

                Text(
                    text = "💡 Pro-Tip: Keep 'Anti-Cheat Jitter (±5%)' enabled in the speed settings so your movement speed matches natural human footsteps.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SetupItemCard(
    title: String,
    description: String,
    isCompleted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) CyberNavySurface else CyberNavySurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (isCompleted) listOf(CyberEmerald.copy(alpha = 0.5f), CyberBorder)
                else listOf(CyberAmber.copy(alpha = 0.6f), CyberBorder)
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isCompleted) CyberEmerald else CyberAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (!isCompleted) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyanPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(actionLabel, color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberEmerald.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("READY", style = MaterialTheme.typography.labelSmall, color = CyberEmerald, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CooldownRow(distance: String, waitTime: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(distance, style = MaterialTheme.typography.bodyMedium, color = CyberTextPrimary)
        Text(waitTime, style = MaterialTheme.typography.bodyMedium, color = CyberCyanGlow, fontWeight = FontWeight.Bold)
    }
}
