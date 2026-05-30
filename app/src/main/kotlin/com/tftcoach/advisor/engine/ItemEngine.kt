// engine/ItemEngine.kt
package com.tftcoach.advisor.engine

import com.tftcoach.advisor.data.*

data class ItemAdvice(
    val item: String,
    val holder: String?,
    val isFinal: Boolean,
    val reason: String,
    val counterTarget: String? = null,
    val confidence: Float = 0.8f,
    val alternative: String? = null
)

class ItemEngine {

    fun recommend(gs: GameState): List<ItemAdvice> {
        val p = gs.player
        val result = mutableListOf<ItemAdvice>()

        // 1. 카운터 아이템 분석
        result += counterRecommendations(gs)

        // 2. 캐리 BIS 추천
        result += bisRecommendations(p)

        // 3. 조합 가능한 아이템
        result += craftableRecommendations(p)

        // 4. 템포 슬램 체크
        result += tempoSlamCheck(p)

        return result.distinctBy { it.item }.take(4)
    }

    private fun counterRecommendations(gs: GameState): List<ItemAdvice> {
        val result = mutableListOf<ItemAdvice>()
        val p = gs.player
        val adCarry = findAdCarry(p)
        val frontline = findFrontline(p)

        // 상대 보드 분석
        var avgArmor = 50f
        var healingScore = 0f
        var hasAssassins = false

        for (opp in gs.activeOpponents()) {
            val snap = opp.latest ?: continue
            for (u in snap.board) {
                val data = Set17Data.CHAMPIONS[u.name]
                if (data?.isTank == false && data.damageType == DamageType.PHYSICAL &&
                    listOf("Challenger","NOVA").any { it in data.traits }) {
                    hasAssassins = true
                }
                if (data?.isTank == true) avgArmor += 15f * u.starLevel
            }
            for (trait in snap.traitsActive) {
                if (trait.contains("Groovian") || trait.contains("Shepherd")) healingScore += 20f
            }
        }
        avgArmor = (avgArmor / maxOf(1, gs.activeOpponents().size)).coerceIn(30f, 120f)
        healingScore = healingScore.coerceIn(0f, 100f)

        val compData = Set17Data.COMP_META[p.currentComp]
        val isDamageAD = compData?.keyCarry?.let {
            Set17Data.CHAMPIONS[it]?.damageType == DamageType.PHYSICAL
        } ?: true

        if (avgArmor > 80 && isDamageAD && adCarry != null) {
            result += ItemAdvice(
                "LastWhisper", adCarry, true,
                "적 평균 방어력 ${avgArmor.toInt()} — Sunder 효과 필수",
                counterTarget = "고방어력 앞줄",
                confidence = 0.90f, alternative = "MortalReminder"
            )
        }

        if (healingScore > 50 && adCarry != null) {
            result += ItemAdvice(
                "MortalReminder", adCarry, true,
                "적 힐링 점수 ${healingScore.toInt()} — Wound 효과 필수 (17.3 EoN 너프로 더 중요)",
                counterTarget = "힐링 과다",
                confidence = 0.88f
            )
        }

        if (hasAssassins) {
            val carry = findMainCarry(p)
            result += ItemAdvice(
                "Quicksilver", carry, true,
                "적 암살자/Challenger 유닛 — 캐리 보호",
                counterTarget = "백라인 접근",
                confidence = 0.80f, alternative = "SteraksCage"
            )
        }

        return result
    }

    private fun bisRecommendations(p: PlayerState): List<ItemAdvice> {
        val result = mutableListOf<ItemAdvice>()
        for (unit in p.carries.take(2)) {
            val data = Set17Data.CHAMPIONS[unit.name] ?: continue
            val needed = data.bisItems.filter { it !in unit.items }
            if (needed.isNotEmpty()) {
                result += ItemAdvice(
                    needed[0], unit.name, true,
                    "${unit.name} BIS: ${needed[0]} 미완성",
                    confidence = 0.85f,
                    alternative = data.acceptableItems.firstOrNull()
                )
            }
        }
        return result.take(2)
    }

    private fun craftableRecommendations(p: PlayerState): List<ItemAdvice> {
        val result = mutableListOf<ItemAdvice>()
        val components = p.components
        val found = mutableSetOf<String>()

        for (i in components.indices) {
            for (j in i + 1 until components.size) {
                val crafted = Set17Data.findRecipe(components[i], components[j]) ?: continue
                if (crafted in found) continue
                found.add(crafted)

                val bestHolder = findBestHolder(p, crafted)
                result += ItemAdvice(
                    "${components[i]} + ${components[j]} → $crafted",
                    bestHolder, false,
                    "조합 가능한 아이템",
                    confidence = 0.75f
                )
            }
        }
        return result.take(2)
    }

    private fun tempoSlamCheck(p: PlayerState): List<ItemAdvice> {
        if (p.health > 35) return emptyList()
        val components = p.components
        val slamOptions = mapOf(
            "BrambleVest"  to Pair("ChainVest", "ChainVest"),
            "DragonsClaw"  to Pair("NegatronCloak", "NegatronCloak"),
            "BlueBuff"     to Pair("TearoftheGoddess", "TearoftheGoddess")
        )
        for ((item, recipe) in slamOptions) {
            if (recipe.first in components && recipe.second in components) {
                return listOf(ItemAdvice(
                    item, findFrontline(p), false,
                    "체력 ${p.health} — 즉시 슬램으로 이번 라운드 생존",
                    confidence = 0.75f
                ))
            }
        }
        return emptyList()
    }

    private fun findMainCarry(p: PlayerState) =
        p.carries.maxByOrNull { it.starLevel * 10 + it.items.size }?.name

    private fun findAdCarry(p: PlayerState) = p.carries
        .firstOrNull { Set17Data.CHAMPIONS[it.name]?.damageType == DamageType.PHYSICAL }?.name
        ?: findMainCarry(p)

    private fun findFrontline(p: PlayerState) =
        p.tanks.maxByOrNull { it.starLevel }?.name
        ?: p.board.firstOrNull { !it.isCarry }?.name

    private fun findBestHolder(p: PlayerState, item: String): String? {
        for (unit in p.carries) {
            val data = Set17Data.CHAMPIONS[unit.name] ?: continue
            if (item in data.bisItems || item in data.acceptableItems) return unit.name
        }
        return findMainCarry(p)
    }
}
