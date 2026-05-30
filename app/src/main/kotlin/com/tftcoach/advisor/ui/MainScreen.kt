// ui/MainScreen.kt
package com.tftcoach.advisor.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.tftcoach.advisor.data.AdviceType
import com.tftcoach.advisor.ui.theme.*
import com.tftcoach.advisor.viewmodel.MainUiState

@Composable
fun MainScreen(
    uiState: MainUiState,
    onRequestCapture: () -> Unit,
    onStop: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onAlphaChange: (Float) -> Unit,
    onToggleFilter: (AdviceType) -> Unit
) {
    TFTCoachTheme {
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(TftDarkBg, TftPanelBg2))
        )) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 로고
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎮", fontSize = 44.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("롤체 훈수봇", color = TftGold, fontSize = 24.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("실시간 Set 17 전략 어드바이저", color = TextSecondary,
                        fontSize = 12.sp)
                    Text("Patch 17.3", color = TextDim, fontSize = 10.sp)
                }

                // 상태
                val dotColor by animateColorAsState(
                    if (uiState.isRunning) PriorityLow else TextDim, tween(400), label = "dc")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(TftPanelBg)
                        .border(0.5.dp, OverlayBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(Modifier.size(10.dp).background(dotColor, RoundedCornerShape(50)))
                    Text(if (uiState.isRunning) "모니터링 중" else "대기 중",
                        color = if (uiState.isRunning) PriorityLow else TextSecondary,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                // 오류
                uiState.errorMessage?.let { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PriorityHighBg)
                            .border(1.dp, PriorityHigh.copy(0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                        Text(msg, color = PriorityHigh, fontSize = 12.sp)
                    }
                }

                // 시작/중지
                Button(
                    onClick = { if (uiState.isRunning) onStop() else onRequestCapture() },
                    enabled = uiState.hasOverlayPermission || uiState.isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isRunning) PriorityHigh else TftAccent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(if (uiState.isRunning) "⏹ 중지" else "▶ 시작",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                if (!uiState.hasOverlayPermission && !uiState.isRunning) {
                    Text("오버레이 권한을 먼저 허용해주세요",
                        color = PriorityMedium, fontSize = 11.sp, textAlign = TextAlign.Center)
                }

                Divider(color = OverlayBorder, thickness = 0.5.dp)

                // 권한 상태
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("권한 상태", color = TftGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    PermRow("오버레이 권한", uiState.hasOverlayPermission, onOpenOverlaySettings)
                    PermRow("화면 캡처 권한", true, {})
                }

                Divider(color = OverlayBorder, thickness = 0.5.dp)

                // 설정
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("설정", color = TftGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("오버레이 투명도", color = TextPrimary, fontSize = 13.sp)
                        Text("${(uiState.overlayAlpha * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
                    }
                    Slider(uiState.overlayAlpha, onAlphaChange, 0.3f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = TftGold, activeTrackColor = TftAccent),
                        modifier = Modifier.fillMaxWidth())

                    Text("표시할 조언 필터", color = TextSecondary, fontSize = 12.sp)
                    val labels = listOf(
                        AdviceType.ECON to "💰 골드/이자",
                        AdviceType.LEVEL_UP to "⬆️ 레벨업",
                        AdviceType.ROLL to "🔄 리롤",
                        AdviceType.AUGMENT to "✨ 증강",
                        AdviceType.ITEM to "⚔️ 아이템",
                        AdviceType.COUNTER_ITEM to "🛡️ 카운터",
                        AdviceType.POSITIONING to "🗺️ 배치",
                        AdviceType.SCOUTING to "👁️ 스카우팅",
                        AdviceType.STREAK to "🔥 스트릭",
                        AdviceType.ANIMA to "🔴 Anima"
                    )
                    for (i in labels.indices step 2) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(labels[i].first, labels[i].second,
                                labels[i].first in uiState.filterTypes, onToggleFilter,
                                Modifier.weight(1f))
                            if (i + 1 < labels.size) FilterChip(labels[i+1].first, labels[i+1].second,
                                labels[i+1].first in uiState.filterTypes, onToggleFilter,
                                Modifier.weight(1f))
                            else Spacer(Modifier.weight(1f))
                        }
                    }
                }

                // 현재 게임 상태 (실행 중일 때)
                if (uiState.isRunning) {
                    uiState.currentGameState?.player?.let { p ->
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(TftPanelBg)
                                .border(0.5.dp, OverlayBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("현재 게임 상태", color = TftGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${p.roundStr}  |  레벨 ${p.level}  |  골드 ${p.gold}G  |  체력 ${p.health}HP",
                                color = TextSecondary, fontSize = 11.sp)
                            Text("이자 +${p.interest}G  |  ${p.stageType}  |  ${p.streakType}",
                                color = TextSecondary, fontSize = 11.sp)
                            if (p.currentComp != "Unknown")
                                Text("컴프: ${p.currentComp}", color = TftAccent, fontSize = 11.sp)
                            Text("감지된 조언: ${uiState.currentAdviceSet?.advices?.size ?: 0}개",
                                color = TextDim, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermRow(label: String, granted: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (granted) "✅" else "❌", fontSize = 14.sp)
            Text(label, color = TextPrimary, fontSize = 13.sp)
        }
        if (!granted) TextButton(onClick = onFix) {
            Text("설정", color = TftAccent, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FilterChip(
    type: AdviceType, label: String, checked: Boolean,
    onToggle: (AdviceType) -> Unit, modifier: Modifier
) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(6.dp))
            .background(if (checked) TftAccentDim else TftPanelBg)
            .border(0.5.dp, if (checked) TftAccent else OverlayBorder, RoundedCornerShape(6.dp))
            .clickable { onToggle(type) }
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Checkbox(checked, { onToggle(type) },
            colors = CheckboxDefaults.colors(TftAccent, TextDim, TextPrimary),
            modifier = Modifier.size(14.dp))
        Text(label, color = if (checked) TextPrimary else TextSecondary, fontSize = 9.sp)
    }
}
