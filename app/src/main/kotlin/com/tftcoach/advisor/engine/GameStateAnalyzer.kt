// engine/GameStateAnalyzer.kt
package com.tftcoach.advisor.engine

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tftcoach.advisor.data.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 화면 캡처 Bitmap → GameState 변환.
 * 멀티-스피드 인식:
 *   - FULL_SCAN: 라운드 전환 시 전체 영역
 *   - FAST_SCAN: 상점 슬롯만 (롤다운 중)
 *   - CACHED:    변화 없으면 이전 상태 재사용
 */
class GameStateAnalyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var cachedState = PlayerState()
    private var failCount = 0

    // 화면 영역 비율 (1920×1080 기준)
    private val ROUND_R    = RR(0.392f, 0.009f, 0.453f, 0.031f)
    private val GOLD_R     = RR(0.464f, 0.952f, 0.510f, 0.972f)
    private val LEVEL_R    = RR(0.130f, 0.935f, 0.170f, 0.955f)
    private val HEALTH_R   = RR(0.020f, 0.080f, 0.090f, 0.100f)
    private val SHOP_RS    = listOf(
        RR(0.253f,0.961f,0.315f,0.978f), RR(0.359f,0.961f,0.421f,0.978f),
        RR(0.464f,0.961f,0.525f,0.978f), RR(0.569f,0.961f,0.630f,0.978f),
        RR(0.673f,0.961f,0.734f,0.978f)
    )
    private val AUGMENT_RS = listOf(
        RR(0.280f,0.370f,0.450f,0.420f), RR(0.460f,0.370f,0.630f,0.420f),
        RR(0.640f,0.370f,0.810f,0.420f)
    )
    private val ROUND_RE   = Regex("""(\d)-(\d)""")

    suspend fun fullScan(bitmap: Bitmap): Pair<PlayerState, ConfidenceLevel> {
        return try {
            val w = bitmap.width.toFloat()
            val h = bitmap.height.toFloat()

            val roundText  = ocrRegion(bitmap, ROUND_R, w, h)
            val goldText   = ocrRegion(bitmap, GOLD_R, w, h)
            val levelText  = ocrRegion(bitmap, LEVEL_R, w, h)
            val shopTexts  = SHOP_RS.map { ocrRegion(bitmap, it, w, h) }
            val augTexts   = AUGMENT_RS.map { ocrRegion(bitmap, it, w, h) }

            val parsedRound = ROUND_RE.find(roundText)?.value ?: cachedState.roundStr
            val (stage, rnd) = parseRoundStr(parsedRound)
            val gold  = goldText.filter { it.isDigit() }.toIntOrNull() ?: cachedState.gold
            val level = levelText.filter { it.isDigit() }.toIntOrNull() ?: cachedState.level

            val shop = shopTexts.mapIndexed { i, t ->
                val name = matchChampion(t)
                ShopSlot(i, name, Set17Data.CHAMPIONS[name]?.cost ?: 0)
            }
            val isAugment = augTexts.count { it.length > 3 } >= 2
            val augChoices = if (isAugment) augTexts.filter { it.isNotBlank() } else emptyList()
            val stageType  = detectStage(stage, rnd, isAugment)

            val new = cachedState.copy(
                roundStr      = parsedRound,
                stageNum      = stage, roundNum = rnd,
                stageType     = stageType,
                gold          = gold,
                level         = level,
                shop          = shop,
                augmentChoices= augChoices,
                isAugmentPhase= isAugment,
                stateConfidence = ConfidenceLevel.HIGH
            )
            cachedState = new
            failCount = 0
            Pair(new, ConfidenceLevel.HIGH)

        } catch (e: Exception) {
            failCount++
            val conf = if (failCount >= 3) ConfidenceLevel.LOW else ConfidenceLevel.MEDIUM
            Pair(cachedState.copy(stateConfidence = conf), conf)
        }
    }

    /** 롤다운 중 상점 슬롯만 빠르게 스캔 */
    suspend fun fastShopScan(bitmap: Bitmap): List<ShopSlot> {
        return try {
            val w = bitmap.width.toFloat()
            val h = bitmap.height.toFloat()
            SHOP_RS.mapIndexed { i, r ->
                val text = ocrRegion(bitmap, r, w, h)
                val name = matchChampion(text)
                ShopSlot(i, name, Set17Data.CHAMPIONS[name]?.cost ?: 0,
                    confidence = if (name.isNotBlank()) 0.85f else 0.4f)
            }
        } catch (e: Exception) {
            List(5) { i -> ShopSlot(i, "", confidence = 0.0f) }
        }
    }

    /** 외부에서 체력/레벨 업데이트 (Riot Live Client API) */
    fun updateFromApi(health: Int, level: Int) {
        cachedState = cachedState.copy(
            health = if (health > 0) health else cachedState.health,
            level  = if (level > 0) level else cachedState.level
        )
    }

    fun updateStreak(winStreak: Int, loseStreak: Int) {
        val type = when {
            winStreak >= 3  -> StreakType.WIN
            loseStreak >= 3 -> StreakType.LOSE
            else            -> StreakType.NONE
        }
        cachedState = cachedState.copy(
            streakType = type, winStreak = winStreak, loseStreak = loseStreak
        )
    }

    fun updateAnimaTech(stacks: Int) {
        cachedState = cachedState.copy(animaTechStacks = stacks)
    }

    val consecutiveFailures get() = failCount

    // ── OCR 헬퍼 ─────────────────────────────────────────────────────────────

    private suspend fun ocrRegion(bmp: Bitmap, r: RR, w: Float, h: Float): String {
        val rect = Rect(
            (r.l*w).toInt().coerceIn(0, bmp.width),
            (r.t*h).toInt().coerceIn(0, bmp.height),
            (r.r*w).toInt().coerceIn(0, bmp.width),
            (r.b*h).toInt().coerceIn(0, bmp.height)
        )
        if (rect.width() <= 0 || rect.height() <= 0) return ""
        val crop = Bitmap.createBitmap(bmp, rect.left, rect.top, rect.width(), rect.height())
        return runOcr(crop)
    }

    private suspend fun runOcr(bmp: Bitmap): String = suspendCancellableCoroutine { cont ->
        val img = InputImage.fromBitmap(bmp, 0)
        recognizer.process(img)
            .addOnSuccessListener { cont.resume(it.text.trim()) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    private fun matchChampion(raw: String): String {
        val n = raw.trim().lowercase()
        if (n.isBlank()) return ""
        Set17Data.CHAMPIONS.keys.firstOrNull { it.lowercase() == n }?.let { return it }
        Set17Data.CHAMPIONS.keys.firstOrNull {
            it.lowercase().contains(n) || n.contains(it.lowercase())
        }?.let { return it }
        return Set17Data.CHAMPIONS.keys
            .minByOrNull { levenshtein(it.lowercase(), n) } ?: ""
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length+1) { IntArray(b.length+1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            dp[i][j] = if (a[i-1]==b[j-1]) dp[i-1][j-1]
            else minOf(dp[i-1][j], dp[i][j-1], dp[i-1][j-1]) + 1
        }
        return dp[a.length][b.length]
    }

    private fun parseRoundStr(r: String): Pair<Int,Int> = try {
        val p = r.split("-"); Pair(p[0].toInt(), p[1].toInt())
    } catch (e: Exception) { Pair(1, 1) }

    private fun detectStage(stage: Int, rnd: Int, isAugment: Boolean): StageType = when {
        isAugment    -> StageType.AUGMENT
        rnd == 4     -> StageType.REALM   // Set 17: X-4가 Realm 라운드
        stage == 1   -> StageType.PVE
        rnd in listOf(3, 7) -> StageType.PVE
        else         -> StageType.PVP
    }
}

/** 화면 비율 영역 */
data class RR(val l: Float, val t: Float, val r: Float, val b: Float)
