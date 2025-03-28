package edu.utap.weather.repository

import edu.utap.weather.FloodAlert

interface WeatherRepository {
    suspend fun getFloodAlerts(lat: Double, lon: Double): List<FloodAlert>
}
