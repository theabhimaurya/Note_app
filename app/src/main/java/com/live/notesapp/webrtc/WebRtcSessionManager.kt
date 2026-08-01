package com.live.notesapp.webrtc

import android.content.Context
import com.live.notesapp.domain.model.CallSignal
import com.live.notesapp.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.webrtc.*

import android.media.AudioManager
import org.webrtc.audio.JavaAudioDeviceModule

class WebRtcSessionManager(
    private val context: Context,
    private val chatRepository: ChatRepository,
    private val currentUserId: String,
    private val otherUserId: String,
    private val roomId: String
) {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private val eglBase: EglBase = EglBase.create()
    val eglContext: EglBase.Context = eglBase.eglBaseContext
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioDeviceModule: JavaAudioDeviceModule? = null
    
    private var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoSource: org.webrtc.VideoSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var audioSource: org.webrtc.AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    
    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()
    
    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _callState = MutableStateFlow<CallState>(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    enum class CallState {
        IDLE, CALLING, RINGING, CONNECTED, ENDED
    }

    init {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        // Configure system audio for VoIP communication
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
        } catch (e: Exception) {
            android.util.Log.e("WebRTC", "Failed to configure AudioManager", e)
        }

        audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        val factoryBuilder = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext))
        
        peerConnectionFactory = factoryBuilder.createPeerConnectionFactory()
        
        setupLocalTracks()
        observeSignaling()
    }

    private fun sendSignal(signal: CallSignal) {
        scope.launch {
            try {
                chatRepository.sendCallSignal(roomId, signal)
            } catch (e: Exception) {
                android.util.Log.e("WebRTC", "Error sending signal: ${signal.type}", e)
            }
        }
    }

    private fun setupLocalTracks() {
        // Video
        val vSource = peerConnectionFactory.createVideoSource(false)
        videoSource = vSource
        val helper = SurfaceTextureHelper.create("CaptureThread", eglContext)
        surfaceTextureHelper = helper
        videoCapturer = createVideoCapturer(context)
        
        videoCapturer?.initialize(helper, context, vSource.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)
        
        val videoTrack = peerConnectionFactory.createVideoTrack("video_track", vSource)
        _localVideoTrack.value = videoTrack

        // Audio
        val audioConstraints = MediaConstraints()
        val aSource = peerConnectionFactory.createAudioSource(audioConstraints)
        audioSource = aSource
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track", aSource)
    }

    private fun createMediaConstraints(): MediaConstraints {
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
    }

    private fun createVideoCapturer(context: Context): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        for (name in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                return enumerator.createCapturer(name, null)
            }
        }
        return null
    }

    private val localIceCandidates = java.util.Collections.synchronizedList(mutableListOf<IceCandidate>())
    private val pendingIceCandidates = java.util.Collections.synchronizedList(mutableListOf<IceCandidate>())

    private fun setupPeerConnection() {
        val turnUser = "2e625fee0033c656b6bb88b2"
        val turnPass = "LMKHyoadYxFxxMs/"
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer(),
            PeerConnection.IceServer.builder("turn:in.relay.metered.ca:80")
                .setUsername(turnUser)
                .setPassword(turnPass)
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:in.relay.metered.ca:80?transport=tcp")
                .setUsername(turnUser)
                .setPassword(turnPass)
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:in.relay.metered.ca:443")
                .setUsername(turnUser)
                .setPassword(turnPass)
                .createIceServer(),
            PeerConnection.IceServer.builder("turns:in.relay.metered.ca:443?transport=tcp")
                .setUsername(turnUser)
                .setPassword(turnPass)
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80")
                .setUsername(turnUser)
                .setPassword(turnPass)
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443")
                .setUsername(turnUser)
                .setPassword(turnPass)
                .createIceServer(),
            PeerConnection.IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
                .setUsername(turnUser)
                .setPassword(turnPass)
                .createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
        }
        
        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                android.util.Log.d("WebRTC", "onIceCandidate generated: mid=${candidate.sdpMid}, line=${candidate.sdpMLineIndex}, sdp=${candidate.sdp}")
                localIceCandidates.add(candidate)
                sendIceCandidate(candidate)
            }
            override fun onAddStream(stream: MediaStream) {
                android.util.Log.d("WebRTC", "onAddStream: video=${stream.videoTracks.size}, audio=${stream.audioTracks.size}")
                if (stream.videoTracks.isNotEmpty()) {
                    val track = stream.videoTracks[0]
                    track.setEnabled(true)
                    _remoteVideoTrack.value = track
                }
                if (stream.audioTracks.isNotEmpty()) {
                    stream.audioTracks[0].setEnabled(true)
                }
            }
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                android.util.Log.d("WebRTC", "onSignalingChange: $p0")
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                android.util.Log.d("WebRTC", "onIceConnectionChange: $state")
                if (state == PeerConnection.IceConnectionState.CONNECTED || state == PeerConnection.IceConnectionState.COMPLETED) {
                    _callState.value = CallState.CONNECTED
                } else if (state == PeerConnection.IceConnectionState.FAILED || state == PeerConnection.IceConnectionState.CLOSED) {
                    android.util.Log.e("WebRTC", "IceConnection failed or closed: $state")
                }
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                android.util.Log.d("WebRTC", "onIceGatheringChange: $p0")
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                android.util.Log.d("WebRTC", "onAddTrack: kind=${receiver?.track()?.kind()}, id=${receiver?.track()?.id()}, enabled=${receiver?.track()?.enabled()}")
                receiver?.track()?.let { track ->
                    track.setEnabled(true)
                    if (track is VideoTrack) {
                        _remoteVideoTrack.value = track
                        android.util.Log.d("WebRTC", "Remote video track SET: ${track.id()}")
                    }
                }
            }
        }

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, observer)
        _localVideoTrack.value?.let { 
            peerConnection?.addTrack(it, listOf("stream_id"))
        }
        localAudioTrack?.let {
            peerConnection?.addTrack(it, listOf("stream_id"))
        }
        android.util.Log.d("WebRTC", "PeerConnection created, local tracks added: video=${_localVideoTrack.value != null}, audio=${localAudioTrack != null}")
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        val data = buildJsonObject {
            put("sdp", candidate.sdp)
            candidate.sdpMid?.let { put("sdpMid", it) }
            put("sdpMLineIndex", candidate.sdpMLineIndex)
        }.toString()
        sendSignal(CallSignal("ice_candidate", currentUserId, otherUserId, data))
    }

    fun startCall() {
        _callState.value = CallState.CALLING
        setupPeerConnection()
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(this, sdp)
                scope.launch {
                    var retries = 0
                    val maxRetries = 15 // 30 seconds total
                    while (_callState.value == CallState.CALLING && retries < maxRetries) {
                        sendSignal(CallSignal("offer", currentUserId, otherUserId, sdp.description))
                        // Also retransmit any already gathered local candidates so callee gets them when joining
                        synchronized(localIceCandidates) {
                            localIceCandidates.forEach { sendIceCandidate(it) }
                        }
                        delay(2000)
                        retries++
                    }
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {
                android.util.Log.e("WebRTC", "createOffer failed: $p0")
            }
            override fun onSetFailure(p0: String?) {
                android.util.Log.e("WebRTC", "setLocalDescription failed: $p0")
            }
        }, createMediaConstraints())
    }

    private fun observeSignaling() {
        scope.launch {
            chatRepository.observeCallSignals(roomId).collectLatest { signal ->
                if (signal.senderId == currentUserId) return@collectLatest
                android.util.Log.d("WebRTC", "Received signal: type=${signal.type}, from=${signal.senderId}")
                
                when (signal.type) {
                    "offer" -> handleOffer(signal)
                    "answer" -> handleAnswer(signal)
                    "ice_candidate" -> handleIceCandidate(signal)
                    "hangup" -> endCall()
                }
            }
        }
    }

    private fun handleOffer(signal: CallSignal) {
        android.util.Log.d("WebRTC", "handleOffer: callState=${_callState.value}, peerConnection=${peerConnection != null}")
        if (_callState.value == CallState.CONNECTED) return
        if (peerConnection == null) setupPeerConnection()
        _callState.value = CallState.RINGING
        
        val sdp = SessionDescription(SessionDescription.Type.OFFER, signal.data!!)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                android.util.Log.d("WebRTC", "handleOffer: remote description SET, creating answer")
                peerConnection?.createAnswer(object : SdpObserver {
                    var localAnswer: SessionDescription? = null
                    override fun onCreateSuccess(answerSdp: SessionDescription) {
                        localAnswer = answerSdp
                        peerConnection?.setLocalDescription(this, answerSdp)
                    }
                    override fun onSetSuccess() {
                        val answer = localAnswer ?: peerConnection?.localDescription ?: return
                        android.util.Log.d("WebRTC", "handleOffer: answer created and set as local description, sending")
                        drainPendingIceCandidates()
                        scope.launch {
                            var retries = 0
                            val maxRetries = 5
                            while (_callState.value != CallState.ENDED && retries < maxRetries) {
                                sendSignal(CallSignal("answer", currentUserId, otherUserId, answer.description))
                                synchronized(localIceCandidates) {
                                    localIceCandidates.forEach { sendIceCandidate(it) }
                                }
                                delay(1500)
                                retries++
                            }
                        }
                        _callState.value = CallState.CONNECTED
                    }
                    override fun onCreateFailure(p0: String?) {
                        android.util.Log.e("WebRTC", "createAnswer failed: $p0")
                    }
                    override fun onSetFailure(p0: String?) {
                        android.util.Log.e("WebRTC", "setLocalDescription (answer) failed: $p0")
                    }
                }, createMediaConstraints())
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {
                android.util.Log.e("WebRTC", "setRemoteDescription (offer) failed: $p0")
            }
        }, sdp)
    }

    private fun handleAnswer(signal: CallSignal) {
        if (_callState.value == CallState.CONNECTED || _callState.value == CallState.ENDED) {
            android.util.Log.d("WebRTC", "handleAnswer: skipping, already ${_callState.value}")
            return
        }
        android.util.Log.d("WebRTC", "handleAnswer: setting remote description")
        val sdp = SessionDescription(SessionDescription.Type.ANSWER, signal.data!!)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                android.util.Log.d("WebRTC", "handleAnswer: remote description SET successfully")
                drainPendingIceCandidates()
                // Retransmit all local ICE candidates to callee now that we know callee is listening!
                scope.launch {
                    var retries = 0
                    val maxRetries = 3
                    while (_callState.value != CallState.ENDED && retries < maxRetries) {
                        synchronized(localIceCandidates) {
                            android.util.Log.d("WebRTC", "handleAnswer: Retransmitting ${localIceCandidates.size} local ICE candidates to callee (retry $retries)")
                            localIceCandidates.forEach { sendIceCandidate(it) }
                        }
                        delay(1500)
                        retries++
                    }
                }
                _callState.value = CallState.CONNECTED
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {
                android.util.Log.e("WebRTC", "setRemoteDescription (answer) failed: $p0")
            }
        }, sdp)
    }

    private fun handleIceCandidate(signal: CallSignal) {
        try {
            val data = signal.data ?: return
            val candidate = if (data.startsWith("{")) {
                val json = Json.parseToJsonElement(data).jsonObject
                val mid = json["sdpMid"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "null" && it.isNotBlank() }
                val lineIndex = json["sdpMLineIndex"]?.jsonPrimitive?.intOrNull ?: 0
                val sdp = json["sdp"]?.jsonPrimitive?.contentOrNull ?: ""
                if (sdp.isBlank()) return
                IceCandidate(mid, lineIndex, sdp)
            } else {
                IceCandidate(null, 0, data)
            }
            
            if (peerConnection != null && peerConnection?.remoteDescription != null && peerConnection?.localDescription != null) {
                val added = peerConnection?.addIceCandidate(candidate)
                android.util.Log.d("WebRTC", "handleIceCandidate: added candidate mid=${candidate.sdpMid}, line=${candidate.sdpMLineIndex}, result=$added")
            } else {
                synchronized(pendingIceCandidates) {
                    pendingIceCandidates.add(candidate)
                }
                android.util.Log.d("WebRTC", "handleIceCandidate: pending candidate mid=${candidate.sdpMid}, line=${candidate.sdpMLineIndex}")
            }
        } catch (e: Exception) {
            android.util.Log.e("WebRTC", "Error parsing ice candidate: ${signal.data}", e)
        }
    }

    private fun drainPendingIceCandidates() {
        synchronized(pendingIceCandidates) {
            android.util.Log.d("WebRTC", "drainPendingIceCandidates: draining ${pendingIceCandidates.size} candidates")
            pendingIceCandidates.forEach { candidate ->
                peerConnection?.addIceCandidate(candidate)
            }
            pendingIceCandidates.clear()
        }
    }

    fun setMicEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun setCameraEnabled(enabled: Boolean) {
        _localVideoTrack.value?.setEnabled(enabled)
    }

    fun endCall() {
        if (_callState.value == CallState.ENDED) return
        _callState.value = CallState.ENDED
        
        try {
            sendSignal(CallSignal("hangup", currentUserId, otherUserId))
        } catch (_: Exception) {}
        
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (_: Exception) {}

        // Clear UI video track references first so Compose renderers detach sinks before native disposal
        _localVideoTrack.value = null
        _remoteVideoTrack.value = null

        job.cancel()
        
        synchronized(pendingIceCandidates) {
            pendingIceCandidates.clear()
        }
        synchronized(localIceCandidates) {
            localIceCandidates.clear()
        }

        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {}
        try {
            videoCapturer?.dispose()
        } catch (_: Exception) {}
        videoCapturer = null

        try {
            surfaceTextureHelper?.dispose()
        } catch (_: Exception) {}
        surfaceTextureHelper = null

        try {
            videoSource?.dispose()
        } catch (_: Exception) {}
        videoSource = null

        try {
            localAudioTrack?.dispose()
        } catch (_: Exception) {}
        localAudioTrack = null

        try {
            audioSource?.dispose()
        } catch (_: Exception) {}
        audioSource = null

        try {
            peerConnection?.close()
            peerConnection?.dispose()
        } catch (_: Exception) {}
        peerConnection = null

        try {
            audioDeviceModule?.release()
        } catch (_: Exception) {}
        audioDeviceModule = null
    }
}
