// ui/OverlayContent.kt
package com.tftcoach.advisor.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.tftcoach.advisor.data.*
import com.tftcoach.advisor.ui.theme.*

@Composable
fun OverlayContent(
    adviceSet: AdviceSet?,
    gameState: GameState?,
    isMinimized: Boolean,
    onMinimize: () -> Unit,
    onClose: () -> Unit
) {
    TFTCoachTheme {
        AnimatedContent(
            targetState = isMinimized,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "overlay"
        ) { mini ->
            if (mini) MiniFab(onMinimize)
            else FullOverlay(adviceSet, gameState, onMinimize, onClose)
        }
    }
}

@Composable
private fun MiniFab(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(56.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(TftAccent, TftPanelBg)))
            .border(1.dp, TftGold, CircleShape)
            .clickable(onClick = onClick)
    ) { Text("🎮", fontSize = 24.sp) }
}

@Composable
private fun FullOverlay(
    adviceSet: AdviceSet?,
    gs: GameState?,
    onMin: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.width(210.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(OverlayBg)
            .border(1.dp, OverlayBorder, RoundedCornerShape(10.dp))
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(TftPanelBg2, TftPanelBg)))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🎮 롤체 훈수봇", color = TftGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallBtn("━", onMin); SmallBtn("×", onClose)
            }
        }

        // 안전 모드 배너
        if (adviceSet?.isSafeMode == true) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(PriorityHighBg)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("⚠️", fontSize = 14.sp)
                Text(adviceSet.safetyMessage, color = PriorityHigh, fontSize = 11.sp)
            }
        }

        // 게임 상태 바
        gs?.player?.let { StatusBar(it) }

        // 조언 목록
        if (adviceSet == null || adviceSet.advices.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(12.dp), Alignment.Center) {
                Text("분석 중...", color = TextDim, fontSize = 11.sp)
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                adviceSet.top(6).forEach { advice ->
                    key(advice.message) {
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically { -it } + fadeIn(tween(180))
                        ) { AdviceCard(advice) }
                    }
                }
            }
        }

        // 상점 프리뷰
        gs?.player?.shop?.let { shop ->
            if (shop.any { it.championName.isNotBlank() }) ShopPreview(shop)
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun StatusBar(p: PlayerState) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(TftPanelBg2.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Chip("📅 ${p.roundStr}", TftAccent)
            Chip("💰 ${p.gold}G", TftGold)
            Chip("Lv.${p.level}", TftGoldLight)
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            val hpColor = when { p.health <= 20 -> PriorityHigh; p.health <= 40 -> PriorityMedium; else -> PriorityLow }
            Chip("❤️ ${p.health}", hpColor)
            Chip("+${p.interest}G/R", TftGold.copy(alpha = 0.7f))
            val stageIcon = when (p.stageType) {
                StageType.PVP -> "⚔️"; StageType.PVE -> "🐉"
                StageType.REALM -> "🌟"; StageType.AUGMENT -> "✨"; else -> "?"
            }
            Chip(stageIcon, TextSecondary)
        }
        if (p.currentComp != "Unknown") {
            Text("🎯 ${p.currentComp}", color = TftAccent,
                fontSize = 9.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ShopPreview(shop: List<ShopSlot>) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(TftPanelBg2.copy(alpha = 0.5f))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text("상점", color = TextDim, fontSize = 9.sp)
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            shop.take(5).forEach { slot ->
                if (slot.championName.isNotBlank()) {
                    val costColor = when (slot.cost) {
                        1 -> Color(0xFFB0B0B0); 2 -> Color(0xFF4CAF50)
                        3 -> Color(0xFF2196F3); 4 -> Color(0xFF9C27B0)
                        5 -> Color(0xFFFFD700); else -> TextDim
                    }
                    Text(
                        slot.championName.take(5),
                        color = costColor, fontSize = 9.sp,
                        modifier = Modifier.clip(RoundedCornerShape(3.dp))
                            .background(TftPanelBg)
                            .border(0.5.dp, costColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun SmallBtn(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(18.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(TftPanelBg)
            .border(0.5.dp, OverlayBorder, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
    ) { Text(label, color = TextSecondary, fontSize = 10.sp) }
}
