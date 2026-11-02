package com.example.ui.screens

import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.BookmarkLocation
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
fun TeleportScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationState by JoyStickController.locationState.collectAsState()
    val bookmarks by JoyStickController.bookmarks.collectAsState()

    var latInput by remember { mutableStateOf("") }
    var lngInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    var showBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkNameInput by remember { mutableStateOf("") }
    var bookmarkCategoryInput by remember { mutableStateOf("Farming Hub") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberNavyBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Direct Teleport Input Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavySurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyberBorder, CyberCyanPrimary.copy(alpha = 0.4f)))),
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
                        Icon(imageVector = Icons.Default.Explore, contentDescription = null, tint = CyberCyanPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INSTANT TELEPORT",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = {
                            latInput = "%.6f".format(locationState.latitude)
                            lngInput = "%.6f".format(locationState.longitude)
                        }
                    ) {
                        Text("Paste Current", color = CyberCyanPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it },
                        label = { Text("Latitude (-90 to 90)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyanPrimary,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("latitude_input")
                    )

                    OutlinedTextField(
                        value = lngInput,
                        onValueChange = { lngInput = it },
                        label = { Text("Longitude (-180 to 180)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyanPrimary,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("longitude_input")
                    )
                }

                // Cooldown Warning preview if both fields filled
                val targetLat = latInput.toDoubleOrNull()
                val targetLng = lngInput.toDoubleOrNull()
                if (targetLat != null && targetLng != null) {
                    val distArray = FloatArray(1)
                    Location.distanceBetween(
                        locationState.latitude, locationState.longitude,
                        targetLat, targetLng,
                        distArray
                    )
                    val distKm = distArray[0] / 1000f
                    val recommendedWaitMin = calculateCooldownMinutes(distKm)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberNavySurfaceVariant)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Distance: %.1f km  •  Safety Cooldown: ~%s".format(
                                distKm,
                                if (recommendedWaitMin == 0) "Immediate" else "$recommendedWaitMin mins"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (distKm > 20) CyberAmber else CyberEmerald
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val lat = latInput.toDoubleOrNull()
                            val lng = lngInput.toDoubleOrNull()
                            if (lat != null && lng != null) {
                                JoyStickController.teleportTo(lat, lng)
                                Toast.makeText(context, "Teleported to: $lat, $lng", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter valid decimal coordinates", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyanPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("teleport_button")
                    ) {
                        Icon(imageVector = Icons.Default.NearMe, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Teleport", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (targetLat != null && targetLng != null) {
                                bookmarkNameInput = "Spot (${"%.4f".format(targetLat)}, ${"%.4f".format(targetLng)})"
                                showBookmarkDialog = true
                            } else {
                                bookmarkNameInput = "Spot (${"%.4f".format(locationState.latitude)}, ${"%.4f".format(locationState.longitude)})"
                                showBookmarkDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNavySurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(46.dp).testTag("save_bookmark_button")
                    ) {
                        Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = CyberCyanGlow)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", color = CyberTextPrimary)
                    }
                }
            }
        }

        // Tabs: [Gaming Hotspots] vs [My Bookmarks]
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CyberNavySurface,
            contentColor = CyberCyanPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberCyanPrimary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Gaming Hotspots", style = MaterialTheme.typography.titleMedium) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Saved Bookmarks (${bookmarks.size})", style = MaterialTheme.typography.titleMedium) }
            )
        }

        // List of Locations
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(bookmarks) { item ->
                LocationItemCard(
                    item = item,
                    currentLat = locationState.latitude,
                    currentLng = locationState.longitude,
                    onTeleport = {
                        JoyStickController.teleportTo(item.latitude, item.longitude, item.name)
                        Toast.makeText(context, "Teleported to ${item.name}!", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        JoyStickController.deleteBookmark(item.id)
                        Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Add Bookmark Dialog
    if (showBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            containerColor = CyberNavySurface,
            title = {
                Text("Save Location Bookmark", style = MaterialTheme.typography.titleLarge, color = CyberCyanPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bookmarkNameInput,
                        onValueChange = { bookmarkNameInput = it },
                        label = { Text("Bookmark Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyanPrimary,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bookmarkCategoryInput,
                        onValueChange = { bookmarkCategoryInput = it },
                        label = { Text("Category / Tag (e.g. Raids, Lures)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyanPrimary,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = latInput.toDoubleOrNull() ?: locationState.latitude
                        val lng = lngInput.toDoubleOrNull() ?: locationState.longitude
                        JoyStickController.addBookmark(
                            name = bookmarkNameInput,
                            category = bookmarkCategoryInput,
                            lat = lat,
                            lng = lng
                        )
                        showBookmarkDialog = false
                        Toast.makeText(context, "Bookmark saved!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyanPrimary)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookmarkDialog = false }) {
                    Text("Cancel", color = CyberTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun LocationItemCard(
    item: BookmarkLocation,
    currentLat: Double,
    currentLng: Double,
    onTeleport: () -> Unit,
    onDelete: () -> Unit
) {
    val distArray = remember(currentLat, currentLng, item.latitude, item.longitude) {
        val result = FloatArray(1)
        Location.distanceBetween(currentLat, currentLng, item.latitude, item.longitude, result)
        result[0] / 1000f
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberNavySurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyberBorder, CyberBorder))),
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
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberNavySurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.category, style = MaterialTheme.typography.labelSmall, color = CyberCyanPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "%.4f, %.4f".format(item.latitude, item.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberTextSecondary
                    )
                }

                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Distance: %.1f km".format(distArray),
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberIndigoSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onTeleport,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyanPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("teleport_to_${item.id}")
                ) {
                    Text("Go", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                // If custom bookmark, allow delete
                if (item.id.startsWith("bm_")) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CyberRose)
                    }
                }
            }
        }
    }
}

private fun calculateCooldownMinutes(distanceKm: Float): Int {
    return when {
        distanceKm < 1f -> 0
        distanceKm < 5f -> 2
        distanceKm < 10f -> 7
        distanceKm < 25f -> 15
        distanceKm < 50f -> 22
        distanceKm < 100f -> 35
        distanceKm < 250f -> 45
        distanceKm < 500f -> 65
        distanceKm < 1000f -> 90
        else -> 120 // Max softban safety window is 2 hours
    }
}
