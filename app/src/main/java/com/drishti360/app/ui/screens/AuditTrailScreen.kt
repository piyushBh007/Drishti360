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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.data.models.AuditEvent
import com.drishti360.app.ui.theme.LightBackground
import com.drishti360.app.ui.theme.LightBorder
import com.drishti360.app.ui.theme.LightSurface
import com.drishti360.app.ui.theme.PrimaryBlue
import com.drishti360.app.ui.theme.RiskHighRed
import com.drishti360.app.ui.theme.TextDarkPrimary
import com.drishti360.app.ui.theme.TextDarkSecondary
import com.drishti360.app.ui.viewmodels.AuditViewModel

@Composable
fun AuditTrailScreen(
    viewModel: AuditViewModel,
    onBackToCommandCenter: () -> Unit
) {
    val auditEvents by viewModel.auditEvents.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    var selectedEvent by remember { mutableStateOf<AuditEvent?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAuditTrail()
    }

    val filteredEvents = when (selectedFilter) {
        "Inspections" -> auditEvents.filter { it.iconType == "SUBMIT" || it.title.contains("Inspection", ignoreCase = true) }
        "Alerts" -> auditEvents.filter { it.iconType == "TRIGGER" || it.title.contains("Alert", ignoreCase = true) || it.title.contains("Anomaly", ignoreCase = true) }
        "System" -> auditEvents.filter { it.iconType != "SUBMIT" && it.iconType != "TRIGGER" && !it.title.contains("Inspection", ignoreCase = true) && !it.title.contains("Alert", ignoreCase = true) && !it.title.contains("Anomaly", ignoreCase = true) }
        else -> auditEvents
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
            IconButton(onClick = onBackToCommandCenter) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDarkPrimary
                )
            }
            Text(
                text = "Audit",
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
            // Filter Chips (All | Inspections | Alerts | System)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip("All", selectedFilter == "All") { viewModel.setFilter("All") }
                    FilterChip("Inspections", selectedFilter == "Inspections") { viewModel.setFilter("Inspections") }
                    FilterChip("Alerts", selectedFilter == "Alerts") { viewModel.setFilter("Alerts") }
                    FilterChip("System", selectedFilter == "System") { viewModel.setFilter("System") }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Vertical Timeline Items
            itemsIndexed(filteredEvents) { index, item ->
                TimelineNodeRow(
                    item = item,
                    isLast = index == filteredEvents.size - 1,
                    onClick = { selectedEvent = item }
                )
            }
        }
    }

    // Detail Dialog when audit item is clicked
    selectedEvent?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedEvent = null },
            title = { Text(item.title, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("• Event ID: ${item.id}")
                    Text("• Timestamp: ${formatAuditTimestamp(item.time)}")
                    Text("• Details: ${item.description}")
                    Text("• Actor: ${item.actor}")
                    Text("• Audit Seal: SHA256:0x" + item.id.hashCode().toString(16))
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEvent = null }) {
                    Text("Close", color = PrimaryBlue)
                }
            }
        )
    }
}

fun formatAuditTimestamp(rawTimestamp: String): String {
    if (rawTimestamp.isBlank()) return ""
    val trimmed = rawTimestamp.trim()

    val isoPatterns = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    for (pattern in isoPatterns) {
        try {
            val parser = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(trimmed)
            if (date != null) {
                val outFormat = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getDefault()
                }
                return outFormat.format(date)
            }
        } catch (_: Exception) {}
    }

    val clean = trimmed.replace("·", ",").replace("  ", " ")
    val userPatterns = arrayOf(
        "dd MMM yyyy, h:mm:ss a",
        "dd MMM yyyy, hh:mm:ss a",
        "dd MMM yyyy, h:mm a",
        "dd MMM yyyy, hh:mm a"
    )
    for (pattern in userPatterns) {
        try {
            val parser = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
            val date = parser.parse(clean)
            if (date != null) {
                val outFormat = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US)
                return outFormat.format(date)
            }
        } catch (_: Exception) {}
    }

    try {
        val parser = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
        val timeDate = parser.parse(trimmed)
        if (timeDate != null) {
            val cal = java.util.Calendar.getInstance()
            val timeCal = java.util.Calendar.getInstance().apply { time = timeDate }
            cal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY))
            cal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE))
            cal.set(java.util.Calendar.SECOND, timeCal.get(java.util.Calendar.SECOND))
            val outFormat = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US)
            return outFormat.format(cal.time)
        }
    } catch (_: Exception) {}

    return clean
}

@Composable
private fun FilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) PrimaryBlue else LightSurface)
            .border(1.dp, if (isSelected) PrimaryBlue else LightBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else TextDarkSecondary
        )
    }
}

@Composable
private fun TimelineNodeRow(item: AuditEvent, isLast: Boolean, onClick: () -> Unit) {
    val icon = when (item.iconType) {
        "SUBMIT" -> Icons.Default.Assignment
        "TRIGGER" -> Icons.Default.Warning
        else -> Icons.Default.BarChart
    }
    val iconColor = if (item.iconType == "TRIGGER") RiskHighRed else PrimaryBlue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.Top
    ) {
        // Vertical Line & Dot Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(56.dp)
                        .background(PrimaryBlue.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Event Card Detail
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(LightSurface)
                    .border(1.dp, LightBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Text(
                    text = formatAuditTimestamp(item.time),
                    fontSize = 11.sp,
                    color = TextDarkSecondary
                )
                Text(
                    text = "${item.actor} • ${item.description}",
                    fontSize = 10.sp,
                    color = TextDarkSecondary
                )
            }
        }
    }
}
