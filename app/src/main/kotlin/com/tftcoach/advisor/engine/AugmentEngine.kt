// engine/AugmentEngine.kt
package com.tftcoach.advisor.engine

import com.tftcoach.advisor.data.*
import kotlin.math.min

class AugmentEngine {

    fun scoreAugment(
        augName: String,
        health: Int, gold: Int, level: Int,
        streakType: StreakType,
        activeTraits: List<String>,
        currentComp: String,
        stage: Int,
        itemsOnBench: List<String>
    ): Pair<Float, String> {
        val profile = findProfile(augName)
            ?: return Pair(5.0f, "알 수 없는 증강 — 기본 점수")

        if (profile.category == Set17Data.AugCategory.AVOID)
            return Pair(0f, "기피 증강: ${profile.avoidReason}")

        var score = profile.baseScore
        val reasons = mutableListOf<String>()

        // 체력 조건
        if (health < profile.requiresHpAbove) {
            score *= 0.5f
            reasons += "체력 ${health} < 요구 ${profile.requiresHpAbove}"
        }
        if (health > profile.requiresHpBelow && profile.requiresHpBelow < 100) {
            score *= 0.6f
            reasons += "체력 여유 시 이 증강 약함"
        }

        // 경제 증강 — 체력 위기 시 가치 하락
        if (profile.category == Set17Data.AugCategory.ECONOMY) {
            when {
                health < 30 -> { score *= 0.4f; reasons += "체력 위기 — 경제 증강 우선도 하락" }
                health > 60 -> { score *= 1.2f; reasons += "체력 여유 — 경제 증강 선호" }
            }
        }

        // 아이템 증강 — 아이템 부족 시 가치 상승
        if (profile.category == Set17Data.AugCategory.ITEMS) {
            val completedCount = itemsOnBench.count { !it.contains("Component") }
            when {
                completedCount <= 2 -> { score *= 1.3f; reasons += "아이템 부족 — 아이템 증강 선호" }
                completedCount >= 6 -> { score *= 0.8f; reasons += "아이템 충분" }
            }
        }

        // Last Stand 특수 처리
        if (augName.startsWith("Last Stand") && health <= 30) {
            return Pair(9.5f, "체력 30 이하 — Last Stand 최우선")
        }

        // 트레이트 시너지
        val traitOverlap = profile.synergyTraits.intersect(activeTraits.toSet())
        if (traitOverlap.isNotEmpty()) {
            score += traitOverlap.size * 0.8f
            reasons += "트레이트 시너지: ${traitOverlap.joinToString()}"
        }

        // 컴프 시너지
        if (currentComp in profile.synergyComps) {
            score += 1.5f
            reasons += "현재 컴프 '$currentComp'와 직접 시너지"
        }

        // 스트릭 연동
        when {
            streakType == StreakType.LOSE && profile.category == Set17Data.AugCategory.COMBAT -> {
                score += 1.0f; reasons += "연패 중 — 전투 증강 우선"
            }
            streakType == StreakType.WIN && profile.category == Set17Data.AugCategory.ECONOMY -> {
                score += 0.5f; reasons += "연승 중 — 경제 증강 고려"
            }
        }

        val reason = if (reasons.isEmpty()) "기본 평가" else reasons.joinToString(" | ")
        return Pair(score.coerceIn(0f, 10f), reason)
    }

    fun decide(
        choices: List<String>,
        health: Int, gold: Int, level: Int,
        streakType: StreakType,
        activeTraits: List<String>,
        currentComp: String,
        stage: Int,
        itemsOnBench: List<String>
    ): Triple<String, Float, String> {
        if (choices.isEmpty()) return Triple("", 0f, "선택지 없음")

        val scored = choices.map { aug ->
            val (score, reason) = scoreAugment(
                aug, health, gold, level, streakType,
                activeTraits, currentComp, stage, itemsOnBench
            )
            Triple(aug, score, reason)
        }.sortedByDescending { it.second }

        val best = scored.first()
        val detail = scored.mapIndexed { i, (a, s, r) ->
            "${if (i==0) "→" else " "} [${String.format("%.1f", s)}] $a: $r"
        }.joinToString("\n")

        return Triple(best.first, best.second, detail)
    }

    private fun findProfile(name: String): Set17Data.AugmentProfile? {
        Set17Data.AUGMENTS[name]?.let { return it }
        return Set17Data.AUGMENTS.entries
            .firstOrNull { it.key.lowercase() in name.lowercase() || name.lowercase() in it.key.lowercase() }
            ?.value
    }
}
