package com.drishti360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.drishti360.app.ui.theme.TextDarkPrimary
import com.drishti360.app.ui.viewmodels.AuthViewModel
import com.drishti360.app.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageInspectorsScreen(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel?,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
                text = "Manage Inspectors",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Create New Inspector", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = employeeId,
                onValueChange = { employeeId = it },
                label = { Text("Employee ID") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            errorMsg?.let { Text(it, color = androidx.compose.ui.graphics.Color.Red) }
            successMsg?.let { Text(it, color = androidx.compose.ui.graphics.Color.Green) }

            Spacer(modifier = Modifier.height(16.dp))

            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                PrimaryActionButton(
                    text = "Create Inspector",
                    onClick = {
                        val token = (authViewModel?.authState?.value as? AuthViewModel.AuthState.Authenticated)?.token ?: ""
                        isSubmitting = true
                        errorMsg = null
                        successMsg = null
                        coroutineScope.launch {
                            val result = mainViewModel.addInspector(token, name, employeeId, username, password)
                            isSubmitting = false
                            if (result.isSuccess) {
                                successMsg = "Inspector created successfully!"
                                name = ""
                                employeeId = ""
                                username = ""
                                password = ""
                            } else {
                                val ex = result.exceptionOrNull()
                                errorMsg = if (ex != null) {
                                    com.drishti360.app.utils.ErrorMapper.mapException(ex).message
                                } else {
                                    "Failed to create inspector. Please try again."
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
