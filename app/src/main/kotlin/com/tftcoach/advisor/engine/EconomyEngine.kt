// engine/EconomyEngine.kt
package com.tftcoach.advisor.engine

import com.tftcoach.advisor.data.*

enum class EconAction {
    SAVE, GREED, LEVEL_UP, SLOW_ROLL,
    ROLL_SMALL, ROLL_DEEP, DONKEY_ROLL,
    PUSH_8, PUSH_9, STABILIZE, PRE_LEVEL
}

data class EconDecision(
    val action: EconAction,
    val goldToSpend: Int = 0,
    val reason: String = "",
    val urgency: Priority = Priority.LOW,
    val confidence: Float = 0.8f,
    val source: String = ""
)

class EconomyEngine {

    fun decide(gs: GameState): EconDecision {
        val p = gs.player
        val (stage, rnd) = p.parseRound()

        // 1. 체력 위기 → 즉시 리롤
        if (p.isCriticalHealth && p.gold >= 20) return EconDecision(
            EconAction.DONKEY_ROLL,
            goldToSpend = maxOf(0, p.gold - 10),
            reason = "체력 ${p.health}HP 위기 — 생존 우선 전체 리롤",
            urgency = Priority.CRITICAL, confidence = 0.95f
        )
        if (p.isLowHealth && p.gold >= 30) return EconDecision(
            EconAction.ROLL_DEEP,
            goldToSpend = minOf(p.gold - 20, 30),
            reason = "체력 ${p.health}HP — 보드 강화 우선 리롤",
            urgency = Priority.HIGH, confidence = 0.85f
        )

        // 2. 레벨업 타이밍
        checkLevelTiming(p, stage, rnd)?.let { return it }

        // 3. 슬로우롤 컴프
        if (isRerollComp(p.currentComp) && stage <= 4) {
            return slowRollDecision(p)
        }

        // 4. 이자 최적화
        checkInterest(p)?.let { return it }

        // 5. 연승 레벨 푸시
        if (p.streakType == StreakType.WIN && p.winStreak >= 3 && p.gold >= 50) {
            return EconDecision(
                if (p.level < 8) EconAction.PUSH_8 else EconAction.PUSH_9,
                goldToSpend = 4,
                reason = "연승 ${p.winStreak}연 — 레벨 푸시",
                urgency = Priority.MEDIUM, confidence = 0.75f,
                source = "bunnymuffins Challenger guide"
            )
        }

        return EconDecision(EconAction.SAVE, reason = "이자 유지 및 레벨업 준비", confidence = 0.70f)
    }

    private fun checkLevelTiming(p: PlayerState, stage: Int, rnd: Int): EconDecision? {
        val timing = Set17Data.LEVEL_TIMINGS[Pair(stage, rnd)] ?: return null
        if (p.level >= timing.targetLevel) return null

        val xpNeeded = Set17Data.LEVEL_XP[p.level + 1] ?: 99
        val xpBuys = maxOf(0, xpNeeded - p.xp)
        val goldNeeded = xpBuys * 4

        if (p.gold < goldNeeded + 10) return null

        return EconDecision(
            EconAction.LEVEL_UP, goldToSpend = goldNeeded,
            reason = "레벨 ${timing.targetLevel} 타이밍 ($stage-$rnd) — 현재 ${p.level}레벨",
            urgency = if (stage >= 4) Priority.HIGH else Priority.MEDIUM,
            confidence = 0.90f, source = timing.source
        )
    }

    private fun slowRollDecision(p: PlayerState): EconDecision {
        if (p.gold < 50) return EconDecision(
            EconAction.SAVE,
            reason = "슬로우롤 준비: 50G 목표 (${p.gold}/50G)",
            urgency = Priority.MEDIUM, confidence = 0.85f
        )
        val overflow = p.gold - 50
        return if (overflow >= 2) EconDecision(
            EconAction.SLOW_ROLL, goldToSpend = overflow,
            reason = "슬로우롤: 50G 유지, 초과 ${overflow}G 리롤",
            urgency = Priority.MEDIUM, confidence = 0.90f,
            source = "bunnymuffins Primordian guide"
        ) else EconDecision(EconAction.SAVE, reason = "50G 경계 유지", confidence = 0.85f)
    }

    private fun checkInterest(p: PlayerState): EconDecision? {
        val toNext = p.goldToNextInterest
        if (toNext in 1..3 && p.interest < 5) return EconDecision(
            EconAction.SAVE,
            reason = "이자 +1까지 ${toNext}G 부족. 리롤 잠깐 중지.",
            urgency = Priority.LOW, confidence = 0.80f
        )
        if (p.gold >= 50 && p.health > 40) return EconDecision(
            EconAction.GREED,
            reason = "50G 이자 달성! 레벨업 타이밍까지 유지.",
            urgency = Priority.LOW, confidence = 0.75f
        )
        return null
    }

    private fun isRerollComp(comp: String) = comp in setOf("Primordian Reroll", "Anima Cashout")

    fun shouldLevelNow(p: PlayerState): Pair<Boolean, String> {
        val (stage, rnd) = p.parseRound()
        val timing = Set17Data.LEVEL_TIMINGS[Pair(stage, rnd)] ?: return Pair(false, "레벨업 불필요")
        if (p.level >= timing.targetLevel) return Pair(false, "이미 충분")
        if (p.gold < 8) return Pair(false, "골드 부족")
        return Pair(true, "레벨 ${timing.targetLevel} 표준 타이밍 (${timing.source})")
    }
}
