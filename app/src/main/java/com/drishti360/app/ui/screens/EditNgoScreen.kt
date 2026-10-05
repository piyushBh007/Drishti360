package com.drishti360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drishti360.app.data.models.NGO
import com.drishti360.app.ui.components.PrimaryActionButton
import com.drishti360.app.ui.theme.LightBackground
import com.drishti360.app.ui.theme.LightSurface
import com.drishti360.app.ui.theme.TextDarkPrimary
import com.drishti360.app.ui.viewmodels.AuthViewModel
import com.drishti360.app.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNgoScreen(
    ngo: NGO,
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel?,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(ngo.name) }
    var registrationNo by remember { mutableStateOf(ngo.registrationNo) }
    var category by remember { mutableStateOf(ngo.category) }
    var city by remember { mutableStateOf(ngo.location.city) }
    var state by remember { mutableStateOf(ngo.location.state) }
    var address by remember { mutableStateOf(ngo.location.address) }
    var latitude by remember { mutableStateOf(ngo.location.latitude.toString()) }
    var longitude by remember { mutableStateOf(ngo.location.longitude.toString()) }
    var geofenceRadius by remember { mutableStateOf(ngo.location.geofenceRadiusM.toString()) }

    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
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
                text = "Edit NGO",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("NGO Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = registrationNo,
                    onValueChange = { registrationNo = it },
                    label = { Text("Registration Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
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
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = geofenceRadius,
                    onValueChange = { geofenceRadius = it },
                    label = { Text("Geofence Radius (m)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                errorMsg?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red) }
                successMsg?.let { Text(it, color = androidx.compose.ui.graphics.Color.Green) }

                Spacer(modifier = Modifier.height(16.dp))

                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
                } else {
                    PrimaryActionButton(
                        text = "Save Changes",
                        onClick = {
                            val token = (authViewModel?.authState?.value as? AuthViewModel.AuthState.Authenticated)?.token ?: ""
                            val lat = latitude.toDoubleOrNull()
                            val lng = longitude.toDoubleOrNull()
                            val rad = geofenceRadius.toIntOrNull()
                            
                            if (lat == null || lng == null || rad == null) {
                                errorMsg = "Please enter valid coordinates and radius"
                                return@PrimaryActionButton
                            }
                            
                            isSubmitting = true
                            errorMsg = null
                            successMsg = null
                            coroutineScope.launch {
                                val result = mainViewModel.updateNgo(
                                    token = token,
                                    id = ngo.id,
                                    name = name,
                                    registrationNo = registrationNo,
                                    category = category,
                                    city = city,
                                    state = state,
                                    address = address,
                                    latitude = lat,
                                    longitude = lng,
                                    geofenceRadius = rad
                                )
                                isSubmitting = false
                                if (result.isSuccess) {
                                    successMsg = "NGO updated successfully!"
                                } else {
                                    val ex = result.exceptionOrNull()
                                    errorMsg = if (ex != null) {
                                        com.drishti360.app.utils.ErrorMapper.mapException(ex).message
                                    } else {
                                        "Failed to update NGO"
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
