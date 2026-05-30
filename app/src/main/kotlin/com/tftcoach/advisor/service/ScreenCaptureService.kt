// service/ScreenCaptureService.kt
package com.tftcoach.advisor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tftcoach.advisor.MainActivity
import com.tftcoach.advisor.R
import com.tftcoach.advisor.data.GameState
import com.tftcoach.advisor.engine.AdviceEngine
import com.tftcoach.advisor.engine.GameStateAnalyzer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIF_CHANNEL_ID = "tft_coach_channel"
        private const val NOTIF_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP  = "ACTION_STOP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"

        // 다른 컴포넌트가 상태를 구독
        val gameStateFlow = MutableStateFlow(GameState())
        val isRunningFlow = MutableStateFlow(false)
        val errorFlow     = MutableStateFlow<String?>(null)
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val analyzer = GameStateAnalyzer()

    // Riot Live Client API — 자기 서명 인증서 허용 클라이언트
    private val httpClient: OkHttpClient by lazy {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAll, java.security.SecureRandom())
        }
        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAll[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultData != null) {
                    startForeground(NOTIF_ID, buildNotification())
                    startCapture(resultCode, resultData)
                }
            }
            ACTION_STOP -> {
                stopCapture()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startCapture(resultCode: Int, resultData: Intent) {
        val metrics = resources.displayMetrics
        val width   = metrics.widthPixels
        val height  = metrics.heightPixels
        val density = metrics.densityDpi

        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData).also { mp ->
            mp.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopCapture() }
            }, null)
        }

        // ImageReader: 초당 2프레임 (배터리 절약)
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).also { ir ->
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "TFTCoach",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                ir.surface, null, null
            )
        }

        isRunningFlow.value = true
        startAnalysisLoop()
        startApiPollingLoop()
        Log.d(TAG, "캡처 시작 ($width×$height)")
    }

    private fun startAnalysisLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val bitmap = grabBitmap()
                    if (bitmap != null) {
                        val (state, ok) = analyzer.analyze(bitmap)
                        gameStateFlow.value = state
                        bitmap.recycle()

                        if (!ok && analyzer.consecutiveFailures >= 3) {
                            errorFlow.value = "화면 인식 오류 (3회 연속 실패)"
                        } else {
                            errorFlow.value = null
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "분석 오류", e)
                }
                delay(500L) // 500ms = 초당 2프레임
            }
        }
    }

    private fun startApiPollingLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    fetchLiveClientData()
                } catch (e: Exception) {
                    // Live Client API는 TFT 실행 중이 아니면 실패 — 무시
                }
                delay(5000L) // 5초마다 폴링
            }
        }
    }

    private fun fetchLiveClientData() {
        val req = Request.Builder()
            .url("https://127.0.0.1:2999/liveclientdata/allgamedata")
            .build()
        httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: return
                val json = JSONObject(body)
                val activePlayer = json.optJSONObject("activePlayer") ?: return
                val health = activePlayer.optJSONObject("championStats")
                    ?.optDouble("currentHealth", -1.0)?.toInt() ?: -1
                val level = activePlayer.optInt("level", -1)
                analyzer.updateFromApi(health, level)
            }
        }
    }

    private fun grabBitmap(): Bitmap? {
        val reader = imageReader ?: return null
        val image = reader.acquireLatestImage() ?: return null
        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride   = planes[0].rowStride
            val rowPadding  = rowStride - pixelStride * image.width
            val bmp = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height, Bitmap.Config.ARGB_8888
            )
            bmp.copyPixelsFromBuffer(buffer)
            // 패딩 제거
            Bitmap.createBitmap(bmp, 0, 0, image.width, image.height)
        } finally {
            image.close()
        }
    }

    private fun stopCapture() {
        serviceScope.coroutineContext.cancelChildren()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        isRunningFlow.value = false
        Log.d(TAG, "캡처 중지")
    }

    override fun onDestroy() {
        stopCapture()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ────────────────────────────────────────────────────────────────
    // 알림 채널 / 포그라운드 알림
    // ────────────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIF_CHANNEL_ID,
            "롤체 훈수봇",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "롤체 화면 모니터링 중"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ScreenCaptureService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("🎮 롤체 훈수봇 실행 중")
            .setContentText("실시간으로 게임 화면을 분석하고 있습니다")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "중지", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
