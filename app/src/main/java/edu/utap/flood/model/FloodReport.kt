package edu.utap.flood.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a flood report in Firestore.
 *
 * This class stores flood report information that will be synchronized with
 * the local database. It includes all necessary fields for flood reporting
 * and community interaction features.
 */
data class FloodReport(
    @DocumentId
    val reportId: String = "",
    val userId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String = "",
    val photoUrls: List<String> = emptyList(),
    val status: String = "pending",
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Timestamp = Timestamp.now(),
    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Timestamp = Timestamp.now(),
    @get:PropertyName("is_manual_location")
    @set:PropertyName("is_manual_location")
    var isManualLocation: Boolean = false,
    @get:PropertyName("confirmed_count")
    @set:PropertyName("confirmed_count")
    var confirmedCount: Int = 0,
    @get:PropertyName("denied_count")
    @set:PropertyName("denied_count")
    var deniedCount: Int = 0
)
