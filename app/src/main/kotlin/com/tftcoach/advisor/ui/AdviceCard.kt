// ui/AdviceCard.kt
package com.tftcoach.advisor.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.tftcoach.advisor.data.*
import com.tftcoach.advisor.ui.theme.*

@Composable
fun AdviceCard(advice: Advice) {
    var expanded by remember { mutableStateOf(false) }

    val priorityColor = Color(advice.priorityColor)
    val bgColor = priorityColor.copy(alpha = 0.15f)

    // CRITICAL/HIGH → 펄스 애니메이션
    val pulse = rememberInfiniteTransition(label = "pulse")
    val borderAlpha by if (advice.priority in listOf(Priority.CRITICAL, Priority.HIGH)) {
        pulse.animateFloat(0.4f, 1.0f,
            infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "ba")
    } else remember { mutableStateOf(0.5f) }

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(
                if (advice.priority == Priority.CRITICAL) 1.5.dp else 0.5.dp,
                priorityColor.copy(alpha = borderAlpha),
                RoundedCornerShape(6.dp)
            )
            .clickable(enabled = advice.detail.isNotBlank()) { expanded = !expanded }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // 우선순위 도트
            Box(Modifier.size(7.dp).background(priorityColor, RoundedCornerShape(50)))
            Text(
                "${advice.emoji} ${advice.message}",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = if (advice.priority in listOf(Priority.CRITICAL, Priority.HIGH))
                    FontWeight.Bold else FontWeight.Normal,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (advice.detail.isNotBlank()) {
                Text(if (expanded) "▲" else "▼", color = TextSecondary, fontSize = 9.sp)
            }
        }

        // 우선순위 라벨
        Text(
            advice.priorityLabel,
            color = priorityColor,
            fontSize = 8.sp,
            modifier = Modifier.padding(start = 12.dp, top = 1.dp)
        )

        // 확장 상세
        if (expanded && advice.detail.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                advice.detail,
                color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
            if (advice.source.isNotBlank()) {
                Text(
                    "📌 ${advice.source}",
                    color = TextDim, fontSize = 8.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                )
            }
            if (advice.alternatives.isNotEmpty()) {
                Text(
                    "대안: ${advice.alternatives.joinToString()}",
                    color = TextDim, fontSize = 9.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 1.dp)
                )
            }
        }
    }
}
