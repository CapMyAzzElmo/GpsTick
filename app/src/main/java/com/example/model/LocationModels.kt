package com.example.model

enum class SpeedUnit(val symbol: String, val multiplierFromKmh: Float) {
    KMH("km/h", 1.0f),
    MPH("mph", 0.621371f),
    MPS("m/s", 0.277778f)
}

enum class SpeedPreset(val label: String, val defaultSpeedKmh: Float) {
    WALK("Walk", 4.5f),
    JOG("Jog", 11.0f),
    BIKE("Bike", 22.0f),
    DRIVE("Drive", 55.0f)
}

data class SpeedConfig(
    val selectedPreset: SpeedPreset = SpeedPreset.WALK,
    val speedKmh: Float = 4.5f,
    val unit: SpeedUnit = SpeedUnit.KMH,
    val randomVariance: Boolean = true, // ±5% realistic human motion jitter to prevent anti-cheat triggers
    val lockWalk: Boolean = false       // Auto-walk continuous movement
) {
    fun displaySpeed(): String {
        val converted = speedKmh * unit.multiplierFromKmh
        return "%.1f %s".format(converted, unit.symbol)
    }
}

data class MockLocationState(
    val latitude: Double = 37.8087,     // Default: Pier 39, SF (famous gaming hotspot)
    val longitude: Double = -122.4098,
    val altitude: Double = 15.0,
    val bearing: Float = 0.0f,
    val speedKmh: Float = 0.0f,
    val accuracyMeters: Float = 3.0f,
    val isMockingActive: Boolean = false,
    val isPaused: Boolean = false,
    val isOverlayActive: Boolean = false,
    val lastError: String? = null,
    val isMockLocationAuthorized: Boolean = false
)

data class BookmarkLocation(
    val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val description: String = ""
)

data class RouteWaypoint(
    val latitude: Double,
    val longitude: Double,
    val name: String = ""
)
