package com.drishti360.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.data.models.Evidence
import com.drishti360.app.data.models.NGO
import com.drishti360.app.ui.components.PrimaryActionButton
import com.drishti360.app.ui.theme.LightBackground
import com.drishti360.app.ui.theme.LightBorder
import com.drishti360.app.ui.theme.LightSurface
import com.drishti360.app.ui.theme.PrimaryBlue
import com.drishti360.app.ui.theme.RiskHighRed
import com.drishti360.app.ui.theme.RiskLowBg
import com.drishti360.app.ui.theme.RiskLowGreen
import com.drishti360.app.ui.theme.TextDarkPrimary
import com.drishti360.app.ui.theme.TextDarkSecondary
import com.drishti360.app.ui.viewmodels.InspectionViewModel
import com.drishti360.app.utils.PdfUtils

@Composable
fun InspectionReportScreen(
    ngo: NGO,
    viewModel: InspectionViewModel,
    onBack: () -> Unit = {},
    onSubmitComplete: () -> Unit
) {
    val context = LocalContext.current
    val evidenceList by viewModel.evidenceList.collectAsState()
    val isSubmitted by viewModel.isSubmitted.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val submittedId by viewModel.submittedInspectionId.collectAsState()
    val submittedTimestamp by viewModel.submittedTimestamp.collectAsState()
    val submittedInspection by viewModel.submittedInspection.collectAsState()

    val isLocationVerified by viewModel.locationVerified.collectAsState()
    val isCctvFunctional by viewModel.cctvFunctional.collectAsState()
    val isRecordsVerified by viewModel.recordsVerified.collectAsState()
    val isStaffPresent by viewModel.staffPresent.collectAsState()

    var observationsText by remember {
        mutableStateOf("Facility was operational. Attendance records matched. CCTV streams verified.")
    }

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            viewModel.addPhotoUri(uri.toString())
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
                text = "Report",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Target NGO Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = ngo.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "📍 ${ngo.location.city}, ${ngo.location.state}",
                                fontSize = 11.sp,
                                color = TextDarkSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Meta Info Grid
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Inspector", fontSize = 12.sp, color = TextDarkSecondary)
                            Text("👤 Aarav Mehta", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDarkPrimary)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Overall Status", fontSize = 12.sp, color = TextDarkSecondary)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RiskLowBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isLocationVerified && isRecordsVerified) "SATISFACTORY" else "ISSUES FOUND",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLocationVerified && isRecordsVerified) RiskLowGreen else RiskHighRed
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Observations Section
            item {
                Text(
                    text = "Observations",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = observationsText,
                        fontSize = 12.sp,
                        color = TextDarkSecondary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Photos Section (Photos (${evidenceList.size}) + Photo Picker Add Button)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Photos (${evidenceList.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(evidenceList) { evidence ->
                        PhotoThumbnailItem(evidence = evidence) {
                            viewModel.removeEvidence(evidence.id)
                        }
                    }
                    item {
                        AddPhotoButton {
                            try {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            } catch (e: Exception) {
                                viewModel.capturePhotoEvidence()
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Checklist Section
            item {
                Text(
                    text = "Checklist",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, LightBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        ChecklistRow("Location verified", if (isLocationVerified) "Yes" else "No", isLocationVerified)
                        ChecklistRow("CCTV functional", if (isCctvFunctional) "Yes" else "No", isCctvFunctional)
                        ChecklistRow("Records verified", if (isRecordsVerified) "Yes" else "No", isRecordsVerified)
                        ChecklistRow("Staff present", if (isStaffPresent) "Yes" else "No", isStaffPresent)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                if (isSubmitting) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                } else {
                    PrimaryActionButton(
                        text = "Submit Report",
                        onClick = {
                            viewModel.submitInspection(
                                ngoId = ngo.id,
                                ngoName = ngo.name,
                                observations = observationsText
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Success Dialog - Appears ONLY after successful submit POST to Node.js backend
    if (isSubmitted) {
        val currentInsp = submittedInspection ?: com.drishti360.app.data.models.Inspection(
            id = submittedId,
            ngoId = ngo.id,
            ngoName = ngo.name,
            inspectorName = "Aarav Mehta",
            timestamp = submittedTimestamp,
            status = com.drishti360.app.data.models.InspectionStatus.COMPLETED,
            distanceMeters = submittedInspection?.distanceMeters ?: 0,
            withinGeofence = isLocationVerified,
            beneficiariesObserved = 24,
            staffPresent = 4,
            facilitiesStatus = "Satisfactory",
            cctvCompliance = if (isCctvFunctional) "YES" else "NO",
            observations = observationsText,
            evidenceList = evidenceList
        )

        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = RiskLowGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report Submitted!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("• Inspection ID: $submittedId", fontWeight = FontWeight.SemiBold)
                    Text("• Time: ${submittedTimestamp.ifEmpty { "Server Timestamp" }}")
                    Text("• Geofence: ${if (currentInsp.withinGeofence) "Verified (${currentInsp.distanceMeters}m)" else "Outside (${currentInsp.distanceMeters}m)"}")
                    Text("• Photos Attached: ${evidenceList.size}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inspection record saved to Node.js backend and appended to Audit Trail.",
                        fontSize = 11.sp,
                        color = TextDarkSecondary
                    )
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                val pdfFile = PdfUtils.generateInspectionReportPdf(context, currentInsp, ngo)
                                PdfUtils.savePdfToStorage(context, pdfFile)
                            }
                        ) {
                            Text("Save PDF", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = {
                                val pdfFile = PdfUtils.generateInspectionReportPdf(context, currentInsp, ngo)
                                PdfUtils.sharePdf(context, pdfFile)
                            }
                        ) {
                            Text("Share PDF", color = PrimaryBlue, fontSize = 12.sp)
                        }
                    }
                    PrimaryActionButton(
                        text = "View Audit",
                        onClick = {
                            viewModel.resetSubmissionState()
                            onSubmitComplete()
                        },
                        modifier = Modifier.height(36.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun PhotoThumbnailItem(evidence: Evidence, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF64748B))
            .border(1.dp, LightBorder, RoundedCornerShape(8.dp))
    ) {
        Text(
            text = "📷 ${evidence.title.take(12)}",
            fontSize = 9.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(RiskHighRed)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun AddPhotoButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(LightSurface)
            .border(1.dp, PrimaryBlue, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Text("Add", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
        }
    }
}

@Composable
private fun ChecklistRow(title: String, status: String, isOk: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isOk) RiskLowGreen else RiskHighRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, fontSize = 12.sp, color = TextDarkPrimary)
        }
        Text(text = status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isOk) TextDarkPrimary else RiskHighRed)
    }
}
