package com.drishti360.app.utils

import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException

object ErrorMapper {

    /**
     * Standard human-readable error response.
     */
    data class AppError(
        val message: String,
        val isAuthError: Boolean = false
    )

    fun mapException(throwable: Throwable): AppError {
        return when (throwable) {
            is UnknownHostException -> AppError("Unable to reach the server. Please check your internet connection.")
            is ConnectException -> AppError("Unable to connect to the server. Please try again.")
            is SocketTimeoutException -> AppError("The request timed out. Please try again.")
            is IOException -> AppError("A network error occurred. Please try again.")
            is ApiException -> parseApiException(throwable)
            is org.json.JSONException -> AppError("Unable to process the server response. Please try again.")
            else -> AppError("Something went wrong. Please try again.")
        }
    }

    private fun parseApiException(apiEx: ApiException): AppError {
        // Try to parse standard error JSON: { "success": false, "error": { "code": "...", "message": "..." } }
        try {
            if (!apiEx.responseBody.isNullOrBlank() && apiEx.responseBody.startsWith("{")) {
                val json = JSONObject(apiEx.responseBody)
                if (json.has("error")) {
                    val errorObj = json.optJSONObject("error")
                    if (errorObj != null) {
                        val msg = errorObj.optString("message", "")
                        if (msg.isNotBlank()) {
                            return AppError(msg, isAuthError = apiEx.statusCode == 401)
                        }
                    } else {
                        val errorString = json.optString("error", "")
                        if (errorString.isNotBlank()) {
                            // Map some basic known strings from backend
                            if (errorString.contains("Invalid credentials")) return AppError("Incorrect username or password.", true)
                            if (errorString.contains("Unauthorized") || errorString.contains("Invalid token")) return AppError("Your session has expired. Please log in again.", true)
                            if (errorString.contains("Forbidden")) return AppError("You do not have permission to perform this action.")
                            // otherwise return mapped generic
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // ignore JSON parse error of the error body itself
        }

        // Fallback to HTTP Status code mapping
        val defaultMessage = when (apiEx.statusCode) {
            400 -> "Please check the information you entered."
            401 -> "Your session has expired. Please log in again."
            403 -> "You do not have permission to perform this action."
            404 -> "The requested information could not be found."
            409 -> "The information provided conflicts with an existing record."
            413 -> "The selected image is too large. Please try a smaller image."
            422 -> "Some of the information is invalid. Please check the form."
            429 -> "Too many requests. Please wait a moment and try again."
            500 -> "Something went wrong on the server. Please try again."
            502, 503, 504 -> "Service is temporarily unavailable. Please try again later."
            else -> "Something went wrong. Please try again."
        }

        return AppError(defaultMessage, isAuthError = apiEx.statusCode == 401)
    }
}

class ApiException(val statusCode: Int, val responseBody: String?) : Exception("HTTP $statusCode")
