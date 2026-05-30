// data/GameState.kt
package com.tftcoach.advisor.data

import kotlin.math.min

// ── 열거형 ────────────────────────────────────────────────────────────────────

enum class StageType { PVP, PVE, REALM, AUGMENT, UNKNOWN }
enum class StreakType { WIN, LOSE, NONE }
enum class DamageType { PHYSICAL, MAGIC, MIXED }
enum class Priority { CRITICAL, HIGH, MEDIUM, LOW, INFO }
enum class AdviceType {
    HEALTH, LEVEL_UP, ROLL, ECON, AUGMENT, COMP, PIVOT,
    ITEM, COUNTER_ITEM, POSITIONING, SCOUTING, REALM,
    ANIMA, STREAK, ROLLDOWN, SAFETY
}
enum class ConfidenceLevel { HIGH, MEDIUM, LOW, CACHED }

// ── 기본 데이터 단위 ──────────────────────────────────────────────────────────

data class UnitInstance(
    val name: String,
    val starLevel: Int = 1,
    val position: Int = -1,
    val items: List<String> = emptyList(),
    val components: List<String> = emptyList(),
    val isCarry: Boolean = false,
    val isTank: Boolean = false,
    val confidence: Float = 1.0f
) {
    val totalItemCount: Int get() = items.size + components.size
}

data class ShopSlot(
    val index: Int,
    val championName: String,
    val cost: Int = 0,
    val confidence: Float = 1.0f
)

data class TraitStatus(
    val traitName: String,
    val currentCount: Int,
    val breakpoints: List<Int>,
    val isActive: Boolean = false,
    val activeBreakpoint: Int = 0,
    val nextBreakpoint: Int = 0
)

data class ItemBenchEntry(
    val itemName: String,
    val slotIndex: Int,
    val isComponent: Boolean = true,
    val confidence: Float = 1.0f
)

// ── 상대 플레이어 ─────────────────────────────────────────────────────────────

data class OpponentSnapshot(
    val playerId: Int,
    val roundStr: String,
    val board: List<UnitInstance> = emptyList(),
    val bench: List<UnitInstance> = emptyList(),
    val traitsActive: List<String> = emptyList(),
    val health: Int = -1,
    val level: Int = -1,
    var inferredComp: String = "Unknown",
    var compConfidence: Float = 0f,
    var isCommitted: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

data class OpponentState(
    val playerId: Int,
    var isEliminated: Boolean = false,
    val snapshots: MutableList<OpponentSnapshot> = mutableListOf()
) {
    fun addSnapshot(snap: OpponentSnapshot) {
        snapshots.add(snap)
        if (snapshots.size > 20) snapshots.removeAt(0)
    }
    val latest: OpponentSnapshot? get() = snapshots.lastOrNull()
    val allKnownUnits: Set<String> get() =
        snapshots.takeLast(5).flatMap { it.board + it.bench }.map { it.name }.toSet()
    fun isPivoting(): Boolean =
        snapshots.size >= 2 && snapshots.last().inferredComp != snapshots[snapshots.size - 2].inferredComp
}

// ── 플레이어 상태 ─────────────────────────────────────────────────────────────

data class PlayerState(
    val gold: Int = 0,
    val health: Int = 100,
    val level: Int = 1,
    val xp: Int = 0,
    val xpToNext: Int = 2,
    val roundStr: String = "1-1",
    val stageNum: Int = 1,
    val roundNum: Int = 1,
    val stageType: StageType = StageType.UNKNOWN,
    val streakType: StreakType = StreakType.NONE,
    val winStreak: Int = 0,
    val loseStreak: Int = 0,
    val board: List<UnitInstance> = emptyList(),
    val bench: List<UnitInstance> = emptyList(),
    val shop: List<ShopSlot> = emptyList(),
    val itemBench: List<ItemBenchEntry> = emptyList(),
    val traits: List<TraitStatus> = emptyList(),
    val augmentsTaken: List<String> = emptyList(),
    val augmentChoices: List<String> = emptyList(),
    val isAugmentPhase: Boolean = false,
    val godAlignment: String = "None",
    val godBlessingChoices: List<String> = emptyList(),
    val isRealmRound: Boolean = false,
    val animaTechStacks: Int = 0,
    val currentComp: String = "Unknown",
    val compConfidence: Float = 0f,
    val stateConfidence: ConfidenceLevel = ConfidenceLevel.LOW
) {
    val interest: Int get() = min(gold / 10, 5)
    val goldToNextInterest: Int get() {
        val cur = interest
        return if (cur >= 5) 0 else (cur + 1) * 10 - gold
    }
    val isLowHealth: Boolean get() = health <= 25
    val isCriticalHealth: Boolean get() = health <= 15
    val carries: List<UnitInstance> get() = board.filter { it.isCarry }
    val tanks: List<UnitInstance> get() = board.filter { it.isTank }
    val activeTraits: List<TraitStatus> get() = traits.filter { it.isActive }
    val components: List<String> get() = itemBench.filter { it.isComponent }.map { it.itemName }

    fun parseRound(): Pair<Int, Int> = try {
        val p = roundStr.split("-")
        Pair(p[0].toInt(), p[1].toInt())
    } catch (e: Exception) { Pair(1, 1) }

    fun validate(): List<String> {
        val issues = mutableListOf<String>()
        if (gold !in 0..99) issues += "비정상 gold: $gold"
        if (health !in 0..100) issues += "비정상 health: $health"
        if (level !in 1..10) issues += "비정상 level: $level"
        return issues
    }
}

// ── 전체 게임 상태 ─────────────────────────────────────────────────────────────

data class GameState(
    val player: PlayerState = PlayerState(),
    val opponents: MutableMap<Int, OpponentState> = mutableMapOf(),
    val patchVersion: String = "17.3"
) {
    fun activeOpponents(): List<OpponentState> =
        opponents.values.filter { !it.isEliminated }
    val isSafeToAutomate: Boolean
        get() = player.stateConfidence == ConfidenceLevel.HIGH
}
