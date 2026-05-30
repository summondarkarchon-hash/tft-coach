// service/OverlayService.kt
package com.tftcoach.advisor.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.tftcoach.advisor.data.Advice
import com.tftcoach.advisor.data.GameState
import com.tftcoach.advisor.engine.AdviceEngine
import com.tftcoach.advisor.ui.OverlayContent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Lifecycle 구현 (ComposeView 사용을 위해 필수)
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // 오버레이 위치 상태
    private var overlayX = 0
    private var overlayY = 100
    private var isMinimized = false

    companion object {
        const val ACTION_SHOW = "ACTION_SHOW"
        const val ACTION_HIDE = "ACTION_HIDE"
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay()
            ACTION_HIDE -> hideOverlay()
        }
        return START_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val params = buildLayoutParams(200, WindowManager.LayoutParams.WRAP_CONTENT)

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
        }

        // 드래그 이동 처리
        var startX = 0; var startY = 0
        var startParamX = 0; var startParamY = 0
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX.toInt()
                    startY = event.rawY.toInt()
                    startParamX = params.x
                    startParamY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startParamX + (event.rawX.toInt() - startX)
                    params.y = startParamY + (event.rawY.toInt() - startY)
                    overlayX = params.x
                    overlayY = params.y
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }

        // 게임 상태 구독 후 Compose 렌더링
        serviceScope.launch {
            ScreenCaptureService.gameStateFlow.collect { state ->
                val advices = AdviceEngine.analyze(state)
                view.setContent {
                    OverlayContent(
                        state = state,
                        advices = advices,
                        isMinimized = isMinimized,
                        onMinimize = {
                            isMinimized = !isMinimized
                            val newWidth = if (isMinimized) 64 else 200
                            val newHeight = if (isMinimized) 64 else WindowManager.LayoutParams.WRAP_CONTENT
                            params.width  = dpToPx(newWidth)
                            params.height = newHeight
                            windowManager.updateViewLayout(view, params)
                        },
                        onClose = { hideOverlay(); stopSelf() }
                    )
                }
            }
        }

        overlayView = view
        windowManager.addView(view, params)
    }

    private fun hideOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onDestroy() {
        hideOverlay()
        serviceScope.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildLayoutParams(widthDp: Int, height: Int) = WindowManager.LayoutParams(
        dpToPx(widthDp), height,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = overlayX
        y = overlayY
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()
}
