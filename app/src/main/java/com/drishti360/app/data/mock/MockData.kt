package com.drishti360.app.data.mock

import com.drishti360.app.data.models.Alert
import com.drishti360.app.data.models.AlertSeverity
import com.drishti360.app.data.models.AuditEvent
import com.drishti360.app.data.models.CCTVStatus
import com.drishti360.app.data.models.CameraState
import com.drishti360.app.data.models.Evidence
import com.drishti360.app.data.models.Inspection
import com.drishti360.app.data.models.InspectionStatus
import com.drishti360.app.data.models.LocationData
import com.drishti360.app.data.models.NGO
import com.drishti360.app.data.models.RiskFactor
import com.drishti360.app.data.models.RiskLevel
import com.drishti360.app.data.models.RiskScore

object MockData {

    val heroNgoId = "ngo-001"

    val sampleNgos = listOf(
        NGO(
            id = heroNgoId,
            name = "SUNRISE WELFARE FOUNDATION",
            location = LocationData(
                latitude = 26.2389,
                longitude = 73.0243,
                city = "Jodhpur",
                state = "Rajasthan",
                address = "Sector 4, Shastri Nagar, Jodhpur"
            ),
            riskScore = RiskScore(
                score = 87,
                level = RiskLevel.HIGH,
                factors = listOf(
                    RiskFactor("CCTV instability", 22, "Repeated stream disconnects during operational hours"),
                    RiskFactor("Attendance anomaly", 18, "Discrepancy between biometric log and physical count"),
                    RiskFactor("Network interruptions", 17, "Router power toggled 14 times in 24 hours"),
                    RiskFactor("Previous inspection", 15, "Minor non-compliance observed in Q2 audit"),
                    RiskFactor("Other indicators", 15, "Delayed report filings & geo-mismatch alerts")
                ),
                trend = "INCREASING (+12%)"
            ),
            cctvOnlineCount = 2,
            cctvTotalCount = 3,
            attendanceRate = 62,
            networkUptime = 71,
            lastInspectionDate = "2026-08-14",
            activeAlertsCount = 3
        ),
        NGO(
            id = "ngo-002",
            name = "ASHA CHILD CARE CENTRE",
            location = LocationData(
                latitude = 26.9124,
                longitude = 75.7873,
                city = "Jaipur",
                state = "Rajasthan",
                address = "Plot 12, Malviya Nagar, Jaipur"
            ),
            riskScore = RiskScore(
                score = 79,
                level = RiskLevel.HIGH,
                factors = listOf(
                    RiskFactor("Attendance anomaly", 30, "Low beneficiary presence during meal distribution"),
                    RiskFactor("CCTV instability", 25, "Camera 2 offline for over 6 hours"),
                    RiskFactor("Other indicators", 24, "Missing staff log entries")
                ),
                trend = "STABLE"
            ),
            cctvOnlineCount = 1,
            cctvTotalCount = 4,
            attendanceRate = 54,
            networkUptime = 82,
            lastInspectionDate = "2026-07-29",
            activeAlertsCount = 2
        ),
        NGO(
            id = "ngo-003",
            name = "SHAKTI FOUNDATION",
            location = LocationData(
                latitude = 24.5854,
                longitude = 73.7125,
                city = "Udaipur",
                state = "Rajasthan",
                address = "Lake Road, Hiran Magri, Udaipur"
            ),
            riskScore = RiskScore(
                score = 62,
                level = RiskLevel.MEDIUM,
                factors = listOf(
                    RiskFactor("Network interruptions", 30, "Intermittent Wi-Fi dropping")
                ),
                trend = "STABLE"
            ),
            cctvOnlineCount = 0,
            cctvTotalCount = 3,
            attendanceRate = 88,
            networkUptime = 65,
            lastInspectionDate = "2026-09-01",
            activeAlertsCount = 1
        ),
        NGO(
            id = "ngo-004",
            name = "SAMARPAN TRUST FOR WOMEN",
            location = LocationData(
                latitude = 24.5854,
                longitude = 73.7125,
                city = "Udaipur",
                state = "Rajasthan",
                address = "Sector 3, Hiran Magri, Udaipur"
            ),
            riskScore = RiskScore(
                score = 42,
                level = RiskLevel.MEDIUM,
                factors = listOf(
                    RiskFactor("Documentation update", 20, "Pending annual renewal")
                ),
                trend = "DECREASING"
            ),
            cctvOnlineCount = 4,
            cctvTotalCount = 4,
            attendanceRate = 88,
            networkUptime = 91,
            lastInspectionDate = "2026-09-01",
            activeAlertsCount = 1
        ),
        NGO(
            id = "ngo-005",
            name = "NAVJEEVAN SKILL ACADEMY",
            location = LocationData(
                latitude = 28.6139,
                longitude = 77.2090,
                city = "New Delhi",
                state = "Delhi",
                address = "Block C, Okhla Phase III, New Delhi"
            ),
            riskScore = RiskScore(
                score = 18,
                level = RiskLevel.LOW,
                factors = listOf(
                    RiskFactor("Other indicators", 18, "Routine compliance verification due")
                ),
                trend = "STABLE"
            ),
            cctvOnlineCount = 6,
            cctvTotalCount = 6,
            attendanceRate = 96,
            networkUptime = 99,
            lastInspectionDate = "2026-09-10",
            activeAlertsCount = 0
        )
    )

    val sampleCctvFeeds = listOf(
        CCTVStatus(
            id = "camera-001",
            ngoId = heroNgoId,
            ngoName = "SUNRISE WELFARE FOUNDATION",
            cameraName = "Cam 01",
            state = CameraState.ONLINE,
            peopleCount = 27,
            anomalyProbability = 82,
            lastFrameSecAgo = 4,
            source = "LOCAL_VIDEO",
            type = "LOCAL_DEMO",
            protocol = "LOCAL_FILE",
            mediaRef = "cctv_demo.mp4"
        ),
        CCTVStatus(
            id = "camera-002",
            ngoId = heroNgoId,
            ngoName = "SUNRISE WELFARE FOUNDATION",
            cameraName = "Cam 02",
            state = CameraState.ONLINE,
            peopleCount = 31,
            anomalyProbability = 14,
            lastFrameSecAgo = 2,
            source = "LOCAL_VIDEO",
            type = "LOCAL_DEMO",
            protocol = "LOCAL_FILE",
            mediaRef = "cctv_demo_2.mp4"
        ),
        CCTVStatus(
            id = "camera-003",
            ngoId = heroNgoId,
            ngoName = "SUNRISE WELFARE FOUNDATION",
            cameraName = "Cam 03",
            state = CameraState.OFFLINE,
            peopleCount = 0,
            anomalyProbability = 95,
            lastFrameSecAgo = 240,
            source = "RTSP",
            type = "IP_CAMERA",
            protocol = "RTSP",
            mediaRef = null
        )
    )

    val sampleAlerts = listOf(
        Alert(
            id = "alt-001",
            ngoId = heroNgoId,
            ngoName = "SUNRISE WELFARE FOUNDATION",
            title = "High Risk Threshold Exceeded",
            timestamp = "14:28:10",
            severity = AlertSeverity.CRITICAL,
            message = "Combined risk score jumped to 87 due to camera drop and attendance discrepancy."
        ),
        Alert(
            id = "alt-002",
            ngoId = heroNgoId,
            ngoName = "SUNRISE WELFARE FOUNDATION",
            title = "CAM-003 Feed Interrupted",
            timestamp = "14:26:04",
            severity = AlertSeverity.WARNING,
            message = "Main dining area camera signal lost. Last seen 4 minutes ago."
        ),
        Alert(
            id = "alt-003",
            ngoId = "ngo-005",
            ngoName = "PRERANA YOUTH FOUNDATION",
            title = "Complete Stream Blackout",
            timestamp = "14:15:30",
            severity = AlertSeverity.CRITICAL,
            message = "All 3 registered CCTV channels are un-reachable."
        ),
        Alert(
            id = "alt-004",
            ngoId = "ngo-002",
            ngoName = "ASHA CHILD CARE CENTRE",
            title = "Attendance Anomaly Flagged",
            timestamp = "13:45:12",
            severity = AlertSeverity.WARNING,
            message = "Headcount detected by Edge AI (12) is 50% below registered count (24)."
        )
    )

    val sampleAuditEvents = listOf(
        AuditEvent("aud-001", "14:31:02", "Inspection triggered", "Surprise inspection initiated by Command Center Operator", "System Admin", "TRIGGER"),
        AuditEvent("aud-002", "14:31:16", "Inspector assigned", "Field Inspector Vikram Singh (ID: INS-892) assigned", "Auto Dispatcher", "ASSIGN"),
        AuditEvent("aud-003", "14:32:04", "GPS verified", "Location confirmed 31 metres from Sunrise Welfare Foundation (Within Geofence)", "GPS Engine", "GPS"),
        AuditEvent("aud-004", "14:32:18", "Video inspection started", "Encrypted WebRTC P2P stream established with field unit", "WebRTC Gateway", "VIDEO"),
        AuditEvent("aud-005", "14:35:42", "Evidence captured", "2 High-res geo-tagged photos and biometric snapshot attached", "Inspector App", "EVIDENCE"),
        AuditEvent("aud-006", "14:36:11", "Report submitted", "Inspection compliance report submitted with rating 68%", "Inspector App", "SUBMIT"),
        AuditEvent("aud-007", "14:36:12", "Evidence record sealed", "Cryptographic SHA-256 seal (0x8F4A...C291) written to audit trail", "Blockchain Notary", "SEAL")
    )
}
