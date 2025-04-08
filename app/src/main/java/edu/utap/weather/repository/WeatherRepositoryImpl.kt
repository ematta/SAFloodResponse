package edu.utap.weather.repository

import edu.utap.weather.FloodAlert
import edu.utap.weather.NOAAService

/**
 * Implementation of [WeatherRepository] that fetches flood alerts from NOAA.
 *
 * @property noaaService The NOAA service used to fetch flood alerts.
 */
class WeatherRepositoryImpl(private val noaaService: NOAAService) : WeatherRepository {
    /**
     * Fetches flood alerts for the specified latitude and longitude.
     *
     * @param lat Latitude coordinate.
     * @param lon Longitude coordinate.
     * @return List of [FloodAlert]s near the specified location.
     */
    override suspend fun getFloodAlerts(lat: Double, lon: Double): List<FloodAlert> =
        noaaService.getFloodAlerts(lat, lon)
}
