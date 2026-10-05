package com.drishti360.app.ui.screens

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import android.widget.VideoView
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.ui.components.VideoPlayerCanvas
import com.drishti360.app.ui.theme.LightBackground
import com.drishti360.app.ui.theme.LightBorder
import com.drishti360.app.ui.theme.LightSurface
import com.drishti360.app.ui.theme.PrimaryBlue
import com.drishti360.app.ui.theme.RiskHighRed
import com.drishti360.app.ui.theme.RiskLowGreen
import com.drishti360.app.ui.theme.RiskMediumAmber
import com.drishti360.app.ui.theme.TextDarkPrimary
import androidx.compose.runtime.collectAsState
import com.drishti360.app.data.models.CCTVStatus
import com.drishti360.app.ui.theme.TextDarkSecondary
import com.drishti360.app.ui.viewmodels.CctvViewModel
import java.util.Locale

@Composable
fun CctvScreen(
    viewModel: CctvViewModel,
    onBack: () -> Unit = {}
) {
    val cameraFeeds by viewModel.cameraFeeds.collectAsState()
    var selectedCam by remember { mutableStateOf(0) } // 0: Cam 01, 1: Cam 02, 2: Cam 03
    var showMenu by remember { mutableStateOf(false) }
    var showCameraInfoDialog by remember { mutableStateOf(false) }
    var showAllEventsDialog by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    // Current-frame YOLO analysis (independent from historical Video Avg People)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cctvVideoView by remember { mutableStateOf<VideoView?>(null) }
    var analysisState by remember { mutableStateOf<FrameAnalysisState>(FrameAnalysisState.Idle) }

    val currentCam = cameraFeeds.getOrNull(selectedCam)
    val isRecordedDemo = selectedCam == 0 || selectedCam == 1 || currentCam?.source == "LOCAL_VIDEO" || currentCam?.type == "LOCAL_DEMO"
    val isLiveIp = !isRecordedDemo && (currentCam?.source == "RTSP" || currentCam?.type == "IP_CAMERA") && currentCam?.state == com.drishti360.app.data.models.CameraState.ONLINE && !currentCam.streamUrl.isNullOrBlank()
    val isOffline = !isRecordedDemo && !isLiveIp

    val feedBadgeText = when {
        isRecordedDemo -> "RECORDED FEED"
        isLiveIp -> "LIVE IP CAMERA"
        else -> "OFFLINE"
    }

    val feedBadgeColor = when {
        isLiveIp -> RiskLowGreen
        isRecordedDemo -> PrimaryBlue
        else -> RiskHighRed
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightSurface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
                text = "CCTV",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextDarkPrimary
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Refresh Feed") },
                        onClick = {
                            showMenu = false
                            viewModel.loadCameraFeeds()
                            refreshKey++
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Info") },
                        onClick = {
                            showMenu = false
                            showCameraInfoDialog = true
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Camera Selector Pills (Cam 01 | Cam 02 | Cam 03)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CamPill("Cam 01", selectedCam == 0, Modifier.weight(1f)) { selectedCam = 0; analysisState = FrameAnalysisState.Idle }
                    CamPill("Cam 02", selectedCam == 1, Modifier.weight(1f)) { selectedCam = 1; analysisState = FrameAnalysisState.Idle }
                    CamPill("Cam 03", selectedCam == 2, Modifier.weight(1f)) { selectedCam = 2; analysisState = FrameAnalysisState.Idle }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // CCTV Video Canvas / Feed Container
            item {
                Column {
                    // Feed Status Badge Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentCam?.cameraName ?: "Cam 0${selectedCam + 1}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                        Box(
                            modifier = Modifier
                                .background(feedBadgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = feedBadgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = feedBadgeColor
                            )
                        }
                    }

                    if (isRecordedDemo) {
                        // Cam 01 / Cam 02: Plays real bundled video file (cctv_demo.mp4 or cctv_demo_2.mp4)
                        key(selectedCam, refreshKey) {
                            val videoResName = if (selectedCam == 1) "cctv_demo_2" else "cctv_demo"
                            VideoPlayerCanvas(
                                videoResName = videoResName,
                                onVideoViewCreated = { cctvVideoView = it }
                            )
                        }
                    } else if (isLiveIp) {
                        // Real IP Camera with configured RTSP Stream
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                                .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "RTSP STREAM CONNECTED",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RiskLowGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentCam?.streamUrl ?: "rtsp://configured-ip-camera",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    } else {
                        // Offline Camera State Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                                .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = LightSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideocamOff,
                                    contentDescription = "Offline",
                                    tint = RiskHighRed,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${currentCam?.cameraName ?: "Camera 0" + (selectedCam + 1)} Feed Offline",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                                Text(
                                    text = if (currentCam?.source == "RTSP") "No RTSP stream URL configured in environment." else "No video signal received from sensor.",
                                    fontSize = 11.sp,
                                    color = TextDarkSecondary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Current-frame YOLO analysis (Cam 01 / Cam 02 recorded feeds)
            if (isRecordedDemo) {
                item {
                    FrameAnalysisPanel(
                        state = analysisState,
                        onAnalyze = {
                            Log.d("CCTV_ANALYZE", "Analyze Fresh Frame clicked (cam index $selectedCam)")
                            if (analysisState !is FrameAnalysisState.Analyzing) {
                                analysisState = FrameAnalysisState.Analyzing
                                val resName = if (selectedCam == 1) "cctv_demo_2" else "cctv_demo"
                                val camId = if (selectedCam == 1) "camera-002" else "camera-001"
                                scope.launch {
                                    analysisState = FrameAnalyzer.analyze(context, cctvVideoView, resName, camId)
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // AI Check / AI Analysis Section Card matching reference layout
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "AI Check",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val yoloLabel: String
                        val yoloVal: String
                        if (currentCam?.personCount != null) {
                            yoloLabel = "Video People (historical)"
                            yoloVal = "${currentCam.personCount}"
                        } else if (currentCam?.averagePersonCount != null) {
                            yoloLabel = "Video Avg People (historical)"
                            yoloVal = String.format(Locale.US, "%.2f", currentCam.averagePersonCount)
                        } else {
                            yoloLabel = "Video Avg People (historical)"
                            yoloVal = "Unavailable"
                        }

                        AiMetricRow(yoloLabel, yoloVal)
                        AiMetricRow("Anomaly", if (isRecordedDemo) "● Medium" else if (isLiveIp) "● Low" else "● None", color = if (isRecordedDemo) RiskMediumAmber else RiskLowGreen)
                        AiMetricRow("Crowd", if (!isOffline) "● Normal" else "● Offline", color = if (!isOffline) RiskLowGreen else TextDarkSecondary)
                        AiMetricRow("Status", "● $feedBadgeText", color = feedBadgeColor)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Events Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Events",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "See all",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue,
                        modifier = Modifier.clickable { showAllEventsDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        EventRow("Crowding detected", "10:22 AM", RiskMediumAmber)
                        EventRow("Motion anomaly", "09:18 AM", RiskHighRed)
                        EventRow("Normal activity", "08:45 AM", RiskLowGreen)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Camera Info Dialog
    if (showCameraInfoDialog) {
        val camName = currentCam?.cameraName ?: "Cam 0${selectedCam + 1}"
        val isLocal = selectedCam == 0 || selectedCam == 1 || currentCam?.source == "LOCAL_VIDEO"
        val statusText = if (isLocal) "Online" else if (currentCam?.state == com.drishti360.app.data.models.CameraState.ONLINE) "Online" else "Offline"
        val sourceText = if (isLocal) "Local Video" else (currentCam?.source ?: "RTSP")
        val typeText = if (isLocal) "Recorded Feed" else (currentCam?.type ?: "IP Camera")
        val protocolText = if (isLocal) "Local File" else (currentCam?.protocol ?: "RTSP")

        AlertDialog(
            onDismissRequest = { showCameraInfoDialog = false },
            title = { Text("$camName Information", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("• Camera: $camName")
                    Text("• Status: $statusText")
                    Text("• Source: $sourceText")
                    Text("• Type: $typeText")
                    Text("• Protocol: $protocolText")
                    if (isLocal) {
                        val file = if (selectedCam == 1) "cctv_demo_2.mp4" else "cctv_demo.mp4"
                        Text("• Bundled File: $file")
                    } else if (!currentCam?.streamUrl.isNullOrBlank()) {
                        Text("• Stream URL: ${currentCam.streamUrl}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCameraInfoDialog = false }) {
                    Text("OK", color = PrimaryBlue)
                }
            }
        )
    }

    // All Events Dialog
    if (showAllEventsDialog) {
        AlertDialog(
            onDismissRequest = { showAllEventsDialog = false },
            title = { Text("Recent Camera Events Log", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    EventRow("Crowding detected in dining hall", "10:22 AM", RiskMediumAmber)
                    EventRow("Motion anomaly near entrance", "09:18 AM", RiskHighRed)
                    EventRow("Normal activity resumed", "08:45 AM", RiskLowGreen)
                    EventRow("CCTV feed connected", "08:00 AM", PrimaryBlue)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllEventsDialog = false }) {
                    Text("Close", color = PrimaryBlue)
                }
            }
        )
    }
}

@Composable
private fun CamPill(title: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) PrimaryBlue else LightSurface)
            .border(1.dp, if (isSelected) PrimaryBlue else LightBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else TextDarkSecondary
        )
    }
}

@Composable
private fun AiMetricRow(label: String, value: String, color: Color = TextDarkPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextDarkSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun EventRow(title: String, time: String, dotColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextDarkPrimary)
        }
        Text(text = time, fontSize = 11.sp, color = TextDarkSecondary)
    }
}
