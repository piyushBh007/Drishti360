package com.drishti360.app.data.repositories

import com.drishti360.app.data.datasource.RemoteDataSource
import com.drishti360.app.data.mock.MockData
import com.drishti360.app.data.models.Alert
import com.drishti360.app.data.models.AuditEvent
import com.drishti360.app.data.models.Inspection
import com.drishti360.app.data.models.NGO
import com.drishti360.app.data.models.RiskLevel
import com.drishti360.app.domain.repositories.AlertRepository
import com.drishti360.app.domain.repositories.InspectionRepository
import com.drishti360.app.domain.repositories.NGORepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object LocalAuditRepository {
    private val extraAuditEvents = mutableListOf<AuditEvent>()

    fun addAuditEvent(event: AuditEvent) {
        if (!extraAuditEvents.any { it.id == event.id }) {
            extraAuditEvents.add(0, event)
        }
    }

    fun getExtraAuditEvents(): List<AuditEvent> = extraAuditEvents
}

object LocalAlertRepository {
    private val extraAlerts = mutableListOf<Alert>()

    fun addAlert(alert: Alert) {
        if (!extraAlerts.any { it.id == alert.id }) {
            extraAlerts.add(0, alert)
        }
    }

    fun getExtraAlerts(): List<Alert> = extraAlerts
}

class NGORepositoryImpl(
    private val remoteDataSource: RemoteDataSource = RemoteDataSource()
) : NGORepository {

    override fun getAllNgos(): Flow<List<NGO>> = flow {
        val result = remoteDataSource.fetchNgos()
        if (result.isSuccess) {
            emit(result.getOrDefault(MockData.sampleNgos))
        } else {
            emit(MockData.sampleNgos)
        }
    }

    override suspend fun getNgoById(id: String): NGO? {
        val result = remoteDataSource.fetchNgoById(id)
        return if (result.isSuccess) {
            result.getOrNull()
        } else {
            MockData.sampleNgos.find { it.id.equals(id, ignoreCase = true) }
        }
    }

    override fun getHighRiskNgos(): Flow<List<NGO>> = flow {
        val result = remoteDataSource.fetchNgos()
        if (result.isSuccess) {
            val list = result.getOrDefault(MockData.sampleNgos)
            emit(list.filter { it.riskScore?.level == RiskLevel.HIGH })
        } else {
            emit(MockData.sampleNgos.filter { it.riskScore?.level == RiskLevel.HIGH })
        }
    }
}

class InspectionRepositoryImpl(
    private val remoteDataSource: RemoteDataSource = RemoteDataSource()
) : InspectionRepository {

    private val localInspections = mutableListOf<Inspection>()

    override fun getActiveInspections(): Flow<List<Inspection>> = flow {
        val result = remoteDataSource.fetchInspectionsList()
        val remoteList = if (result.isSuccess) result.getOrDefault(emptyList()) else emptyList()
        val combined = mutableListOf<Inspection>()
        val seenIds = mutableSetOf<String>()
        (localInspections + remoteList).forEach { insp ->
            if (seenIds.add(insp.id)) {
                combined.add(insp)
            }
        }
        emit(combined)
    }

    override suspend fun getInspectionById(id: String): Inspection? {
        return localInspections.find { it.id == id }
    }

    override suspend fun saveInspection(inspection: Inspection) {
        val idx = localInspections.indexOfFirst { it.id == inspection.id }
        if (idx >= 0) {
            localInspections[idx] = inspection
        } else {
            localInspections.add(0, inspection)
        }
    }

    override suspend fun getAuditTrail(inspectionId: String): List<AuditEvent> {
        val result = remoteDataSource.fetchAuditEvents()
        val remoteList = if (result.isSuccess) result.getOrDefault(emptyList()) else emptyList()
        val localList = LocalAuditRepository.getExtraAuditEvents()
        val mockList = MockData.sampleAuditEvents

        val combined = mutableListOf<AuditEvent>()
        val seenIds = mutableSetOf<String>()

        (localList + remoteList + mockList).forEach { event ->
            if (seenIds.add(event.id)) {
                combined.add(event)
            }
        }
        return combined
    }
}

class AlertRepositoryImpl(
    private val remoteDataSource: RemoteDataSource = RemoteDataSource()
) : AlertRepository {

    override fun getRecentAlerts(): Flow<List<Alert>> = flow {
        val result = remoteDataSource.fetchAlerts()
        val remoteList = if (result.isSuccess) result.getOrDefault(emptyList()) else emptyList()
        val localList = LocalAlertRepository.getExtraAlerts()
        val mockList = MockData.sampleAlerts

        val combined = mutableListOf<Alert>()
        val seenIds = mutableSetOf<String>()

        (localList + remoteList + mockList).forEach { alert ->
            if (seenIds.add(alert.id)) {
                combined.add(alert)
            }
        }
        emit(combined)
    }

    override suspend fun dismissAlert(alertId: String) {
        // Dismiss alert locally
    }
}
