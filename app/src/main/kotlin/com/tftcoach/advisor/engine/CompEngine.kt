// engine/CompEngine.kt
package com.tftcoach.advisor.engine

import com.tftcoach.advisor.data.*
import kotlin.math.min

data class CompScore(val name: String, val score: Float, val reason: String)

class CompEngine {

    private val TIER_SCORE = mapOf("S" to 10f, "A" to 8f, "B" to 6f, "C" to 4f)

    // Set 17 컴프 메타 정보 [bunnymuffins patch 17.3]
    data class CompMeta(
        val tier: String,
        val strategyType: String,  // "fast8","reroll","fast9","anima_cashout"
        val keyCarry: String,
        val keyUnits: List<String>,
        val enablingAugments: List<String>,
        val enablingItems: List<String>,
        val contestSensitive: Boolean,
        val levelTiming: Map<String, Int>,
        val positioningNote: String,
        val gameplan: String
    )

    val COMP_META: Map<String, CompMeta> = mapOf(
        "Dark Star Jhin" to CompMeta(
            "S", "fast8", "Jhin",
            listOf("Jhin","Xayah","Gwen","Riven","Lissandra","Morgana"),
            listOf("You Have My Bow","Hold the Line","Component Grab Bag"),
            listOf("BFSword","NeedlesslyLargeRod"),
            false,
            mapOf("4-1" to 7, "4-2" to 8, "5-2" to 9),
            "Jhin 절대 최원거리 코너. Morgana 17.3 4코 탱커로 앞줄 투입.",
            "2-1 Dark Star 확보. Morgana 4코로 앞줄 안정화. 6 Dark Star에서 Jhin Supermassive."
        ),
        "Primordian Reroll" to CompMeta(
            "A", "reroll", "BelVeth",
            listOf("Kindred","BelVeth","Briar"),
            listOf("Grab Bag","Buried Treasures","Featherweights"),
            listOf("RecurveBow","ChainVest"),
            true,
            mapOf("3-1" to 4, "3-2" to 5, "5-2" to 9),
            "BelVeth 뒷줄. Swarmling 소환 공간 확보.",
            "50G 슬로우롤. Kindred/BelVeth/Briar 3성 완성 후 Challenger 추가. 17.3 너프로 경쟁 없을 때만 권장."
        ),
        "Anima Cashout" to CompMeta(
            "A", "anima_cashout", "Jinx",
            listOf("Briar","Jinx","Aurora","Illaoi","Fiora"),
            listOf("Tiny but Deadly","Combat Training"),
            listOf("RecurveBow","NegatronCloak"),
            false,
            mapOf("2-1" to 3, "4-1" to 7, "5-2" to 9),
            "연패 중 배치 최적화 불필요. Tech 현금화 후 재배치.",
            "2-1 Anima 확보 이상적. 17.3에서 6 Anima 가능해짐."
        ),
        "Psionic Yi" to CompMeta(
            "A", "fast8", "Yi",
            listOf("Yi","Kindred","Vex","Viktor","Lulu"),
            listOf("Axiom Arc","Pumping Up","Extended Duel"),
            listOf("BFSword","RecurveBow","SparringGloves"),
            false,
            mapOf("4-1" to 7, "4-2" to 8, "5-2" to 9),
            "Yi Edge of Night aggro drop. 적 캐리 반대쪽 배치.",
            "다양한 운영에서 진입 가능. Guinsoo 슬램 우선."
        ),
        "Arbiter LeBlanc" to CompMeta(
            "A", "fast8", "LeBlanc",
            listOf("LeBlanc","Diana","Leona","Zoe"),
            listOf("InfiniTeam","Better Together","Axiom Arc"),
            listOf("TearoftheGoddess","RecurveBow"),
            false,
            mapOf("4-1" to 7, "4-2" to 8, "5-2" to 9),
            "LeBlanc 뒷줄. Arbiter 법칙으로 추가 강화.",
            "4 Vanguard 버전이 가장 안정적. LeBlanc BIS 먼저 완성."
        ),
        "Shepherd Corki" to CompMeta(
            "A", "fast8", "Corki",
            listOf("Corki","Bard","Gnar","Fizz","Riven"),
            listOf("You Have My Bow","Tri Force"),
            listOf("BFSword","NeedlesslyLargeRod"),
            false,
            mapOf("4-1" to 7, "4-2" to 8, "5-2" to 9),
            "Corki 뒷줄. Riven/Rammus 앞줄.",
            "17.3 메타. 4-2에 레벨 8. Riven은 AD+AP 모두 스케일."
        ),
        "Groovian" to CompMeta(
            "B", "fast8", "Jinx",
            listOf("Milio","Jinx","Gragas","John","TahmKench"),
            listOf("Gotta Go Fast","Sunfire Board"),
            listOf("RecurveBow","NegatronCloak"),
            false,
            mapOf("4-1" to 7, "4-2" to 8, "5-2" to 9),
            "Jinx 뒷줄. Gragas/John/TahmKench 앞줄.",
            "Groove 버프로 Jinx 극대화."
        )
    )

    fun evaluate(
        compName: String,
        itemsOnBench: List<String>,
        augmentsTaken: List<String>,
        currentBoard: List<String>,
        scout: ScoutingEngine,
        gs: GameState,
        selfId: Int,
        health: Int,
        stage: Int
    ): CompScore {
        val meta = COMP_META[compName]
            ?: return CompScore(compName, 3f, "메타 정보 없음")

        var score = TIER_SCORE[meta.tier] ?: 5f
        val reasons = mutableListOf<String>()

        // 아이템 시너지
        val itemOverlap = meta.enablingItems.intersect(itemsOnBench.toSet())
        if (itemOverlap.isNotEmpty()) {
            score += itemOverlap.size * 1.5f
            reasons += "핵심 아이템 보유: ${itemOverlap.joinToString()}"
        }

        // 증강 시너지
        val augOverlap = meta.enablingAugments.intersect(augmentsTaken.toSet())
        if (augOverlap.isNotEmpty()) {
            score += augOverlap.size * 2.0f
            reasons += "증강 시너지: ${augOverlap.joinToString()}"
        }

        // 현재 보드 유닛 겹침
        val boardOverlap = meta.keyUnits.intersect(currentBoard.toSet())
        if (boardOverlap.isNotEmpty()) {
            score += boardOverlap.size * 1.0f
            reasons += "핵심 유닛 보유: ${boardOverlap.joinToString()}"
        }

        // 경쟁도 반영
        if (meta.contestSensitive) {
            val contestedUnits = meta.keyUnits.filter {
                scout.unitContest(gs, it, selfId).contestCount >= 2
            }
            if (contestedUnits.size >= 2) {
                score -= 3.0f
                reasons += "경쟁 심함: ${contestedUnits.take(2).joinToString()}"
            } else if (contestedUnits.size == 1) {
                score -= 1.0f
                reasons += "경쟁 1명: ${contestedUnits[0]}"
            }
        } else {
            val carryContest = scout.unitContest(gs, meta.keyCarry, selfId).contestCount
            if (carryContest >= 2) {
                score -= 1.5f
                reasons += "핵심 캐리 '${meta.keyCarry}' ${carryContest}명 경쟁"
            }
        }

        // Anima 스테이지 4 이후 체크
        if (meta.strategyType == "anima_cashout" && stage >= 4) {
            val hasAnima = augmentsTaken.any { it.contains("Anima") }
            if (!hasAnima) {
                score -= 4.0f
                reasons += "Anima 증강 미확보 — 스테이지 4 이후 불가"
            }
        }

        // 체력 위기 시 빠른 컴프 선호
        if (health < 30 && meta.strategyType in listOf("fast8", "fast9")) {
            score += 1.0f
            reasons += "체력 위기 — 빠른 레벨업 컴프 가중치"
        }

        val reason = if (reasons.isEmpty()) "기본 평가" else reasons.joinToString(" | ")
        return CompScore(compName, score.coerceIn(0f, 15f), reason)
    }

    fun selectTop3(
        itemsOnBench: List<String>,
        augmentsTaken: List<String>,
        currentBoard: List<String>,
        scout: ScoutingEngine,
        gs: GameState,
        selfId: Int,
        health: Int,
        stage: Int
    ): List<CompScore> = COMP_META.keys
        .map { evaluate(it, itemsOnBench, augmentsTaken, currentBoard, scout, gs, selfId, health, stage) }
        .sortedByDescending { it.score }
        .take(3)

    fun pivotCheck(
        currentComp: String,
        itemsOnBench: List<String>,
        augmentsTaken: List<String>,
        currentBoard: List<String>,
        scout: ScoutingEngine,
        gs: GameState,
        selfId: Int,
        health: Int,
        stage: Int
    ): String? {
        val curScore = evaluate(currentComp, itemsOnBench, augmentsTaken, currentBoard,
            scout, gs, selfId, health, stage).score
        val alts = selectTop3(itemsOnBench, augmentsTaken, currentBoard,
            scout, gs, selfId, health, stage)
        val best = alts.firstOrNull { it.name != currentComp } ?: return null

        return if (best.score - curScore >= 2.0f)
            "🔀 피벗 권고: '$currentComp'(${String.format("%.1f", curScore)}) → '${best.name}'(${String.format("%.1f", best.score)})\n${best.reason}"
        else null
    }

    fun getPositioningNote(compName: String): String =
        COMP_META[compName]?.positioningNote ?: "탱커 앞줄(1~2열), 딜러 뒷줄(3~4열) 기본 배치"

    fun getGameplan(compName: String): String =
        COMP_META[compName]?.gameplan ?: "정보 없음"

    fun isRerollComp(compName: String): Boolean =
        COMP_META[compName]?.strategyType in listOf("reroll", "anima_cashout")
}
