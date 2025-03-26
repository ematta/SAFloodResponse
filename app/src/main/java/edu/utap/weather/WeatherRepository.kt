package edu.utap.weather

interface WeatherRepository {
    suspend fun getFloodAlerts(lat: Double, lon: Double): List<FloodAlert>
}