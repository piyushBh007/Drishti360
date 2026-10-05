package com.drishti360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.data.models.Alert
import com.drishti360.app.data.models.AlertSeverity
import com.drishti360.app.ui.theme.*
import com.drishti360.app.ui.viewmodels.MainViewModel

@Composable
fun AlertsListScreen(
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit,
    onSelectNgo: (String) -> Unit = {}
) {
    val alerts by viewModel.alerts.collectAsState()

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
                    text = "System Alerts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
                Text(
                    text = "${alerts.size} Active alerts in monitoring system",
                    fontSize = 11.sp,
                    color = TextDarkSecondary
                )
            }
            IconButton(onClick = { viewModel.refreshAlerts() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = PrimaryBlue
                )
            }
        }

        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No active alerts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkSecondary
                    )
                    Text(
                        text = "All monitored NGO facilities are operating normally.",
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
                items(alerts, key = { it.id }) { alert ->
                    AlertItemCard(alert = alert, onClick = { onSelectNgo(alert.ngoId) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun AlertItemCard(
    alert: Alert,
    onClick: () -> Unit
) {
    val severityColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> RiskHighRed
        AlertSeverity.WARNING -> RiskMediumAmber
        AlertSeverity.INFO -> PrimaryBlue
    }
    val severityBg = when (alert.severity) {
        AlertSeverity.CRITICAL -> RiskHighBg
        AlertSeverity.WARNING -> RiskMediumBg
        AlertSeverity.INFO -> Color(0xFFEFF6FF)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, severityColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(severityColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alert.ngoName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDarkPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(severityBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = alert.severity.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = alert.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDarkPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = alert.message,
                fontSize = 11.sp,
                color = TextDarkSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "🕒 ${alert.timestamp}",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}
