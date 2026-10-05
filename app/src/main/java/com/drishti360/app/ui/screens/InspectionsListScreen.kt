package com.drishti360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.data.datasource.RemoteDataSource
import com.drishti360.app.data.models.Inspection
import com.drishti360.app.data.models.InspectionStatus
import com.drishti360.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun InspectionsListScreen(
    remoteDataSource: RemoteDataSource = remember { RemoteDataSource() },
    onBack: () -> Unit,
    onSelectInspection: (Inspection) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var inspections by remember { mutableStateOf<List<Inspection>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadInspections() {
        coroutineScope.launch {
            isLoading = true
            val res = remoteDataSource.fetchInspectionsList()
            if (res.isSuccess) {
                inspections = res.getOrDefault(emptyList())
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadInspections()
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDarkPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Field Inspections",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Text(
                    text = "${inspections.size} Inspection records in system",
                    fontSize = 11.sp,
                    color = TextDarkSecondary
                )
            }
            IconButton(onClick = { loadInspections() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = PrimaryBlue
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (inspections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No inspections recorded",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary
                    )
                    Text(
                        text = "Initiate a surprise inspection from the NGO detail screen.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                items(inspections, key = { it.id }) { insp ->
                    InspectionItemCard(inspection = insp, onClick = { onSelectInspection(insp) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun InspectionItemCard(
    inspection: Inspection,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, LightBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = inspection.id,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryBlue
                    )
                    Text(
                        text = inspection.ngoName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextDarkPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (inspection.status == InspectionStatus.COMPLETED) RiskLowBg else RiskHighBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = inspection.status.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (inspection.status == InspectionStatus.COMPLETED) RiskLowGreen else RiskHighRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "👤 ${inspection.inspectorName}",
                fontSize = 12.sp,
                color = TextDarkSecondary
            )

            if (inspection.observations.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📝 ${inspection.observations}",
                    fontSize = 11.sp,
                    color = TextDarkSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (inspection.withinGeofence) "📍 Geofence Verified (${inspection.distanceMeters}m)" else "📍 Outside Geofence (${inspection.distanceMeters}m)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (inspection.withinGeofence) RiskLowGreen else RiskHighRed
                )
                Text(
                    text = "🕒 ${inspection.timestamp}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
