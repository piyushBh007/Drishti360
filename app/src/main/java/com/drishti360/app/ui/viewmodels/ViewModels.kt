package com.drishti360.app.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drishti360.app.data.datasource.RemoteDataSource
import com.drishti360.app.data.mock.MockData
import com.drishti360.app.data.models.Alert
import com.drishti360.app.data.models.AuditEvent
import com.drishti360.app.data.models.CCTVStatus
import com.drishti360.app.data.models.Evidence
import com.drishti360.app.data.models.Inspection
import com.drishti360.app.data.models.NGO
import com.drishti360.app.data.repositories.AlertRepositoryImpl
import com.drishti360.app.data.repositories.InspectionRepositoryImpl
import com.drishti360.app.data.repositories.NGORepositoryImpl
import com.drishti360.app.data.services.mock.MockCCTVService
import com.drishti360.app.data.services.mock.MockEvidenceService
import com.drishti360.app.data.services.mock.MockInspectionService
import com.drishti360.app.data.services.mock.MockLocationService
import com.drishti360.app.domain.repositories.AlertRepository
import com.drishti360.app.domain.repositories.InspectionRepository
import com.drishti360.app.domain.repositories.NGORepository
import com.drishti360.app.domain.services.CCTVService
import com.drishti360.app.domain.services.EvidenceService
import com.drishti360.app.domain.services.InspectionService
import com.drishti360.app.domain.services.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val ngoRepository: NGORepository = NGORepositoryImpl(),
    private val alertRepository: AlertRepository = AlertRepositoryImpl(),
    private val inspectionRepository: InspectionRepository = InspectionRepositoryImpl(),
    private val remoteDataSource: RemoteDataSource = RemoteDataSource()
) : ViewModel() {

    private val _ngos = MutableStateFlow<List<NGO>>(emptyList())
    val ngos: StateFlow<List<NGO>> = _ngos.asStateFlow()

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _inspections = MutableStateFlow<List<Inspection>>(emptyList())
    val inspections: StateFlow<List<Inspection>> = _inspections.asStateFlow()

    private val _overviewMetrics = MutableStateFlow<com.drishti360.app.data.datasource.RemoteDataSource.OverviewMetrics?>(null)
    val overviewMetrics: StateFlow<com.drishti360.app.data.datasource.RemoteDataSource.OverviewMetrics?> = _overviewMetrics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _anomalySubmitting = MutableStateFlow(false)
    val anomalySubmitting: StateFlow<Boolean> = _anomalySubmitting.asStateFlow()

    init {
        loadData()
    }

    fun refreshData() {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val overviewRes = remoteDataSource.fetchOverviewMetrics()
                if (overviewRes.isSuccess) {
                    _overviewMetrics.value = overviewRes.getOrNull()
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Overview fetch error: ${e.message}")
            }

            try {
                ngoRepository.getAllNgos().collect { list ->
                    _ngos.value = list
                }
            } catch (e: Exception) {
                _error.value = com.drishti360.app.utils.ErrorMapper.mapException(e).message
            } finally {
                _isLoading.value = false
            }
        }

        viewModelScope.launch {
            try {
                inspectionRepository.getActiveInspections().collect { list ->
                    _inspections.value = list
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Inspections fetch error: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                alertRepository.getRecentAlerts().collect { list ->
                    _alerts.value = list
                }
            } catch (e: Exception) {
                // Ignore alert errors
            }
        }
    }

    fun getNgoById(id: String): NGO? {
        return _ngos.value.find { it.id.equals(id, ignoreCase = true) }
            ?: MockData.sampleNgos.find { it.id.equals(id, ignoreCase = true) }
    }

    fun reportAnomaly(
        ngoId: String,
        ngoName: String,
        details: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (details.isBlank()) {
            onError("Observation details cannot be empty.")
            return
        }
        viewModelScope.launch {
            _anomalySubmitting.value = true
            val result = remoteDataSource.postAnomaly(ngoId, ngoName, details)
            _anomalySubmitting.value = false

            result.onSuccess {
                refreshAlerts()
                onSuccess()
            }.onFailure { err ->
                val appError = com.drishti360.app.utils.ErrorMapper.mapException(err)
                onError(appError.message)
            }
        }
    }

    fun refreshAlerts() {
        viewModelScope.launch {
            try {
                alertRepository.getRecentAlerts().collect { list ->
                    _alerts.value = list
                }
            } catch (e: Exception) {
                // Ignore alert refresh errors; local state already updated
            }
        }
    }

    suspend fun addNgo(
        token: String,
        name: String,
        regNo: String,
        category: String,
        city: String,
        state: String,
        address: String,
        lat: Double,
        lng: Double,
        geofenceRadius: Int
    ): Result<Boolean> {
        return try {
            val result = remoteDataSource.postNgo(
                token, name, regNo, category, city, state, address, lat, lng, geofenceRadius
            )
            if (result.isSuccess) {
                refreshData() // reload NGOs from server
                
                // Add Audit Event for NGO creation
                val finalTs = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US).format(java.util.Date())
                val auditEvent = com.drishti360.app.data.models.AuditEvent(
                    id = "AUD-${System.currentTimeMillis()}",
                    time = finalTs,
                    title = "NGO Registered",
                    description = "$name added to the system.",
                    actor = "System Admin",
                    iconType = "SUBMIT"
                )
                com.drishti360.app.data.repositories.LocalAuditRepository.addAuditEvent(auditEvent)
                
                Result.success(true)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Add NGO Error", e)
            Result.failure(e)
        }
    }

    suspend fun updateNgo(
        token: String,
        id: String,
        name: String,
        registrationNo: String,
        category: String,
        city: String,
        state: String,
        address: String,
        latitude: Double,
        longitude: Double,
        geofenceRadius: Int
    ): Result<Boolean> {
        return try {
            val result = remoteDataSource.putNgo(
                token, id, name, registrationNo, category, city, state, address, latitude, longitude, geofenceRadius
            )
            if (result.isSuccess) {
                refreshData()
                Result.success(true)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Update NGO Error", e)
            Result.failure(e)
        }
    }

    suspend fun addInspector(
        token: String,
        name: String,
        employeeId: String,
        username: String,
        pass: String
    ): Result<Boolean> {
        return try {
            val result = remoteDataSource.postInspector(token, name, employeeId, username, pass)
            if (result.isSuccess) {
                // Add Audit Event for Inspector creation
                val finalTs = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US).format(java.util.Date())
                val auditEvent = com.drishti360.app.data.models.AuditEvent(
                    id = "AUD-${System.currentTimeMillis()}",
                    time = finalTs,
                    title = "Inspector Created",
                    description = "Inspector $name ($employeeId) created.",
                    actor = "System Admin",
                    iconType = "SUBMIT"
                )
                com.drishti360.app.data.repositories.LocalAuditRepository.addAuditEvent(auditEvent)
                Result.success(true)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Add Inspector Error", e)
            Result.failure(e)
        }
    }
}

class InspectionViewModel(
    private val inspectionService: InspectionService = MockInspectionService(),
    private val locationService: LocationService = MockLocationService(),
    private val evidenceService: EvidenceService = MockEvidenceService(),
    private val inspectionRepository: InspectionRepository = InspectionRepositoryImpl(),
    private val remoteDataSource: RemoteDataSource = RemoteDataSource()
) : ViewModel() {

    private val _currentInspection = MutableStateFlow<Inspection?>(null)
    val currentInspection: StateFlow<Inspection?> = _currentInspection.asStateFlow()

    private val _locationVerified = MutableStateFlow(false)
    val locationVerified: StateFlow<Boolean> = _locationVerified.asStateFlow()

    private val _cctvFunctional = MutableStateFlow(true)
    val cctvFunctional: StateFlow<Boolean> = _cctvFunctional.asStateFlow()

    private val _recordsVerified = MutableStateFlow(true)
    val recordsVerified: StateFlow<Boolean> = _recordsVerified.asStateFlow()

    private val _staffPresent = MutableStateFlow(true)
    val staffPresent: StateFlow<Boolean> = _staffPresent.asStateFlow()

    private val _distanceMeters = MutableStateFlow(-1)
    val distanceMeters: StateFlow<Int> = _distanceMeters.asStateFlow()

    private val _gpsState = MutableStateFlow<com.drishti360.app.utils.InspectorGpsState>(
        com.drishti360.app.utils.InspectorGpsState.LocationUnavailable
    )
    val gpsState: StateFlow<com.drishti360.app.utils.InspectorGpsState> = _gpsState.asStateFlow()

    private val _evidenceList = MutableStateFlow<List<Evidence>>(
        listOf(
            Evidence("E1", "Facility Exterior", "PHOTO", "10:14 AM", "0x8F4A"),
            Evidence("E2", "Attendance Log", "PHOTO", "10:15 AM", "0x3D9B")
        )
    )
    val evidenceList: StateFlow<List<Evidence>> = _evidenceList.asStateFlow()

    private val _isSubmitted = MutableStateFlow(false)
    val isSubmitted: StateFlow<Boolean> = _isSubmitted.asStateFlow()

    private val _submittedInspection = MutableStateFlow<Inspection?>(null)
    val submittedInspection: StateFlow<Inspection?> = _submittedInspection.asStateFlow()

    private val _submittedInspectionId = MutableStateFlow("INSP-1001")
    val submittedInspectionId: StateFlow<String> = _submittedInspectionId.asStateFlow()

    private val _submittedTimestamp = MutableStateFlow("")
    val submittedTimestamp: StateFlow<String> = _submittedTimestamp.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _activeNgoId = MutableStateFlow<String?>(null)
    val activeNgoId: StateFlow<String?> = _activeNgoId.asStateFlow()

    fun setActiveNgoId(ngoId: String) {
        _activeNgoId.value = ngoId
    }

    fun initiateSurpriseInspection(ngoId: String, ngoName: String) {
        _activeNgoId.value = ngoId
        viewModelScope.launch {
            val insp = inspectionService.initiateInspection(ngoId, ngoName, "Inspector Aarav Mehta")
            _currentInspection.value = insp
            _locationVerified.value = false
            _distanceMeters.value = -1
            _isSubmitted.value = false
        }
    }

    fun toggleChecklistItem(itemKey: String) {
        when (itemKey) {
            "location" -> _locationVerified.value = !_locationVerified.value
            "cctv" -> _cctvFunctional.value = !_cctvFunctional.value
            "records" -> _recordsVerified.value = !_recordsVerified.value
            "staff" -> _staffPresent.value = !_staffPresent.value
        }
    }

    private var locationHelper: com.drishti360.app.utils.GpsLocationHelper? = null

    fun updateGpsStatus(
        context: android.content.Context,
        ngoLat: Double,
        ngoLon: Double,
        geofenceRadiusM: Int,
        address: String
    ) {
        viewModelScope.launch {
            val helper = locationHelper ?: com.drishti360.app.utils.GpsLocationHelper(context.applicationContext).also { locationHelper = it }
            val state = helper.resolveInspectorGps(ngoLat, ngoLon, geofenceRadiusM, address)
            _gpsState.value = state
            when (state) {
                is com.drishti360.app.utils.InspectorGpsState.ValidFix -> {
                    _distanceMeters.value = state.distanceMeters
                    _locationVerified.value = state.isWithinGeofence
                }
                else -> {
                    _distanceMeters.value = -1
                    _locationVerified.value = false
                }
            }
        }
    }

    fun calculateDistance(context: android.content.Context, ngoLat: Double, ngoLon: Double) {
        updateGpsStatus(context, ngoLat, ngoLon, 100, "")
    }

    fun verifyLocation() {
        viewModelScope.launch {
            _locationVerified.value = true
        }
    }

    fun capturePhotoEvidence() {
        viewModelScope.launch {
            val evidence = evidenceService.captureEvidence(
                "PHOTO",
                "Inspection Photo #${_evidenceList.value.size + 1}"
            )
            _evidenceList.value = _evidenceList.value + evidence
        }
    }

    fun addPhotoUri(uriString: String) {
        val count = _evidenceList.value.size + 1
        val newEvidence = Evidence(
            id = "E$count-${System.currentTimeMillis() % 1000}",
            title = "Inspection Photo #$count",
            type = "PHOTO",
            timestamp = "Just now",
            hash = "0x" + System.currentTimeMillis().toString(16).takeLast(4),
            previewUrl = uriString
        )
        _evidenceList.value = _evidenceList.value + newEvidence
    }

    fun removeEvidence(evidenceId: String) {
        _evidenceList.value = _evidenceList.value.filter { it.id != evidenceId }
    }

    fun submitInspection(
        ngoId: String,
        ngoName: String,
        inspector: String = "Aarav Mehta",
        observations: String = "Facility operational. Checklist verified."
    ) {
        Log.d("NGO_FLOW", "[NGO_FLOW]\nSUBMIT_INSPECTION ngoId=$ngoId")
        viewModelScope.launch {
            _isSubmitting.value = true
            val checklistMap = mapOf(
                "location" to _locationVerified.value,
                "cctv" to _cctvFunctional.value,
                "records" to _recordsVerified.value,
                "staff" to _staffPresent.value
            )

            val result = remoteDataSource.postInspection(
                ngoId = ngoId,
                ngoName = ngoName,
                inspector = inspector,
                status = if (_locationVerified.value && _recordsVerified.value) "SATISFACTORY" else "ISSUES_FOUND",
                observations = observations,
                photoCount = _evidenceList.value.size,
                checklist = checklistMap
            )

            val ts = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US).format(java.util.Date())

            val (newId, finalTs) = if (result.isSuccess) {
                val json = result.getOrNull()
                val inspObj = json?.optJSONObject("inspection")
                val id = inspObj?.optString("id") ?: "INSP-${System.currentTimeMillis() % 100000}"
                val resTs = inspObj?.optString("timestamp") ?: ts
                Pair(id, resTs)
            } else {
                Pair("INSP-${System.currentTimeMillis() % 100000}", ts)
            }

            val finalInspection = Inspection(
                id = newId,
                ngoId = ngoId,
                ngoName = ngoName,
                inspectorName = inspector,
                timestamp = finalTs,
                status = com.drishti360.app.data.models.InspectionStatus.COMPLETED,
                distanceMeters = 18,
                withinGeofence = _locationVerified.value,
                beneficiariesObserved = 24,
                staffPresent = 4,
                facilitiesStatus = "Satisfactory",
                cctvCompliance = if (_cctvFunctional.value) "YES" else "NO",
                observations = observations,
                evidenceList = _evidenceList.value
            )

            inspectionRepository.saveInspection(finalInspection)

            val auditEvent = AuditEvent(
                id = "AUD-${System.currentTimeMillis()}",
                time = finalTs,
                title = "Inspection Report Submitted",
                description = "$newId - $ngoName (${if (_locationVerified.value && _recordsVerified.value) "SATISFACTORY" else "ISSUES_FOUND"})",
                actor = "$inspector (Inspector)",
                iconType = "SUBMIT"
            )
            com.drishti360.app.data.repositories.LocalAuditRepository.addAuditEvent(auditEvent)

            _submittedInspection.value = finalInspection
            _submittedInspectionId.value = newId
            _submittedTimestamp.value = finalTs
            _isSubmitted.value = true
            _isSubmitting.value = false
        }
    }

    fun resetSubmissionState() {
        _isSubmitted.value = false
    }
}

class CctvViewModel(
    private val cctvService: CCTVService = MockCCTVService(),
    private val remoteDataSource: RemoteDataSource = RemoteDataSource()
) : ViewModel() {

    private val _cameraFeeds = MutableStateFlow<List<CCTVStatus>>(emptyList())
    val cameraFeeds: StateFlow<List<CCTVStatus>> = _cameraFeeds.asStateFlow()

    init {
        loadCameraFeeds()
    }

    fun loadCameraFeeds() {
        viewModelScope.launch {
            val result = remoteDataSource.fetchCameras()
            result.onSuccess { remoteFeeds ->
                if (remoteFeeds.isNotEmpty()) {
                    _cameraFeeds.value = remoteFeeds
                    return@launch
                }
            }
            cctvService.getLiveFeeds(MockData.heroNgoId).collect { feeds ->
                _cameraFeeds.value = feeds
            }
        }
    }
}

class AuditViewModel(
    private val inspectionRepository: InspectionRepository = InspectionRepositoryImpl(),
    private val remoteDataSource: RemoteDataSource = RemoteDataSource()
) : ViewModel() {

    private val _auditEvents = MutableStateFlow<List<AuditEvent>>(emptyList())
    val auditEvents: StateFlow<List<AuditEvent>> = _auditEvents.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    init {
        loadAuditTrail()
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun loadAuditTrail() {
        viewModelScope.launch {
            val remoteResult = remoteDataSource.fetchAuditEvents()
            val remoteList = if (remoteResult.isSuccess) remoteResult.getOrDefault(emptyList()) else emptyList()
            val localList = com.drishti360.app.data.repositories.LocalAuditRepository.getExtraAuditEvents()
            val mockList = MockData.sampleAuditEvents

            val combined = mutableListOf<AuditEvent>()
            val seenIds = mutableSetOf<String>()

            (localList + remoteList + mockList).forEach { event ->
                if (seenIds.add(event.id)) {
                    combined.add(event)
                }
            }
            _auditEvents.value = combined
        }
    }
}
