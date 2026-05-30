// engine/GMAdvisor.kt
// 통합 그랜드마스터 어드바이저 — 모든 엔진 오케스트레이션
package com.tftcoach.advisor.engine

import com.tftcoach.advisor.data.*

class GMAdvisor {

    private val scoutingEngine  = ScoutingEngine()
    private val augmentEngine   = AugmentEngine()
    private val compEngine      = CompEngine()
    private val economyEngine   = EconomyEngine()
    private val itemEngine      = ItemEngine()

    fun analyze(gs: GameState): AdviceSet {
        val p = gs.player
        val advices = mutableListOf<Advice>()

        // ── 안전 검사 먼저 ─────────────────────────────────────────────────
        if (p.stateConfidence == ConfidenceLevel.LOW) {
            return AdviceSet(
                p.roundStr, listOf(Advice(
                    AdviceType.SAFETY, Priority.CRITICAL,
                    "화면 인식 불확실 — 수동 확인 필요",
                    "OCR 신뢰도가 낮습니다. 자동화를 중단하고 직접 확인하세요.",
                    "⚠️", 0.3f
                )), 0.3f, isSafeMode = true,
                safetyMessage = "OCR 신뢰도 낮음"
            )
        }

        val confidence = when (p.stateConfidence) {
            ConfidenceLevel.HIGH   -> 0.92f
            ConfidenceLevel.MEDIUM -> 0.65f
            ConfidenceLevel.CACHED -> 0.75f
            else -> 0.4f
        }

        // ── 1. 체력 위기 ────────────────────────────────────────────────────
        when {
            p.isCriticalHealth -> advices += Advice(
                AdviceType.HEALTH, Priority.CRITICAL,
                "체력 ${p.health}HP — 즉시 전체 리롤",
                "이자 포기. 지금 당장 리롤로 생존 확보. 골드 전부 사용하세요.",
                "🚨", 0.98f
            )
            p.isLowHealth -> advices += Advice(
                AdviceType.HEALTH, Priority.HIGH,
                "체력 ${p.health}HP — 리롤 우선",
                "이자보다 생존 우선. 보드 강화가 필요합니다.",
                "❤️", 0.95f
            )
            p.health <= 35 -> advices += Advice(
                AdviceType.HEALTH, Priority.MEDIUM,
                "체력 주의 ${p.health}HP",
                "체력이 낮습니다. 2라운드 안에 보드를 개선하세요.",
                "💛", 0.90f
            )
        }

        // ── 2. 증강 선택 ────────────────────────────────────────────────────
        if (p.isAugmentPhase && p.augmentChoices.isNotEmpty()) {
            val (chosen, score, detail) = augmentEngine.decide(
                p.augmentChoices, p.health, p.gold, p.level,
                p.streakType,
                p.activeTraits.map { it.traitName },
                p.currentComp, p.stageNum,
                p.components
            )
            advices += Advice(
                AdviceType.AUGMENT, Priority.HIGH,
                "✨ 증강 추천: $chosen",
                detail, "✨",
                minOf(0.95f, score / 10f),
                source = "AugmentEngine (컨텍스트 점수)"
            )
        }

        // ── 3. Realm of the Gods ────────────────────────────────────────────
        if (p.isRealmRound && p.godBlessingChoices.isNotEmpty()) {
            advices += realmAdvice(p)
        }

        // ── 4. Anima 전략 ───────────────────────────────────────────────────
        if (p.augmentsTaken.any { it.contains("Anima") }) {
            animaAdvice(p)?.let { advices += it }
        }

        // ── 5. 경제·레벨링 ─────────────────────────────────────────────────
        val econ = economyEngine.decide(gs)
        advices += Advice(
            econToAdviceType(econ.action), econ.urgency,
            econ.reason,
            "행동: ${econ.action.name}" +
                    if (econ.goldToSpend > 0) ", 사용: ${econ.goldToSpend}G" else "",
            econEmoji(econ.action), econ.confidence,
            source = econ.source,
            goldCost = econ.goldToSpend
        )

        // ── 6. 컴프 방향 및 피벗 ───────────────────────────────────────────
        if (p.stageNum >= 3) {
            val boardNames = p.board.map { it.name }
            val items = p.itemBench.map { it.itemName }

            if (p.currentComp == "Unknown") {
                val top = compEngine.selectTop3(items, p.augmentsTaken,
                    boardNames, scoutingEngine, gs, 0, p.health, p.stageNum)
                top.firstOrNull()?.let {
                    advices += Advice(
                        AdviceType.COMP, Priority.MEDIUM,
                        "🎯 컴프 추천: ${it.name} (${String.format("%.1f", it.score)}점)",
                        it.reason + "\n\n게임플랜: ${compEngine.getGameplan(it.name)}",
                        "🎯", 0.70f, source = "CompEngine + ScoutingEngine"
                    )
                }
            } else {
                compEngine.pivotCheck(
                    p.currentComp, items, p.augmentsTaken, boardNames,
                    scoutingEngine, gs, 0, p.health, p.stageNum
                )?.let {
                    advices += Advice(
                        AdviceType.PIVOT, Priority.MEDIUM,
                        it.lines().firstOrNull() ?: "피벗 권고",
                        it, "🔀", 0.68f, source = "CompEngine + ScoutingEngine"
                    )
                }
            }
        }

        // ── 7. 아이템 추천 ──────────────────────────────────────────────────
        for (ia in itemEngine.recommend(gs).take(3)) {
            val type = if (ia.counterTarget != null) AdviceType.COUNTER_ITEM else AdviceType.ITEM
            val pri  = if (ia.counterTarget != null) Priority.MEDIUM else Priority.LOW
            advices += Advice(
                type, pri,
                "${ia.item} → ${ia.holder ?: "?"}",
                ia.reason + (ia.alternative?.let { "\n대안: $it" } ?: ""),
                if (ia.counterTarget != null) "🛡️" else "⚔️",
                ia.confidence
            )
        }

        // ── 8. 스카우팅 ─────────────────────────────────────────────────────
        val lobby = scoutingEngine.lobbyAnalysis(gs, selfId = 0)
        lobby.mostContestedUnits.firstOrNull { it.contestCount >= 2 }?.let {
            advices += Advice(
                AdviceType.SCOUTING, Priority.MEDIUM,
                "👁️ '${it.unitName}' ${it.contestCount}명 경쟁",
                "이 유닛을 노리면 3성이 매우 어렵습니다. 대안을 고려하세요.",
                "👁️", 0.75f * it.contestScore, source = "ScoutingEngine"
            )
        }
        for ((pid, comp) in lobby.pivotWarnings.take(1)) {
            advices += Advice(
                AdviceType.SCOUTING, Priority.LOW,
                "👁️ 플레이어${pid} → $comp 피벗 감지",
                "경쟁 유닛이 줄어들거나 늘어날 수 있습니다.",
                "👁️", 0.55f, source = "ScoutingEngine"
            )
        }

        // ── 9. 스트릭 ───────────────────────────────────────────────────────
        streakAdvice(p)?.let { advices += it }

        // ── 10. 이자 최적화 ─────────────────────────────────────────────────
        if (p.goldToNextInterest in 1..3 && p.interest < 5) {
            advices += Advice(
                AdviceType.ECON, Priority.LOW,
                "💰 이자 +1까지 ${p.goldToNextInterest}G 부족",
                "골드 ${p.gold + p.goldToNextInterest}G까지 모으면 이자 ${p.interest + 1}G/라운드",
                "💰", 0.95f, source = "게임 메카닉 [VERIFIED_OFFICIAL]"
            )
        }

        // ── 11. 현재 컴프 포지셔닝 힌트 ────────────────────────────────────
        if (p.currentComp != "Unknown" && p.stageType == StageType.PVP) {
            advices += Advice(
                AdviceType.POSITIONING, Priority.LOW,
                "🗺️ ${p.currentComp} 배치",
                compEngine.getPositioningNote(p.currentComp),
                "🗺️", 0.80f, source = "bunnymuffins + tftactics"
            )
        }

        return AdviceSet(
            roundStr = p.roundStr,
            advices = advices.sortedBy { it.priority.ordinal },
            stateConfidence = confidence,
            isSafeMode = false
        )
    }

    // ── 헬퍼들 ───────────────────────────────────────────────────────────────

    private fun realmAdvice(p: PlayerState): Advice {
        val choices = p.godBlessingChoices
        val aligned = p.godAlignment

        if (aligned != "None" && aligned in choices) return Advice(
            AdviceType.REALM, Priority.HIGH,
            "🌟 Realm: $aligned 정렬 유지",
            "4-7 Major Blessing을 위해 $aligned 계속 선택.",
            "🌟", 0.92f, source = "games.gg Set 17 [VERIFIED_GUIDE]"
        )

        val hints = choices.mapNotNull { god ->
            Set17Data.GOD_PROFILES[god]?.let { "$god: $it" }
        }
        return Advice(
            AdviceType.REALM, Priority.MEDIUM,
            "🌟 Realm 선택: ${choices.take(2).joinToString(" / ")}",
            hints.joinToString("\n").ifEmpty { "신 선택 정보 부족" },
            "🌟", 0.65f, source = "games.gg + [INFERRED]"
        )
    }

    private fun animaAdvice(p: PlayerState): Advice? {
        val tech = p.animaTechStacks
        return when {
            tech >= 600 -> Advice(
                AdviceType.ANIMA, Priority.HIGH,
                "🔴 Anima Tech $tech — 지금 현금화!",
                "600 Tech 달성. Tier 3 무기 교환 후 레벨 8 진입하세요.",
                "🔴", 0.95f, source = "mobalytics Set 17 Anima guide [VERIFIED_GUIDE]"
            )
            tech >= 400 -> Advice(
                AdviceType.ANIMA, Priority.LOW,
                "🔴 Anima Tech $tech/600 — 연패 계속",
                "Tech 600 목표. 의도적 연패로 스택 쌓기.",
                "🔴", 0.85f
            )
            else -> null
        }
    }

    private fun streakAdvice(p: PlayerState): Advice? = when {
        p.streakType == StreakType.WIN && p.winStreak >= 3 -> Advice(
            AdviceType.STREAK, Priority.LOW,
            "🔥 연승 ${p.winStreak}연 — 이자+레벨 유지",
            "연승 보너스 유지. 골드 쌓으면서 레벨 푸시.",
            "🔥", 0.80f, source = "bunnymuffins Challenger guide"
        )
        p.streakType == StreakType.LOSE && p.loseStreak >= 3 &&
                !p.augmentsTaken.any { it.contains("Anima") } -> {
            val pri = if (p.health <= 35) Priority.HIGH else Priority.MEDIUM
            Advice(
                AdviceType.STREAK, pri,
                "📉 연패 ${p.loseStreak}연 — 전략 재검토",
                "체력 여유 있으면 이자 유지. 낮으면 즉시 리롤.",
                "📉", 0.75f
            )
        }
        else -> null
    }

    private fun econToAdviceType(action: EconAction) = when (action) {
        EconAction.SLOW_ROLL, EconAction.ROLL_DEEP,
        EconAction.DONKEY_ROLL, EconAction.ROLL_SMALL -> AdviceType.ROLL
        EconAction.LEVEL_UP, EconAction.PRE_LEVEL,
        EconAction.PUSH_8, EconAction.PUSH_9 -> AdviceType.LEVEL_UP
        else -> AdviceType.ECON
    }

    private fun econEmoji(action: EconAction) = when (action) {
        EconAction.SAVE, EconAction.GREED -> "💰"
        EconAction.LEVEL_UP, EconAction.PUSH_8, EconAction.PUSH_9 -> "⬆️"
        EconAction.SLOW_ROLL, EconAction.ROLL_DEEP,
        EconAction.DONKEY_ROLL -> "🔄"
        else -> "💰"
    }

    // 스카우팅 업데이트 외부에서 호출
    fun updateOpponent(gs: GameState, snap: OpponentSnapshot) {
        scoutingEngine.updateOpponent(gs, snap)
    }
}
