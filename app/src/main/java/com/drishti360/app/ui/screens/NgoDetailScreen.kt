package com.drishti360.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.data.models.NGO
import com.drishti360.app.data.models.RiskLevel
import com.drishti360.app.ui.components.PrimaryActionButton
import com.drishti360.app.ui.theme.LightBackground
import com.drishti360.app.ui.theme.LightBorder
import com.drishti360.app.ui.theme.LightSurface
import com.drishti360.app.ui.theme.PrimaryBlue
import com.drishti360.app.ui.theme.RiskHighBg
import com.drishti360.app.ui.theme.RiskHighRed
import com.drishti360.app.ui.theme.RiskLowBg
import com.drishti360.app.ui.theme.RiskLowGreen
import com.drishti360.app.ui.theme.RiskMediumBg
import com.drishti360.app.ui.theme.RiskMediumAmber
import com.drishti360.app.ui.theme.TextDarkPrimary
import com.drishti360.app.ui.theme.TextDarkSecondary
import com.drishti360.app.ui.viewmodels.AuthViewModel
import com.drishti360.app.ui.viewmodels.MainViewModel
import com.drishti360.app.utils.PdfUtils
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@Composable
fun NgoDetailScreen(
    ngo: NGO,
    viewModel: MainViewModel,
    authViewModel: AuthViewModel? = null,
    onBack: () -> Unit,
    onEditNgo: () -> Unit = {},
    onInitiateInspection: () -> Unit,
    onRefreshAudit: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: About, 1: History, 2: Docs
    var showMenu by remember { mutableStateOf(false) }

    // PDF Dialog State
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfDialog by remember { mutableStateOf(false) }

    // Anomaly Dialog State
    var showAnomalyDialog by remember { mutableStateOf(false) }
    var anomalyObservation by remember { mutableStateOf("") }
    var isSubmittingAnomaly by remember { mutableStateOf(false) }
    var anomalyResultMsg by remember { mutableStateOf<String?>(null) }

    // Bitmap loaded from backend static media
    var loadedBitmap by remember(ngo.id, ngo.image) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(ngo.id, ngo.image) {
        withContext(Dispatchers.IO) {
            var bmp: Bitmap? = null
            try {
                val cleanName = (ngo.image ?: ngo.id)
                    .replace(".", "_")
                    .replace("-", "_")
                    .lowercase()
                val drawableId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
                if (drawableId != 0) {
                    bmp = BitmapFactory.decodeResource(context.resources, drawableId)
                }
            } catch (e: Exception) {
                bmp = null
            }

            if (bmp == null && !ngo.image.isNullOrBlank()) {
                try {
                    val imageUrl = "${com.drishti360.app.config.AppConfig.HTTP_BASE_URL}/media/ngos/${ngo.image}"
                    val stream = URL(imageUrl).openStream()
                    bmp = BitmapFactory.decodeStream(stream)
                } catch (e: Exception) {
                    bmp = null
                }
            }
            loadedBitmap = bmp
        }
    }

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
                text = "NGO Details",
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
                    val authState = authViewModel?.authState?.collectAsState()?.value
                    val isAdmin = (authState as? AuthViewModel.AuthState.Authenticated)?.role == "admin"
                    if (isAdmin) {
                        DropdownMenuItem(
                            text = { Text("Edit NGO") },
                            onClick = {
                                showMenu = false
                                onEditNgo()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Share NGO Link") },
                        onClick = {
                            showMenu = false
                            PdfUtils.shareNgoLink(context, ngo)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export Audit PDF") },
                        onClick = {
                            showMenu = false
                            val file = PdfUtils.generateNgoAuditPdf(context, ngo)
                            generatedPdfFile = file
                            showPdfDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Report Anomaly") },
                        onClick = {
                            showMenu = false
                            anomalyObservation = ""
                            showAnomalyDialog = true
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
            // Per-NGO Image Visual Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    if (loadedBitmap != null) {
                        Image(
                            bitmap = loadedBitmap!!.asImageBitmap(),
                            contentDescription = ngo.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            drawRect(color = Color(0xFFBAE6FD), size = Size(w, h * 0.65f))
                            drawRect(color = Color(0xFF86EFAC), topLeft = Offset(0f, h * 0.65f), size = Size(w, h * 0.35f))
                            drawRoundRect(
                                color = Color(0xFFD97706),
                                topLeft = Offset(w * 0.15f, h * 0.25f),
                                size = Size(w * 0.7f, h * 0.45f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRect(color = Color(0xFF991B1B), topLeft = Offset(w * 0.12f, h * 0.2f), size = Size(w * 0.76f, h * 0.08f))
                            drawRect(color = Color(0xFFFFFFFF), topLeft = Offset(w * 0.25f, h * 0.35f), size = Size(w * 0.12f, h * 0.15f))
                            drawRect(color = Color(0xFFFFFFFF), topLeft = Offset(w * 0.44f, h * 0.35f), size = Size(w * 0.12f, h * 0.15f))
                            drawRect(color = Color(0xFFFFFFFF), topLeft = Offset(w * 0.63f, h * 0.35f), size = Size(w * 0.12f, h * 0.15f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // NGO Name & Location
            item {
                Text(
                    text = ngo.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Text(
                    text = "📍 ${ngo.location.city}, ${ngo.location.state}",
                    fontSize = 12.sp,
                    color = TextDarkSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Risk Score Section Card
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
                            text = "Risk Score",
                            fontSize = 11.sp,
                            color = TextDarkSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (ngo.riskScore != null) {
                                Text(
                                    text = "${ngo.riskScore.score}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (ngo.riskScore.level) {
                                        RiskLevel.HIGH -> RiskHighRed
                                        RiskLevel.MEDIUM -> RiskMediumAmber
                                        RiskLevel.LOW -> RiskLowGreen
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (ngo.riskScore.level) {
                                                RiskLevel.HIGH -> RiskHighBg
                                                RiskLevel.MEDIUM -> RiskMediumBg
                                                RiskLevel.LOW -> RiskLowBg
                                            }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${ngo.riskScore.level.name} Risk",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (ngo.riskScore.level) {
                                            RiskLevel.HIGH -> RiskHighRed
                                            RiskLevel.MEDIUM -> RiskMediumAmber
                                            RiskLevel.LOW -> RiskLowGreen
                                        }
                                    )
                                }
                            } else {
                                Text(
                                    text = "Not Assessed",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = LightBorder)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Details List
                        val cctvText = if (ngo.cctvTotalCount != null && ngo.cctvTotalCount > 0) "${ngo.cctvOnlineCount}/${ngo.cctvTotalCount} Online" else "Not Configured"
                        val isCctvGreen = ngo.cctvOnlineCount != null && ngo.cctvOnlineCount > 0
                        val isCctvRed = ngo.cctvOnlineCount != null && ngo.cctvOnlineCount == 0

                        val attendanceText = if (ngo.attendanceRate != null) "${ngo.attendanceRate}%" else "No Data"
                        val isAttendanceGreen = ngo.attendanceRate != null && ngo.attendanceRate >= 70
                        val isAttendanceRed = ngo.attendanceRate != null && ngo.attendanceRate < 70

                        val lastInspText = ngo.lastInspectionDate ?: "Never"

                        NgoDetailRow("CCTV Status", cctvText, isGreen = isCctvGreen, isRed = isCctvRed)
                        NgoDetailRow("Attendance", attendanceText, isGreen = isAttendanceGreen, isRed = isAttendanceRed)
                        NgoDetailRow("Last Inspection", lastInspText)
                        NgoDetailRow("Category", ngo.category)
                        NgoDetailRow("Registration No.", ngo.registrationNo)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Tabs Row (About | History | Docs)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LightBorder, RoundedCornerShape(8.dp))
                        .background(LightSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TabButton("About", selectedTab == 0) { selectedTab = 0 }
                    TabButton("History", selectedTab == 1) { selectedTab = 1 }
                    TabButton("Docs", selectedTab == 2) { selectedTab = 2 }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Tab Content
            item {
                if (selectedTab == 0) {
                    Text(
                        text = "About",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ngo.description,
                        fontSize = 12.sp,
                        color = TextDarkSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, LightBorder, RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = LightSurface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${ngo.establishedYear}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                Text("Established", fontSize = 11.sp, color = TextDarkSecondary)
                            }
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, LightBorder, RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = LightSurface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${ngo.beneficiaries}+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                Text("Beneficiaries", fontSize = 11.sp, color = TextDarkSecondary)
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    Text("Inspection History for ${ngo.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (ngo.lastInspectionDate != null) {
                        val issuesFlagged = ngo.riskScore?.level == RiskLevel.HIGH
                        Text("• ${ngo.lastInspectionDate}: Routine Field Audit (${if (issuesFlagged) "Issues Flagged" else "Satisfactory"})", fontSize = 12.sp, color = TextDarkSecondary)
                        Text("• 14 Jun 2026: Biometric Log Verification (Compliance Clear)", fontSize = 12.sp, color = TextDarkSecondary)
                        Text("• 02 Feb 2026: CCTV Network Calibration Check", fontSize = 12.sp, color = TextDarkSecondary)
                    } else {
                        Text("No inspection history available yet.", fontSize = 12.sp, color = TextDarkSecondary)
                    }
                } else {
                    Text("Attached Documents (Docs)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("📄 FCRA Registration Certificate (${ngo.registrationNo}).pdf", fontSize = 12.sp, color = PrimaryBlue)
                    Text("📄 Annual Financial Statement 2025.pdf", fontSize = 12.sp, color = PrimaryBlue)
                    Text("📄 Beneficiary Attendance Log Q2.xlsx", fontSize = 12.sp, color = PrimaryBlue)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Button: Start Inspect
                PrimaryActionButton(
                    text = "Start Inspect",
                    onClick = onInitiateInspection
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // PDF Export Dialog with BOTH Share and Download options
    if (showPdfDialog && generatedPdfFile != null) {
        AlertDialog(
            onDismissRequest = { showPdfDialog = false },
            title = { Text("Audit Report PDF Generated", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("PDF report for ${ngo.name} generated successfully.")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("File: ${generatedPdfFile?.name}", fontSize = 11.sp, color = TextDarkSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPdfDialog = false
                        generatedPdfFile?.let { file ->
                            PdfUtils.savePdfToStorage(context, file)
                        }
                    }
                ) {
                    Text("Save / Download", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPdfDialog = false
                        generatedPdfFile?.let { file ->
                            PdfUtils.sharePdf(context, file)
                        }
                    }
                ) {
                    Text("Share", color = PrimaryBlue)
                }
            }
        )
    }

    // Report Anomaly Interactive Dialog
    if (showAnomalyDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmittingAnomaly) showAnomalyDialog = false },
            title = { Text("Report Anomaly: ${ngo.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter anomaly observations for field investigation:", fontSize = 12.sp, color = TextDarkSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = anomalyObservation,
                        onValueChange = { anomalyObservation = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = { Text("e.g., Unannounced closure during feeding hours...", fontSize = 12.sp) }
                    )
                }
            },
            confirmButton = {
                if (isSubmittingAnomaly) {
                    CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(24.dp))
                } else {
                    TextButton(
                        onClick = {
                            if (anomalyObservation.isNotBlank()) {
                                isSubmittingAnomaly = true
                                viewModel.reportAnomaly(
                                    ngoId = ngo.id,
                                    ngoName = ngo.name,
                                    details = anomalyObservation,
                                    onSuccess = {
                                        isSubmittingAnomaly = false
                                        showAnomalyDialog = false
                                        anomalyResultMsg = "Anomaly recorded and submitted to backend audit trail."
                                        onRefreshAudit()
                                    },
                                    onError = { err ->
                                        isSubmittingAnomaly = false
                                        anomalyResultMsg = err
                                    }
                                )
                            }
                        }
                    ) {
                        Text("Submit", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAnomalyDialog = false },
                    enabled = !isSubmittingAnomaly
                ) {
                    Text("Cancel", color = TextDarkSecondary)
                }
            }
        )
    }

    // Anomaly result feedback dialog
    anomalyResultMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { anomalyResultMsg = null },
            title = { Text("Anomaly Submission", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { anomalyResultMsg = null }) {
                    Text("OK", color = PrimaryBlue)
                }
            }
        )
    }
}

@Composable
private fun NgoDetailRow(label: String, value: String, isGreen: Boolean = false, isRed: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextDarkSecondary)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                isGreen -> RiskLowGreen
                isRed -> RiskHighRed
                else -> TextDarkPrimary
            }
        )
    }
}

@Composable
private fun TabButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) PrimaryBlue else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 6.dp),
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
