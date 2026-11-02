package com.example.data

import com.example.model.BookmarkLocation

object PresetLocations {
    val HOTSPOTS = listOf(
        BookmarkLocation(
            id = "pier_39",
            name = "Pier 39 (San Francisco)",
            category = "Waterfront / Spawns",
            latitude = 37.8087,
            longitude = -122.4098,
            description = "High density Pokéstops, raids, and water spawns"
        ),
        BookmarkLocation(
            id = "central_park",
            name = "Central Park (New York)",
            category = "Park / Lures",
            latitude = 40.7812,
            longitude = -73.9665,
            description = "The classic legendary farming route and lure hub"
        ),
        BookmarkLocation(
            id = "akihabara",
            name = "Akihabara (Tokyo)",
            category = "Urban / Raids",
            latitude = 35.6997,
            longitude = 139.7714,
            description = "Electric town with non-stop raid lobbies"
        ),
        BookmarkLocation(
            id = "zaragoza",
            name = "Parque Grande (Zaragoza)",
            category = "Cluster / Lures",
            latitude = 41.6328,
            longitude = -0.8996,
            description = "World famous clustered lured stop hotspot"
        ),
        BookmarkLocation(
            id = "santa_monica",
            name = "Santa Monica Pier (Los Angeles)",
            category = "Pier / Farming",
            latitude = 34.0101,
            longitude = -118.4962,
            description = "Crowded pier with rare coastal biome encounters"
        ),
        BookmarkLocation(
            id = "shibuya",
            name = "Shibuya Crossing (Tokyo)",
            category = "Urban Hub",
            latitude = 35.6595,
            longitude = 139.7005,
            description = "Packed intersection with endless gyms and points"
        ),
        BookmarkLocation(
            id = "sydney_quay",
            name = "Circular Quay (Sydney)",
            category = "Harbor / Landmarks",
            latitude = -33.8617,
            longitude = 151.2108,
            description = "Harbor waterside walks with regional spawns"
        ),
        BookmarkLocation(
            id = "london_eye",
            name = "London Eye & Hyde Park",
            category = "Historic / Park",
            latitude = 51.5033,
            longitude = -0.1195,
            description = "River Thames walking route with dense gyms"
        )
    )
}
