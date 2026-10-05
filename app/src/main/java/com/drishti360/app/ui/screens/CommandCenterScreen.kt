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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.data.models.Alert
import com.drishti360.app.data.models.NGO
import com.drishti360.app.ui.components.MainHeaderBar
import com.drishti360.app.ui.components.NgoCardItem
import com.drishti360.app.ui.components.StatCard
import com.drishti360.app.ui.theme.LightBackground
import com.drishti360.app.ui.theme.LightBorder
import com.drishti360.app.ui.theme.LightSurface
import com.drishti360.app.ui.theme.PrimaryBlue
import com.drishti360.app.ui.theme.RiskHighBg
import com.drishti360.app.ui.theme.RiskHighRed
import com.drishti360.app.ui.theme.TextDarkPrimary
import com.drishti360.app.ui.theme.TextDarkSecondary
import com.drishti360.app.ui.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.drishti360.app.ui.viewmodels.AuthViewModel

@Composable
fun CommandCenterScreen(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel? = null,
    onSelectNgo: (NGO) -> Unit,
    onSeeAllNgos: () -> Unit = {}
) {
    val ngos by viewModel.ngos.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val inspections by viewModel.inspections.collectAsState()
    val overviewMetrics by viewModel.overviewMetrics.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showAlertsDialog by remember { mutableStateOf(false) }
    var selectedAlert by remember { mutableStateOf<Alert?>(null) }

    // Refresh on every resume so the count updates when returning from an inspection
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Live clock updating every second
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("dd MMM yyyy · h:mm:ss a", Locale.getDefault())
        while (true) {
            currentTimeString = formatter.format(Date())
            delay(1000L)
        }
    }

    val ngosCount = overviewMetrics?.ngosMonitored ?: ngos.size
    val highRiskCount = overviewMetrics?.highRisk ?: ngos.count { it.riskScore?.level?.name == "HIGH" || (it.riskScore?.score ?: 0) >= 75 }
    val inspectionsCount = overviewMetrics?.inspections ?: inspections.size
    val priorityNgos = remember(ngos) {
        ngos.sortedByDescending { it.riskScore?.score ?: 0 }.take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        MainHeaderBar(
            title = "Drishti 360",
            subtitle = "Transparency. Accountability. Impact.",
            onProfileClick = { showProfileDialog = true }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Header: Monitoring Overview + Live Date & Time
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Monitoring Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (currentTimeString.isNotEmpty()) currentTimeString else "21 Sep 2026 · 9:17:42 PM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDarkSecondary
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Summary Statistics Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        number = String.format("%02d", ngosCount),
                        label = "NGOs Monitored",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        number = String.format("%02d", highRiskCount),
                        label = "High Risk",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        number = String.format("%02d", inspectionsCount),
                        label = "Inspections",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Priority NGOs Header + Working "See all" navigating to NGO List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Priority NGOs",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "See all",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue,
                        modifier = Modifier.clickable {
                            onSeeAllNgos()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Priority NGO Cards: Clicking individual NGO opens THAT NGO's details
            items(priorityNgos) { ngo ->
                NgoCardItem(
                    ngo = ngo,
                    onClick = {
                        onSelectNgo(ngo)
                    }
                )
            }

            // Recent Alerts Header + Working "See all"
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Alerts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "See all",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue,
                        modifier = Modifier.clickable { showAlertsDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Recent Alert Cards
            items(alerts.take(3)) { alert ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedAlert = alert }
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RiskHighBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = RiskHighRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alert.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.ngoName,
                                fontSize = 11.sp,
                                color = TextDarkSecondary
                            )
                            Text(
                                text = alert.timestamp,
                                fontSize = 10.sp,
                                color = TextDarkSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextDarkSecondary
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Profile Dialog
    if (showProfileDialog) {
        val authState = authViewModel?.authState?.collectAsState()?.value
        val user = authState as? AuthViewModel.AuthState.Authenticated
        
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("User Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("• Name: ${user?.name ?: "System User"}", fontWeight = FontWeight.SemiBold)
                    Text("• Role: ${user?.role ?: "User"}")
                    Text("• ID: ${user?.userId ?: "U-000"}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    authViewModel?.logout()
                    showProfileDialog = false 
                }) {
                    Text("Logout", color = RiskHighRed)
                }
            }
        )
    }

    // Alerts List Dialog
    if (showAlertsDialog) {
        AlertDialog(
            onDismissRequest = { showAlertsDialog = false },
            title = { Text("All Active Alerts (${alerts.size})", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    alerts.forEach { alert ->
                        Text("• ${alert.title}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("  ${alert.ngoName} - ${alert.message}", fontSize = 11.sp, color = TextDarkSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlertsDialog = false }) {
                    Text("Close", color = PrimaryBlue)
                }
            }
        )
    }

    // Selected Alert Detail Dialog
    selectedAlert?.let { alert ->
        AlertDialog(
            onDismissRequest = { selectedAlert = null },
            title = { Text(alert.title, fontWeight = FontWeight.Bold, color = RiskHighRed) },
            text = {
                Column {
                    Text("NGO: ${alert.ngoName}", fontWeight = FontWeight.SemiBold)
                    Text("Timestamp: ${alert.timestamp}")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(alert.message, color = TextDarkSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAlert = null }) {
                    Text("OK", color = PrimaryBlue)
                }
            }
        )
    }
}
