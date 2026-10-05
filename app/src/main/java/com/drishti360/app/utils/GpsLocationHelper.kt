package com.drishti360.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class InspectorGpsState {
    object CheckingLocation : InspectorGpsState()
    object PermissionDenied : InspectorGpsState()
    object LocationServicesDisabled : InspectorGpsState()
    object LocationUnavailable : InspectorGpsState()
    data class ValidFix(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float,
        val distanceMeters: Int,
        val isWithinGeofence: Boolean,
        val ngoAddress: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : InspectorGpsState()
}

/**
 * Stabilizer that prevents GPS noise jitter from causing UI flickering near geofence boundaries.
 * Uses a Schmitt-trigger hysteresis buffer and consecutive-reading confirmation.
 */
class GpsStabilizer(
    private val hysteresisBufferMeters: Double = 5.0,
    private val requiredConsecutiveReadings: Int = 2
) {
    private var lastVerifiedState: Boolean? = null
    private var consecutiveCount: Int = 0
    private var lastCandidateState: Boolean? = null

    fun evaluateGeofence(
        distanceMeters: Double,
        geofenceRadiusMeters: Double,
        accuracyMeters: Float
    ): Pair<Boolean, String> {
        val currentVerified = lastVerifiedState

        val rawInside = if (currentVerified == true) {
            // When already verified, require distance to exceed (radius + hysteresisBuffer) to transition outside
            distanceMeters <= (geofenceRadiusMeters + hysteresisBufferMeters)
        } else {
            // When currently outside, require distance to be strictly within geofence radius
            distanceMeters <= geofenceRadiusMeters
        }

        if (currentVerified == null) {
            lastVerifiedState = rawInside
            consecutiveCount = 1
            lastCandidateState = rawInside
            return Pair(rawInside, "Initial GPS fix evaluation (distance: ${distanceMeters.toInt()}m, radius: ${geofenceRadiusMeters.toInt()}m)")
        }

        if (rawInside == currentVerified) {
            consecutiveCount = 0
            lastCandidateState = null
            return Pair(currentVerified, "Maintained state (distance: ${distanceMeters.toInt()}m, radius: ${geofenceRadiusMeters.toInt()}m)")
        }

        // Candidate state is different from current verified state
        if (rawInside == lastCandidateState) {
            consecutiveCount++
        } else {
            lastCandidateState = rawInside
            consecutiveCount = 1
        }

        if (consecutiveCount >= requiredConsecutiveReadings) {
            val previousState = lastVerifiedState
            lastVerifiedState = rawInside
            consecutiveCount = 0
            lastCandidateState = null
            val reason = "State transitioned from ${if (previousState == true) "VERIFIED" else "OUTSIDE"} to ${if (rawInside) "VERIFIED" else "OUTSIDE"} after $requiredConsecutiveReadings consecutive readings (dist: ${distanceMeters.toInt()}m, acc: ${accuracyMeters.toInt()}m)"
            return Pair(rawInside, reason)
        }

        return Pair(currentVerified, "Debouncing transition (candidate: ${if (rawInside) "VERIFIED" else "OUTSIDE"}, count: $consecutiveCount/$requiredConsecutiveReadings)")
    }

    fun reset() {
        lastVerifiedState = null
        consecutiveCount = 0
        lastCandidateState = null
    }
}

/**
 * High-accuracy FusedLocationProvider helper for field inspections.
 */
class GpsLocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val stabilizer = GpsStabilizer()

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null
        return try {
            val freshLoc = fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()

            if (freshLoc != null && isLocationFresh(freshLoc)) {
                freshLoc
            } else {
                val lastLoc = fusedClient.lastLocation.await()
                if (lastLoc != null && isLocationFresh(lastLoc)) lastLoc else freshLoc
            }
        } catch (e: Exception) {
            Log.w("GpsLocationHelper", "getCurrentLocation failed: ${e.message}")
            try {
                fusedClient.lastLocation.await()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun isLocationFresh(location: Location): Boolean {
        val ageMs = System.currentTimeMillis() - location.time
        // Accept readings up to 60 seconds old
        return ageMs < 60_000L
    }

    suspend fun resolveInspectorGps(
        ngoLat: Double,
        ngoLon: Double,
        geofenceRadiusM: Int,
        address: String
    ): InspectorGpsState {
        if (!hasLocationPermission()) {
            Log.d("GpsLocationHelper", "[GPS] Permission denied for location")
            return InspectorGpsState.PermissionDenied
        }
        if (!isLocationEnabled()) {
            Log.d("GpsLocationHelper", "[GPS] Location services disabled on device")
            return InspectorGpsState.LocationServicesDisabled
        }

        val loc = getCurrentLocation()
        if (loc == null || (loc.latitude == 0.0 && loc.longitude == 0.0)) {
            Log.w("GpsLocationHelper", "[GPS] Unable to obtain valid GPS fix")
            return InspectorGpsState.LocationUnavailable
        }

        val dist = haversineDistanceMeters(loc.latitude, loc.longitude, ngoLat, ngoLon)
        val radius = if (geofenceRadiusM > 0) geofenceRadiusM.toDouble() else 100.0
        val accuracy = if (loc.hasAccuracy()) loc.accuracy else 0f

        val (isWithin, transitionReason) = stabilizer.evaluateGeofence(dist, radius, accuracy)

        val timestamp = System.currentTimeMillis()
        Log.d("GpsLocationHelper", "[GPS_STATE_CHANGE] timestamp=$timestamp phoneLat=${loc.latitude} phoneLon=${loc.longitude} accuracy=${accuracy}m ngoLat=$ngoLat ngoLon=$ngoLon distance=${dist.toInt()}m radius=${radius.toInt()}m state=${if (isWithin) "Location Verified" else "Outside Geofence"} reason=$transitionReason")

        return InspectorGpsState.ValidFix(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracyMeters = accuracy,
            distanceMeters = dist.toInt(),
            isWithinGeofence = isWithin,
            ngoAddress = address,
            timestamp = timestamp
        )
    }

    fun locationUpdatesFlow(intervalMs: Long = 4_000L): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    if (location.latitude != 0.0 && location.longitude != 0.0) {
                        trySend(location)
                    }
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    companion object {
        fun haversineDistanceMeters(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            return results[0].toDouble()
        }
    }
}
