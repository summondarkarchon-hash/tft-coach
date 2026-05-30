// engine/ScoutingEngine.kt
package com.tftcoach.advisor.engine

import com.tftcoach.advisor.data.*
import kotlin.math.abs
import kotlin.math.min

class ScoutingEngine {

    private val DECAY_PER_ROUND = 0.15f
    private val COMP_SIGNATURES = Set17Data.COMP_SIGNATURES

    fun updateOpponent(gs: GameState, snap: OpponentSnapshot) {
        snap.inferredComp = inferComp(snap)
        snap.compConfidence = calcCompConfidence(snap)
        snap.isCommitted = detectCommitment(snap)

        val opp = gs.opponents.getOrPut(snap.playerId) { OpponentState(snap.playerId) }
        opp.addSnapshot(snap)
    }

    fun unitContest(gs: GameState, unitName: String, selfId: Int = -1): ContestReport {
        var score = 0f
        val contestedBy = mutableListOf<Int>()
        val currentRound = gs.player.roundStr

        for ((pid, opp) in gs.opponents) {
            if (pid == selfId || opp.isEliminated) continue
            val latest = opp.latest ?: continue

            val ageRounds = roundDiff(latest.roundStr, currentRound)
            val weight = maxOf(0.1f, 1f - ageRounds * DECAY_PER_ROUND)

            val allUnits = latest.board + latest.bench
            val unit = allUnits.firstOrNull { it.name == unitName }
            if (unit != null) {
                val starWeight = unit.starLevel * 0.5f
                score += weight * (0.5f + starWeight)
                contestedBy.add(pid)
            }
        }
        val count = contestedBy.size
        val normalized = if (count > 0) min(1f, score / maxOf(1, count)) else 0f
        return ContestReport(unitName, count, normalized, contestedBy)
    }

    fun compOpenness(gs: GameState, compName: String, selfId: Int = -1): Float {
        val keyUnits = COMP_SIGNATURES[compName] ?: return 0.5f
        val activeCount = gs.activeOpponents().size
        if (activeCount == 0) return 1f

        val totalContest = keyUnits.sumOf { unitContest(gs, it, selfId).contestCount }
        val maxPossible = keyUnits.size * activeCount
        return 1f - min(1f, totalContest.toFloat() / maxPossible)
    }

    fun lobbyAnalysis(gs: GameState, selfId: Int = -1): LobbyAnalysis {
        val allUnits = gs.activeOpponents()
            .mapNotNull { it.latest }
            .flatMap { it.board + it.bench }
            .map { it.name }.toSet()

        val unitReports = allUnits
            .map { unitContest(gs, it, selfId) }
            .sortedByDescending { it.contestScore }
            .take(5)

        val allTraits = gs.activeOpponents()
            .mapNotNull { it.latest }
            .flatMap { it.traitsActive }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(5).map { Pair(it.key, it.value) }

        val compScores = COMP_SIGNATURES.keys.map { comp ->
            Pair(comp, compOpenness(gs, comp, selfId))
        }
        val openComps = compScores.filter { it.second > 0.5f }
            .sortedByDescending { it.second }.take(3)
        val contestedComps = compScores.filter { it.second <= 0.5f }
            .sortedBy { it.second }.take(3)

        val pivotWarnings = gs.opponents.entries
            .filter { !it.value.isEliminated && it.value.isPivoting() }
            .mapNotNull { (pid, opp) ->
                opp.latest?.let { Pair(pid, it.inferredComp) }
            }

        return LobbyAnalysis(unitReports, allTraits, openComps, contestedComps, pivotWarnings)
    }

    fun pivotWarning(gs: GameState, myComp: String, selfId: Int = -1): String? {
        val keyUnits = COMP_SIGNATURES[myComp] ?: return null
        val contested = keyUnits.filter { unitContest(gs, it, selfId).contestCount >= 1 }
        return if (contested.size >= 2)
            "⚠️ '$myComp' 경쟁 심함: ${contested.take(2).joinToString(", ")} 다른 플레이어 보유"
        else null
    }

    private fun inferComp(snap: OpponentSnapshot): String {
        val allUnits = (snap.board + snap.bench).map { it.name }.toSet()
        var best = "Unknown"
        var bestScore = 0f
        for ((comp, sig) in COMP_SIGNATURES) {
            val overlap = allUnits.intersect(sig.toSet()).size
            val starBonus = (snap.board + snap.bench)
                .filter { it.name in sig }.sumOf { it.starLevel - 1 } * 0.2f
            val score = overlap.toFloat() / sig.size + starBonus
            if (score > bestScore) { bestScore = score; best = comp }
        }
        return if (bestScore >= 0.3f) best else "Unknown"
    }

    private fun calcCompConfidence(snap: OpponentSnapshot): Float {
        val allUnits = (snap.board + snap.bench).map { it.name }.toSet()
        val sig = COMP_SIGNATURES[snap.inferredComp] ?: return 0f
        return min(1f, allUnits.intersect(sig.toSet()).size.toFloat() / sig.size)
    }

    private fun detectCommitment(snap: OpponentSnapshot): Boolean =
        (snap.board + snap.bench).count { it.starLevel >= 2 } >= 3

    private fun roundDiff(a: String, b: String): Int {
        fun toNum(r: String) = try {
            val p = r.split("-"); p[0].toInt() * 10 + p[1].toInt()
        } catch (e: Exception) { 0 }
        return abs(toNum(a) - toNum(b))
    }
}

data class ContestReport(
    val unitName: String,
    val contestCount: Int,
    val contestScore: Float,
    val contestedBy: List<Int>
)

data class LobbyAnalysis(
    val mostContestedUnits: List<ContestReport>,
    val mostContestedTraits: List<Pair<String, Int>>,
    val openComps: List<Pair<String, Float>>,
    val contestedComps: List<Pair<String, Float>>,
    val pivotWarnings: List<Pair<Int, String>>
)
