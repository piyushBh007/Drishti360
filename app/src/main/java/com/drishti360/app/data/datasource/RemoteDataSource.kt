package com.drishti360.app.data.datasource

import android.util.Log
import com.drishti360.app.data.models.Alert
import com.drishti360.app.data.models.AlertSeverity
import com.drishti360.app.data.models.AuditEvent
import com.drishti360.app.data.models.Inspection
import com.drishti360.app.data.models.InspectionStatus
import com.drishti360.app.data.models.LocationData
import com.drishti360.app.data.models.NGO
import com.drishti360.app.data.models.RiskFactor
import com.drishti360.app.data.models.RiskLevel
import com.drishti360.app.data.models.RiskScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

import com.drishti360.app.config.AppConfig

class RemoteDataSource(
    private val baseUrl: String = AppConfig.HTTP_BASE_URL
) {

    private fun httpPut(urlString: String, jsonBody: String, token: String? = null): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.setRequestProperty("Content-Type", "application/json")
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        conn.doOutput = true

        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(jsonBody)
        writer.flush()
        writer.close()

        val statusCode = conn.responseCode
        if (statusCode in 200..299) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            return sb.toString()
        } else {
            val errStream = conn.errorStream ?: conn.inputStream
            val errReader = BufferedReader(InputStreamReader(errStream))
            val errSb = StringBuilder()
            var line: String?
            while (errReader.readLine().also { line = it } != null) {
                errSb.append(line)
            }
            errReader.close()
            throw com.drishti360.app.utils.ApiException(statusCode, errSb.toString())
        }
    }

    suspend fun fetchNgos(): Result<List<NGO>> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = httpGet("$baseUrl/api/ngos")
            val array = JSONArray(jsonString)
            val list = mutableListOf<NGO>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(parseNgo(obj))
            }
            list
        }
    }

    suspend fun fetchNgoById(id: String): Result<NGO> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = httpGet("$baseUrl/api/ngos/$id")
            val obj = JSONObject(jsonString)
            parseNgo(obj)
        }
    }

    suspend fun fetchAlerts(): Result<List<Alert>> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = httpGet("$baseUrl/api/alerts")
            val array = JSONArray(jsonString)
            val list = mutableListOf<Alert>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Alert(
                        id = obj.optString("id", "ALT-0"),
                        ngoId = obj.optString("ngoId", ""),
                        ngoName = obj.optString("ngoName", ""),
                        title = obj.optString("title", "Alert"),
                        timestamp = obj.optString("timestamp", ""),
                        severity = when (obj.optString("severity", "HIGH").uppercase()) {
                            "HIGH" -> AlertSeverity.CRITICAL
                            "MEDIUM" -> AlertSeverity.WARNING
                            else -> AlertSeverity.INFO
                        },
                        message = obj.optString("description", obj.optString("message", ""))
                    )
                )
            }
            list
        }
    }

    suspend fun postInspector(
        token: String,
        name: String,
        employeeId: String,
        username: String,
        pass: String
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val reqBody = JSONObject().apply {
                put("name", name)
                put("employee_id", employeeId)
                put("username", username)
                put("password", pass)
            }
            val resString = httpPost("$baseUrl/api/inspectors", reqBody.toString(), token)
            JSONObject(resString)
        }
    }

    suspend fun postNgo(
        token: String,
        name: String,
        regNo: String,
        category: String,
        city: String,
        state: String,
        address: String,
        latitude: Double,
        longitude: Double,
        geofenceRadius: Int
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val reqBody = JSONObject().apply {
                put("name", name)
                put("registration_no", regNo)
                put("category", category)
                put("city", city)
                put("state", state)
                put("address", address)
                put("latitude", latitude)
                put("longitude", longitude)
                put("geofence_radius_m", geofenceRadius)
            }
            val resString = httpPost("$baseUrl/api/ngos", reqBody.toString(), token)
            JSONObject(resString)
        }
    }

    suspend fun putNgo(
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
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val reqBody = JSONObject().apply {
                put("name", name)
                put("registration_no", registrationNo)
                put("category", category)
                put("city", city)
                put("state", state)
                put("address", address)
                put("latitude", latitude)
                put("longitude", longitude)
                put("geofence_radius_m", geofenceRadius)
            }
            val resString = httpPut("$baseUrl/api/ngos/$id", reqBody.toString(), token)
            JSONObject(resString)
        }
    }

    suspend fun postInspection(
        ngoId: String,
        ngoName: String,
        inspector: String,
        status: String,
        observations: String,
        photoCount: Int,
        checklist: Map<String, Boolean>
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val reqBody = JSONObject().apply {
                put("ngoId", ngoId)
                put("ngoName", ngoName)
                put("inspector", inspector)
                put("status", status)
                put("observations", observations)
                put("photoCount", photoCount)
                put("checklist", JSONObject(checklist))
            }
            val resString = httpPost("$baseUrl/api/inspections", reqBody.toString())
            JSONObject(resString)
        }
    }

    suspend fun postAnomaly(
        ngoId: String,
        ngoName: String,
        details: String,
        reporter: String = "Field Inspector"
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        postAlert(ngoId = ngoId, ngoName = ngoName, message = details)
    }

    suspend fun postAlert(
        ngoId: String,
        ngoName: String,
        message: String,
        severity: String = "HIGH",
        type: String = "ANOMALY"
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/alerts"
        val reqBody = JSONObject().apply {
            put("ngoId", ngoId)
            put("ngoName", ngoName)
            put("type", type)
            put("severity", severity)
            put("message", message)
            put("details", message)
        }
        Log.d("ANOMALY_SUBMIT", "[ANOMALY_SUBMIT]\nngoId=$ngoId\nrequest=$reqBody")
        runCatching {
            val resString = httpPost(url, reqBody.toString())
            Log.d("ANOMALY_RESPONSE", "[ANOMALY_RESPONSE]\nstatus=201\nbody=$resString")
            JSONObject(resString)
        }.onFailure { err ->
            Log.e("ANOMALY_ERROR", "[ANOMALY_ERROR]\nerror=${err.message}")
        }
    }
data class OverviewMetrics(
    val ngosMonitored: Int,
    val highRisk: Int,
    val inspections: Int
)

    suspend fun fetchOverviewMetrics(): Result<OverviewMetrics> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = httpGet("$baseUrl/api/overview")
            val obj = JSONObject(jsonString)
            OverviewMetrics(
                ngosMonitored = obj.optInt("ngosMonitored", 0),
                highRisk = obj.optInt("highRisk", 0),
                inspections = obj.optInt("inspections", 0)
            )
        }
    }

    suspend fun fetchInspectionsList(): Result<List<Inspection>> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = httpGet("$baseUrl/api/inspections")
            val array = JSONArray(jsonString)
            val list = mutableListOf<Inspection>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val statusStr = obj.optString("status", "COMPLETED").uppercase()
                val statusEnum = when {
                    statusStr.contains("PROGRESS") -> InspectionStatus.IN_PROGRESS
                    statusStr.contains("SEALED") -> InspectionStatus.SEALED
                    statusStr.contains("SCHEDULED") -> InspectionStatus.SCHEDULED
                    else -> InspectionStatus.COMPLETED
                }
                list.add(
                    Inspection(
                        id = obj.optString("id", "INSP-${i + 1}"),
                        ngoId = obj.optString("ngoId", obj.optString("ngo_id", "ngo-001")),
                        ngoName = obj.optString("ngoName", obj.optString("ngo_name", "NGO")),
                        inspectorName = obj.optString("inspector", obj.optString("inspector_name", "Inspector Aarav Mehta")),
                        timestamp = obj.optString("timestamp", ""),
                        status = statusEnum,
                        distanceMeters = obj.optInt("distanceMeters", obj.optInt("distance_meters", 18)),
                        withinGeofence = obj.optBoolean("withinGeofence", obj.optBoolean("within_geofence", true)),
                        beneficiariesObserved = obj.optInt("beneficiariesObserved", obj.optInt("beneficiaries_observed", 24)),
                        staffPresent = obj.optInt("staffPresent", obj.optInt("staff_present", 4)),
                        facilitiesStatus = obj.optString("facilitiesStatus", "Satisfactory"),
                        cctvCompliance = obj.optString("cctvCompliance", "YES"),
                        observations = obj.optString("observations", "Facility operational."),
                        evidenceList = emptyList()
                    )
                )
            }
            list
        }
    }

    suspend fun fetchAuditEvents(): Result<List<AuditEvent>> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = httpGet("$baseUrl/api/audit")
            val array = JSONArray(jsonString)
            val list = mutableListOf<AuditEvent>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AuditEvent(
                        id = obj.optString("id", "AUD-0"),
                        time = obj.optString("timestamp", ""),
                        title = obj.optString("title", "Audit Event"),
                        description = obj.optString("details", ""),
                        actor = obj.optString("actor", "System"),
                        iconType = when (obj.optString("type", "SYSTEM").uppercase()) {
                            "INSPECTION" -> "SUBMIT"
                            "ALERT" -> "TRIGGER"
                            else -> "SEAL"
                        }
                    )
                )
            }
            list
        }
    }

    suspend fun fetchCameras(): Result<List<com.drishti360.app.data.models.CCTVStatus>> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = httpGet("$baseUrl/api/cameras")
            val array = JSONArray(jsonString)
            val list = mutableListOf<com.drishti360.app.data.models.CCTVStatus>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "camera-${i + 1}")
                val ngoId = obj.optString("ngoId", obj.optString("ngo_id", "ngo-001"))
                val name = obj.optString("name", obj.optString("cameraName", "Cam 0${i + 1}"))
                val statusStr = obj.optString("status", "OFFLINE").uppercase()
                val state = if (statusStr == "ONLINE") com.drishti360.app.data.models.CameraState.ONLINE else com.drishti360.app.data.models.CameraState.OFFLINE
                val source = obj.optString("source", "LOCAL_VIDEO")
                val type = obj.optString("type", "LOCAL_DEMO")
                val protocol = obj.optString("protocol", "LOCAL_FILE")
                val streamUrl = obj.optString("streamUrl", obj.optString("stream_url", ""))
                val mediaPath = obj.optString("mediaPath", obj.optString("media_path", obj.optString("media", "")))

                // Fetch real YOLO people count from ML activity endpoint
                var parsedPersonCount: Int? = null
                var parsedAvgPersonCount: Double? = null
                var yoloTime: String? = null
                var hasYolo = false

                try {
                    val actJson = httpGet("$baseUrl/api/ml/activity/$id")
                    val actArray = JSONArray(actJson)
                    if (actArray.length() > 0) {
                        val latest = actArray.getJSONObject(0)
                        if (latest.has("person_count")) {
                            parsedPersonCount = latest.optInt("person_count")
                        } else if (latest.has("personCount")) {
                            parsedPersonCount = latest.optInt("personCount")
                        }

                        if (latest.has("average_person_count")) {
                            val v = latest.optDouble("average_person_count")
                            if (!v.isNaN()) parsedAvgPersonCount = v
                        } else if (latest.has("averagePersonCount")) {
                            val v = latest.optDouble("averagePersonCount")
                            if (!v.isNaN()) parsedAvgPersonCount = v
                        }

                        yoloTime = latest.optString("observed_at", latest.optString("observedAt", latest.optString("timestamp", "")))
                        hasYolo = true

                        Log.d("CCTV_YOLO", "[CCTV_YOLO]\ncameraId=$id\npersonCount=${parsedPersonCount ?: "null"}\naveragePersonCount=${parsedAvgPersonCount ?: "null"}\ntimestamp=$yoloTime")
                    } else {
                        Log.d("CCTV_YOLO", "[CCTV_YOLO]\ncameraId=$id\npersonCount=null\naveragePersonCount=null\ntimestamp=NO_DATA")
                    }
                } catch (e: Exception) {
                    Log.w("CCTV_YOLO", "[CCTV_YOLO]\ncameraId=$id\npersonCount=null\naveragePersonCount=null\ntimestamp=ERROR: ${e.message}")
                }

                val resolvedPeopleCount = when {
                    parsedPersonCount != null -> parsedPersonCount
                    parsedAvgPersonCount != null -> Math.round(parsedAvgPersonCount).toInt()
                    else -> 0
                }

                list.add(
                    com.drishti360.app.data.models.CCTVStatus(
                        id = id,
                        ngoId = ngoId,
                        ngoName = "SUNRISE WELFARE FOUNDATION",
                        cameraName = name,
                        state = state,
                        peopleCount = resolvedPeopleCount,
                        personCount = parsedPersonCount,
                        averagePersonCount = parsedAvgPersonCount,
                        yoloTimestamp = yoloTime,
                        hasYoloData = hasYolo,
                        anomalyProbability = if (state == com.drishti360.app.data.models.CameraState.ONLINE) 15 else 0,
                        lastFrameSecAgo = if (state == com.drishti360.app.data.models.CameraState.ONLINE) 2 else 300,
                        streamUrl = streamUrl,
                        mediaRef = if (mediaPath.isNotBlank() && mediaPath != "null") mediaPath else null,
                        source = source,
                        type = type,
                        protocol = protocol
                    )
                )
            }
            list
        }
    }

    suspend fun fetchRiskForNgo(ngoId: String): Result<RiskScore> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = httpGet("$baseUrl/api/risk/$ngoId")
            val obj = JSONObject(jsonString)
            val scoreVal = obj.optInt("score", 70)
            val levelStr = obj.optString("riskLevel", "HIGH").uppercase()
            val factorsArr = obj.optJSONArray("factors") ?: JSONArray()
            val factorsList = mutableListOf<RiskFactor>()
            for (f in 0 until factorsArr.length()) {
                val factor = factorsArr.getJSONObject(f)
                factorsList.add(
                    RiskFactor(
                        title = factor.optString("title", factor.optString("factor", "Factor")),
                        scoreContribution = factor.optInt("scoreContribution", 10),
                        description = factor.optString("description", "")
                    )
                )
            }
            RiskScore(
                score = scoreVal,
                level = when (levelStr) {
                    "LOW" -> RiskLevel.LOW
                    "MEDIUM" -> RiskLevel.MEDIUM
                    else -> RiskLevel.HIGH
                },
                factors = factorsList,
                trend = "STABLE"
            )
        }
    }

    private fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 3000
        conn.readTimeout = 3000

        val statusCode = conn.responseCode
        if (statusCode in 200..299) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            return sb.toString()
        } else {
            val errStream = conn.errorStream ?: conn.inputStream
            var errStr = ""
            if (errStream != null) {
                val errReader = BufferedReader(InputStreamReader(errStream))
                val errSb = StringBuilder()
                var line: String?
                while (errReader.readLine().also { line = it } != null) {
                    errSb.append(line)
                }
                errReader.close()
                errStr = errSb.toString()
            }
            throw com.drishti360.app.utils.ApiException(statusCode, errStr)
        }
    }

    private fun httpPost(urlString: String, jsonBody: String, token: String? = null): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        conn.doOutput = true

        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(jsonBody)
        writer.flush()
        writer.close()

        val statusCode = conn.responseCode
        if (statusCode in 200..299) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            return sb.toString()
        } else {
            val errStream = conn.errorStream ?: conn.inputStream
            val errReader = BufferedReader(InputStreamReader(errStream))
            val errSb = StringBuilder()
            var line: String?
            while (errReader.readLine().also { line = it } != null) {
                errSb.append(line)
            }
            errReader.close()
            throw com.drishti360.app.utils.ApiException(statusCode, errSb.toString())
        }
    }

    private fun parseNgo(obj: JSONObject): NGO {
        val locObj = obj.optJSONObject("location") ?: JSONObject()
        val loc = LocationData(
            latitude = if (locObj.has("latitude")) locObj.optDouble("latitude") else 0.0,
            longitude = if (locObj.has("longitude")) locObj.optDouble("longitude") else 0.0,
            city = locObj.optString("city", ""),
            state = locObj.optString("state", ""),
            address = locObj.optString("address", ""),
            geofenceRadiusM = obj.optInt("geofence_radius_m", locObj.optInt("geofenceRadiusM", 100))
        )

        val riskObj = obj.optJSONObject("riskScore") ?: JSONObject()
        val levelStr = riskObj.optString("level", "HIGH").uppercase()
        val factorsArr = riskObj.optJSONArray("factors") ?: JSONArray()
        val factorsList = mutableListOf<RiskFactor>()
        for (f in 0 until factorsArr.length()) {
            val factor = factorsArr.getJSONObject(f)
            factorsList.add(
                RiskFactor(
                    title = factor.optString("title", "Factor"),
                    scoreContribution = factor.optInt("scoreContribution", 10),
                    description = factor.optString("description", "")
                )
            )
        }

        val riskScore = RiskScore(
            score = riskObj.optInt("score", 75),
            level = when (levelStr) {
                "LOW" -> RiskLevel.LOW
                "MEDIUM" -> RiskLevel.MEDIUM
                else -> RiskLevel.HIGH
            },
            factors = factorsList,
            trend = riskObj.optString("trend", "STABLE")
        )

        val rawImage = obj.optString("image", "")
        val imageVal = if (rawImage.isNotBlank() && rawImage != "null") rawImage else null

        val imagesArr = obj.optJSONArray("images")
        val imagesList = mutableListOf<String>()
        if (imagesArr != null) {
            for (idx in 0 until imagesArr.length()) {
                val imgStr = imagesArr.getString(idx)
                if (imgStr.isNotBlank() && imgStr != "null") {
                    imagesList.add(imgStr)
                }
            }
        }
        if (imagesList.isEmpty() && imageVal != null) {
            imagesList.add(imageVal)
        }

        return NGO(
            id = obj.optString("id", ""),
            name = obj.optString("name", obj.optString("ngoName", "")),
            registrationNo = obj.optString("registrationNo", ""),
            category = obj.optString("category", ""),
            establishedYear = obj.optInt("establishedYear", 0),
            beneficiaries = obj.optInt("beneficiaries", 0),
            description = obj.optString("description", ""),
            image = imageVal,
            images = imagesList,
            status = obj.optString("status", "ACTIVE"),
            location = loc,
            riskScore = riskScore,
            cctvOnlineCount = obj.optInt("cctvOnlineCount", 0),
            cctvTotalCount = obj.optInt("cctvTotalCount", 0),
            attendanceRate = obj.optInt("attendanceRate", 0),
            networkUptime = obj.optInt("networkUptime", 0),
            lastInspectionDate = obj.optString("lastInspectionDate", ""),
            activeAlertsCount = obj.optInt("activeAlertsCount", 0)
        )
    }
}
