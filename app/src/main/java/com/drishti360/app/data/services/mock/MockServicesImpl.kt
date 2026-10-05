package com.drishti360.app.data.services.mock

import com.drishti360.app.data.mock.MockData
import com.drishti360.app.data.models.CCTVStatus
import com.drishti360.app.data.models.Evidence
import com.drishti360.app.data.models.Inspection
import com.drishti360.app.data.models.InspectionStatus
import com.drishti360.app.data.models.LocationData
import com.drishti360.app.data.models.RiskScore
import com.drishti360.app.domain.services.CCTVService
import com.drishti360.app.domain.services.EvidenceService
import com.drishti360.app.domain.services.InspectionService
import com.drishti360.app.domain.services.LocationService
import com.drishti360.app.domain.services.RiskEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class MockRiskEngine : RiskEngine {
    override suspend fun calculateRisk(ngoId: String): RiskScore? {
        val ngo = MockData.sampleNgos.find { it.id.equals(ngoId, ignoreCase = true) }
        return ngo?.riskScore
    }

    override suspend fun getRiskTrends(): Map<String, List<Int>> {
        return mapOf(
            "Sunrise Welfare" to listOf(65, 68, 72, 75, 80, 87),
            "Asha Care" to listOf(70, 72, 75, 78, 79, 79),
            "Samarpan Trust" to listOf(55, 52, 48, 45, 43, 42)
        )
    }
}

class MockCCTVService : CCTVService {
    override fun getLiveFeeds(ngoId: String?): Flow<List<CCTVStatus>> = flow {
        val feeds = if (ngoId != null) {
            MockData.sampleCctvFeeds.filter { it.ngoId.equals(ngoId, ignoreCase = true) }
        } else {
            MockData.sampleCctvFeeds
        }
        emit(feeds)
        while (true) {
            delay(3000)
            // Simulate slight variation in camera count for dynamic feed UI
            val updated = feeds.map { cam ->
                if (cam.state == com.drishti360.app.data.models.CameraState.ONLINE) {
                    cam.copy(peopleCount = (cam.peopleCount + (-2..2).random()).coerceAtLeast(15))
                } else cam
            }
            emit(updated)
        }
    }

    override suspend fun getCameraStatus(cameraId: String): CCTVStatus {
        return MockData.sampleCctvFeeds.find { it.id.equals(cameraId, ignoreCase = true) } ?: MockData.sampleCctvFeeds.first()
    }
}

class MockInspectionService : InspectionService {
    private val activeInspections = mutableListOf<Inspection>()

    override suspend fun initiateInspection(ngoId: String, ngoName: String, inspectorName: String): Inspection {
        val inspection = Inspection(
            id = "INSP-" + System.currentTimeMillis().toString().takeLast(6),
            ngoId = ngoId,
            ngoName = ngoName,
            inspectorName = inspectorName,
            timestamp = "Just Now",
            status = InspectionStatus.IN_PROGRESS,
            distanceMeters = 31,
            withinGeofence = true,
            beneficiariesObserved = 24,
            staffPresent = 4,
            facilitiesStatus = "OPERATIONAL",
            cctvCompliance = "PARTIAL (2/3 Cameras Online)",
            observations = "Surprise inspection initiated via Drishti 360 Command Center. WebRTC stream active."
        )
        activeInspections.add(inspection)
        return inspection
    }

    override suspend fun updateInspectionReport(inspection: Inspection): Inspection {
        val idx = activeInspections.indexOfFirst { it.id == inspection.id }
        if (idx >= 0) {
            activeInspections[idx] = inspection
        } else {
            activeInspections.add(inspection)
        }
        return inspection
    }

    override suspend fun completeInspection(inspectionId: String): Inspection {
        val idx = activeInspections.indexOfFirst { it.id == inspectionId }
        val current = if (idx >= 0) activeInspections[idx] else {
            MockData.sampleNgos.first().let { ngo ->
                Inspection(
                    id = inspectionId,
                    ngoId = ngo.id,
                    ngoName = ngo.name,
                    inspectorName = "Inspector Vikram Singh",
                    timestamp = "14:36:11",
                    status = InspectionStatus.COMPLETED,
                    distanceMeters = 31,
                    withinGeofence = true,
                    beneficiariesObserved = 24,
                    staffPresent = 4,
                    facilitiesStatus = "OPERATIONAL",
                    cctvCompliance = "PARTIAL",
                    observations = "Biometric headcount verified against physical attendees."
                )
            }
        }
        val completed = current.copy(status = InspectionStatus.SEALED)
        if (idx >= 0) activeInspections[idx] = completed
        return completed
    }

    override suspend fun getActiveInspections(): List<Inspection> {
        return activeInspections
    }
}

class MockLocationService : LocationService {
    override suspend fun getCurrentLocation(): LocationData {
        return LocationData(
            latitude = 26.2391,
            longitude = 73.0245,
            city = "Jodhpur",
            state = "Rajasthan",
            address = "Sector 4, Shastri Nagar, Jodhpur"
        )
    }

    override suspend fun verifyGeofence(
        ngoLocation: LocationData,
        inspectorLocation: LocationData
    ): Pair<Int, Boolean> {
        // Simulated distance math (31 meters)
        return Pair(31, true)
    }
}

class MockEvidenceService : EvidenceService {
    override suspend fun captureEvidence(type: String, title: String): Evidence {
        val hash = "0x" + UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
        return Evidence(
            id = "EVI-" + System.currentTimeMillis().toString().takeLast(6),
            title = title,
            type = type,
            timestamp = "14:35:42",
            hash = hash
        )
    }

    override suspend fun sealEvidenceRecord(inspectionId: String, evidenceList: List<Evidence>): String {
        return "SHA256:0x8F4A992C71E04B3D8A291"
    }
}
