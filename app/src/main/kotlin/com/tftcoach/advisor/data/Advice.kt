// data/Advice.kt
package com.tftcoach.advisor.data

data class Advice(
    val type: AdviceType,
    val priority: Priority,
    val message: String,
    val detail: String = "",
    val emoji: String = "",
    val confidence: Float = 0.8f,
    val source: String = "",
    val goldCost: Int = 0,
    val whenExpires: String = "",
    val alternatives: List<String> = emptyList()
) {
    val priorityColor: Long get() = when (priority) {
        Priority.CRITICAL -> 0xFFE53935L
        Priority.HIGH     -> 0xFFE53935L
        Priority.MEDIUM   -> 0xFFFFB300L
        Priority.LOW      -> 0xFF43A047L
        Priority.INFO     -> 0xFF4A90D9L
    }
    val priorityLabel: String get() = when (priority) {
        Priority.CRITICAL -> "즉시"
        Priority.HIGH     -> "높음"
        Priority.MEDIUM   -> "중간"
        Priority.LOW      -> "낮음"
        Priority.INFO     -> "정보"
    }
}

data class AdviceSet(
    val roundStr: String,
    val advices: List<Advice>,
    val stateConfidence: Float,
    val isSafeMode: Boolean = false,
    val safetyMessage: String = ""
) {
    fun top(n: Int = 5): List<Advice> =
        advices.sortedBy { it.priority.ordinal }.take(n)
    fun critical(): List<Advice> =
        advices.filter { it.priority == Priority.CRITICAL }
}
