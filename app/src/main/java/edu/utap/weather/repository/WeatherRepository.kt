package edu.utap.weather.repository

import edu.utap.weather.FloodAlert

/**
 * Interface defining weather-related data operations.
 *
 * Implementations fetch flood alerts from external sources like NOAA.
 */
interface WeatherRepository {
    /**
     * Fetches flood alerts for the specified latitude and longitude.
     *
     * @param lat Latitude coordinate.
     * @param lon Longitude coordinate.
     * @return List of [FloodAlert]s near the specified location.
     */
    suspend fun getFloodAlerts(lat: Double, lon: Double): List<FloodAlert>
}
