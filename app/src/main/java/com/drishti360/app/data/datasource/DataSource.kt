package com.drishti360.app.data.datasource

import com.drishti360.app.data.models.Alert
import com.drishti360.app.data.models.Inspection
import com.drishti360.app.data.models.NGO

/**
 * Data source abstraction layer.
 * Currently backed by MockDataSource.
 * Future extension: Plug in RemoteDataSource (Retrofit/Ktor connecting to Node.js REST API + PostgreSQL / PostGIS).
 */
interface DrishtiDataSource {
    suspend fun fetchNgos(): List<NGO>
    suspend fun fetchNgoById(id: String): NGO?
    suspend fun fetchAlerts(): List<Alert>
    suspend fun fetchInspections(): List<Inspection>
    suspend fun submitInspection(inspection: Inspection): Boolean
}

/**
 * Node.js REST API integration contract placeholder.
 * Endpoints to implement in future release:
 * GET  /api/v1/ngos
 * GET  /api/v1/ngos/{id}
 * GET  /api/v1/alerts
 * GET  /api/v1/risk/{id}
 * POST /api/v1/inspections
 * POST /api/v1/inspections/{id}/complete
 */
class RemoteDataSourcePlaceholder : DrishtiDataSource {
    override suspend fun fetchNgos(): List<NGO> = throw NotImplementedError("Plug Node.js REST client here")
    override suspend fun fetchNgoById(id: String): NGO? = throw NotImplementedError("Plug Node.js REST client here")
    override suspend fun fetchAlerts(): List<Alert> = throw NotImplementedError("Plug Node.js REST client here")
    override suspend fun fetchInspections(): List<Inspection> = throw NotImplementedError("Plug Node.js REST client here")
    override suspend fun submitInspection(inspection: Inspection): Boolean = throw NotImplementedError("Plug Node.js REST client here")
}
