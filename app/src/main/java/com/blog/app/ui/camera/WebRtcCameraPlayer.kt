package com.blog.app.ui.camera

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Call
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import java.net.URLEncoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * MediaMTX WHEP 摄像头播放器。
 */
class WebRtcCameraPlayer(
    private val context: Context, private val renderer: SurfaceViewRenderer
) {
    private val eglBase = EglBase.create()
    private val httpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).build()

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoTrack: VideoTrack? = null
    private var iceGatheringLatch: CountDownLatch? = null
    private var currentCall: Call? = null

    @Volatile
    private var stopped = false

    init {
        initializeFactory()
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setEnableHardwareScaler(true)
        renderer.setMirror(false)
    }

    /**
     * 建立 WHEP 播放连接。
     */
    suspend fun play(whepUrl: String, token: String) = withContext(Dispatchers.IO) {
        stopped = false
        closePeerConnection()

        val peerConnectionFactory = factory ?: throw IllegalStateException("WebRTC 初始化失败")
        val configuration = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        }

        val connection = peerConnectionFactory.createPeerConnection(
            configuration, object : PeerConnection.Observer {
                override fun onSignalingChange(newState: PeerConnection.SignalingState) = Unit
                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) =
                    Unit

                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
                    if (newState == PeerConnection.IceGatheringState.COMPLETE) {
                        iceGatheringLatch?.countDown()
                    }
                }

                override fun onIceCandidate(candidate: IceCandidate) = Unit
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
                override fun onAddStream(stream: MediaStream) {
                    stream.videoTracks.firstOrNull()?.let { attachVideoTrack(it) }
                }

                override fun onRemoveStream(stream: MediaStream) = Unit
                override fun onDataChannel(dataChannel: org.webrtc.DataChannel) = Unit
                override fun onRenegotiationNeeded() = Unit
                override fun onTrack(transceiver: RtpTransceiver) {
                    (transceiver.receiver.track() as? VideoTrack)?.let { attachVideoTrack(it) }
                }

                override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState) =
                    Unit
            }) ?: throw IllegalStateException("创建 WebRTC 连接失败")

        peerConnection = connection
        connection.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )
        connection.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )

        val offer = createOffer(connection)
        iceGatheringLatch = CountDownLatch(1)
        setLocalDescription(connection, offer)
        waitForIceGathering(connection)

        if (stopped) throw kotlinx.coroutines.CancellationException()

        val localDescription =
            connection.localDescription ?: throw IllegalStateException("WebRTC 本地 SDP 创建失败")
        val answer = postOffer(whepUrl, token, localDescription.description)
        setRemoteDescription(connection, answer)
    }

    /**
     * 释放播放器资源。
     */
    fun stop() {
        stopped = true
        currentCall?.cancel()
        currentCall = null
        iceGatheringLatch?.countDown()
        closePeerConnection()
    }

    /**
     * 释放播放器全部资源。
     */
    fun release() {
        stop()
        renderer.release()
        factory?.dispose()
        factory = null
        eglBase.release()
        httpClient.dispatcher.executorService.shutdown()
    }

    private fun initializeFactory() {
        synchronized(WebRtcCameraPlayer::class.java) {
            if (!factoryInitialized) {
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                        .createInitializationOptions()
                )
                factoryInitialized = true
            }
        }

        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
            ).createPeerConnectionFactory()
    }

    private fun createOffer(connection: PeerConnection): SessionDescription {
        val latch = CountDownLatch(1)
        var offer: SessionDescription? = null
        var error: String? = null
        connection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription) {
                offer = description
                latch.countDown()
            }

            override fun onSetSuccess() = Unit
            override fun onCreateFailure(message: String) {
                error = message
                latch.countDown()
            }

            override fun onSetFailure(message: String) = Unit
        }, MediaConstraints())
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw IllegalStateException("创建 WebRTC Offer 超时")
        }
        error?.let { throw IllegalStateException("创建 WebRTC Offer 失败：$it") }
        return offer ?: throw IllegalStateException("WebRTC Offer 为空")
    }

    private fun setLocalDescription(connection: PeerConnection, description: SessionDescription) {
        val latch = CountDownLatch(1)
        var error: String? = null
        connection.setLocalDescription(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription) = Unit
            override fun onSetSuccess() {
                latch.countDown()
            }

            override fun onCreateFailure(message: String) = Unit
            override fun onSetFailure(message: String) {
                error = message
                latch.countDown()
            }
        }, description)
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw IllegalStateException("设置 WebRTC 本地 SDP 超时")
        }
        error?.let { throw IllegalStateException("设置 WebRTC 本地 SDP 失败：$it") }
    }

    private fun waitForIceGathering(connection: PeerConnection) {
        if (connection.iceGatheringState() != PeerConnection.IceGatheringState.COMPLETE) {
            iceGatheringLatch?.await(5, TimeUnit.SECONDS)
        }
        iceGatheringLatch = null
    }

    private fun postOffer(whepUrl: String, token: String, offerSdp: String): String {
        val encodedToken = URLEncoder.encode(token, Charsets.UTF_8.name())
        val url = "$whepUrl?token=$encodedToken"
        val request =
            Request.Builder().url(url).post(offerSdp.toRequestBody("application/sdp".toMediaType()))
                .build()
        val call = httpClient.newCall(request)
        currentCall = call
        call.execute().use { response ->
            currentCall = null
            if (!response.isSuccessful) {
                throw IllegalStateException("摄像头连接失败：HTTP ${response.code}")
            }
            return response.body?.string().orEmpty().ifBlank {
                throw IllegalStateException("摄像头未返回 WebRTC SDP")
            }
        }
    }

    private fun setRemoteDescription(connection: PeerConnection, sdp: String) {
        val latch = CountDownLatch(1)
        var error: String? = null
        connection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription) = Unit
            override fun onSetSuccess() {
                latch.countDown()
            }

            override fun onCreateFailure(message: String) = Unit
            override fun onSetFailure(message: String) {
                error = message
                latch.countDown()
            }
        }, SessionDescription(SessionDescription.Type.ANSWER, sdp))
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw IllegalStateException("设置摄像头 SDP 超时")
        }
        error?.let { throw IllegalStateException("设置摄像头 SDP 失败：$it") }
    }

    private fun attachVideoTrack(track: VideoTrack) {
        videoTrack?.removeSink(renderer)
        videoTrack = track
        track.addSink(renderer)
    }

    private fun closePeerConnection() {
        videoTrack?.removeSink(renderer)
        videoTrack = null
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        iceGatheringLatch?.countDown()
        iceGatheringLatch = null
    }

    companion object {
        @Volatile
        private var factoryInitialized = false
    }
}
