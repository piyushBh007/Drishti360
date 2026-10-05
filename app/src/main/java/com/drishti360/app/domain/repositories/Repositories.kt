package com.drishti360.app.domain.repositories

import com.drishti360.app.data.models.Alert
import com.drishti360.app.data.models.AuditEvent
import com.drishti360.app.data.models.Inspection
import com.drishti360.app.data.models.NGO
import kotlinx.coroutines.flow.Flow

interface NGORepository {
    fun getAllNgos(): Flow<List<NGO>>
    suspend fun getNgoById(id: String): NGO?
    fun getHighRiskNgos(): Flow<List<NGO>>
}

interface InspectionRepository {
    fun getActiveInspections(): Flow<List<Inspection>>
    suspend fun getInspectionById(id: String): Inspection?
    suspend fun saveInspection(inspection: Inspection)
    suspend fun getAuditTrail(inspectionId: String): List<AuditEvent>
}

interface AlertRepository {
    fun getRecentAlerts(): Flow<List<Alert>>
    suspend fun dismissAlert(alertId: String)
}
