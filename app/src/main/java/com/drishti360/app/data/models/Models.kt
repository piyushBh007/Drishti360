package com.drishti360.app.data.models

enum class RiskLevel {
    LOW, MEDIUM, HIGH
}

data class RiskFactor(
    val title: String,
    val scoreContribution: Int,
    val description: String
)

data class RiskScore(
    val score: Int,
    val level: RiskLevel,
    val factors: List<RiskFactor>,
    val trend: String = "STABLE"
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val state: String,
    val address: String,
    val geofenceRadiusM: Int = 100
)

data class NGO(
    val id: String,
    val name: String,
    val registrationNo: String = "RJ-2020-1142",
    val category: String = "Child Welfare",
    val establishedYear: Int = 2018,
    val beneficiaries: Int = 120,
    val description: String = "Works for child education and community development.",
    val image: String? = null,
    val images: List<String> = emptyList(),
    val status: String = "ACTIVE",
    val location: LocationData,
    val riskScore: RiskScore?,
    val cctvOnlineCount: Int?,
    val cctvTotalCount: Int?,
    val attendanceRate: Int?, // percentage
    val networkUptime: Int?, // percentage
    val lastInspectionDate: String?,
    val activeAlertsCount: Int?
)

enum class CameraState {
    ONLINE, OFFLINE, INSTABLE
}

data class CCTVStatus(
    val id: String,
    val ngoId: String,
    val ngoName: String,
    val cameraName: String, // e.g. CAM-001
    val state: CameraState,
    val peopleCount: Int = 0,
    val personCount: Int? = null,
    val averagePersonCount: Double? = null,
    val yoloTimestamp: String? = null,
    val hasYoloData: Boolean = false,
    val anomalyProbability: Int = 0,
    val lastFrameSecAgo: Int = 0,
    val streamUrl: String = "",
    val mediaRef: String? = null,
    val source: String = "LOCAL_VIDEO",
    val type: String = "LOCAL_DEMO",
    val protocol: String = "LOCAL_FILE"
)

enum class AlertSeverity {
    CRITICAL, WARNING, INFO
}

data class Alert(
    val id: String,
    val ngoId: String,
    val ngoName: String,
    val title: String,
    val timestamp: String,
    val severity: AlertSeverity,
    val message: String
)

enum class InspectionStatus {
    SCHEDULED, IN_PROGRESS, COMPLETED, SEALED
}

data class Evidence(
    val id: String,
    val title: String,
    val type: String, // "PHOTO", "VIDEO", "AUDIO", "METADATA"
    val timestamp: String,
    val hash: String, // Cryptographic SHA256 simulation
    val previewUrl: String = ""
)

data class Inspection(
    val id: String,
    val ngoId: String,
    val ngoName: String,
    val inspectorName: String,
    val timestamp: String,
    val status: InspectionStatus,
    val distanceMeters: Int,
    val withinGeofence: Boolean,
    val beneficiariesObserved: Int,
    val staffPresent: Int,
    val facilitiesStatus: String,
    val cctvCompliance: String,
    val observations: String,
    val evidenceList: List<Evidence> = emptyList()
)

data class AuditEvent(
    val id: String,
    val time: String,
    val title: String,
    val description: String,
    val actor: String,
    val iconType: String // "TRIGGER", "ASSIGN", "GPS", "VIDEO", "EVIDENCE", "SUBMIT", "SEAL"
)
