package edu.utap.weather.repository

import edu.utap.weather.FloodAlert
import edu.utap.weather.NOAAService

class WeatherRepositoryImpl(private val noaaService: NOAAService) : WeatherRepository {
    override suspend fun getFloodAlerts(lat: Double, lon: Double): List<FloodAlert> =
        noaaService.getFloodAlerts(lat, lon)
}
