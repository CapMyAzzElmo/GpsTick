package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.service.JoyStickController
import com.example.ui.screens.ControllerScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.TeleportScreen
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyanPrimary
import com.example.ui.theme.CyberNavyBg
import com.example.ui.theme.CyberNavySurface
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize JoyStickController with application context
        JoyStickController.initialize(this)

        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    var selectedScreen by rememberSaveable { mutableIntStateOf(0) }

    // If on a secondary tab, pressing system back returns to the primary Controller tab
    BackHandler(enabled = selectedScreen != 0) {
        selectedScreen = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = CyberNavySurface,
                contentColor = CyberCyanPrimary
            ) {
                NavigationBarItem(
                    selected = selectedScreen == 0,
                    onClick = { selectedScreen = 0 },
                    icon = { Icon(Icons.Default.SportsEsports, contentDescription = "Controller") },
                    label = { Text("Joystick") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyberCyanPrimary,
                        indicatorColor = CyberCyanPrimary,
                        unselectedIconColor = CyberTextSecondary,
                        unselectedTextColor = CyberTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_joystick")
                )

                NavigationBarItem(
                    selected = selectedScreen == 1,
                    onClick = { selectedScreen = 1 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Teleport") },
                    label = { Text("Teleport") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyberCyanPrimary,
                        indicatorColor = CyberCyanPrimary,
                        unselectedIconColor = CyberTextSecondary,
                        unselectedTextColor = CyberTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_teleport")
                )

                NavigationBarItem(
                    selected = selectedScreen == 2,
                    onClick = { selectedScreen = 2 },
                    icon = { Icon(Icons.Default.HelpOutline, contentDescription = "Guide & Setup") },
                    label = { Text("Guide") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyberCyanPrimary,
                        indicatorColor = CyberCyanPrimary,
                        unselectedIconColor = CyberTextSecondary,
                        unselectedTextColor = CyberTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_guide")
                )
            }
        }
    ) { innerPadding ->
        when (selectedScreen) {
            0 -> ControllerScreen(
                onNavigateToSetup = { selectedScreen = 2 },
                modifier = Modifier.padding(innerPadding)
            )
            1 -> TeleportScreen(
                modifier = Modifier.padding(innerPadding)
            )
            2 -> GuideScreen(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
