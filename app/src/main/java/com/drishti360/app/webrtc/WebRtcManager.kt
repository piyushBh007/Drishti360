package com.drishti360.app.webrtc

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.drishti360.app.config.AppConfig
import org.webrtc.*

class WebRtcManager(private val context: Context) {

    companion object {
        private const val TAG_RTC = "Drishti360-WebRTC"
        private const val TAG_CAM = "Drishti360-Camera"
        private const val TAG_AUD = "Drishti360-Audio"
        private const val TAG_REN = "Drishti360-Renderer"
    }

    interface PeerConnectionListener {
        fun onIceCandidateCreated(candidate: IceCandidate)
        fun onConnectionStateChanged(state: PeerConnection.PeerConnectionState)
        fun onRemoteTrackReceived()
    }

    interface CameraEventListener {
        fun onCameraOpening(deviceName: String)
        fun onFirstFrameAvailable()
        fun onCameraClosed()
        fun onCameraError(errorDescription: String)
    }

    var cameraEventListener: CameraEventListener? = null

    val eglBase: EglBase by lazy { EglBase.create() }
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var remoteVideoTrack: VideoTrack? = null
    private var remoteRendererRef: SurfaceViewRenderer? = null
    private var localRendererRef: SurfaceViewRenderer? = null

    private var isFrontFacingCamera = true

    private val cameraEventsHandler = object : CameraVideoCapturer.CameraEventsHandler {
        override fun onCameraOpening(deviceName: String?) {
            Log.d(TAG_CAM, "onCameraOpening: deviceName=$deviceName")
            cameraEventListener?.onCameraOpening(deviceName ?: "Unknown Camera")
        }

        override fun onFirstFrameAvailable() {
            Log.d(TAG_CAM, "onFirstFrameAvailable: First local camera frame received successfully")
            cameraEventListener?.onFirstFrameAvailable()
        }

        override fun onCameraClosed() {
            Log.d(TAG_CAM, "onCameraClosed: Camera hardware closed")
            cameraEventListener?.onCameraClosed()
        }

        override fun onCameraError(errorDescription: String?) {
            Log.e(TAG_CAM, "onCameraError: $errorDescription")
            cameraEventListener?.onCameraError(errorDescription ?: "Camera initialization error")
        }

        override fun onCameraDisconnected() {
            Log.w(TAG_CAM, "onCameraDisconnected: Camera disconnected from system")
        }

        override fun onCameraFreezed(errorDescription: String?) {
            Log.w(TAG_CAM, "onCameraFreezed: $errorDescription")
        }
    }

    init {
        initFactory(context)
    }

    private fun initFactory(context: Context) {
        try {
            val options = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(options)

            val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
            val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

            factory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()
            Log.d(TAG_RTC, "PeerConnectionFactory initialized successfully with Hardware Codecs")
        } catch (e: Exception) {
            Log.e(TAG_RTC, "Failed to initialize PeerConnectionFactory: ${e.message}", e)
        }
    }

    fun initLocalRenderers(localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer) {
        this.localRendererRef = localView
        this.remoteRendererRef = remoteView

        // Initialize Local PIP Renderer
        try {
            try {
                localView.release()
            } catch (_: Exception) {}

            localView.init(eglBase.eglBaseContext, object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    Log.d(TAG_REN, "Local SurfaceViewRenderer: First frame rendered successfully")
                    cameraEventListener?.onFirstFrameAvailable()
                }

                override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
                    Log.d(TAG_REN, "Local SurfaceViewRenderer resolution: ${videoWidth}x${videoHeight}, rot=$rotation")
                }
            })
            localView.setMirror(true)
            localView.setEnableHardwareScaler(true)
            localView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            localView.setZOrderMediaOverlay(true)
            Log.d(TAG_REN, "Local SurfaceViewRenderer initialized and configured as media overlay")
        } catch (e: Exception) {
            Log.w(TAG_REN, "Local view init note: ${e.message}")
        }

        // Initialize Remote Fullscreen Renderer
        try {
            try {
                remoteView.release()
            } catch (_: Exception) {}

            remoteView.init(eglBase.eglBaseContext, object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    Log.d(TAG_REN, "Remote SurfaceViewRenderer: First frame rendered successfully")
                }

                override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
                    Log.d(TAG_REN, "Remote SurfaceViewRenderer resolution: ${videoWidth}x${videoHeight}, rot=$rotation")
                }
            })
            remoteView.setMirror(false)
            remoteView.setEnableHardwareScaler(true)
            remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            Log.d(TAG_REN, "Remote SurfaceViewRenderer initialized successfully")
        } catch (e: Exception) {
            Log.w(TAG_REN, "Remote view init note: ${e.message}")
        }

        // Attach existing remote track if available
        remoteVideoTrack?.let { track ->
            Log.d(TAG_RTC, "Attaching existing remote video track to newly ready remote renderer")
            track.addSink(remoteView)
        }
    }

    fun startLocalMedia(): Boolean {
        val pcf = factory ?: run {
            Log.e(TAG_RTC, "PeerConnectionFactory is null in startLocalMedia")
            return false
        }

        if (localVideoTrack != null) {
            Log.d(TAG_CAM, "startLocalMedia called but localVideoTrack already active")
            return true
        }

        try {
            Log.d(TAG_CAM, "Creating camera capturer...")
            videoCapturer = createCameraCapturer()
            if (videoCapturer == null) {
                Log.e(TAG_CAM, "No compatible camera capturer found on device")
                cameraEventListener?.onCameraError("No camera device found on this phone.")
                return false
            }

            surfaceTextureHelper = SurfaceTextureHelper.create("Drishti360CaptureThread", eglBase.eglBaseContext)
            localVideoSource = pcf.createVideoSource(videoCapturer?.isScreencast ?: false)
            videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)

            val width = 640
            val height = 480
            val fps = 30
            Log.d(TAG_CAM, "Starting camera capture: ${width}x${height}@${fps}fps")
            try {
                videoCapturer?.startCapture(width, height, fps)
            } catch (e: Exception) {
                Log.w(TAG_CAM, "startCapture failed at 640x480: ${e.message}. Retrying at 320x240@15fps")
                videoCapturer?.startCapture(320, 240, 15)
            }

            localVideoTrack = pcf.createVideoTrack("video_track_local", localVideoSource).apply {
                setEnabled(true)
                localRendererRef?.let { renderer ->
                    Log.d(TAG_CAM, "Attaching localVideoTrack sink to local renderer")
                    addSink(renderer)
                }
            }
            Log.d(TAG_CAM, "Local video track created and enabled")

            // Initialize Audio Track
            try {
                val audioConstraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                }
                localAudioSource = pcf.createAudioSource(audioConstraints)
                localAudioTrack = pcf.createAudioTrack("audio_track_local", localAudioSource).apply {
                    setEnabled(true)
                }
                Log.d(TAG_AUD, "Local audio track created and enabled with noise suppression")
            } catch (ae: Exception) {
                Log.e(TAG_AUD, "Audio initialization error (non-fatal): ${ae.message}", ae)
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG_CAM, "Failed to start local media: ${e.message}", e)
            cameraEventListener?.onCameraError(e.localizedMessage ?: "Unable to start the camera.")
            return false
        }
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val isCamera2Supported = try {
            Camera2Enumerator.isSupported(context)
        } catch (e: Exception) {
            Log.w(TAG_CAM, "Camera2 check failed: ${e.message}")
            false
        }
        Log.d(TAG_CAM, "Camera2Enumerator isSupported on this device: $isCamera2Supported")

        if (isCamera2Supported) {
            try {
                val enumerator = Camera2Enumerator(context)
                val capturer = createCapturerFromEnumerator(enumerator, "Camera2")
                if (capturer != null) {
                    Log.d(TAG_CAM, "Successfully created capturer using Camera2Enumerator")
                    return capturer
                }
                Log.w(TAG_CAM, "Camera2Enumerator returned null capturer, trying Camera1 fallback")
            } catch (e: Exception) {
                Log.e(TAG_CAM, "Camera2Enumerator error: ${e.message}, falling back to Camera1", e)
            }
        }

        try {
            Log.d(TAG_CAM, "Attempting Camera1Enumerator fallback...")
            val enumerator = Camera1Enumerator(true)
            val capturer = createCapturerFromEnumerator(enumerator, "Camera1")
            if (capturer != null) {
                Log.d(TAG_CAM, "Successfully created capturer using Camera1Enumerator")
                return capturer
            }
        } catch (e: Exception) {
            Log.e(TAG_CAM, "Camera1Enumerator failed: ${e.message}", e)
        }

        return null
    }

    private fun createCapturerFromEnumerator(enumerator: CameraEnumerator, typeName: String): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        Log.d(TAG_CAM, "[$typeName] Available camera devices: ${deviceNames.joinToString()}")

        // Prioritize Front-facing camera for inspector preview
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG_CAM, "[$typeName] Found front camera: $deviceName")
                try {
                    val capturer = enumerator.createCapturer(deviceName, cameraEventsHandler)
                    if (capturer != null) {
                        isFrontFacingCamera = true
                        return capturer
                    }
                } catch (e: Exception) {
                    Log.e(TAG_CAM, "[$typeName] Failed to create front capturer for $deviceName: ${e.message}", e)
                }
            }
        }

        // Fallback to back camera
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG_CAM, "[$typeName] Found back camera: $deviceName")
                try {
                    val capturer = enumerator.createCapturer(deviceName, cameraEventsHandler)
                    if (capturer != null) {
                        isFrontFacingCamera = false
                        return capturer
                    }
                } catch (e: Exception) {
                    Log.e(TAG_CAM, "[$typeName] Failed to create back capturer for $deviceName: ${e.message}", e)
                }
            }
        }
        return null
    }

    fun createPeerConnection(
        stunUrls: List<String> = AppConfig.STUN_SERVER_URLS,
        turnServers: List<AppConfig.TurnServerConfig> = AppConfig.TURN_SERVERS,
        listener: PeerConnectionListener
    ) {
        val pcf = factory ?: run {
            Log.e(TAG_RTC, "PeerConnectionFactory is null in createPeerConnection")
            return
        }

        val iceServers = mutableListOf<PeerConnection.IceServer>()
        for (stun in stunUrls) {
            iceServers.add(PeerConnection.IceServer.builder(stun).createIceServer())
        }
        for (turn in turnServers) {
            val builder = PeerConnection.IceServer.builder(turn.url)
            turn.username?.let { builder.setUsername(it) }
            turn.credential?.let { builder.setPassword(it) }
            iceServers.add(builder.createIceServer())
        }

        Log.d(TAG_RTC, "Creating PeerConnection with ${iceServers.size} ICE servers")
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = pcf.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                Log.d(TAG_RTC, "[ICE] Local candidate gathered: sdpMid=${candidate?.sdpMid}, index=${candidate?.sdpMLineIndex}")
                candidate?.let { listener.onIceCandidateCreated(it) }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d(TAG_RTC, "[PeerConnection] Connection state changed -> $newState")
                newState?.let { listener.onConnectionStateChanged(it) }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                Log.d(TAG_RTC, "[onTrack] Received remote track: kind=${track?.kind()} id=${track?.id()}")
                if (track is VideoTrack) {
                    remoteVideoTrack = track
                    track.setEnabled(true)
                    remoteRendererRef?.let {
                        Log.d(TAG_RTC, "Adding remote video track to remoteRenderer sink")
                        track.addSink(it)
                    }
                    listener.onRemoteTrackReceived()
                }
            }

            override fun onAddStream(stream: MediaStream?) {
                Log.d(TAG_RTC, "[onAddStream] Remote stream added: ${stream?.id}")
                val videoTrack = stream?.videoTracks?.firstOrNull()
                if (videoTrack != null) {
                    remoteVideoTrack = videoTrack
                    videoTrack.setEnabled(true)
                    remoteRendererRef?.let {
                        Log.d(TAG_RTC, "Adding remote stream video track to remoteRenderer sink")
                        videoTrack.addSink(it)
                    }
                    listener.onRemoteTrackReceived()
                }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG_RTC, "[SignalingState] $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG_RTC, "[IceConnectionState] $state")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG_RTC, "[IceReceiving] $receiving")
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG_RTC, "[IceGatheringState] $state")
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
        })

        // Add local tracks to PeerConnection
        val streamIds = listOf("drishti360_stream")
        localVideoTrack?.let {
            Log.d(TAG_RTC, "Adding local video track to PeerConnection")
            peerConnection?.addTrack(it, streamIds)
        }
        localAudioTrack?.let {
            Log.d(TAG_AUD, "Adding local audio track to PeerConnection")
            peerConnection?.addTrack(it, streamIds)
        }
    }

    fun createOffer(onSuccess: (SessionDescription) -> Unit) {
        val pc = peerConnection ?: run {
            Log.e(TAG_RTC, "createOffer: PeerConnection is null")
            return
        }
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        Log.d(TAG_RTC, "Creating WebRTC SDP offer...")
        pc.createOffer(object : SdpObserverAdapter("CreateOffer") {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    Log.d(TAG_RTC, "SDP offer created successfully. Setting local description...")
                    pc.setLocalDescription(SdpObserverAdapter("SetLocalDescriptionOffer"), it)
                    onSuccess(it)
                }
            }
        }, constraints)
    }

    fun createAnswer(onSuccess: (SessionDescription) -> Unit) {
        val pc = peerConnection ?: run {
            Log.e(TAG_RTC, "createAnswer: PeerConnection is null")
            return
        }
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        Log.d(TAG_RTC, "Creating WebRTC SDP answer...")
        pc.createAnswer(object : SdpObserverAdapter("CreateAnswer") {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    Log.d(TAG_RTC, "SDP answer created successfully. Setting local description...")
                    pc.setLocalDescription(SdpObserverAdapter("SetLocalDescriptionAnswer"), it)
                    onSuccess(it)
                }
            }
        }, constraints)
    }

    private val queuedCandidates = mutableListOf<IceCandidate>()
    private var isRemoteDescriptionSet = false

    fun setRemoteDescription(sdp: SessionDescription, onSuccess: (() -> Unit)? = null) {
        Log.d(TAG_RTC, "Setting remote description: type=${sdp.type}")
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter("SetRemoteDescription") {
            override fun onSetSuccess() {
                Log.d(TAG_RTC, "Remote description set successfully")
                isRemoteDescriptionSet = true
                drainQueuedCandidates()
                configureAudioForCall()
                onSuccess?.invoke()
            }
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        if (isRemoteDescriptionSet && peerConnection != null) {
            Log.d(TAG_RTC, "Adding remote ICE candidate directly: sdpMid=${candidate.sdpMid}")
            peerConnection?.addIceCandidate(candidate)
        } else {
            Log.d(TAG_RTC, "Queuing remote ICE candidate (waiting for remote description): sdpMid=${candidate.sdpMid}")
            synchronized(queuedCandidates) {
                queuedCandidates.add(candidate)
            }
        }
    }

    private fun drainQueuedCandidates() {
        synchronized(queuedCandidates) {
            Log.d(TAG_RTC, "Draining ${queuedCandidates.size} queued ICE candidates")
            for (candidate in queuedCandidates) {
                peerConnection?.addIceCandidate(candidate)
            }
            queuedCandidates.clear()
        }
    }

    private fun configureAudioForCall() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true
            Log.d(TAG_AUD, "Configured AudioManager: mode=MODE_IN_COMMUNICATION, isSpeakerphoneOn=true")
        } catch (e: Exception) {
            Log.w(TAG_AUD, "AudioManager setup warning: ${e.message}")
        }
    }

    fun toggleMute(isMuted: Boolean) {
        Log.d(TAG_AUD, "Toggling microphone: isMuted=$isMuted")
        localAudioTrack?.setEnabled(!isMuted)
    }

    fun toggleCamera(isOff: Boolean) {
        Log.d(TAG_CAM, "Toggling local camera: isOff=$isOff")
        localVideoTrack?.setEnabled(!isOff)
    }

    fun switchCamera() {
        Log.d(TAG_CAM, "Switching camera facing...")
        val camCapturer = videoCapturer as? CameraVideoCapturer
        if (camCapturer != null) {
            camCapturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                    isFrontFacingCamera = isFrontCamera
                    Log.d(TAG_CAM, "Camera switched successfully. isFrontCamera=$isFrontCamera")
                    localRendererRef?.setMirror(isFrontCamera)
                }

                override fun onCameraSwitchError(errorDescription: String?) {
                    Log.e(TAG_CAM, "Camera switch failed: $errorDescription")
                }
            })
        } else {
            Log.w(TAG_CAM, "Capturer does not support dynamic camera switching")
        }
    }

    fun close() {
        Log.d(TAG_RTC, "Closing WebRtcManager and releasing all media and network resources...")
        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            Log.w(TAG_CAM, "Capturer stopCapture error: ${e.message}")
        }
        try {
            videoCapturer?.dispose()
        } catch (e: Exception) {}
        videoCapturer = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        localVideoTrack?.setEnabled(false)
        localVideoSource?.dispose()
        localVideoSource = null
        localVideoTrack = null

        localAudioTrack?.setEnabled(false)
        localAudioSource?.dispose()
        localAudioSource = null
        localAudioTrack = null

        try {
            peerConnection?.close()
        } catch (e: Exception) {}
        peerConnection = null

        try {
            localRendererRef?.release()
        } catch (e: Exception) {
            Log.w(TAG_REN, "localRenderer release warning: ${e.message}")
        }
        try {
            remoteRendererRef?.release()
        } catch (e: Exception) {
            Log.w(TAG_REN, "remoteRenderer release warning: ${e.message}")
        }
        localRendererRef = null
        remoteRendererRef = null
        remoteVideoTrack = null

        try {
            factory?.dispose()
        } catch (e: Exception) {}
        factory = null

        try {
            eglBase.release()
        } catch (e: Exception) {
            Log.w(TAG_RTC, "eglBase release warning: ${e.message}")
        }
        Log.d(TAG_RTC, "WebRtcManager resources completely released")
    }

    open class SdpObserverAdapter(private val tag: String) : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {
            Log.d(TAG_RTC, "$tag: onCreateSuccess")
        }

        override fun onSetSuccess() {
            Log.d(TAG_RTC, "$tag: onSetSuccess")
        }

        override fun onCreateFailure(error: String?) {
            Log.e(TAG_RTC, "$tag: onCreateFailure: $error")
        }

        override fun onSetFailure(error: String?) {
            Log.e(TAG_RTC, "$tag: onSetFailure: $error")
        }
    }
}
