package com.drishti360.app.domain.services

import com.drishti360.app.data.models.CCTVStatus
import com.drishti360.app.data.models.Evidence
import com.drishti360.app.data.models.Inspection
import com.drishti360.app.data.models.LocationData
import com.drishti360.app.data.models.NGO
import com.drishti360.app.data.models.RiskScore
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for AI Risk engine (Currently Mock, future XGBoost implementation)
 */
interface RiskEngine {
    suspend fun calculateRisk(ngoId: String): RiskScore?
    suspend fun getRiskTrends(): Map<String, List<Int>>
}

/**
 * Abstraction for CCTV & Edge AI video monitoring (Currently Mock, future RTSP/YOLO implementation)
 */
interface CCTVService {
    fun getLiveFeeds(ngoId: String? = null): Flow<List<CCTVStatus>>
    suspend fun getCameraStatus(cameraId: String): CCTVStatus
}

/**
 * Abstraction for Inspection management (Currently Mock, future WebRTC implementation)
 */
interface InspectionService {
    suspend fun initiateInspection(ngoId: String, ngoName: String, inspectorName: String): Inspection
    suspend fun updateInspectionReport(inspection: Inspection): Inspection
    suspend fun completeInspection(inspectionId: String): Inspection
    suspend fun getActiveInspections(): List<Inspection>
}

/**
 * Abstraction for GPS & Geofencing (Currently Mock, future FusedLocationProvider implementation)
 */
interface LocationService {
    suspend fun getCurrentLocation(): LocationData
    suspend fun verifyGeofence(ngoLocation: LocationData, inspectorLocation: LocationData): Pair<Int, Boolean>
}

/**
 * Abstraction for Evidence capture and sealing (Currently Mock, future S3/IPFS implementation)
 */
interface EvidenceService {
    suspend fun captureEvidence(type: String, title: String): Evidence
    suspend fun sealEvidenceRecord(inspectionId: String, evidenceList: List<Evidence>): String
}
