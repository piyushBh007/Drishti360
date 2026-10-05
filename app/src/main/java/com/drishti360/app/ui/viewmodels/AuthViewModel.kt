package com.drishti360.app.ui.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drishti360.app.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Authenticated(val token: String, val userId: String, val username: String, val name: String, val role: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState
    
    private val prefs = application.getSharedPreferences("drishti_auth", Context.MODE_PRIVATE)

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val token = prefs.getString("token", null)
        val role = prefs.getString("role", null)
        val name = prefs.getString("name", null)
        if (token != null && role != null && name != null) {
            _authState.value = AuthState.Authenticated(token, "", "", name, role)
        }
    }

    fun login(username: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL("${AppConfig.HTTP_BASE_URL}/api/auth/login")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    
                    val body = JSONObject().apply {
                        put("username", username)
                        put("password", pass)
                    }
                    
                    conn.outputStream.use { os ->
                        os.write(body.toString().toByteArray())
                    }
                    
                    val status = conn.responseCode
                    if (status in 200..299) {
                        val resp = conn.inputStream.bufferedReader().readText()
                        JSONObject(resp)
                    } else {
                        val errStream = conn.errorStream ?: conn.inputStream
                        var errStr = ""
                        if (errStream != null) {
                            errStr = errStream.bufferedReader().readText()
                        }
                        throw com.drishti360.app.utils.ApiException(status, errStr)
                    }
                }
                
                val token = result.getString("token")
                val user = result.getJSONObject("user")
                val role = user.getString("role")
                val name = user.getString("name")
                
                prefs.edit()
                    .putString("token", token)
                    .putString("role", role)
                    .putString("name", name)
                    .apply()
                
                _authState.value = AuthState.Authenticated(token, user.getString("id"), user.getString("username"), name, role)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login error: ${e.message}")
                val appError = com.drishti360.app.utils.ErrorMapper.mapException(e)
                _authState.value = AuthState.Error(appError.message)
            }
        }
    }
    
    fun logout() {
        prefs.edit().clear().apply()
        _authState.value = AuthState.Idle
    }
}
