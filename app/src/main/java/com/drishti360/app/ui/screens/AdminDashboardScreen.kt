package com.drishti360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.ui.theme.LightBackground
import com.drishti360.app.ui.theme.PrimaryBlue
import com.drishti360.app.ui.theme.RiskHighRed
import com.drishti360.app.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onAddNgo: () -> Unit = {},
    onManageInspectors: () -> Unit = {},
    onOpenNgos: () -> Unit = {},
    onOpenCameras: () -> Unit = {},
    onOpenInspections: () -> Unit = {},
    onOpenAlerts: () -> Unit = {},
    onOpenAudit: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Admin Dashboard",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            TextButton(onClick = {
                authViewModel.logout()
                onLogout()
            }) {
                Text("Logout", color = RiskHighRed)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Inspectors Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onManageInspectors() },
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Inspectors", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    IconButton(onClick = onManageInspectors) {
                        Icon(Icons.Default.Add, contentDescription = "Add Inspector", tint = PrimaryBlue)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Manage inspector accounts and assignments.", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // NGOs Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenNgos() },
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NGOs", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    IconButton(onClick = onAddNgo) {
                        Icon(Icons.Default.Add, contentDescription = "Add NGO", tint = PrimaryBlue)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Manage registered NGOs and their geofences.", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cameras Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenCameras() },
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cameras", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("View live CCTV feeds and AI inference status.", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Inspections Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenInspections() },
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Inspections", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Review field inspection records and reports.", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Alerts Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAlerts() },
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Alerts", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Monitor active anomalies and CCTV security alerts.", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audit Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAudit() },
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Audit", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("View cryptographic audit logs and immutable trail.", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
            }
        }
    }
}
