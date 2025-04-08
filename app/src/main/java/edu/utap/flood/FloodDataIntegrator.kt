package edu.utap.flood

import edu.utap.weather.FloodAlert
import edu.utap.flood.model.FloodReport
import kotlin.math.*

object FloodDataIntegrator {

    // Bexar County bounding box (approximate)
    private val minLat = 29.1
    private val maxLat = 29.7
    private val minLon = -98.8
    private val maxLon = -98.2

    // Bexar County polygon (simplified, replace with accurate polygon if available)
    private val bexarCountyPolygon = listOf(
        Pair(29.7, -98.8),
        Pair(29.7, -98.2),
        Pair(29.1, -98.2),
        Pair(29.1, -98.8)
    )

    fun integrateData(
        firestoreReports: List<FloodReport>,
        nwsAlerts: List<FloodAlert>
    ): List<UnifiedFloodReport> {
        val filteredFirestore = firestoreReports.filter { isInBexarCounty(it.latitude, it.longitude) }
        val filteredNws = nwsAlerts.filter { isInBexarCounty(it.latitude, it.longitude) }

        val convertedFirestore = filteredFirestore.map {
            UnifiedFloodReport(
                id = it.reportId,
                latitude = it.latitude,
                longitude = it.longitude,
                timestamp = it.createdAt.toDate().time,
                severity = it.severity,
                description = "[Internal] ${it.description}",
                sources = listOf("Internal")
            )
        }.toMutableList()

        val convertedNws = filteredNws.map {
            UnifiedFloodReport(
                id = it.id,
                latitude = it.latitude,
                longitude = it.longitude,
                timestamp = it.timestamp,
                severity = it.severity,
                description = "[NWS] ${it.description}",
                sources = listOf("NWS")
            )
        }.toMutableList()

        val merged = mutableListOf<UnifiedFloodReport>()
        val usedNws = mutableSetOf<String>()

        for (fsReport in convertedFirestore) {
            val overlapping = convertedNws.find { nwsReport ->
                nwsReport.id !in usedNws &&
                areClose(fsReport, nwsReport) &&
                areCloseInTime(fsReport, nwsReport)
            }
            if (overlapping != null) {
                usedNws.add(overlapping.id)
                merged.add(mergeReports(fsReport, overlapping))
            } else {
                merged.add(fsReport)
            }
        }

        for (nwsReport in convertedNws) {
            if (nwsReport.id !in usedNws) {
                merged.add(nwsReport)
            }
        }

        return merged
    }

    private fun isInBexarCounty(lat: Double, lon: Double): Boolean {
        if (lat !in minLat..maxLat || lon !in minLon..maxLon) return false
        return pointInPolygon(lat, lon, bexarCountyPolygon)
    }

    private fun pointInPolygon(lat: Double, lon: Double, polygon: List<Pair<Double, Double>>): Boolean {
        var intersectCount = 0
        for (j in polygon.indices) {
            val i = (j + 1) % polygon.size
            val (lat1, lon1) = polygon[j]
            val (lat2, lon2) = polygon[i]
            if (((lon1 > lon) != (lon2 > lon)) &&
                (lat < (lat2 - lat1) * (lon - lon1) / (lon2 - lon1) + lat1)
            ) {
                intersectCount++
            }
        }
        return (intersectCount % 2 == 1)
    }

    private fun areClose(r1: UnifiedFloodReport, r2: UnifiedFloodReport): Boolean {
        val distance = haversine(r1.latitude, r1.longitude, r2.latitude, r2.longitude)
        return distance <= 0.2 // 200 meters
    }

    private fun areCloseInTime(r1: UnifiedFloodReport, r2: UnifiedFloodReport): Boolean {
        val diffMillis = abs(r1.timestamp - r2.timestamp)
        return diffMillis <= 60 * 60 * 1000 // 1 hour
    }

    private fun mergeReports(r1: UnifiedFloodReport, r2: UnifiedFloodReport): UnifiedFloodReport {
        val avgLat = (r1.latitude + r2.latitude) / 2
        val avgLon = (r1.longitude + r2.longitude) / 2
        val earliestTime = min(r1.timestamp, r2.timestamp)
        val mergedDescription = "${r1.description}\n${r2.description}"
        val mergedSources = (r1.sources + r2.sources).distinct()
        val severity = if (r2.sources.contains("NWS")) r2.severity else r1.severity

        return UnifiedFloodReport(
            id = "${r1.id}_${r2.id}",
            latitude = avgLat,
            longitude = avgLon,
            timestamp = earliestTime,
            severity = severity,
            description = mergedDescription,
            sources = mergedSources
        )
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
