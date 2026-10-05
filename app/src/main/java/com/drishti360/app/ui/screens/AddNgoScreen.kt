package com.drishti360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.ui.components.PrimaryActionButton
import com.drishti360.app.ui.theme.LightBackground
import com.drishti360.app.ui.theme.LightSurface
import com.drishti360.app.ui.theme.PrimaryBlue
import com.drishti360.app.ui.theme.TextDarkPrimary
import com.drishti360.app.ui.viewmodels.AuthViewModel
import com.drishti360.app.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNgoScreen(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel?,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var regNo by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Welfare") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latStr by remember { mutableStateOf("") }
    var lngStr by remember { mutableStateOf("") }
    var radStr by remember { mutableStateOf("100") }

    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Header Bar
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
                text = "Add NGO",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("NGO Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = regNo,
                onValueChange = { regNo = it },
                label = { Text("Registration Number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (e.g. Healthcare, Welfare)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state,
                onValueChange = { state = it },
                label = { Text("State") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Full Address") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Location", fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Provide Coordinates. Alternatively use 'Pick Location' (requires map selection).", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latStr,
                    onValueChange = { latStr = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lngStr,
                    onValueChange = { lngStr = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = radStr,
                onValueChange = { radStr = it },
                label = { Text("Geofence Radius (m)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            errorMsg?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red) }
            Spacer(modifier = Modifier.height(16.dp))

            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                PrimaryActionButton(
                    text = "Save NGO",
                    onClick = {
                        val lat = latStr.toDoubleOrNull() ?: 0.0
                        val lng = lngStr.toDoubleOrNull() ?: 0.0
                        val rad = radStr.toIntOrNull() ?: 100
                        val token = (authViewModel?.authState?.value as? AuthViewModel.AuthState.Authenticated)?.token ?: ""
                        
                        isSubmitting = true
                        errorMsg = null
                        coroutineScope.launch {
                            val result = mainViewModel.addNgo(
                                token = token,
                                name = name,
                                regNo = regNo,
                                category = category,
                                city = city,
                                state = state,
                                address = address,
                                lat = lat,
                                lng = lng,
                                geofenceRadius = rad
                            )
                            isSubmitting = false
                            if (result.isSuccess) {
                                onBack()
                            } else {
                                val ex = result.exceptionOrNull()
                                 errorMsg = if (ex != null) {
                                    com.drishti360.app.utils.ErrorMapper.mapException(ex).message
                                } else {
                                    "Unable to save the NGO. Please try again."
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
