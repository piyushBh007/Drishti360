package com.drishti360.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drishti360.app.ui.navigation.DrishtiNavGraph
import com.drishti360.app.ui.screens.LoginScreen
import com.drishti360.app.ui.theme.DrishtiTheme
import com.drishti360.app.ui.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        android.util.Log.d("Drishti360", "MainActivity onCreate")
        android.util.Log.d("Drishti360", "Before setContent")
        setContent {
            android.util.Log.d("Drishti360", "Inside setContent")
            DrishtiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel()
                    android.util.Log.d("Drishti360", "AuthViewModel initialized")
                    val authState by authViewModel.authState.collectAsState()
                    android.util.Log.d("Drishti360", "authState: ${authState.javaClass.simpleName}")

                    if (authState is AuthViewModel.AuthState.Authenticated) {
                        val auth = authState as AuthViewModel.AuthState.Authenticated
                        val startDest = if (auth.role == "admin") "admin_dashboard" else com.drishti360.app.ui.navigation.Screen.Home.route
                        DrishtiNavGraph(authViewModel = authViewModel, startDestination = startDest)
                    } else {
                        android.util.Log.d("Drishti360", "Rendering LoginScreen")
                        LoginScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = {} // State is handled by collectAsState
                        )
                    }
                }
            }
        }
    }
}
