package com.drishti360.app.config

object AppConfig {
    /**
     * Base HTTP/HTTPS API URL configured per build type.
     * Debug: DEV_API_URL (e.g. http://10.0.2.2:3000)
     * Release: PROD_API_URL (must be HTTPS production URL)
     *
     * In release builds, local IP addresses (127.0.0.1, 10.0.2.2, 192.168.x.x)
     * and unencrypted http:// schemes are strictly prohibited.
     */
    val HTTP_BASE_URL: String
        get() {
            val url = com.drishti360.app.BuildConfig.API_URL
            if (!com.drishti360.app.BuildConfig.DEBUG) {
                val forbidden = listOf("localhost", "127.0.0.1", "10.0.2.2", "192.168.")
                if (forbidden.any { url.contains(it) } || url.startsWith("http://")) {
                    throw IllegalStateException("Release builds must NEVER use local or unencrypted URLs: $url")
                }
            }
            return url
        }

    /**
     * WebSocket Signaling URL derived from the HTTP base URL.
     * Automatically maps http:// -> ws:// and https:// -> wss://
     */
    val WS_SIGNALING_URL: String
        get() {
            val base = HTTP_BASE_URL
            val wsBase = if (base.startsWith("https://")) {
                base.replace("https://", "wss://")
            } else {
                base.replace("http://", "ws://")
            }
            return "${wsBase.trimEnd('/')}/ws"
        }

    /**
     * STUN servers for NAT traversal, loaded from build configuration or fallback defaults.
     */
    val STUN_SERVER_URLS: List<String>
        get() {
            val configVal = com.drishti360.app.BuildConfig.STUN_SERVERS
            return if (configVal.isNotBlank()) {
                configVal.split(",").map { it.trim() }.filter { it.isNotBlank() }
            } else {
                listOf(
                    "stun:stun.l.google.com:19302",
                    "stun:stun1.l.google.com:19302",
                    "stun:stun2.l.google.com:19302"
                )
            }
        }

    /**
     * Optional TURN servers for strict symmetric NAT / firewall traversal in production.
     * Initialized from build configuration and can be overridden dynamically at runtime.
     */
    var TURN_SERVERS: List<TurnServerConfig> = run {
        val url = com.drishti360.app.BuildConfig.TURN_SERVER_URL
        if (url.isNotBlank()) {
            val user = com.drishti360.app.BuildConfig.TURN_SERVER_USER.takeIf { it.isNotBlank() }
            val pass = com.drishti360.app.BuildConfig.TURN_SERVER_PASS.takeIf { it.isNotBlank() }
            listOf(TurnServerConfig(url = url, username = user, credential = pass))
        } else {
            emptyList()
        }
    }

    data class TurnServerConfig(
        val url: String,
        val username: String? = null,
        val credential: String? = null
    )
}

