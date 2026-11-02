package com.example.service

import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.data.PresetLocations
import com.example.model.BookmarkLocation
import com.example.model.MockLocationState
import com.example.model.RouteWaypoint
import com.example.model.SpeedConfig
import com.example.model.SpeedPreset
import com.example.model.SpeedUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object JoyStickController {
    private const val TAG = "JoyStickController"
    private const val TICK_INTERVAL_MS = 100L // 10 updates per second for smooth movement
    private const val EARTH_RADIUS = 6378137.0 // in meters

    private val scope = CoroutineScope(Dispatchers.Default)
    private var simulationJob: Job? = null

    private val _locationState = MutableStateFlow(MockLocationState())
    val locationState: StateFlow<MockLocationState> = _locationState.asStateFlow()

    private val _speedConfig = MutableStateFlow(SpeedConfig())
    val speedConfig: StateFlow<SpeedConfig> = _speedConfig.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkLocation>>(PresetLocations.HOTSPOTS)
    val bookmarks: StateFlow<List<BookmarkLocation>> = _bookmarks.asStateFlow()

    private val _waypoints = MutableStateFlow<List<RouteWaypoint>>(emptyList())
    val waypoints: StateFlow<List<RouteWaypoint>> = _waypoints.asStateFlow()

    private val _isRouteRunning = MutableStateFlow(false)
    val isRouteRunning: StateFlow<Boolean> = _isRouteRunning.asStateFlow()

    // Joystick deflection: dx in [-1, 1], dy in [-1, 1] (screen coordinate: dy > 0 is down, dy < 0 is up/north)
    private var deflectionX = 0f
    private var deflectionY = 0f

    // Locked direction for auto-walk
    private var lockedBearing: Float? = null

    private var locationManager: LocationManager? = null

    fun initialize(context: Context) {
        if (locationManager == null) {
            locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        }
        checkMockPermission(context)
    }

    fun checkMockPermission(context: Context): Boolean {
        val lm = locationManager ?: (context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
        if (lm == null) return false
        return try {
            lm.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false, false, false, false, true, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE
            )
            lm.removeTestProvider(LocationManager.GPS_PROVIDER)
            _locationState.update { it.copy(isMockLocationAuthorized = true, lastError = null) }
            true
        } catch (e: SecurityException) {
            _locationState.update {
                it.copy(
                    isMockLocationAuthorized = false,
                    lastError = "Mock location not set in Developer Options. Please select GPS JoyStick under 'Select mock location app'."
                )
            }
            false
        } catch (e: Exception) {
            // Already added or another transient issue
            _locationState.update { it.copy(isMockLocationAuthorized = true) }
            true
        }
    }

    fun startMocking(context: Context) {
        initialize(context)
        val lm = locationManager ?: return

        try {
            setupTestProviders(lm)
            _locationState.update { it.copy(isMockingActive = true, isPaused = false, lastError = null) }

            // Start foreground service to keep spoofing active in background
            val serviceIntent = Intent(context, MockLocationService::class.java).apply {
                action = MockLocationService.ACTION_START_MOCK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            startSimulationLoop()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting mock: ${e.message}")
            _locationState.update {
                it.copy(
                    isMockingActive = false,
                    isMockLocationAuthorized = false,
                    lastError = "Mock location permission denied. Enable in Developer Options -> Select mock location app."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting mock: ${e.message}")
            _locationState.update {
                it.copy(isMockingActive = false, lastError = e.localizedMessage ?: "Unknown error starting mock location")
            }
        }
    }

    fun stopMocking(context: Context) {
        simulationJob?.cancel()
        simulationJob = null

        val lm = locationManager
        if (lm != null) {
            removeTestProviders(lm)
        }

        _locationState.update {
            it.copy(isMockingActive = false, isPaused = false, speedKmh = 0f)
        }

        val serviceIntent = Intent(context, MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_STOP_MOCK
        }
        context.startService(serviceIntent)
    }

    fun togglePause() {
        _locationState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun updateJoystick(dx: Float, dy: Float) {
        deflectionX = dx.coerceIn(-1f, 1f)
        deflectionY = dy.coerceIn(-1f, 1f)
    }

    fun setSpeedPreset(preset: SpeedPreset) {
        _speedConfig.update { it.copy(selectedPreset = preset, speedKmh = preset.defaultSpeedKmh) }
    }

    fun setCustomSpeedKmh(speedKmh: Float) {
        _speedConfig.update { it.copy(speedKmh = speedKmh.coerceIn(0.5f, 150f)) }
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        _speedConfig.update { it.copy(unit = unit) }
    }

    fun toggleRandomVariance(enabled: Boolean) {
        _speedConfig.update { it.copy(randomVariance = enabled) }
    }

    fun toggleLockWalk() {
        _speedConfig.update {
            val next = !it.lockWalk
            if (next) {
                // Lock current bearing or default to North
                lockedBearing = _locationState.value.bearing
            } else {
                lockedBearing = null
            }
            it.copy(lockWalk = next)
        }
    }

    fun teleportTo(lat: Double, lng: Double, name: String? = null) {
        _locationState.update {
            it.copy(
                latitude = lat.coerceIn(-85.0, 85.0),
                longitude = lng.coerceIn(-180.0, 180.0),
                lastError = null
            )
        }
        // Immediately push one location update if mocking
        if (_locationState.value.isMockingActive && !_locationState.value.isPaused) {
            publishLocationToSystem(_locationState.value.latitude, _locationState.value.longitude, 0f, 0f)
        }
    }

    fun addBookmark(name: String, category: String, lat: Double, lng: Double, description: String = "") {
        val newBookmark = BookmarkLocation(
            id = "bm_${System.currentTimeMillis()}",
            name = name.ifBlank { "Location (${"%.4f".format(lat)}, ${"%.4f".format(lng)})" },
            category = category.ifBlank { "Custom" },
            latitude = lat,
            longitude = lng,
            description = description
        )
        _bookmarks.update { listOf(newBookmark) + it }
    }

    fun deleteBookmark(id: String) {
        _bookmarks.update { it.filterNot { bm -> bm.id == id } }
    }

    fun setOverlayActive(active: Boolean) {
        _locationState.update { it.copy(isOverlayActive = active) }
    }

    private fun setupTestProviders(lm: LocationManager) {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                lm.addTestProvider(
                    provider,
                    false, false, false, false, true, true, true,
                    Criteria.POWER_LOW, Criteria.ACCURACY_FINE
                )
            } catch (e: Exception) {
                // Ignore if already added
            }
            try {
                lm.setTestProviderEnabled(provider, true)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enable test provider $provider: ${e.message}")
            }
        }
    }

    private fun removeTestProviders(lm: LocationManager) {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                lm.setTestProviderEnabled(provider, false)
                lm.removeTestProvider(provider)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove test provider $provider: ${e.message}")
            }
        }
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            val dt = TICK_INTERVAL_MS / 1000.0 // seconds

            while (isActive) {
                val state = _locationState.value
                val config = _speedConfig.value

                if (state.isMockingActive && !state.isPaused) {
                    val magnitude = sqrt((deflectionX * deflectionX + deflectionY * deflectionY).toDouble()).toFloat()
                    val isDeflected = magnitude > 0.05f

                    if (isDeflected || config.lockWalk) {
                        // Calculate bearing in degrees:
                        // dy < 0 is North (0 deg), dx > 0 is East (90 deg), dy > 0 is South (180 deg), dx < 0 is West (270 deg)
                        val bearingDeg = if (isDeflected) {
                            var b = Math.toDegrees(atan2(deflectionX.toDouble(), -deflectionY.toDouble())).toFloat()
                            if (b < 0) b += 360f
                            lockedBearing = b
                            b
                        } else {
                            lockedBearing ?: state.bearing
                        }

                        // Determine effective speed
                        val speedMult = if (isDeflected) magnitude.coerceAtMost(1f) else 1f
                        var baseSpeed = config.speedKmh * speedMult

                        // Add realistic human walking speed jitter if enabled
                        if (config.randomVariance && baseSpeed > 0f) {
                            val jitterPercent = (Math.random() - 0.5) * 0.1 // ±5%
                            baseSpeed = (baseSpeed * (1.0 + jitterPercent)).toFloat().coerceAtLeast(0.1f)
                        }

                        val speedMps = baseSpeed * (1000.0 / 3600.0)
                        val distanceMeters = speedMps * dt

                        // Geodesic forward update
                        val bearingRad = Math.toRadians(bearingDeg.toDouble())
                        val currentLatRad = Math.toRadians(state.latitude)

                        val deltaLat = (distanceMeters * cos(bearingRad)) / EARTH_RADIUS
                        val deltaLng = (distanceMeters * sin(bearingRad)) / (EARTH_RADIUS * cos(currentLatRad))

                        val newLat = (state.latitude + Math.toDegrees(deltaLat)).coerceIn(-85.0, 85.0)
                        val newLng = (state.longitude + Math.toDegrees(deltaLng)).coerceIn(-180.0, 180.0)

                        // Slight altitude variation (±0.05m) for realism
                        val altJitter = if (config.randomVariance) (Math.random() - 0.5) * 0.1 else 0.0
                        val newAlt = (state.altitude + altJitter).coerceIn(0.0, 500.0)

                        _locationState.update {
                            it.copy(
                                latitude = newLat,
                                longitude = newLng,
                                altitude = newAlt,
                                bearing = bearingDeg,
                                speedKmh = baseSpeed
                            )
                        }

                        publishLocationToSystem(newLat, newLng, speedMps.toFloat(), bearingDeg)
                    } else {
                        // Standing still: publish current static location to keep GPS lock alive
                        _locationState.update { it.copy(speedKmh = 0f) }
                        publishLocationToSystem(state.latitude, state.longitude, 0f, state.bearing)
                    }
                }

                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private fun publishLocationToSystem(lat: Double, lng: Double, speedMps: Float, bearing: Float) {
        val lm = locationManager ?: return
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

        val now = System.currentTimeMillis()
        val elapsedNanos = SystemClock.elapsedRealtimeNanos()

        for (provider in providers) {
            try {
                val loc = Location(provider).apply {
                    latitude = lat
                    longitude = lng
                    altitude = _locationState.value.altitude
                    time = now
                    elapsedRealtimeNanos = elapsedNanos
                    accuracy = 3.0f
                    speed = speedMps
                    this.bearing = bearing
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        bearingAccuracyDegrees = 1.0f
                        speedAccuracyMetersPerSecond = 0.2f
                        verticalAccuracyMeters = 1.0f
                    }
                }
                lm.setTestProviderLocation(provider, loc)
            } catch (e: Exception) {
                // If permission was revoked or app not mock
                Log.w(TAG, "Failed to publish test location: ${e.message}")
            }
        }
    }
}
