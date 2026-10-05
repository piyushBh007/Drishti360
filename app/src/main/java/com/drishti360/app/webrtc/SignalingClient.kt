package com.drishti360.app.webrtc

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.TimeUnit

class SignalingClient(
    private val listener: SignalingListener
) {
    interface SignalingListener {
        fun onJoined(participantId: String, participantCount: Int)
        fun onParticipantJoined(participantId: String)
        fun onOfferReceived(sdp: SessionDescription)
        fun onAnswerReceived(sdp: SessionDescription)
        fun onIceCandidateReceived(candidate: IceCandidate)
        fun onParticipantLeft(participantId: String)
        fun onError(message: String)
        fun onDisconnected()
    }

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var currentInspectionId: String = ""
    private var currentParticipantId: String = "android-inspector"
    private var isConnected: Boolean = false

    fun connect(wsUrl: String, inspectionId: String, participantId: String = "android-inspector") {
        this.currentInspectionId = inspectionId
        this.currentParticipantId = participantId

        disconnect()

        client = OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        Log.d("SignalingClient", "Connecting to WebSocket at $wsUrl for inspectionId: $inspectionId")

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("SignalingClient", "WebSocket connected. Joining room: $currentInspectionId")
                isConnected = true
                val joinMsg = JSONObject().apply {
                    put("type", "join")
                    put("inspectionId", currentInspectionId)
                    put("participantId", currentParticipantId)
                    put("role", "inspector")
                }
                webSocket.send(joinMsg.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("SignalingClient", "Signaling message received: $text")
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")
                    when (type) {
                        "joined" -> {
                            val pid = json.optString("participantId", currentParticipantId)
                            val count = json.optInt("participantCount", 1)
                            listener.onJoined(pid, count)
                        }
                        "participant-joined" -> {
                            val pid = json.optString("participantId")
                            listener.onParticipantJoined(pid)
                        }
                        "offer" -> {
                            val sdpObj = json.optJSONObject("sdp")
                            val sdpString = if (sdpObj != null) sdpObj.optString("sdp") else json.optString("sdp")
                            val sdp = SessionDescription(SessionDescription.Type.OFFER, sdpString)
                            listener.onOfferReceived(sdp)
                        }
                        "answer" -> {
                            val sdpObj = json.optJSONObject("sdp")
                            val sdpString = if (sdpObj != null) sdpObj.optString("sdp") else json.optString("sdp")
                            val sdp = SessionDescription(SessionDescription.Type.ANSWER, sdpString)
                            listener.onAnswerReceived(sdp)
                        }
                        "ice-candidate" -> {
                            val candObj = json.optJSONObject("candidate")
                            if (candObj != null) {
                                val sdpMid = candObj.optString("sdpMid", "")
                                val sdpMLineIndex = candObj.optInt("sdpMLineIndex", 0)
                                val sdp = candObj.optString("candidate", "")
                                if (sdp.isNotEmpty()) {
                                    listener.onIceCandidateReceived(IceCandidate(sdpMid, sdpMLineIndex, sdp))
                                }
                            }
                        }
                        "participant-left" -> {
                            val pid = json.optString("participantId")
                            listener.onParticipantLeft(pid)
                        }
                        "error" -> {
                            listener.onError(json.optString("message", "Signaling server error"))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SignalingClient", "Error parsing signaling message: ${e.message}", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("SignalingClient", "WebSocket failure: ${t.message}")
                isConnected = false
                listener.onError("Connection failed: ${t.localizedMessage ?: "Network error"}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("SignalingClient", "WebSocket closed: $reason")
                isConnected = false
                listener.onDisconnected()
            }
        })
    }

    fun sendOffer(sdp: SessionDescription) {
        val sdpJson = JSONObject().apply {
            put("type", "offer")
            put("sdp", sdp.description)
        }
        val msg = JSONObject().apply {
            put("type", "offer")
            put("inspectionId", currentInspectionId)
            put("participantId", currentParticipantId)
            put("sdp", sdpJson)
        }
        webSocket?.send(msg.toString())
    }

    fun sendAnswer(sdp: SessionDescription) {
        val sdpJson = JSONObject().apply {
            put("type", "answer")
            put("sdp", sdp.description)
        }
        val msg = JSONObject().apply {
            put("type", "answer")
            put("inspectionId", currentInspectionId)
            put("participantId", currentParticipantId)
            put("sdp", sdpJson)
        }
        webSocket?.send(msg.toString())
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        val candJson = JSONObject().apply {
            put("candidate", candidate.sdp)
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
        }
        val msg = JSONObject().apply {
            put("type", "ice-candidate")
            put("inspectionId", currentInspectionId)
            put("participantId", currentParticipantId)
            put("candidate", candJson)
        }
        webSocket?.send(msg.toString())
    }

    fun sendLeave() {
        if (isConnected) {
            val msg = JSONObject().apply {
                put("type", "leave")
                put("inspectionId", currentInspectionId)
                put("participantId", currentParticipantId)
            }
            webSocket?.send(msg.toString())
        }
    }

    fun disconnect() {
        try {
            sendLeave()
            webSocket?.close(1000, "Leaving call")
        } catch (e: Exception) {
            // ignore
        }
        webSocket = null
        client?.dispatcher?.executorService?.shutdown()
        client = null
        isConnected = false
    }
}
