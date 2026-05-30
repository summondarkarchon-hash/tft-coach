// viewmodel/MainViewModel.kt
package com.tftcoach.advisor.viewmodel

import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tftcoach.advisor.data.*
import com.tftcoach.advisor.engine.GMAdvisor
import com.tftcoach.advisor.service.OverlayService
import com.tftcoach.advisor.service.ScreenCaptureService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val isRunning: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val currentAdviceSet: AdviceSet? = null,
    val currentGameState: GameState? = null,
    val errorMessage: String? = null,
    val overlayAlpha: Float = 0.88f,
    val filterTypes: Set<AdviceType> = AdviceType.values().toSet()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    val advisor = GMAdvisor()

    private val _ui = MutableStateFlow(MainUiState())
    val ui: StateFlow<MainUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                ScreenCaptureService.gameStateFlow,
                ScreenCaptureService.isRunningFlow,
                ScreenCaptureService.errorFlow
            ) { gs, running, err -> Triple(gs, running, err) }
            .collect { (gs, running, err) ->
                val adviceSet = advisor.analyze(gs)
                val filtered = adviceSet.copy(
                    advices = adviceSet.advices.filter {
                        it.type in _ui.value.filterTypes
                    }
                )
                _ui.update { it.copy(
                    isRunning = running,
                    currentAdviceSet = filtered,
                    currentGameState = gs,
                    errorMessage = err
                )}
            }
        }
    }

    fun checkOverlayPermission() {
        _ui.update { it.copy(hasOverlayPermission = Settings.canDrawOverlays(ctx)) }
    }

    fun openOverlayPermissionSettings() {
        ctx.startActivity(Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${ctx.packageName}")
        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    }

    fun startCapture(resultCode: Int, data: Intent) {
        ctx.startForegroundService(
            Intent(ctx, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
            }
        )
        ctx.startService(Intent(ctx, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW
        })
    }

    fun stopCapture() {
        ctx.startService(Intent(ctx, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP
        })
        ctx.startService(Intent(ctx, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE
        })
    }

    fun setAlpha(v: Float) = _ui.update { it.copy(overlayAlpha = v) }
    fun toggleFilter(t: AdviceType) {
        val cur = _ui.value.filterTypes.toMutableSet()
        if (t in cur) cur.remove(t) else cur.add(t)
        _ui.update { it.copy(filterTypes = cur) }
    }
}
