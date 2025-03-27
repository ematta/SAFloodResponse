package edu.utap.weather

class WeatherRepositoryImpl(private val noaaService: NOAAService) : WeatherRepository {
    override suspend fun getFloodAlerts(lat: Double, lon: Double): List<FloodAlert> =
        noaaService.getFloodAlerts(lat, lon)
}
