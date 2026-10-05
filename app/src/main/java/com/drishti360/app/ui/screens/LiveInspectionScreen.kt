package com.drishti360.app.ui.screens

import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.data.models.NGO
import com.drishti360.app.ui.components.PrimaryActionButton
import com.drishti360.app.ui.theme.LightBackground
import com.drishti360.app.ui.theme.LightBorder
import com.drishti360.app.ui.theme.LightSurface
import com.drishti360.app.ui.theme.PrimaryBlue
import com.drishti360.app.ui.theme.RiskHighRed
import com.drishti360.app.ui.theme.RiskHighBg
import com.drishti360.app.ui.theme.RiskLowBg
import com.drishti360.app.ui.theme.RiskLowGreen
import com.drishti360.app.ui.theme.TextDarkPrimary
import com.drishti360.app.ui.theme.TextDarkSecondary
import com.drishti360.app.ui.viewmodels.InspectionViewModel

import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope

private const val TAG_CALL = "Drishti360-Call"

@Composable
fun LiveInspectionScreen(
    ngo: NGO,
    allNgos: List<NGO> = emptyList(),
    onSelectNgo: (NGO) -> Unit = {},
    viewModel: InspectionViewModel,
    onBack: () -> Unit,
    onVerifyLocation: (String) -> Unit = {}
) {
    var activeStep by remember { mutableStateOf(1) } // 1: Location, 2: Call, 3: Check, 4: Report
    var isMuted by remember { mutableStateOf(false) }
    var expandedNgoDropdown by remember { mutableStateOf(false) }

    val activeNgoId by viewModel.activeNgoId.collectAsState()
    val ngoListToDisplay = remember(allNgos, ngo) {
        if (allNgos.isNotEmpty()) allNgos else listOf(ngo)
    }

    val selectedNgoState = remember(activeNgoId, ngoListToDisplay) {
        ngoListToDisplay.find { it.id == activeNgoId } ?: ngo
    }
    
    val gpsState by viewModel.gpsState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val (inspectorLat, inspectorLon) = when (val state = gpsState) {
        is com.drishti360.app.utils.InspectorGpsState.ValidFix -> Pair(state.latitude, state.longitude)
        else -> Pair(null, null)
    }

    LaunchedEffect(selectedNgoState.id) {
        viewModel.setActiveNgoId(selectedNgoState.id)
        viewModel.updateGpsStatus(
            context = context,
            ngoLat = selectedNgoState.location.latitude,
            ngoLon = selectedNgoState.location.longitude,
            geofenceRadiusM = selectedNgoState.location.geofenceRadiusM,
            address = selectedNgoState.location.address
        )
        Log.d("NGO_FLOW", "[NGO_FLOW]\nINSPECT_SELECTION selectedNgoId=${selectedNgoState.id}\n[INSPECT_SELECTION] selectedNgo=${selectedNgoState.name}\n[INSPECT_SELECTION] lat=${selectedNgoState.location.latitude} lon=${selectedNgoState.location.longitude}\n[INSPECT_SELECTION] distance=${viewModel.distanceMeters.value}")
    }

    val defaultNgoLat = if (selectedNgoState.location.latitude in -90.0..90.0) selectedNgoState.location.latitude else 20.5937
    val defaultNgoLon = if (selectedNgoState.location.longitude in -180.0..180.0) selectedNgoState.location.longitude else 78.9629
    // Removed Google Maps CameraPositionState

    LaunchedEffect(activeStep, selectedNgoState.id) {
        when (activeStep) {
            1 -> Log.d("NGO_FLOW", "[NGO_FLOW]\nLOCATION ngoId=${selectedNgoState.id}")
            2 -> Log.d("NGO_FLOW", "[NGO_FLOW]\nCALL ngoId=${selectedNgoState.id}")
            3 -> Log.d("NGO_FLOW", "[NGO_FLOW]\nCHECK ngoId=${selectedNgoState.id}")
        }
    }

    val isLocationVerified by viewModel.locationVerified.collectAsState()
    val isCctvFunctional by viewModel.cctvFunctional.collectAsState()
    val isRecordsVerified by viewModel.recordsVerified.collectAsState()
    val isStaffPresent by viewModel.staffPresent.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightSurface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDarkPrimary
                )
            }
            Text(
                text = "Field Inspection",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
        }

        // Target NGO Selector Dropdown (Allows inspecting ANY loaded NGO)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .border(1.dp, LightBorder, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = LightSurface),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedNgoDropdown = !expandedNgoDropdown }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Target NGO:",
                            fontSize = 10.sp,
                            color = TextDarkSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = selectedNgoState.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                        Text(
                            text = "📍 ${selectedNgoState.location.city}, ${selectedNgoState.location.state} • ${if (selectedNgoState.riskScore != null) "${selectedNgoState.riskScore.level.name} Risk (${selectedNgoState.riskScore.score})" else "Not Assessed"}",
                            fontSize = 11.sp,
                            color = TextDarkSecondary
                        )
                    }
                    Icon(
                        imageVector = if (expandedNgoDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Select NGO",
                        tint = PrimaryBlue
                    )
                }

                if (expandedNgoDropdown) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(LightBorder)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                    ) {
                        ngoListToDisplay.forEach { item ->
                            val isSelected = item.id.equals(selectedNgoState.id, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        android.util.Log.d("NGO_SELECTION", "clicked ID = ${item.id}, name = ${item.name}")
                                        viewModel.setActiveNgoId(item.id)
                                        viewModel.initiateSurpriseInspection(item.id, item.name)
                                        onSelectNgo(item)
                                        expandedNgoDropdown = false
                                        android.util.Log.d("NGO_SELECTION", "selected ID = ${item.id}")
                                    }
                                    .background(if (isSelected) PrimaryBlue.copy(alpha = 0.08f) else Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) PrimaryBlue else TextDarkPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "📍 ${item.location.city}, ${item.location.state} • ${if (item.riskScore != null) "${item.riskScore.level.name} Risk (${item.riskScore.score})" else "Not Assessed"}",
                                        fontSize = 11.sp,
                                        color = TextDarkSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(LightBorder.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Interactive Four-step progress bar (1 Location | 2 Live Call | 3 Checklist | 4 Report)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepItem(number = "1", title = "Location", isActive = activeStep >= 1) { activeStep = 1 }
                    StepDivider()
                    StepItem(number = "2", title = "Live Call", isActive = activeStep >= 2) { activeStep = 2 }
                    StepDivider()
                    StepItem(number = "3", title = "Checklist", isActive = activeStep >= 3) { activeStep = 3 }
                    StepDivider()
                    StepItem(number = "4", title = "Report", isActive = activeStep >= 4) { onVerifyLocation(selectedNgoState.id) }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Step 1: Location & Map
            if (activeStep == 1) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE2E8F0))
                            .border(1.dp, LightBorder, RoundedCornerShape(12.dp))
                    ) {
                        val ngoLat = selectedNgoState.location.latitude
                        val ngoLon = selectedNgoState.location.longitude
                        
                        val ctx = LocalContext.current
                        androidx.compose.ui.viewinterop.AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                org.osmdroid.config.Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
                                org.osmdroid.views.MapView(context).apply {
                                    setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(true)
                                    controller.setZoom(15.0)
                                }
                            },
                            update = { mapView ->
                                mapView.overlays.clear()
                                
                                // DO NOT use 0,0 as a valid fallback for missing coordinates.
                                if (ngoLat != 0.0 && ngoLon != 0.0 && ngoLat in -90.0..90.0 && ngoLon in -180.0..180.0) {
                                    val ngoPoint = org.osmdroid.util.GeoPoint(ngoLat, ngoLon)
                                    val ngoMarker = org.osmdroid.views.overlay.Marker(mapView)
                                    ngoMarker.position = ngoPoint
                                    ngoMarker.title = selectedNgoState.name
                                    mapView.overlays.add(ngoMarker)
                                    mapView.controller.animateTo(ngoPoint)
                                }
                                
                                if (inspectorLat != null && inspectorLon != null && inspectorLat != 0.0 && inspectorLon != 0.0 && inspectorLat!! in -90.0..90.0 && inspectorLon!! in -180.0..180.0) {
                                    val insPoint = org.osmdroid.util.GeoPoint(inspectorLat!!, inspectorLon!!)
                                    val insMarker = org.osmdroid.views.overlay.Marker(mapView)
                                    insMarker.position = insPoint
                                    insMarker.title = "Inspector"
                                    mapView.overlays.add(insMarker)
                                }
                                
                                mapView.invalidate()
                            }
                        )

                        Text(
                            text = "${selectedNgoState.location.city}, ${selectedNgoState.location.state}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkSecondary,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(LightSurface)
                                .border(1.dp, LightBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = TextDarkPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    val cardStatusTitle: String
                    val cardStatusSubtitle: String
                    val cardStatusColor: Color
                    val cardStatusIcon: androidx.compose.ui.graphics.vector.ImageVector
                    val cardContainerColor: Color

                    when (val state = gpsState) {
                        is com.drishti360.app.utils.InspectorGpsState.CheckingLocation -> {
                            cardStatusTitle = "Checking Location"
                            cardStatusSubtitle = "Acquiring high-accuracy GPS fix..."
                            cardStatusColor = PrimaryBlue
                            cardStatusIcon = Icons.Default.MyLocation
                            cardContainerColor = LightSurface
                        }
                        is com.drishti360.app.utils.InspectorGpsState.ValidFix -> {
                            if (state.isWithinGeofence) {
                                cardStatusTitle = "Location Verified"
                                cardStatusSubtitle = "Within geofence (${state.distanceMeters}m) - ${state.ngoAddress}"
                                cardStatusColor = RiskLowGreen
                                cardStatusIcon = Icons.Default.CheckCircle
                                cardContainerColor = RiskLowBg
                            } else {
                                cardStatusTitle = "Outside Geofence"
                                cardStatusSubtitle = "Outside geofence (${state.distanceMeters}m)"
                                cardStatusColor = RiskHighRed
                                cardStatusIcon = Icons.Default.Warning
                                cardContainerColor = RiskHighBg
                            }
                        }
                        is com.drishti360.app.utils.InspectorGpsState.PermissionDenied -> {
                            cardStatusTitle = "Permission Denied"
                            cardStatusSubtitle = "Location permission is required to verify your presence."
                            cardStatusColor = RiskHighRed
                            cardStatusIcon = Icons.Default.Warning
                            cardContainerColor = LightSurface
                        }
                        is com.drishti360.app.utils.InspectorGpsState.LocationServicesDisabled -> {
                            cardStatusTitle = "Location Unavailable"
                            cardStatusSubtitle = "Please turn on location services."
                            cardStatusColor = Color.Gray
                            cardStatusIcon = Icons.Default.Warning
                            cardContainerColor = LightSurface
                        }
                        is com.drishti360.app.utils.InspectorGpsState.LocationUnavailable -> {
                            cardStatusTitle = "Location Unavailable"
                            cardStatusSubtitle = "Unable to get your current location"
                            cardStatusColor = Color.Gray
                            cardStatusIcon = Icons.Default.Warning
                            cardContainerColor = LightSurface
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardStatusColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = cardStatusIcon, contentDescription = null, tint = cardStatusColor, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(cardStatusTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cardStatusColor)
                                Text(cardStatusSubtitle, fontSize = 11.sp, color = cardStatusColor)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = LightSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            GridDetailRow("City", "${selectedNgoState.location.city}, ${selectedNgoState.location.state}")
                            GridDetailRow("Address", selectedNgoState.location.address)
                            GridDetailRow("Latitude", "${selectedNgoState.location.latitude}")
                            GridDetailRow("Longitude", "${selectedNgoState.location.longitude}")
                            GridDetailRow("Risk Level", selectedNgoState.riskScore?.let { "${it.level.name} (${it.score})" } ?: "Not Assessed")
                            if (gpsState is com.drishti360.app.utils.InspectorGpsState.ValidFix) {
                                val fix = gpsState as com.drishti360.app.utils.InspectorGpsState.ValidFix
                                GridDetailRow("Accuracy", "${kotlin.math.round(fix.accuracyMeters).toInt()} m")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryActionButton(
                        text = "Continue to Live Call",
                        onClick = { activeStep = 2 }
                    )
                }
            }

            // Step 2: Real WebRTC Live Call
            if (activeStep == 2) {
                item {
                    val inspId = "insp-${selectedNgoState.id}"
                    RealWebRtcCallContent(
                        inspectionId = inspId,
                        ngoName = selectedNgoState.name,
                        onContinueToChecklist = { activeStep = 3 }
                    )
                }
            }

            // Step 3: Check (Interactive Checklist Switches)
            if (activeStep == 3) {
                item {
                    Text("Interactive Inspection Checklist", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = LightSurface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            ChecklistSwitchRow("Location verified", isLocationVerified) { viewModel.toggleChecklistItem("location") }
                            ChecklistSwitchRow("CCTV functional", isCctvFunctional) { viewModel.toggleChecklistItem("cctv") }
                            ChecklistSwitchRow("Records verified", isRecordsVerified) { viewModel.toggleChecklistItem("records") }
                            ChecklistSwitchRow("Staff present", isStaffPresent) { viewModel.toggleChecklistItem("staff") }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryActionButton(
                        text = "Continue to Report",
                        onClick = {
                            viewModel.verifyLocation()
                            onVerifyLocation(selectedNgoState.id)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StepItem(number: String, title: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isActive) PrimaryBlue else Color(0xFFCBD5E1)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) PrimaryBlue else TextDarkSecondary
        )
    }
}

@Composable
private fun StepDivider() {
    Box(
        modifier = Modifier
            .width(16.dp)
            .height(1.dp)
            .background(Color(0xFFCBD5E1))
    )
}

@Composable
private fun GridDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextDarkSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDarkPrimary)
    }
}

@Composable
private fun ChecklistSwitchRow(title: String, isChecked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextDarkPrimary)
        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RealWebRtcCallContent — Full two-party WebRTC call (Android ↔ any participant)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RealWebRtcCallContent(
    inspectionId: String,
    ngoName: String,
    onContinueToChecklist: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var callState by remember { mutableStateOf("Requesting permissions...") }
    var userFacingErrorMessage by remember { mutableStateOf<String?>(null) }
    var isMuted by remember { mutableStateOf(false) }
    var isCamOff by remember { mutableStateOf(false) }

    // Track readiness of both renderers — effect must not start until both are available
    var localSvr by remember { mutableStateOf<org.webrtc.SurfaceViewRenderer?>(null) }
    var remoteSvr by remember { mutableStateOf<org.webrtc.SurfaceViewRenderer?>(null) }

    // Keep WebRtcManager and SignalingClient in refs so controls can reach them
    val webRtcManagerRef = remember { androidx.compose.runtime.mutableStateOf<com.drishti360.app.webrtc.WebRtcManager?>(null) }
    val signalingClientRef = remember { androidx.compose.runtime.mutableStateOf<com.drishti360.app.webrtc.SignalingClient?>(null) }

    // Glare guard: only one side creates the PeerConnection+Offer
    val peerConnectionCreated = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    // ── Permission handling ──────────────────────────────────────────────────
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val camGranted = perms[android.Manifest.permission.CAMERA] == true
        val audGranted = perms[android.Manifest.permission.RECORD_AUDIO] == true
        hasCameraPermission = camGranted
        hasAudioPermission = audGranted
        Log.d(TAG_CALL, "Permission result: camera=$camGranted, audio=$audGranted")
        if (!camGranted) callState = "Camera permission required"
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            Log.d(TAG_CALL, "Requesting camera and audio permissions...")
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO
                )
            )
        } else {
            callState = "Initializing camera..."
        }
    }

    // ── Core WebRTC setup — only when BOTH renderers are ready + permissions granted ──
    DisposableEffect(hasCameraPermission, hasAudioPermission, localSvr, remoteSvr) {
        val localView = localSvr
        val remoteView = remoteSvr

        if (!hasCameraPermission || localView == null || remoteView == null) {
            Log.d(TAG_CALL, "DisposableEffect skipped: camera=$hasCameraPermission local=${localView != null} remote=${remoteView != null}")
            return@DisposableEffect onDispose { }
        }

        Log.d(TAG_CALL, "DisposableEffect fired: initializing WebRTC for session $inspectionId")
        peerConnectionCreated.set(false)

        val mgr = com.drishti360.app.webrtc.WebRtcManager(context)
        webRtcManagerRef.value = mgr

        // ── Camera event callbacks ───────────────────────────────────────────
        mgr.cameraEventListener = object : com.drishti360.app.webrtc.WebRtcManager.CameraEventListener {
            override fun onCameraOpening(deviceName: String) {
                Log.d(TAG_CALL, "[Camera] Opening: device=$deviceName")
                callState = "Starting camera..."
                userFacingErrorMessage = null
            }
            override fun onFirstFrameAvailable() {
                Log.d(TAG_CALL, "[Camera] First local frame available — preview live")
                if (callState.startsWith("Starting camera") || callState.startsWith("Initializing")) {
                    callState = "Waiting for remote participant..."
                }
            }
            override fun onCameraClosed() {
                Log.d(TAG_CALL, "[Camera] Hardware closed")
            }
            override fun onCameraError(errorDescription: String) {
                Log.e(TAG_CALL, "[Camera] Error: $errorDescription")
                callState = "Camera unavailable"
                userFacingErrorMessage = "Unable to start the camera. Close other apps using the camera and try again. ($errorDescription)"
            }
        }

        // ── Initialize renderers (must happen before startLocalMedia) ────────
        Log.d(TAG_CALL, "[Renderer] Calling initLocalRenderers(localView=$localView, remoteView=$remoteView)")
        mgr.initLocalRenderers(localView, remoteView)

        // ── Start camera + mic ───────────────────────────────────────────────
        Log.d(TAG_CALL, "[Media] Starting local media (camera + mic)...")
        callState = "Initializing camera..."
        val mediaStarted = mgr.startLocalMedia()
        if (!mediaStarted) {
            Log.e(TAG_CALL, "[Media] startLocalMedia() returned false")
            callState = "Camera unavailable"
            userFacingErrorMessage = "Unable to start the camera. Check camera permissions and try again."
        } else {
            Log.d(TAG_CALL, "[Media] startLocalMedia() succeeded")
            callState = "Waiting for remote participant..."
        }

        // ── Build a stable reference so signaling callbacks can reach sendXxx ──
        var sigClientInternal: com.drishti360.app.webrtc.SignalingClient? = null

        // ── Helper: build a PeerConnectionListener with full logging ─────────
        fun buildPcListener(): com.drishti360.app.webrtc.WebRtcManager.PeerConnectionListener {
            return object : com.drishti360.app.webrtc.WebRtcManager.PeerConnectionListener {
                override fun onIceCandidateCreated(candidate: org.webrtc.IceCandidate) {
                    Log.d(TAG_CALL, "[ICE] Local ICE candidate generated — sdpMid=${candidate.sdpMid}, sending to signaling")
                    sigClientInternal?.sendIceCandidate(candidate)
                }
                override fun onConnectionStateChanged(state: org.webrtc.PeerConnection.PeerConnectionState) {
                    Log.d(TAG_CALL, "[PeerConnection] State changed → $state")
                    when (state) {
                        org.webrtc.PeerConnection.PeerConnectionState.CONNECTED -> {
                            Log.d(TAG_CALL, "[PeerConnection] CONNECTED — media pipeline established")
                            callState = "Connected"
                        }
                        org.webrtc.PeerConnection.PeerConnectionState.DISCONNECTED -> callState = "Reconnecting..."
                        org.webrtc.PeerConnection.PeerConnectionState.FAILED -> {
                            Log.e(TAG_CALL, "[PeerConnection] FAILED — check ICE/STUN")
                            callState = "Connection failed"
                            userFacingErrorMessage = "Unable to establish video connection. Check network."
                        }
                        else -> Log.d(TAG_CALL, "[PeerConnection] Intermediate state: $state")
                    }
                }
                override fun onRemoteTrackReceived() {
                    Log.d(TAG_CALL, "[Video] Remote video track received and attached to remote renderer")
                    callState = "Connected"
                }
            }
        }

        // ── Signaling listener ───────────────────────────────────────────────
        val sigClient = com.drishti360.app.webrtc.SignalingClient(object : com.drishti360.app.webrtc.SignalingClient.SignalingListener {

            override fun onJoined(participantId: String, participantCount: Int) {
                Log.d(TAG_CALL, "[Signaling] Joined room '$inspectionId' as '$participantId'. Participants in room: $participantCount")
                if (participantCount > 1) {
                    // Room already has another peer — we are the OFFERER
                    if (peerConnectionCreated.compareAndSet(false, true)) {
                        Log.d(TAG_CALL, "[Signaling] Room has $participantCount peers — we are offerer, creating PeerConnection + offer")
                        callState = "Connecting..."
                        mgr.createPeerConnection(listener = buildPcListener())
                        mgr.createOffer { offer ->
                            Log.d(TAG_CALL, "[SDP] Sending offer to signaling server")
                            sigClientInternal?.sendOffer(offer)
                        }
                    }
                } else {
                    Log.d(TAG_CALL, "[Signaling] We are the first peer — waiting for remote participant...")
                    callState = "Waiting for remote participant..."
                }
            }

            override fun onParticipantJoined(participantId: String) {
                Log.d(TAG_CALL, "[Signaling] Remote participant joined: '$participantId'")
                // We were waiting (first in room). We become the OFFERER now.
                if (peerConnectionCreated.compareAndSet(false, true)) {
                    Log.d(TAG_CALL, "[Signaling] We are offerer — creating PeerConnection + offer for new participant '$participantId'")
                    callState = "Connecting..."
                    mgr.createPeerConnection(listener = buildPcListener())
                    mgr.createOffer { offer ->
                        Log.d(TAG_CALL, "[SDP] Sending offer to remote participant '$participantId'")
                        sigClientInternal?.sendOffer(offer)
                    }
                } else {
                    Log.w(TAG_CALL, "[Signaling] PeerConnection already created — skipping duplicate offer (glare guard) for '$participantId'")
                }
            }

            override fun onOfferReceived(sdp: org.webrtc.SessionDescription) {
                Log.d(TAG_CALL, "[SDP] Offer received — we are ANSWERER, creating PeerConnection + answer")
                // Answerer side: mark as created so onParticipantJoined glare guard blocks
                peerConnectionCreated.set(true)
                mgr.createPeerConnection(listener = buildPcListener())
                mgr.setRemoteDescription(sdp) {
                    Log.d(TAG_CALL, "[SDP] Remote description set — creating answer")
                    mgr.createAnswer { answer ->
                        Log.d(TAG_CALL, "[SDP] Sending answer to signaling server")
                        sigClientInternal?.sendAnswer(answer)
                    }
                }
            }

            override fun onAnswerReceived(sdp: org.webrtc.SessionDescription) {
                Log.d(TAG_CALL, "[SDP] Answer received — setting remote description")
                mgr.setRemoteDescription(sdp)
            }

            override fun onIceCandidateReceived(candidate: org.webrtc.IceCandidate) {
                Log.d(TAG_CALL, "[ICE] Remote ICE candidate received — sdpMid=${candidate.sdpMid}")
                mgr.addIceCandidate(candidate)
            }

            override fun onParticipantLeft(participantId: String) {
                Log.d(TAG_CALL, "[Signaling] Participant left: '$participantId'")
                callState = "Call ended"
            }

            override fun onError(message: String) {
                Log.e(TAG_CALL, "[Signaling] Error: $message")
                callState = "Connection failed"
                userFacingErrorMessage = "Unable to connect to the inspection server. Check your network connection."
            }

            override fun onDisconnected() {
                Log.w(TAG_CALL, "[Signaling] WebSocket disconnected")
                if (callState != "Call ended") {
                    callState = "Disconnected"
                }
            }
        })

        sigClientInternal = sigClient
        signalingClientRef.value = sigClient

        Log.d(TAG_CALL, "[Signaling] Connecting WebSocket to ${com.drishti360.app.config.AppConfig.WS_SIGNALING_URL}, room=$inspectionId, participantId=inspector-android")
        sigClient.connect(
            wsUrl = com.drishti360.app.config.AppConfig.WS_SIGNALING_URL,
            inspectionId = inspectionId,
            participantId = "inspector-android"
        )

        onDispose {
            Log.d(TAG_CALL, "[Cleanup] Disposing WebRTC resources for session $inspectionId")
            try { signalingClientRef.value?.disconnect() } catch (_: Exception) {}
            try { webRtcManagerRef.value?.close() } catch (_: Exception) {}
            signalingClientRef.value = null
            webRtcManagerRef.value = null
            peerConnectionCreated.set(false)
        }
    }

    // ── Permission gate ──────────────────────────────────────────────────────
    if (!hasCameraPermission) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = LightSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Camera Permission Required",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Camera permission is required to start the inspection call.",
                    fontSize = 12.sp,
                    color = TextDarkSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                PrimaryActionButton(
                    text = "Grant Camera Permission",
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.CAMERA,
                                android.Manifest.permission.RECORD_AUDIO
                            )
                        )
                    }
                )
            }
        }
    } else {
        Column {
            // ── Call status header ───────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = LightSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live Inspection Call",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                        Text(
                            text = ngoName,
                            fontSize = 11.sp,
                            color = TextDarkSecondary
                        )
                    }
                    // Colored state pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                when (callState) {
                                    "Connected" -> RiskLowGreen.copy(alpha = 0.2f)
                                    "Call ended", "Camera unavailable", "Connection failed" -> RiskHighRed.copy(alpha = 0.2f)
                                    else -> PrimaryBlue.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = callState,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (callState) {
                                "Connected" -> RiskLowGreen
                                "Call ended", "Camera unavailable", "Connection failed" -> RiskHighRed
                                else -> PrimaryBlue
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Main video surface ───────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    // Remote video (full background)
                    com.drishti360.app.webrtc.WebRtcVideoView(
                        onViewReady = { renderer ->
                            Log.d(TAG_CALL, "[Renderer] Remote SurfaceViewRenderer ready")
                            remoteSvr = renderer
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay when not yet connected
                    if (callState != "Connected") {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = callState,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            userFacingErrorMessage?.let { errText ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = errText,
                                    fontSize = 11.sp,
                                    color = RiskHighRed,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            // Show room info only — no raw server URL in production UI
                            if (callState == "Waiting for remote participant...") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Room: $inspectionId",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "The NGO representative must join this inspection room to connect.",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Local preview (PIP — top-right)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .width(100.dp)
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, PrimaryBlue, RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        com.drishti360.app.webrtc.WebRtcVideoView(
                            onViewReady = { renderer ->
                                Log.d(TAG_CALL, "[Renderer] Local SurfaceViewRenderer ready")
                                localSvr = renderer
                            },
                            isOverlay = true,
                            mirror = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Call controls (bottom-center)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Mute mic
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) RiskHighRed else Color.White.copy(alpha = 0.25f))
                                .clickable {
                                    isMuted = !isMuted
                                    webRtcManagerRef.value?.toggleMute(isMuted)
                                    Log.d(TAG_CALL, "[Control] Mic muted=$isMuted")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute Microphone",
                                tint = Color.White
                            )
                        }

                        // End call
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(RiskHighRed)
                                .clickable {
                                    Log.d(TAG_CALL, "[Control] End call pressed")
                                    signalingClientRef.value?.disconnect()
                                    webRtcManagerRef.value?.close()
                                    signalingClientRef.value = null
                                    webRtcManagerRef.value = null
                                    callState = "Call ended"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White
                            )
                        }

                        // Toggle camera
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isCamOff) RiskHighRed else Color.White.copy(alpha = 0.25f))
                                .clickable {
                                    isCamOff = !isCamOff
                                    webRtcManagerRef.value?.toggleCamera(isCamOff)
                                    Log.d(TAG_CALL, "[Control] Camera off=$isCamOff")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Toggle Camera",
                                tint = Color.White
                            )
                        }

                        // Switch camera
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .clickable {
                                    webRtcManagerRef.value?.switchCamera()
                                    Log.d(TAG_CALL, "[Control] Switch camera pressed")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryActionButton(
                text = "Continue to Checklist",
                onClick = onContinueToChecklist
            )
        }
    }
}
